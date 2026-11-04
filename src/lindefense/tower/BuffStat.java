package lindefense.tower;

/**
 * Tower stats that other towers can buff (spec 3.0 "Buff stacking"). A tower's total for a stat is the SUM over
 * source tower TYPES of the highest value that type contributes (same type never stacks), then capped.
 */
public enum BuffStat {
    /** Attack speed fraction; cap +1.0 (+100%). */
    RATE(1.0),
    /** Flat damage added to each attack; cap +5. */
    DAMAGE_FLAT(5),
    /** Flat pierce; cap +10 (shared with {@link #PIERCE_PCT}). */
    PIERCE_FLAT(10),
    /** Pierce as a fraction of the tower's base pierce (Brad Inspiration Wave +50%); counts toward the +10 cap. */
    PIERCE_PCT(Double.MAX_VALUE),
    /** Range fraction (no cap in the spec). */
    RANGE(Double.MAX_VALUE),
    /** Extra damage fraction vs MOAB-class (Grok Real-Time Override +100%). */
    BOSS_DAMAGE_PCT(Double.MAX_VALUE);

    /** Maximum total from buffs. */
    public final double cap;
    BuffStat(double cap) { this.cap = cap; }
}
