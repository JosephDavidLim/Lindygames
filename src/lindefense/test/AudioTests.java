package lindefense.test;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import lindefense.audio.AudioManager;
import lindefense.audio.AudioSettings;
import lindefense.audio.MusicDirector;
import lindefense.audio.PcmDecoder;
import lindefense.audio.Sfx;
import lindefense.audio.Throttle;
import lindefense.audio.Track;

/**
 * Headless tests for the audio system. None of them opens an audio device, so they pass on machines without one.
 * Run from {@link HeadlessTests}; returns the list of failure messages.
 */
public final class AudioTests {
    private static int checks;
    private static final List<String> failures = new ArrayList<>();

    private AudioTests() { }

    public static int checkCount() { return checks; }

    private static void check(boolean ok, String msg) {
        checks++;
        if (!ok) failures.add(msg);
    }

    public static List<String> run() {
        failures.clear();
        checks = 0;
        settingsRoundTrip();
        throttleCaps();
        noDeviceIsGraceful();
        missingMusic();
        musicDirectorRules();
        decoderHandlesExtensibleMulaw();
        shippedMusicDecodes();
        return failures;
    }

    private static Preferences testNode() {
        return Preferences.userRoot().node("lindefense-test/audio-" + System.nanoTime());
    }

    private static void settingsRoundTrip() {
        Preferences node = testNode();
        try {
            AudioSettings a = new AudioSettings(node);
            check(a.isSound() && a.isMusic(), "both switches default to on");
            a.setSound(false);
            check(!a.isSound() && a.isMusic(), "switches are independent (sound off, music on)");
            a.setMusic(false);
            a.setSound(true);
            AudioSettings b = new AudioSettings(node);
            check(b.isSound() && !b.isMusic(), "settings persist across instances");
            AudioManager m = new AudioManager(node, new File("no-such-dir"), false);
            check(m.isSoundEnabled() && !m.isMusicEnabled(), "manager reads persisted settings");
            m.toggleMusic();
            m.toggleSound();
            AudioSettings c = new AudioSettings(node);
            check(c.isMusic() && !c.isSound(), "manager toggles persist");
            check(!m.play(Sfx.UI_CLICK), "no effects accepted while sound is off");
        } finally {
            try { node.removeNode(); node.flush(); } catch (Exception ignored) { /* best effort cleanup */ }
        }
    }

    private static void throttleCaps() {
        Throttle t = new Throttle();
        int pops = 0, lasers = 0;
        long step = 1_000_000L;   // attempt every millisecond for one second
        for (int i = 0; i < 1000; i++) {
            long now = i * step;
            if (t.allow(Sfx.ENEMY_POP, now)) pops++;
            if (t.allow(Sfx.ATK_LASER, now)) lasers++;
        }
        check(pops <= 21 && pops >= 15, "enemy pops capped near 20/s (got " + pops + ")");
        check(lasers <= 16 && lasers >= 12, "attack family capped near 15/s (got " + lasers + ")");
        check(t.allow(Sfx.UI_CLICK, 0) && !t.allow(Sfx.UI_CLICK, 1_000_000L), "same sound within its gap is rejected");
        check(t.allow(Sfx.UI_CLICK, 40_000_000L), "same sound after its gap is accepted");

        AudioManager m = new AudioManager(testNode(), new File("no-such-dir"), false);
        int accepted = 0;
        for (int i = 0; i < 5000; i++) if (m.play(Sfx.ENEMY_POP)) accepted++;
        check(accepted <= 2, "burst of 5000 pops in a tight loop accepts at most 2 (got " + accepted + ")");
        check(m.getAcceptedCount(Sfx.ENEMY_POP) == accepted, "accepted counter matches");
    }

    private static void noDeviceIsGraceful() {
        AudioManager m = new AudioManager(testNode(), new File("no-such-dir"), false);
        m.init();
        check(!m.isReady(), "manager without a device is never ready");
        boolean threw = false;
        try {
            m.play(Sfx.WAVE_START);
            m.setMusic(new Track[] { Track.BATTLE });
            m.setMusicEnabled(false);
            m.setSoundEnabled(false);
            m.setMusicEnabled(true);
            AudioManager.sfx(Sfx.UI_CLICK);   // static hooks with nothing installed
            AudioManager.menuMusic();
        } catch (Throwable e) {
            threw = true;
        }
        check(!threw, "all calls are safe without a device");
    }

    private static void missingMusic() {
        File dir = null;
        try {
            dir = Files.createTempDirectory("lindefense-audio-test").toFile();
            AudioManager none = new AudioManager(testNode(), dir, false);
            for (Track t : Track.values()) check(!none.hasTrack(t), "no track reported when music/ is missing: " + t);
            none.setMusic(new Track[] { Track.BOSS, Track.BATTLE });
            check(none.getDesiredTrack() == null, "missing music resolves to silence");

            File music = new File(dir, "music");
            check(music.mkdirs(), "temp music dir created");
            Files.write(new File(music, "battle.wav").toPath(), new byte[] { 1, 2, 3 });   // present but not real audio
            AudioManager some = new AudioManager(testNode(), dir, false);
            check(some.hasTrack(Track.BATTLE) && !some.hasTrack(Track.BOSS), "only existing tracks are available");
            some.setMusic(new Track[] { Track.BOSS, Track.BATTLE_LATE, Track.BATTLE });
            check(some.getDesiredTrack() == Track.BATTLE, "falls back along the preference list to an existing track");
            some.setMusic(new Track[] { Track.PALANTIR });
            check(some.getDesiredTrack() == null, "a missing track with no fallback plays nothing");
        } catch (IOException e) {
            check(false, "temp dir: " + e);
        } finally {
            if (dir != null) {
                File[] fs = new File(dir, "music").listFiles();
                if (fs != null) for (File f : fs) f.delete();
                new File(dir, "music").delete();
                dir.delete();
            }
        }
    }

    private static void musicDirectorRules() {
        MusicDirector d = new MusicDirector();
        check(d.choose(false, false, 5, false, false, false, 0)[0] == Track.BATTLE, "early waves use battle");
        Track[] late = d.choose(false, false, 60, false, false, false, 1);
        check(late[0] == Track.BATTLE_LATE && late[late.length - 1] == Track.BATTLE, "wave 60+ uses battle_late then battle");
        Track[] onn = d.choose(false, false, 10, true, false, false, 2);
        check(onn[0] == Track.BATTLE_ONN && onn[1] == Track.BATTLE_LATE && onn[2] == Track.BATTLE, "O(N^N) overrides battle tracks");
        Track[] boss = d.choose(false, false, 40, false, false, true, 3);
        check(boss[0] == Track.BOSS && boss[1] == Track.BATTLE, "boss theme falls back to the current battle track");
        check(d.choose(false, false, 40, false, false, false, 4)[0] == Track.BOSS, "boss theme is held right after the boss dies");
        check(d.choose(false, false, 40, false, false, false, 3 + MusicDirector.BOSS_HOLD_SECONDS + 0.1)[0] == Track.BATTLE, "returns to battle after the hold");
        Track[] pal = d.choose(false, false, 100, false, true, true, 20);
        check(pal[0] == Track.PALANTIR && pal[1] == Track.BOSS, "Palantir overrides everything, falling back to boss");
        check(new MusicDirector().choose(false, false, 100, false, false, false, 0)[0] == Track.PALANTIR, "wave 100 uses the Palantir theme");
        check(d.choose(true, false, 100, false, true, false, 30)[0] == Track.END, "victory uses the end-screen track");
        check(d.choose(false, true, 33, false, false, false, 31)[0] == Track.END, "defeat uses the end-screen track");
        check(Track.END.loop && Track.BATTLE.loop && Track.MENU.loop, "all tracks loop");
    }

    private static void le(ByteArrayOutputStream o, long v, int bytes) { for (int i = 0; i < bytes; i++) o.write((int) (v >> (8 * i)) & 0xff); }

    /** Builds a stereo mu-law WAV with the WAVE_FORMAT_EXTENSIBLE header (fmt size 40, subformat MULAW) that afconvert writes. */
    private static byte[] extensibleUlaw(byte[] data) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write('R'); o.write('I'); o.write('F'); o.write('F');
        le(o, 4 + 8 + 40 + 8 + 4 + 8 + data.length, 4);
        for (char c : "WAVE".toCharArray()) o.write(c);
        for (char c : "fmt ".toCharArray()) o.write(c);
        le(o, 40, 4); le(o, 0xFFFE, 2); le(o, 2, 2); le(o, 22050, 4); le(o, 22050 * 2, 4); le(o, 2, 2); le(o, 8, 2);
        le(o, 22, 2); le(o, 8, 2); le(o, 3, 4);                       // cbSize, valid bits, channel mask
        le(o, 7, 2); le(o, 0, 2); le(o, 0, 2); le(o, 0x0010, 2); le(o, 0xAA000080L, 4); le(o, 0x719B3800L, 4);   // KSDATAFORMAT_SUBTYPE_MULAW GUID
        for (char c : "FLLR".toCharArray()) o.write(c);               // afconvert pads with a filler chunk
        le(o, 4, 4); le(o, 0, 4);
        for (char c : "data".toCharArray()) o.write(c);
        le(o, data.length, 4);
        o.write(data, 0, data.length);
        return o.toByteArray();
    }

    private static void decoderHandlesExtensibleMulaw() {
        byte[] data = { (byte) 0xFF, (byte) 0x80, 0x00, 0x7F, (byte) 0xC0, 0x40 };   // 3 stereo frames
        try (AudioInputStream in = PcmDecoder.decodeWav(new ByteArrayInputStream(extensibleUlaw(data)))) {
            check(in.getFormat().getChannels() == 2 && in.getFormat().getSampleSizeInBits() == 16, "extensible mu-law decodes to 16-bit stereo");
            check(in.getFormat().getSampleRate() == 22050f, "sample rate preserved");
            check(in.getFrameLength() == 3, "frame length (got " + in.getFrameLength() + ")");
            byte[] pcm = in.readAllBytes();
            check(pcm.length == 12, "decoded byte count (got " + pcm.length + ")");
            if (pcm.length == 12) {
                int[] s = new int[6];
                for (int i = 0; i < 6; i++) s[i] = (short) ((pcm[i * 2] & 0xff) | (pcm[i * 2 + 1] << 8));
                check(s[0] == 0, "mu-law 0xFF is silence");
                check(s[1] == 32124, "mu-law 0x80 is full positive scale (got " + s[1] + ")");
                check(s[2] == -32124, "mu-law 0x00 is full negative scale (got " + s[2] + ")");
                check(s[3] == 0 && s[4] > 0 && s[5] < 0, "mu-law sign and zero handling");
            }
        } catch (Exception e) {
            check(false, "extensible mu-law WAV failed to decode: " + e);
        }
        try {
            PcmDecoder.decodeWav(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
            check(false, "garbage must be rejected");
        } catch (Exception expected) {
            check(true, "garbage is rejected with an exception");
        }
    }

    /** Every music file that ships in assets/music must decode end to end (skipped when the folder is empty). */
    private static void shippedMusicDecodes() {
        File dir = lindefense.Assets.file("assets/music");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".wav"));
        if (files == null) return;
        for (File f : files) {
            try (AudioInputStream in = PcmDecoder.open(f)) {
                byte[] buf = new byte[65536];
                long bytes = 0; double sum = 0; int n;
                while ((n = in.read(buf)) > 0) {
                    bytes += n;
                    for (int i = 0; i + 1 < n; i += 64) sum += Math.abs((short) ((buf[i] & 0xff) | (buf[i + 1] << 8)));
                }
                double seconds = bytes / (double) (in.getFormat().getFrameSize() * in.getFormat().getFrameRate());
                check(seconds > 5 && seconds < 400, f.getName() + " duration is sensible (" + (int) seconds + " s)");
                check(sum > 0, f.getName() + " is not silent");
            } catch (Exception e) {
                check(false, f.getName() + " failed to decode: " + e);
            }
        }
    }
}
