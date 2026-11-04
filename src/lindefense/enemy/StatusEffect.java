package lindefense.enemy;

/** A timed status applied to an enemy. One instance per kind is kept (strongest wins). */
public final class StatusEffect {
    public enum Kind {
        /** magnitude = fraction of speed removed (0.3 = 30% slower). */
        SLOW,
        /** magnitude unused; enemy cannot move while active. */
        STUN,
        /** magnitude = damage per second (TRUE damage, bypasses armour). */
        DOT,
        /** magnitude = armour fraction removed. */
        ARMOR_BREAK,
        /** magnitude = extra damage taken fraction (0.25 = +25%). */
        MARKED,
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
