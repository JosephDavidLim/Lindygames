package lindefense.tower;

import java.util.ArrayList;
import java.util.List;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;

/**
 * A placed tower. Holds position, progression tier, targeting choice, timed
 * buffs, aura-granted bonuses, ability cooldown and the per-tower attack state
 * (cooldown, Kimi stacks, pulse counters). Attack execution itself is delegated
 * to {@link lindefense.combat.Combat} so towers stay data-driven.
 */
public class Tower {
    private static int nextId = 1;

    /** A timed multiplicative buff. */
    public static final class Buff {
        public double rateMult, dmgMult, rangeMult, remaining;
        Buff(double r, double d, double g, double t) { rateMult = r; dmgMult = d; rangeMult = g; remaining = t; }
    }

    private final int id;
    private final TowerDef def;
    private final Vec position;
    private int tier;
    private TowerStats stats;
    private Targeting targeting;
    private boolean alive = true;
    private final boolean temporary;
    private double lifetime;
    private final Tower owner;

    private double attackCooldown;
    private double abilityCooldown;
    private double surveilledRemaining;
    private double summonTimer;
    private int pulseCounter;
    private final List<Buff> buffs = new ArrayList<>();

    // aura-granted (recomputed by TowerManager)
    private double auraRateBonus, auraDamageBonus, auraRangeBonus;
    private boolean grantedDetection, surveillanceImmune;

    // Kimi stacks
    private int stacks;
    private int stackTargetId = -1;
    private double stackLingerTimer;

    // bookkeeping
    private double damageDealt;
    private int kills;
    private Enemy lastTarget;
    private double lastFireTime = -10;

    public Tower(TowerDef def, Vec position, int tier, boolean temporary, double lifetime, Tower owner) {
        this.id = nextId++;
        this.def = def;
        this.position = position;
        this.tier = tier;
        this.stats = def.statsForTier(tier);
        this.temporary = temporary;
        this.lifetime = lifetime;
        this.owner = owner;
        this.targeting = def.type == TowerType.BELLARMINE_STUDENT_AI ? Targeting.SMART : Targeting.FIRST;
        this.summonTimer = stats.summonInterval;
    }

    public Tower(TowerDef def, Vec position) { this(def, position, 0, false, 0, null); }

    // ---------------- identity ----------------
    public int getId() { return id; }
    public TowerDef getDef() { return def; }
    public TowerType getType() { return def.type; }
    public Vec getPosition() { return position; }
    public int getTier() { return tier; }
    public int getMaxTier() { return def.upgrades.length; }
    public boolean isSuperclass() { return tier >= def.upgrades.length; }
    public TowerStats getStats() { return stats; }
    public Targeting getTargeting() { return targeting; }
    public void setTargeting(Targeting t) { if (def.type != TowerType.BELLARMINE_STUDENT_AI) targeting = t; }
    public boolean isAlive() { return alive; }
    public void markRemoved() { alive = false; }
    public boolean isTemporary() { return temporary; }
    public double getLifetime() { return lifetime; }
    public Tower getOwner() { return owner; }
    public String getTierLabel() { return def.tierLabels[Math.min(tier, def.tierLabels.length - 1)]; }
    public double getRadius() { return def.radius; }
    public double getDamageDealt() { return damageDealt; }
    public int getKills() { return kills; }
    public void addDamageDealt(double d) { damageDealt += d; }
    public void addKill() { kills++; }
    public Enemy getLastTarget() { return lastTarget; }
    public void setLastTarget(Enemy e) { lastTarget = e; }
    public double getLastFireTime() { return lastFireTime; }
    public void setLastFireTime(double t) { lastFireTime = t; }

    // ---------------- economy ----------------
    /** Total cash spent on this tower (base + purchased upgrades). */
    public int getInvestedCost() { return def.totalCostForTier(tier); }
    public boolean canUpgrade() { return tier < def.upgrades.length; }
    public UpgradeDef nextUpgrade() { return canUpgrade() ? def.upgrades[tier] : null; }

    /** Applies the next upgrade (cash handling is done by the Shop / TowerManager). */
    public void applyUpgrade() {
        if (!canUpgrade()) return;
        tier++;
        stats = def.statsForTier(tier);
        if (stats.summonInterval > 0 && summonTimer <= 0) summonTimer = stats.summonInterval;
    }

    // ---------------- buffs & auras ----------------
    public void addBuff(double rateMult, double dmgMult, double rangeMult, double duration) {
        buffs.add(new Buff(rateMult, dmgMult, rangeMult, duration));
    }
    public List<Buff> getBuffs() { return buffs; }

    public void resetAuraBonuses() {
        auraRateBonus = 0; auraDamageBonus = 0; auraRangeBonus = 0; grantedDetection = false; surveillanceImmune = false;
    }
    public void addAuraRate(double v) { auraRateBonus += v; }
    public void addAuraDamage(double v) { auraDamageBonus += v; }
    public void addAuraRange(double v) { auraRangeBonus += v; }
    public void grantDetection() { grantedDetection = true; }
    public void grantSurveillanceImmunity() { surveillanceImmune = true; }
    public boolean isSurveillanceImmune() { return surveillanceImmune || stats.auraSurveillanceImmunity; }
    public boolean canSeeCamo() { return stats.seesCamo || grantedDetection; }
    public double getAuraRateBonus() { return auraRateBonus; }
    public double getAuraDamageBonus() { return auraDamageBonus; }

    public void setSurveilled(double duration) { surveilledRemaining = Math.max(surveilledRemaining, duration); }
    public void clearSurveilled() { surveilledRemaining = 0; }
    public boolean isSurveilled() { return surveilledRemaining > 0; }
    public double getSurveilledRemaining() { return surveilledRemaining; }

    // ---------------- ability ----------------
    public AbilityDef getAbility() { return def.ability; }
    public boolean isAbilityReady() { return def.ability != null && abilityCooldown <= 0 && !isSurveilled(); }
    public double getAbilityCooldownRemaining() { return abilityCooldown; }
    public double getAbilityCooldownFraction() { return def.ability == null ? 0 : Math.max(0, abilityCooldown / def.ability.cooldown); }
    public void startAbilityCooldown() { if (def.ability != null) abilityCooldown = def.ability.cooldown; }
    public void resetAbilityCooldown() { abilityCooldown = 0; }
    public void reduceAbilityCooldown(double fraction) { abilityCooldown *= (1 - fraction); }

    // ---------------- Kimi stacks ----------------
    public int getStacks() { return stacks; }
    public int getStackTargetId() { return stackTargetId; }
    /** Records a hit on an enemy and returns the stack count to use for this hit. */
    public int registerStackHit(int enemyId) {
        if (stats.stackCap <= 0) return 0;
        if (enemyId == stackTargetId) {
            stacks = Math.min(stats.stackCap, stacks + 1);
        } else if (stackLingerTimer > 0 && stats.stackLinger > 0) {
            stackTargetId = enemyId; // keep stacks while lingering
            stacks = Math.min(stats.stackCap, stacks + 1);
        } else {
            stackTargetId = enemyId;
            stacks = 1;
        }
        stackLingerTimer = stats.stackLinger;
        return stacks - 1;
    }
    public void resetStacks() { stacks = 0; stackTargetId = -1; }

    // ---------------- effective numbers ----------------
    public double buffRateMult() { double m = 1; for (Buff b : buffs) m *= b.rateMult; return m; }
    public double buffDamageMult() { double m = 1; for (Buff b : buffs) m *= b.dmgMult; return m; }
    public double buffRangeMult() { double m = 1; for (Buff b : buffs) m *= b.rangeMult; return m; }

    public double effectiveRange(Game game) {
        double r = stats.range * (1 + auraRangeBonus) * buffRangeMult();
        if (game != null) r *= game.getMap().towerRangeMultiplier(game, this);
        if (isSurveilled()) r *= 0.7;
        return r;
    }

    public double effectiveRate(Game game) {
        double bonus = auraRateBonus;
        double r = stats.rate * (1 + bonus) * buffRateMult();
        if (game != null) {
            r *= game.getMap().towerRateMultiplier(game, this);
            r *= game.getGlobalRateMultiplier();
        }
        if (isSurveilled()) r *= 0.5;
        return r;
    }

    public double damageMultiplier(Game game) {
        double m = (1 + auraDamageBonus) * buffDamageMult();
        if (game != null) {
            m *= game.getMap().towerDamageMultiplier(game, this);
            m *= game.getGlobalDamageMultiplier();
        }
        return m;
    }

    // ---------------- per tick ----------------
    /** Advances timers. Returns true if this tower wants to attack this tick. */
    public boolean tickTimers(Game game, double dt) {
        if (temporary) {
            lifetime -= dt;
            if (lifetime <= 0) alive = false;
        }
        for (int i = buffs.size() - 1; i >= 0; i--) {
            Buff b = buffs.get(i);
            b.remaining -= dt;
            if (b.remaining <= 0) buffs.remove(i);
        }
        if (abilityCooldown > 0) abilityCooldown = Math.max(0, abilityCooldown - dt);
        if (surveilledRemaining > 0) surveilledRemaining = Math.max(0, surveilledRemaining - dt);
        if (stackLingerTimer > 0) {
            stackLingerTimer -= dt;
            if (stackLingerTimer <= 0 && stats.stackLinger > 0) resetStacks();
        }
        if (stats.kind == AttackKind.NONE) return false;
        attackCooldown -= dt;
        return attackCooldown <= 0;
    }

    /** Called by Combat after a successful attack. */
    public void onAttacked(Game game) {
        double rate = Math.max(0.05, effectiveRate(game));
        attackCooldown = Math.max(attackCooldown + 1.0 / rate, 1.0 / rate * 0.5);
        if (attackCooldown > 1.0 / rate) attackCooldown = 1.0 / rate;
        lastFireTime = game.getTime();
    }

    /** Resets the attack cooldown lightly when no target is available (keeps it ready). */
    public void idle() { attackCooldown = Math.max(attackCooldown, 0); }

    public int nextPulseIndex() { return ++pulseCounter; }

    /** Returns true when the periodic summon timer fires. */
    public boolean tickSummon(double dt) {
        if (stats.summonInterval <= 0) return false;
        summonTimer -= dt;
        if (summonTimer <= 0) { summonTimer = stats.summonInterval; return true; }
        return false;
    }

    @Override public String toString() {
        return def.type.displayName + "#" + id + "(T" + tier + ")";
    }
}
