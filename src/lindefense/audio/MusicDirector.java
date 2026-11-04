package lindefense.audio;

/**
 * Decides which music track should play from the game situation. Pure logic (no audio, no I/O) so it is unit
 * testable. Returns an ordered preference list; the manager plays the first track whose file exists.
 *
 * <p>Rules: victory and defeat share the end-screen track; Palantir (wave 100 or Palantir alive) overrides everything; a MOAB-class
 * boss on the field uses the boss theme; otherwise the era battle track (O(N^N) override, waves 60+ late, else
 * normal). The boss theme is held a few seconds after the last boss dies to avoid flip-flopping.</p>
 */
public final class MusicDirector {
    /** Seconds the boss/Palantir theme lingers after the last boss disappears. */
    public static final double BOSS_HOLD_SECONDS = 6.0;
    /** First wave that uses the late battle track. */
    public static final int LATE_WAVE = 60;
    /** The final wave. */
    public static final int FINAL_WAVE = 100;

    private Track[] held;
    private double heldUntil = -1;

    /**
     * @param victory  the game is won
     * @param defeat   the game is lost
     * @param wave     current wave number (0 before the first wave)
     * @param onn      O(N^N) difficulty
     * @param palantir Palantir is alive
     * @param boss     any other boss is alive
     * @param now      monotonically increasing seconds
     * @return preference-ordered tracks
     */
    public Track[] choose(boolean victory, boolean defeat, int wave, boolean onn, boolean palantir, boolean boss, double now) {
        if (victory || defeat) return new Track[] { Track.END };
        Track[] battle = battleChain(wave, onn);
        Track[] want = null;
        if (palantir || wave >= FINAL_WAVE) want = concat(new Track[] { Track.PALANTIR, Track.BOSS }, battle);
        else if (boss) want = concat(new Track[] { Track.BOSS }, battle);
        if (want != null) {
            held = want; heldUntil = now + BOSS_HOLD_SECONDS;
            return want;
        }
        if (held != null && now < heldUntil) return held;
        held = null;
        return battle;
    }

    /** The era battle track preference chain. */
    public static Track[] battleChain(int wave, boolean onn) {
        if (onn) return new Track[] { Track.BATTLE_ONN, Track.BATTLE_LATE, Track.BATTLE };
        if (wave >= LATE_WAVE) return new Track[] { Track.BATTLE_LATE, Track.BATTLE };
        return new Track[] { Track.BATTLE };
    }

    private static Track[] concat(Track[] a, Track[] b) {
        Track[] r = new Track[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
