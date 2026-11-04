package lindefense.combat;

import lindefense.enemy.Enemy;
import lindefense.tower.Tower;

/**
 * Everything the damage formula needs about one hit, and the object tower behaviours receive in
 * {@code beforeHit} / {@code onHit}. Behaviours may adjust {@link #extra}, {@link #finalMult},
 * {@link #finalFlat} or call {@link #editHit()} to change the spec for this hit only.
 */
public final class HitContext {
    public final Enemy target;
    /** The tower credited with the hit (may be null for map hazards / tests). */
    public final Tower source;
    private HitSpec hit;
    private boolean hitOwned;
    /** Extra flat damage added to the spec's damage before multipliers (streaks, memory bonuses). */
    public double extra;
    /** Tower damage multiplier (map / global); 1 for abilities. */
    public double mult = 1;
    /** True for hits that are part of a tower's attack: these receive the buffed flat damage (cap +5). */
    public boolean fromAttack;
    /** Whether the source can see camo (used by the EXPLOSIVE-vs-camo rule). Companions may differ from their owner. */
    public boolean detection;
    /** The projectile that caused the hit, if any. */
    public Projectile projectile;
    /** Free-form tag set by callers/behaviours (e.g. "burst", "chord"). */
    public String tag;
    /** Multiplier applied last (after armour and marks) -- use for ability-specific bonuses. */
    public double finalMult = 1;
    /** Flat damage added last. */
    public double finalFlat = 0;
    /** Result: HP actually removed. */
    public double dealt;
    /** Result: the hit was blocked by an immunity (Sharp vs Lead, Explosive vs undetected Camo). */
    public boolean immune;

    public HitContext(Enemy target, HitSpec hit, Tower source) {
        this.target = target; this.hit = hit; this.source = source;
        this.detection = source == null || source.canSeeCamo();
    }

    /** The spec for this hit (read-only unless you called {@link #editHit()}). */
    public HitSpec hit() { return hit; }

    /** Returns a private mutable copy of the spec for this hit (copy-on-write). */
    public HitSpec editHit() {
        if (!hitOwned) { hit = hit.copy(); hitOwned = true; }
        return hit;
    }
}
