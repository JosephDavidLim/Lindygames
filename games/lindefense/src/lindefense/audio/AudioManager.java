package lindefense.audio;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;
import java.util.prefs.Preferences;
import lindefense.combat.VisualEvent;

/**
 * Facade for all game audio: sound effects (preloaded, 12-voice pool, per-sound throttling, slight random
 * pitch/volume variation), streamed music with crossfades, and the persisted Sound / Music switches.
 *
 * <p>Everything is best effort. With no audio device, a headless run, missing or unsupported files, the manager
 * logs one line (device failures only) and stays silent; nothing here ever throws into the game. Game code
 * calls the static {@link #sfx(Sfx)} / {@link #event(VisualEvent)} hooks, which are no-ops until
 * {@link #install()} has been called (so headless simulations and tests pay nothing).</p>
 */
public final class AudioManager {
    /** Seconds for music crossfades between contexts. */
    public static final float CROSSFADE = 1.5f;

    private static volatile AudioManager live;

    private final AudioSettings settings;
    private final File sfxDir, musicDir;
    private final boolean openDevice;
    private final Throttle throttle = new Throttle();
    private final SfxBank bank = new SfxBank();
    private final Mixer mixer = new Mixer();
    private final int[] accepted = new int[Sfx.values().length];
    private final Set<Track> available = EnumSet.noneOf(Track.class);
    private final Set<Track> reportedBad = EnumSet.noneOf(Track.class);
    private volatile boolean ready;
    private volatile boolean disabled;
    private boolean loggedFailure;
    private Track desired;
    private Track playing;
    private long rng = 0x9E3779B97F4A7C15L;

    /**
     * @param prefs      preference node for the switches (null = in-memory only)
     * @param assetsRoot directory containing {@code sfx/} and {@code music/}
     * @param openDevice false to never touch the audio device (tests, headless)
     */
    public AudioManager(Preferences prefs, File assetsRoot, boolean openDevice) {
        this.settings = new AudioSettings(prefs);
        this.sfxDir = new File(assetsRoot, "sfx");
        this.musicDir = new File(assetsRoot, "music");
        this.openDevice = openDevice;
        scanMusic();
    }

    // ------------------------------------------------------------------ global access
    /** Creates the game-wide manager (assets located by {@link lindefense.Assets}), starts loading in the background. */
    public static synchronized AudioManager install() {
        if (live == null) {
            File root = lindefense.Assets.file("assets");   // works from any working directory (see lindefense.Assets)
            boolean headless = java.awt.GraphicsEnvironment.isHeadless() || Boolean.getBoolean("lindefense.nosound");
            AudioManager m = new AudioManager(Preferences.userRoot().node("lindefense/audio"), root, !headless);
            m.init();
            live = m;
        }
        return live;
    }

    /** The installed manager, or null before {@link #install()}. */
    public static AudioManager get() { return live; }

    /** AUDIO HOOK entry: plays an effect if audio is installed. */
    public static void sfx(Sfx s) {
        AudioManager m = live;
        if (m != null) m.play(s);
    }

    /** AUDIO HOOK entry: title / how-to-play / map select music. */
    public static void menuMusic() {
        AudioManager m = live;
        if (m != null) m.setMusic(new Track[] { Track.MENU });
    }

    /** AUDIO HOOK entry: plays the effect that matches a simulation event. */
    public static void event(VisualEvent ev) {
        AudioManager m = live;
        if (m == null || !m.ready) return;
        Sfx s = SfxFamilies.forEvent(ev);
        if (s != null) m.play(s);
    }

    // ------------------------------------------------------------------ lifecycle
    /** Loads effects and opens the output on a background thread. Safe to call once; never throws. */
    public void init() {
        if (!openDevice) return;
        Thread t = new Thread(() -> {
            try {
                bank.load(sfxDir);
                if (!mixer.open()) { fail("no audio output device available"); return; }
                applySettings();
                ready = true;
                synchronized (this) { applyMusic(CROSSFADE); }
            } catch (Throwable e) {
                fail("audio init failed: " + e);
            }
        }, "lindefense-audio-init");
        t.setDaemon(true);
        t.start();
    }

    private synchronized void fail(String why) {
        disabled = true; ready = false;
        if (!loggedFailure) { loggedFailure = true; System.err.println("[audio] disabled: " + why); }
    }

    private void applySettings() { mixer.setVolumes(0.9f, 0.6f); }

    // ------------------------------------------------------------------ switches
    public boolean isSoundEnabled() { return settings.isSound(); }
    public boolean isMusicEnabled() { return settings.isMusic(); }
    /** True if an output device is open and working. */
    public boolean isReady() { return ready; }
    public boolean isDisabled() { return disabled; }

    public void setSoundEnabled(boolean on) {
        settings.setSound(on);
        if (!on && ready) mixer.clearVoices();
    }

    public synchronized void setMusicEnabled(boolean on) {
        settings.setMusic(on);
        applyMusic(CROSSFADE);
    }

    public void toggleSound() { setSoundEnabled(!settings.isSound()); if (settings.isSound()) play(Sfx.UI_CLICK); }
    public void toggleMusic() { setMusicEnabled(!settings.isMusic()); }

    // ------------------------------------------------------------------ effects
    /** Plays an effect. Returns true if it was accepted (sound on and not throttled). */
    public boolean play(Sfx s) {
        if (!settings.isSound() || disabled) return false;
        if (!throttle.allow(s, System.nanoTime())) return false;
        accepted[s.ordinal()]++;
        if (ready) {
            float[] d = bank.get(s);
            if (d != null) {
                float gain = s.gain, pitch = 1f;
                if (s.vary) {
                    rng ^= rng << 13; rng ^= rng >>> 7; rng ^= rng << 17;
                    float a = ((rng >>> 40) & 0xffff) / 65535f, b = ((rng >>> 20) & 0xffff) / 65535f;
                    pitch = 0.94f + 0.12f * a;
                    gain *= 0.85f + 0.15f * b;
                }
                mixer.play(d, gain, pitch, s.priority);
            }
        }
        return true;
    }

    /** Number of plays of {@code s} that passed the throttle (for tests / diagnostics). */
    public int getAcceptedCount(Sfx s) { return accepted[s.ordinal()]; }

    /** Number of effect files that loaded. */
    public int getLoadedSfxCount() { return bank.loadedCount(); }

    // ------------------------------------------------------------------ music
    private void scanMusic() {
        for (Track t : Track.values()) if (new File(musicDir, t.file + ".wav").isFile()) available.add(t);
    }

    /** True if the track file was found at start-up. */
    public boolean hasTrack(Track t) { return available.contains(t); }

    /** The track currently requested (after fallbacks), or null for silence. */
    public synchronized Track getDesiredTrack() { return desired; }

    /**
     * Requests the first available track of {@code prefs} (null/empty or none available = silence).
     * Does nothing if that track is already the current one, so tracks are not restarted between waves.
     */
    public synchronized void setMusic(Track[] prefs) {
        Track pick = null;
        if (prefs != null) for (Track t : prefs) if (available.contains(t) && !reportedBad.contains(t)) { pick = t; break; }
        if (pick == desired) return;
        desired = pick;
        applyMusic(CROSSFADE);
    }

    private void applyMusic(float fade) {
        if (!ready) return;
        Track want = settings.isMusic() ? desired : null;
        if (want == playing) return;
        playing = want;
        if (want == null) { mixer.stopMusic(fade); return; }
        final Track t = want;
        mixer.playMusic(new MusicStream(new File(musicDir, t.file + ".wav"), t.loop, () -> badTrack(t)), fade);
    }

    private synchronized void badTrack(Track t) {
        if (reportedBad.add(t)) System.err.println("[audio] cannot play music/" + t.file + ".wav (unsupported or unreadable); skipping");
    }
}
