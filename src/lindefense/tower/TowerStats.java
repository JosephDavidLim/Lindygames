package lindefense.tower;

import lindefense.combat.HitSpec;

/**
 * Complete numeric description of a tower at one progression tier. The catalog
 * defines the base tier; each upgrade mutates a copy, so the four tiers are
 * cumulative. All fields are public so upgrade lambdas can edit them directly.
 */
public class TowerStats {
    public AttackKind kind = AttackKind.PROJECTILE;
    public HitSpec hit = new HitSpec();
    public double range = 150;
    /** Attacks per second. */
    public double rate = 1.0;
    /** Enemies one attack can hit (projectile pierce, beam pass-through, pulse cap). */
    public int pierce = 1;
    public double projectileSpeed = 400;
    /** Splash radius in px (0 = none). Splash hits up to {@code pierce} enemies. */
    public double splashRadius = 0;
    /** Number of extra chain hops (CHAIN) and their reach. */
    public int chainHops = 0;
    public double chainRange = 90;
    /** Projectile bounce count (MiniMax) and reach. */
    public int bounces = 0;
    public double bounceRange = 90;
    /** Projectiles spawned on first hit (MiniMax clones). */
    public int clones = 0;
    /** Projectiles fired per attack at the same target (volley). */
    public int projectilesPerShot = 1;
    /** Distinct simultaneous targets per attack. */
    public int targets = 1;
    /** Every Nth pulse is a "heavy" pulse (Sora Frame Drop / GLM Singularity Well). */
    public int heavyEvery = 0;
    public double heavyStun = 0;
    public double heavyKnockback = 0;
    /** Suno Resonance: +1 damage per N enemies in range (0 = off), capped. */
    public int resonancePer = 0;
    public int resonanceCap = 4;
    /** Kimi: damage stacks per consecutive hit on the same target. */
    public int stackCap = 0;
    public double stackStep = 1;
    public double stackLinger = 0;

    public boolean seesCamo = false;
    /** Towers within aura range gain camo detection. */
    public boolean auraDetection = false;
    public boolean auraSurveillanceImmunity = false;
    public double auraRange = 0;
    /** Aura attack-speed bonus for all towers / for Military towers (additive fractions). */
    public double auraRate = 0;
    public double auraRateMilitary = 0;
    public double auraDamage = 0;
    public double auraRangeBonus = 0;
    /** Mistral Wind Wall: passive slow on enemies in range. */
    public double auraEnemySlow = 0;
    /** Cash paid at the end of every wave (Perplexity). */
    public double incomePerWave = 0;
    /** Periodic summoning (DALL-E sprites). */
    public double summonInterval = 0;
    public int summonCount = 0;
    public double summonLifetime = 0;

    public TowerStats copy() {
        TowerStats s = new TowerStats();
        s.kind = kind; s.hit = hit.copy(); s.range = range; s.rate = rate; s.pierce = pierce;
        s.projectileSpeed = projectileSpeed; s.splashRadius = splashRadius; s.chainHops = chainHops;
        s.chainRange = chainRange; s.bounces = bounces; s.bounceRange = bounceRange; s.clones = clones;
        s.projectilesPerShot = projectilesPerShot; s.targets = targets; s.heavyEvery = heavyEvery;
        s.heavyStun = heavyStun; s.heavyKnockback = heavyKnockback; s.resonancePer = resonancePer;
        s.resonanceCap = resonanceCap; s.stackCap = stackCap; s.stackStep = stackStep; s.stackLinger = stackLinger;
        s.seesCamo = seesCamo; s.auraDetection = auraDetection; s.auraSurveillanceImmunity = auraSurveillanceImmunity;
        s.auraRange = auraRange; s.auraRate = auraRate; s.auraRateMilitary = auraRateMilitary; s.auraDamage = auraDamage;
        s.auraRangeBonus = auraRangeBonus; s.auraEnemySlow = auraEnemySlow; s.incomePerWave = incomePerWave;
        s.summonInterval = summonInterval; s.summonCount = summonCount; s.summonLifetime = summonLifetime;
        return s;
    }

    public boolean isSupportOnly() { return kind == AttackKind.NONE; }
}
