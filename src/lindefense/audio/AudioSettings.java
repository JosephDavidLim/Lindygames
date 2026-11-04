package lindefense.audio;

import java.util.prefs.Preferences;

/**
 * The two independent audio switches (Sound = sound effects, Music), persisted with
 * {@link Preferences}. Both default to on. Persistence failures are ignored (the switches still work in memory).
 */
public final class AudioSettings {
    static final String KEY_SOUND = "sound";
    static final String KEY_MUSIC = "music";

    private final Preferences node;
    private volatile boolean sound;
    private volatile boolean music;

    public AudioSettings(Preferences node) {
        this.node = node;
        boolean s = true, m = true;
        try {
            if (node != null) { s = node.getBoolean(KEY_SOUND, true); m = node.getBoolean(KEY_MUSIC, true); }
        } catch (RuntimeException ignored) {
            // unreadable preference store: use defaults
        }
        sound = s; music = m;
    }

    public boolean isSound() { return sound; }
    public boolean isMusic() { return music; }

    public void setSound(boolean on) { sound = on; save(KEY_SOUND, on); }
    public void setMusic(boolean on) { music = on; save(KEY_MUSIC, on); }

    private void save(String key, boolean v) {
        if (node == null) return;
        try {
            node.putBoolean(key, v);
            node.flush();
        } catch (Exception ignored) {
            // read-only store; keep the in-memory value
        }
    }
}
