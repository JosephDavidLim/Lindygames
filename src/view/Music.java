package view;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Plays the looping soundtrack from {@code res/music}. Asking for a different
 * track fades the old one out and the new one in. If there is no sound device
 * or a file is missing, it quietly does nothing.
 */
public final class Music {
    private static final float LEVEL_DB = -7f;
    private static final float SILENT_DB = -50f;
    private static final int FADE_MS = 700;
    private static final int STEP_MS = 35;

    private static final Map<String, byte[]> DATA = new HashMap<>();
    private static final Map<String, AudioFormat> FORMATS = new HashMap<>();
    private static Clip current;
    private static String wanted;
    private static boolean muted;
    private static boolean broken;

    private Music() {
    }

    /**
     * Starts a track (without its .wav) and loops it. Does nothing if that
     * track is already the one playing.
     *
     * @param name for example "title" or "world1"
     */
    public static synchronized void play(String name) {
        if (name.equals(wanted)) {
            return;
        }
        wanted = name;
        switchTo(muted ? null : name);
    }

    /**
     * Fades the music out. The next call to {@link #play} starts a track again.
     */
    public static synchronized void stop() {
        wanted = null;
        switchTo(null);
    }

    /**
     * Mutes or unmutes the music. Unmuting resumes the last requested track.
     *
     * @param on true to mute
     */
    public static synchronized void setMuted(boolean on) {
        if (muted == on) {
            return;
        }
        muted = on;
        switchTo(on ? null : wanted);
    }

    /**
     * Tells whether the music is muted.
     *
     * @return true if muted
     */
    public static synchronized boolean isMuted() {
        return muted;
    }

    /**
     * Loads a track into memory on a background thread so starting it later
     * does not cause a hiccup in the game loop.
     *
     * @param name the track to load
     */
    public static void preload(String name) {
        Thread t = new Thread(() -> load(name), "music-preload");
        t.setDaemon(true);
        t.start();
    }

    private static void switchTo(String name) {
        if (broken) {
            return;
        }
        Clip old = current;
        current = null;
        Clip next = null;
        if (name != null && load(name)) {
            try {
                next = AudioSystem.getClip();
                byte[] data = DATA.get(name);
                next.open(FORMATS.get(name), data, 0, data.length);
                setGain(next, SILENT_DB);
                next.loop(Clip.LOOP_CONTINUOUSLY);
                current = next;
            } catch (Exception e) {
                broken = true;
                next = null;
            }
        }
        fade(old, next);
    }

    private static synchronized boolean load(String name) {
        if (DATA.containsKey(name)) {
            return DATA.get(name) != null;
        }
        File f = new File("res/music/" + name + ".wav");
        if (!f.exists()) {
            DATA.put(name, null);
            return false;
        }
        try (AudioInputStream in = AudioSystem.getAudioInputStream(f)) {
            DATA.put(name, in.readAllBytes());
            FORMATS.put(name, in.getFormat());
            return true;
        } catch (UnsupportedAudioFileException | IOException e) {
            DATA.put(name, null);
            return false;
        }
    }

    private static void fade(Clip out, Clip in) {
        if (out == null && in == null) {
            return;
        }
        Thread t = new Thread(() -> {
            int steps = FADE_MS / STEP_MS;
            for (int i = 1; i <= steps; i++) {
                float f = (float) i / steps;
                if (out != null) {
                    setGain(out, LEVEL_DB + (SILENT_DB - LEVEL_DB) * f);
                }
                if (in != null) {
                    setGain(in, SILENT_DB + (LEVEL_DB - SILENT_DB) * f);
                }
                try {
                    Thread.sleep(STEP_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (out != null) {
                out.stop();
                out.close();
            }
        }, "music-fade");
        t.setDaemon(true);
        t.start();
    }

    private static void setGain(Clip clip, float db) {
        try {
            FloatControl c = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            c.setValue(Math.max(c.getMinimum(), Math.min(c.getMaximum(), db)));
        } catch (IllegalArgumentException e) {
            // This device has no volume control; the track simply plays at full level.
        }
    }
}
