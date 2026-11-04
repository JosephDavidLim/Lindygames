package view;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Plays short sound effects from {@code res/sfx}. If there is no sound device
 * or a file is missing, it quietly does nothing so the game never crashes
 * over audio.
 */
public final class Sfx {
    private static final int MAX_AT_ONCE = 12;

    private static final Map<String, byte[]> DATA = new HashMap<>();
    private static final Map<String, AudioFormat> FORMATS = new HashMap<>();
    private static boolean muted;
    private static boolean broken;
    private static int playing;

    private Sfx() {
    }

    /**
     * Plays a sound by file name (without the .wav).
     *
     * @param name for example "place" or "boom"
     */
    public static void play(String name) {
        if (muted || broken || playing >= MAX_AT_ONCE) {
            return;
        }
        try {
            if (!DATA.containsKey(name) && !load(name)) {
                return;
            }
            byte[] data = DATA.get(name);
            Clip clip = AudioSystem.getClip();
            clip.open(FORMATS.get(name), data, 0, data.length);
            playing++;
            clip.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    playing--;
                }
            });
            clip.start();
        } catch (Exception e) {
            broken = true;
        }
    }

    private static boolean load(String name) {
        File f = new File("res/sfx/" + name + ".wav");
        if (!f.exists()) {
            DATA.put(name, null);
            return false;
        }
        try (AudioInputStream in = AudioSystem.getAudioInputStream(f)) {
            DATA.put(name, in.readAllBytes());
            FORMATS.put(name, in.getFormat());
            return true;
        } catch (UnsupportedAudioFileException | IOException e) {
            broken = true;
            return false;
        }
    }

    /**
     * Turns all sound on or off.
     *
     * @param on true to mute
     */
    public static void setMuted(boolean on) {
        muted = on;
    }

    /**
     * Tells whether sound is muted.
     *
     * @return true if muted
     */
    public static boolean isMuted() {
        return muted;
    }
}
