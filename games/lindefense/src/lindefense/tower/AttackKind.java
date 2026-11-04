package lindefense.tower;

/** How a tower delivers its base attack. */
public enum AttackKind {
    /** Spawns travelling (homing) projectiles that collide with enemies; supports pierce, splash, bounce, clones. */
    PROJECTILE,
    /**
     * Instant hit on the target (beam). With {@code pierce > 1} the beam continues through the enemies on its line;
     * with {@code chainHops > 0} it then ricochets from the primary target to nearby enemies.
     */
    INSTANT,
    /** Instant hit that chains to additional enemies within chain range. */
    CHAIN,
    /** Periodic pulse hitting every enemy within range (up to pierce). */
    PULSE,
    /** The attack timer fires {@code TowerBehavior.attack}, which does everything (Llama charges, Perplexity radar). */
    CUSTOM,
    /** Pure support tower; never attacks. */
    NONE
}
