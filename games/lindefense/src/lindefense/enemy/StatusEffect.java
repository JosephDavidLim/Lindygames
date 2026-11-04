package lindefense.enemy;

/**
 * A timed status applied to an enemy. See {@link Enemy#applyStatus} for the stacking rules of each kind:
 * SLOW / MARKED / MARKED_FLAT keep every application separately and the strongest active one counts;
 * DOT refreshes; the rest keep one instance with the longest duration.
 */
public final class StatusEffect {
    public enum Kind {
        /** magnitude = fraction of speed removed (0.3 = 30% slower). Strongest wins, never stacks; bosses get 1/3. */
        SLOW,
        /** magnitude unused; enemy cannot move while active. Bosses immune unless the caller overrides. */
        STUN,
        /** Like STUN, and the enemy additionally takes x2 damage from all sources (Claude Time-Lock). */
        FREEZE,
        /** Burn: magnitude = damage per second (TRUE damage). Refreshes, never stacks. */
        DOT,
        /** ARMOR CRACK: Lead immunity removed and the Fortified HP bonus ignored (x2 damage vs Fortified). */
        ARMOR_CRACK,
        /** magnitude = extra damage taken fraction (0.25 = +25%). Strongest percent mark wins. */
        MARKED,
        /** magnitude = flat extra damage taken per hit (+1). Strongest flat mark wins. */
        MARKED_FLAT,
        /** special mechanic disabled while active. */
        DISRUPTED,
        /** camo temporarily ignored (enemy is targetable without detection). */
        REVEALED
    }

    public final Kind kind;
    public double magnitude;
    public double remaining;

    public StatusEffect(Kind kind, double magnitude, double remaining) {
        this.kind = kind; this.magnitude = magnitude; this.remaining = remaining;
    }
}
