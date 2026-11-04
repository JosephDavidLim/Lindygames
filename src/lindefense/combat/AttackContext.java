package lindefense.combat;

import java.util.List;
import lindefense.enemy.Enemy;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * The parameters of one tower attack, filled from the tower's current stats and buffs and handed to
 * {@code TowerBehavior.modifyAttack} so a behaviour can adjust them before the shot is fired.
 */
public final class AttackContext {
    public final Tower tower;
    public final TowerStats stats;
    /** Targets chosen by priority (size &le; stats.targets). Behaviours may reorder or replace. */
    public List<Enemy> targets;
    /** The first (primary) target. */
    public Enemy primary;
    private HitSpec hit;
    private boolean hitOwned;
    /** Tower damage multiplier. */
    public double damageMult = 1;
    /** Extra flat damage for this attack (consecutive-hit bonus, memory bonus, ...). */
    public double extraDamage;
    /** Effective pierce including buffs. For splash shots this caps the enemies hit by the blast. */
    public int pierce;
    public int bounces;
    public int clones;
    public double splash;
    public double speed;
    /** Projectiles (or beams) per target. */
    public int shotsPerTarget = 1;
    /** Set true in {@code modifyAttack} to abort the attack (the tower stays ready). */
    public boolean cancel;
    /** Free-form tag copied onto projectiles ("chord", "heavy", ...). */
    public String tag;
    /** Custom collision logic copied onto the projectiles of this attack (null = generic). */
    public Projectile.ImpactHandler onImpact;

    public AttackContext(Tower tower, TowerStats stats, List<Enemy> targets) {
        this.tower = tower; this.stats = stats; this.targets = targets;
        this.primary = targets.isEmpty() ? null : targets.get(0);
        this.hit = stats.hit;
        this.shotsPerTarget = Math.max(1, stats.projectilesPerShot);
    }

    public HitSpec hit() { return hit; }

    /** Private mutable copy of the stats' hit spec for this attack only. */
    public HitSpec editHit() {
        if (!hitOwned) { hit = hit.copy(); hitOwned = true; }
        return hit;
    }
}
