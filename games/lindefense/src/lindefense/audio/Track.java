package lindefense.audio;

/** The (all optional) music tracks, looked up as {@code assets/music/<file>.wav}. */
public enum Track {
    /** Title screen, How-to-Play and map / difficulty select. */
    MENU("menu", true),
    /** Normal waves 1-59. */
    BATTLE("battle", true),
    /** Normal waves 60-99. Falls back to BATTLE. */
    BATTLE_LATE("battle_late", true),
    /** Any normal wave on O(N^N) difficulty. Falls back to BATTLE_LATE then BATTLE. */
    BATTLE_ONN("battle_onn", true),
    /** A MOAB-class boss is on the field. Falls back to the current battle track. */
    BOSS("boss", true),
    /** Wave 100 / Palantir on the field. Falls back to BOSS. */
    PALANTIR("palantir", true),
    /** End screens (victory and defeat): {@code incoming_data.wav}, looping. */
    END("incoming_data", true);

    /** File name without extension. */
    public final String file;
    /** Whether the track loops seamlessly. */
    public final boolean loop;

    Track(String file, boolean loop) { this.file = file; this.loop = loop; }
}
