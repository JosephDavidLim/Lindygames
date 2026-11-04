package lindefense.enemy;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import lindefense.combat.DamageType;
import lindefense.map.Path;
import lindefense.sim.Vec;

/**
 * A single enemy instance on the track. Holds HP, modifiers, status effects and
 * path progress. Movement is expressed as distance travelled along a
 * {@link Path}, so slows, knockback, teleports and splits all keep progress
 * consistent. Rendering is handled elsewhere (see ui.EnemySprite).
 */
public class Enemy {
    /** Pixels per second for a 1.0x speed enemy. */
    public static final double BASE_SPEED = 55.0;
    /** Seconds between Regrow ticks (restores 1 HP per tick). */
    public static final double REGROW_INTERVAL = 2.0;
    /** Shell Sort gap sequence (pixels). */
    public static final double[] SHELL_GAPS = { 140, 70, 35, 18 };

    private static int nextId = 1;

    private final int id;
    private final EnemyType type;
    private final EnumSet<Modifier> modifiers;
    private final double maxHp;
    private double hp;
    private Path route;
    private int routeIndex;
    private double distance;
    private boolean alive = true;
    private boolean leaked;
    private boolean stored;
    private final double hpScale;
    private final double speedScale;

    private final Map<StatusEffect.Kind, StatusEffect> statuses = new EnumMap<>(StatusEffect.Kind.class);
    private double regrowTimer;
    private double mechanicTimer;
    private int shellJumpIndex;
    private double accelBonus;
    private int shieldedTick = -1000;
    private boolean drifted;
    private int timesHit;
    private double lastHitTime = -10;
    private double spawnTime;
    private Vec cachedPos;
    private Object lastHitBy;
    private double kbWindowStart = -1;
    private double kbUsed;

    /** Enemies absorbed by a Bucket Sort. */
    private final List<Enemy> stored_ = new ArrayList<>();

    public Enemy(EnemyType type, EnumSet<Modifier> modifiers, Path route, int routeIndex, double distance,
                 double hpScale, double speedScale) {
        this.id = nextId++;
        this.type = type;
        this.modifiers = modifiers == null ? EnumSet.noneOf(Modifier.class) : EnumSet.copyOf(modifiers);
        if (type.hasInnateCamo()) this.modifiers.add(Modifier.CAMO);
        this.hpScale = hpScale;
        this.speedScale = speedScale;
        double base = Math.max(1, Math.round(type.baseHp * hpScale));
        this.maxHp = this.modifiers.contains(Modifier.FORTIFIED) ? base * 2 : base;
        this.hp = maxHp;
        this.route = route;
        this.routeIndex = routeIndex;
        this.distance = Math.max(0, distance);
        this.cachedPos = route.positionAt(this.distance);
    }

    // ---------------- basic accessors ----------------
    public int getId() { return id; }
    public EnemyType getType() { return type; }
    public EnumSet<Modifier> getModifiers() { return modifiers; }
    public boolean has(Modifier m) { return modifiers.contains(m); }
    public boolean isCamo() { return modifiers.contains(Modifier.CAMO); }
    public boolean isFortified() { return modifiers.contains(Modifier.FORTIFIED); }
    public boolean isRegrow() { return modifiers.contains(Modifier.REGROW); }
    public boolean isBoss() { return type.isBoss(); }
    public double getMaxHp() { return maxHp; }
    public double getHp() { return hp; }
    public double getHpFraction() { return maxHp <= 0 ? 0 : hp / maxHp; }
    public Path getRoute() { return route; }
    public int getRouteIndex() { return routeIndex; }
    public double getDistance() { return distance; }
    public double getRemainingDistance() { return route.getTotalLength() - distance; }
    public boolean isAlive() { return alive; }
    public boolean hasLeaked() { return leaked; }
    public boolean isStored() { return stored; }
    public double getHpScale() { return hpScale; }
    public double getSpeedScale() { return speedScale; }
    public Vec getPosition() { return cachedPos; }
    public double getRadius() { return type.radius * (isBoss() ? 1.0 : (isFortified() ? 1.15 : 1.0)); }
    public List<Enemy> getStoredEnemies() { return stored_; }
    public boolean hasDrifted() { return drifted; }
    public void setDrifted(boolean d) { drifted = d; }
    public int getTimesHit() { return timesHit; }
    public double getLastHitTime() { return lastHitTime; }
    public double getSpawnTime() { return spawnTime; }
    public void setSpawnTime(double t) { spawnTime = t; }
    public Object getLastHitBy() { return lastHitBy; }
    public void setLastHitBy(Object src) { lastHitBy = src; }
    public double getMechanicTimer() { return mechanicTimer; }
    public void setMechanicTimer(double t) { mechanicTimer = t; }
    public int getShellJumpIndex() { return shellJumpIndex; }
    public void setShellJumpIndex(int i) { shellJumpIndex = i; }
    public double getAccelBonus() { return accelBonus; }

    /** Current Shell Sort gap in pixels. */
    public double currentShellGap() { return SHELL_GAPS[Math.min(shellJumpIndex, SHELL_GAPS.length - 1)]; }

    // ---------------- modifiers ----------------
    /** Removes Fortified (Qwen). Max HP is not changed retroactively; current HP is halved. */
    public boolean stripFortified() {
        if (!modifiers.contains(Modifier.FORTIFIED)) return false;
        modifiers.remove(Modifier.FORTIFIED);
        hp = Math.min(hp, Math.max(1, Math.ceil(maxHp / 2)));
        return true;
    }

    // ---------------- statuses ----------------
    public void applyStatus(StatusEffect.Kind kind, double magnitude, double duration) {
        if (duration <= 0) return;
        StatusEffect s = statuses.get(kind);
        if (s == null) {
            statuses.put(kind, new StatusEffect(kind, magnitude, duration));
        } else {
            s.magnitude = Math.max(s.magnitude, magnitude);
            s.remaining = Math.max(s.remaining, duration);
        }
    }

    public boolean hasStatus(StatusEffect.Kind kind) { return statuses.containsKey(kind); }
    public double statusMagnitude(StatusEffect.Kind kind) {
        StatusEffect s = statuses.get(kind);
        return s == null ? 0 : s.magnitude;
    }
    public void clearStatus(StatusEffect.Kind kind) { statuses.remove(kind); }
    public boolean isStunned() { return statuses.containsKey(StatusEffect.Kind.STUN); }
    public boolean isDisrupted() { return statuses.containsKey(StatusEffect.Kind.DISRUPTED); }
    public boolean isRevealed() { return statuses.containsKey(StatusEffect.Kind.REVEALED); }
    public double getSlowFraction() { return statusMagnitude(StatusEffect.Kind.SLOW); }
    public double getArmorBreak() { return statusMagnitude(StatusEffect.Kind.ARMOR_BREAK); }
    public double getMarkedBonus() { return statusMagnitude(StatusEffect.Kind.MARKED); }

    /** Whether a tower with the given detection can target this enemy. */
    public boolean isTargetableBy(boolean towerSeesCamo) {
        if (!alive || stored) return false;
        return !isCamo() || towerSeesCamo || isRevealed();
    }

    // ---------------- shielding (Heap Sort) ----------------
    public void setShieldedTick(int tick) { shieldedTick = tick; }
    public boolean isShielded(int currentTick) { return shieldedTick >= currentTick - 1; }

    // ---------------- Radix phases ----------------
    /** 0 = layers 8-7 plain, 1 = 6-5 Physical-immune, 2 = 4-3 Energy/Magic -50%, 3 = 2-1 Explosive-immune. */
    public int radixPhase() {
        if (type.mechanic != Mechanic.RADIX_PHASES) return 0;
        double layers = hp / hpScale / (isFortified() ? 2 : 1); // normalise back to 8 layers
        if (layers > 6) return 0;
        if (layers > 4) return 1;
        if (layers > 2) return 2;
        return 3;
    }

    /**
     * Damage multiplier for a damage type including Radix phases and innate
     * resistances. Returns 0 for immunities.
     */
    public double resistanceMultiplier(DamageType dt) {
        double m = type.resistance(dt);
        if (type.mechanic == Mechanic.RADIX_PHASES && dt != DamageType.TRUE) {
            switch (radixPhase()) {
                case 1: if (dt == DamageType.PHYSICAL) m = 0; break;
                case 2: if (dt == DamageType.ENERGY || dt == DamageType.MAGIC) m *= 0.5; break;
                case 3: if (dt == DamageType.EXPLOSIVE) m = 0; break;
                default: break;
            }
        }
        return m;
    }

    /** Effective armour fraction after Armor Break. Bosses override via phases. */
    public double effectiveArmor() {
        return Math.max(0, baseArmor() - getArmorBreak());
    }

    protected double baseArmor() { return type.getArmor(); }

    /** Extra speed multiplier from the enemy itself (bosses phases, DLL acceleration). */
    protected double ownSpeedMultiplier() { return 1.0 + accelBonus; }

    // ---------------- damage ----------------
    /**
     * Applies already-computed damage. Returns the actual HP removed.
     * Callers must use {@link lindefense.combat.Combat} to compute damage so that
     * resistances/armour are consistently applied.
     */
    public double takeDamage(double amount, double now) {
        if (!alive || amount <= 0) return 0;
        double before = hp;
        hp = Math.max(0, hp - amount);
        timesHit++;
        lastHitTime = now;
        if (type.mechanic == Mechanic.ACCELERATE_ON_DAMAGE) {
            accelBonus = Math.min(0.9, accelBonus + 0.15);
        }
        return before - hp;
    }

    public void heal(double amount) {
        if (!alive) return;
        hp = Math.min(maxHp, hp + amount);
    }

    /** Marks the enemy dead. Only EnemyManager should call this. */
    public void markDead() { alive = false; }
    public void markLeaked() { leaked = true; alive = false; }
    public void setStored(boolean s) { stored = s; }

    // ---------------- movement ----------------
    /**
     * Advances along the route.
     * @param dt seconds
     * @param externalMultiplier product of map / global multipliers
     * @return true if the enemy reached the exit this tick
     */
    public boolean advance(double dt, double externalMultiplier) {
        if (!alive || stored) return false;
        if (!isStunned()) {
            double speed = type.speed * BASE_SPEED * speedScale * ownSpeedMultiplier()
                * (1.0 - Math.min(0.9, getSlowFraction())) * externalMultiplier;
            distance += speed * dt;
        }
        cachedPos = route.positionAt(distance);
        return distance >= route.getTotalLength();
    }

    /** Current effective speed in px/s (for display / tests). */
    public double currentSpeed(double externalMultiplier) {
        if (isStunned()) return 0;
        return type.speed * BASE_SPEED * speedScale * ownSpeedMultiplier()
            * (1.0 - Math.min(0.9, getSlowFraction())) * externalMultiplier;
    }

    /** Fraction of unslowed per-second movement that knockback may cancel (non-boss / boss). */
    public static final double KNOCKBACK_BUDGET = 0.40;
    public static final double BOSS_KNOCKBACK_BUDGET = 0.25;

    /**
     * Applies knockback subject to a per-second budget so stacked pushes can
     * never hold an enemy in place forever: within any one-second window the
     * total push is capped at {@link #KNOCKBACK_BUDGET} (bosses
     * {@link #BOSS_KNOCKBACK_BUDGET}) of the enemy's unslowed movement.
     * @return pixels actually pushed
     */
    public double applyKnockback(double px, double now) {
        if (px <= 0 || !alive) return 0;
        if (now - kbWindowStart >= 1.0) { kbWindowStart = now; kbUsed = 0; }
        double cap = type.speed * BASE_SPEED * speedScale * (isBoss() ? BOSS_KNOCKBACK_BUDGET : KNOCKBACK_BUDGET);
        double applied = Math.min(px, Math.max(0, cap - kbUsed));
        if (applied <= 0) return 0;
        kbUsed += applied;
        shiftDistance(-applied);
        return applied;
    }

    /** Moves the enemy along its route by a signed pixel delta (knockback = negative). */
    public void shiftDistance(double delta) {
        distance = Math.max(0, Math.min(route.getTotalLength() - 0.01, distance + delta));
        cachedPos = route.positionAt(distance);
    }

    /** Places the enemy at an exact distance on its route. */
    public void setDistance(double d) {
        distance = Math.max(0, Math.min(route.getTotalLength() - 0.01, d));
        cachedPos = route.positionAt(distance);
    }

    /** Teleports the enemy onto (possibly another) route at the given distance. */
    public void teleportTo(Path newRoute, int newRouteIndex, double newDistance) {
        this.route = newRoute;
        this.routeIndex = newRouteIndex;
        setDistance(newDistance);
    }

    // ---------------- per-tick upkeep ----------------
    /** Ticks statuses and regrow. Returns DOT damage to be applied by the caller. */
    public double tickStatuses(double dt) {
        double dot = 0;
        List<StatusEffect.Kind> expired = null;
        for (StatusEffect s : statuses.values()) {
            s.remaining -= dt;
            if (s.kind == StatusEffect.Kind.DOT) dot += s.magnitude * dt;
            if (s.remaining <= 0) {
                if (expired == null) expired = new ArrayList<>(2);
                expired.add(s.kind);
            }
        }
        if (expired != null) for (StatusEffect.Kind k : expired) statuses.remove(k);
        if (isRegrow() && hp < maxHp) {
            regrowTimer += dt;
            if (regrowTimer >= REGROW_INTERVAL) {
                regrowTimer = 0;
                heal(Math.max(1, Math.round(hpScale)));
            }
        }
        return dot;
    }

    @Override public String toString() {
        return type.displayName + "#" + id + String.format("[hp=%.0f/%.0f d=%.0f]", hp, maxHp, distance);
    }
}
