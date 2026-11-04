package lindefense.tower;

/** How a tower delivers its hits. */
public enum AttackKind {
    /** Spawns travelling projectiles that collide with enemies. */
    PROJECTILE,
    /** Instant hit on the target (beam). With {@code pierce > 1} the beam continues through enemies on its line. */
    INSTANT,
    /** Instant hit that chains to additional enemies within chain range. */
    CHAIN,
    /** Periodic pulse hitting every enemy within range (aura attack). */
    PULSE,
    /** Pure support tower; never attacks. */
    NONE
}
