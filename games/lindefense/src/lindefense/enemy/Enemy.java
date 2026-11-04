package lindefense.enemy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import lindefense.map.Path;
import lindefense.sim.Vec;

/**
 * A single enemy instance on the track. Holds HP, modifiers, status effects and
 * path progress. Movement is expressed as distance travelled along a
 * {@link Path}, so slows, knockback, teleports and splits all keep progress
 * consistent. Rendering is handled elsewhere (see ui.EnemySprite).
 */
public class Enemy {
    /** Pixels per second for a 1.0x speed enemy (spec 3.0 Units). */
    public static final double BASE_SPEED = 60.0;
    /** Slow strength applied to bosses is divided by this (spec 3.0: bosses receive 1/3 of any slow). */
    public static final double BOSS_SLOW_DIVISOR = 3.0;
    /** Speed multiplier at or above which an enemy counts as "fast" for Mistral-style knockback. */
    public static final double FAST_SPEED_MULT = 2.0;
    /** Seconds between Regrow ticks (restores 1 HP per tick). */
    public static final double REGROW_INTERVAL = 2.0;
    /** Shell Sort gap sequence (pixels). */
    public static final double[] SHELL_GAPS = { 140, 70, 35, 18 };

    private static int nextId = 1;

    private final int id;
    private final EnemyType type;
    private final EnumSet<Modifier> modifiers;
    private double maxHp;
    private double hp;
    private Path route;
    private int routeIndex;
    private double distance;
    private boolean alive = true;
    private boolean leaked;
    private boolean stored;
    private final double hpScale;
    private final double speedScale;

    private static final StatusEffect.Kind[] KINDS = StatusEffect.Kind.values();
    /**
     * Active statuses by kind: {@code statuses[kind.ordinal()]} (null until the kind is first applied; emptied, not
     * dropped, when the last instance expires, so re-applying a status allocates nothing). Same iteration order as the
     * EnumMap it replaces (kind ordinal order).
     */
    @SuppressWarnings("unchecked")
    private final ArrayList<StatusEffect>[] statuses = (ArrayList<StatusEffect>[]) new ArrayList<?>[KINDS.length];
    private Object dotSource;
    // enemy-level operations (see EnemyManager / docs/TOWER_ENGINE.md)
    private double reverseRemaining;
    private double reverseDps;
    private Object reverseSource;
    private double holdRemaining;
    private double holdDistance;
    private double holdPull;
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

    // spatial-grid bookkeeping, owned by EnemyGrid (package-private on purpose)
    EnemyGrid grid;
    int gridEpoch = -1;
    int gridIndex, gridCol, gridRow;

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
    /**
     * Removes Fortified (Qwen ability). Max HP is halved back to its normal value and current HP is capped at it.
     * @return true if the enemy was Fortified
     */
    public boolean stripFortified() {
        if (!modifiers.contains(Modifier.FORTIFIED)) return false;
        modifiers.remove(Modifier.FORTIFIED);
        maxHp = Math.max(1, Math.ceil(maxHp / 2));
        hp = Math.min(hp, maxHp);
        return true;
    }

    /**
     * Permanently removes Camo and Regrow (Gemini U2). Bosses are unaffected.
     * @return true if anything was removed
     */
    public boolean stripCamoRegrow() {
        if (isBoss()) return false;
        boolean changed = modifiers.remove(Modifier.CAMO);
        changed |= modifiers.remove(Modifier.REGROW);
        return changed;
    }

    // ---------------- statuses ----------------
    /** True for kinds that keep every application separately (strongest active one counts). */
    private static boolean isMultiKind(StatusEffect.Kind k) {
        return k == StatusEffect.Kind.SLOW || k == StatusEffect.Kind.MARKED || k == StatusEffect.Kind.MARKED_FLAT;
    }

    /** Applies a status using the standard rules (bosses immune to STUN/FREEZE). Returns false if it was ignored. */
    public boolean applyStatus(StatusEffect.Kind kind, double magnitude, double duration) {
        return applyStatus(kind, magnitude, duration, false);
    }

    /**
     * Applies a status.
     * <ul>
     *   <li>SLOW: every application is tracked and the strongest active one counts (slows never stack).
     *       Bosses receive {@code 1/}{@link #BOSS_SLOW_DIVISOR} of the magnitude.</li>
     *   <li>STUN / FREEZE: ignored for bosses unless {@code bossOverride} (a tower that explicitly stuns bosses).</li>
     *   <li>SLOW / STUN / FREEZE are ignored entirely while {@link #isControlImmune()} (Palantir phase 1).</li>
     *   <li>MARKED (percent) and MARKED_FLAT: strongest active one of each kind counts.</li>
     *   <li>DOT: one instance; a new application keeps the larger dps and refreshes the duration.</li>
     *   <li>Others: one instance, longest duration wins.</li>
     * </ul>
     */
    public boolean applyStatus(StatusEffect.Kind kind, double magnitude, double duration, boolean bossOverride) {
        if (duration <= 0 || !alive) return false;
        switch (kind) {
            case SLOW:
                if (isControlImmune()) return false;
                if (isBoss()) magnitude /= BOSS_SLOW_DIVISOR;
                break;
            case STUN: case FREEZE:
                if (isControlImmune()) return false;
                if (isBoss() && !bossOverride) return false;
                break;
            default: break;
        }
        ArrayList<StatusEffect> list = statuses[kind.ordinal()];
        if (list == null) { list = new ArrayList<>(2); statuses[kind.ordinal()] = list; }
        if (isMultiKind(kind)) {
            for (int i = 0, n = list.size(); i < n; i++) {
                StatusEffect s = list.get(i);
                if (Math.abs(s.magnitude - magnitude) < 1e-9) { s.remaining = Math.max(s.remaining, duration); return true; }
            }
            list.add(new StatusEffect(kind, magnitude, duration));
            return true;
        }
        if (list.isEmpty()) {
            list.add(new StatusEffect(kind, magnitude, duration));
        } else {
            StatusEffect s = list.get(0);
            s.magnitude = Math.max(s.magnitude, magnitude);
            s.remaining = kind == StatusEffect.Kind.DOT ? duration : Math.max(s.remaining, duration);
        }
        return true;
    }

    /**
     * A slow whose magnitude is exact for bosses too (Rohan's 15% / 50% boss slows are specified as delivered values,
     * not subject to the usual 1/{@value #BOSS_SLOW_DIVISOR} boss rule). Control immunity still applies.
     */
    public boolean applyExactSlow(double magnitude, double duration) {
        return applyStatus(StatusEffect.Kind.SLOW, isBoss() ? magnitude * BOSS_SLOW_DIVISOR : magnitude, duration);
    }

    /** Applies burn (DoT) that credits {@code source} (a Tower) for kills and damage. */
    public void applyBurn(double dps, double duration, Object source) {
        if (applyStatus(StatusEffect.Kind.DOT, dps, duration) && source != null) dotSource = source;
    }

    public Object getDotSource() { return dotSource; }

    /** Controls (slow, stun, knockback) are ignored entirely (Palantir phase 1 unless its shield is down). */
    public boolean isControlImmune() { return false; }

    public boolean hasStatus(StatusEffect.Kind kind) {
        List<StatusEffect> l = statuses[kind.ordinal()];
        return l != null && !l.isEmpty();
    }
    /** Strongest active magnitude of a kind (0 if none). */
    public double statusMagnitude(StatusEffect.Kind kind) {
        List<StatusEffect> l = statuses[kind.ordinal()];
        double m = 0;
        if (l != null) for (int i = 0, n = l.size(); i < n; i++) m = Math.max(m, l.get(i).magnitude);
        return m;
    }
    public void clearStatus(StatusEffect.Kind kind) { if (statuses[kind.ordinal()] != null) statuses[kind.ordinal()].clear(); }
    public boolean isStunned() { return hasStatus(StatusEffect.Kind.STUN) || hasStatus(StatusEffect.Kind.FREEZE); }
    public boolean isFrozen() { return hasStatus(StatusEffect.Kind.FREEZE); }
    public boolean isDisrupted() { return hasStatus(StatusEffect.Kind.DISRUPTED); }
    public boolean isRevealed() { return hasStatus(StatusEffect.Kind.REVEALED); }
    public double getSlowFraction() { return statusMagnitude(StatusEffect.Kind.SLOW); }
    public boolean isArmorCracked() { return hasStatus(StatusEffect.Kind.ARMOR_CRACK); }
    public double getMarkedBonus() { return statusMagnitude(StatusEffect.Kind.MARKED); }
    public double getMarkedFlat() { return statusMagnitude(StatusEffect.Kind.MARKED_FLAT); }

    // ---------------- armour classes (spec 3.0) ----------------
    /**
     * Lead-type: Sharp damage cannot hurt it. Heap Sort and Lockheed Martin are Lead-type for their whole body;
     * Radix Sort is Lead-type in its <b>armoured phases</b>, i.e. once {@link #radixPhase()} is 1 or higher
     * (HP at or below 6 of its 8 layers). A Radix Sort at 7-8 layers is plain.
     */
    public boolean isLeadType() {
        return type.isLeadBody() || (type.mechanic == Mechanic.RADIX_PHASES && radixPhase() >= 1);
    }

    /**
     * Ceramic-type: the <b>outer layer</b> of a splitting sort, i.e. the body of a Merge Sort, Quick Sort or TimSort
     * (the layer that releases children when destroyed). Merge Runs and Quick Partitions are not Ceramic.
     */
    public boolean isCeramicType() { return type.isCeramicBody(); }

    /** Boss-class ("MOAB-class" in the spec) means any of the five bosses. */
    public boolean isMoabClass() { return isBoss(); }

    /** Fast for Mistral knockback: Singly/Doubly Linked List or any enemy at 2.0x speed or more. */
    public boolean isFastForKnockback() { return type.isLinkedList() || speedMultiplier() >= FAST_SPEED_MULT; }

    /** Whether a tower with the given detection can target this enemy. */
    public boolean isTargetableBy(boolean towerSeesCamo) {
        if (!alive || stored) return false;
        return !isCamo() || towerSeesCamo || isRevealed();
    }

    // ---------------- shielding (Heap Sort) ----------------
    public void setShieldedTick(int tick) { shieldedTick = tick; }
    public boolean isShielded(int currentTick) { return shieldedTick >= currentTick - 1; }

    // ---------------- Radix phases ----------------
    /** 0 = layers 8-7 (plain), 1 = layers 6-5, 2 = layers 4-3, 3 = layers 2-1. Phases 1-3 are the armoured (Lead-type) phases. */
    public int radixPhase() {
        if (type.mechanic != Mechanic.RADIX_PHASES) return 0;
        double layers = hp / hpScale / (isFortified() ? 2 : 1); // normalise back to 8 layers
        if (layers > 6) return 0;
        if (layers > 4) return 1;
        if (layers > 2) return 2;
        return 3;
    }

    /** Effective armour fraction. Bosses override via phases. */
    public double effectiveArmor() {
        return Math.max(0, baseArmor());
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
    /** Unslowed speed multiplier relative to the 1.0x base (type speed x wave scale x own multipliers). */
    public double speedMultiplier() { return type.speed * speedScale * ownSpeedMultiplier(); }

    /** Unslowed, unstunned own travel speed in px/s (used by rewind). */
    public double nominalSpeed() { return speedMultiplier() * BASE_SPEED; }

    private double liveSpeed(double externalMultiplier) {
        return nominalSpeed() * (1.0 - Math.min(0.9, getSlowFraction())) * externalMultiplier;
    }

    /**
     * Advances along the route.
     * <ul>
     *   <li>Stunned/frozen enemies do not move.</li>
     *   <li>Held enemies (Gemini vortex) do not move on their own but are pulled toward their hold distance.</li>
     *   <li>Reversed enemies (Cohere Override) travel backwards at their live speed (never below distance 0).</li>
     * </ul>
     * @param dt seconds
     * @param externalMultiplier product of map / global multipliers
     * @return true if the enemy reached the exit this tick
     */
    public boolean advance(double dt, double externalMultiplier) {
        if (!alive || stored) return false;
        if (holdRemaining > 0) {
            double step = holdPull * dt;
            double diff = holdDistance - distance;
            distance += Math.max(-step, Math.min(step, diff));
        } else if (!isStunned()) {
            double d = liveSpeed(externalMultiplier) * dt;
            if (reverseRemaining > 0) distance = Math.max(0, distance - d);
            else distance += d;
        }
        distance = Math.max(0, distance);
        cachedPos = route.positionAt(distance);
        if (grid != null) grid.moved(this);
        return distance >= route.getTotalLength();
    }

    /** Current effective speed in px/s (for display / tests). */
    public double currentSpeed(double externalMultiplier) {
        if (isStunned() || holdRemaining > 0) return 0;
        return liveSpeed(externalMultiplier);
    }

    /** Fraction of unslowed per-second movement that knockback may cancel (non-boss / boss). */
    public static final double KNOCKBACK_BUDGET = 0.40;
    public static final double BOSS_KNOCKBACK_BUDGET = 0.25;

    /**
     * Applies knockback subject to a per-second budget (anti soft-lock safeguard) so stacked pushes can
     * never hold an enemy in place forever: within any one-second window the
     * total push is capped at {@link #KNOCKBACK_BUDGET} (bosses
     * {@link #BOSS_KNOCKBACK_BUDGET}) of the enemy's unslowed movement. Knockback moves the enemy back
     * along its route only, so it can never leave the path. Palantir phase 1 ignores it.
     * @return pixels actually pushed
     */
    public double applyKnockback(double px, double now) {
        if (px <= 0 || !alive || isControlImmune()) return 0;
        if (now - kbWindowStart >= 1.0) { kbWindowStart = now; kbUsed = 0; }
        double cap = type.speed * BASE_SPEED * speedScale * (isBoss() ? BOSS_KNOCKBACK_BUDGET : KNOCKBACK_BUDGET);
        double applied = Math.min(px, Math.max(0, cap - kbUsed));
        if (applied <= 0) return 0;
        kbUsed += applied;
        shiftDistance(-applied);
        return applied;
    }

    /** Knockback that ignores the per-second budget (ability blasts / spec-scheduled events). Still stays on the path. */
    public double applyKnockbackUncapped(double px) {
        if (px <= 0 || !alive || isControlImmune()) return 0;
        double before = distance;
        shiftDistance(-px);
        return before - distance;
    }

    /** Moves the enemy along its route by a signed pixel delta (knockback = negative). */
    public void shiftDistance(double delta) {
        distance = Math.max(0, Math.min(route.getTotalLength() - 0.01, distance + delta));
        cachedPos = route.positionAt(distance);
        if (grid != null) grid.moved(this);
    }

    /** Places the enemy at an exact distance on its route. */
    public void setDistance(double d) {
        distance = Math.max(0, Math.min(route.getTotalLength() - 0.01, d));
        cachedPos = route.positionAt(distance);
        if (grid != null) grid.moved(this);
    }

    /** Teleports the enemy onto (possibly another) route at the given distance. */
    public void teleportTo(Path newRoute, int newRouteIndex, double newDistance) {
        this.route = newRoute;
        this.routeIndex = newRouteIndex;
        setDistance(newDistance);
    }

    // ---------------- enemy-level operations ----------------
    /**
     * Rewinds the enemy by {@code seconds} of its own travel distance (Sora). Ignores the knockback budget.
     * Callers decide boss amounts. Palantir phase 1 is not exempt: rewind is time, not a control effect.
     * @return pixels moved back
     */
    public double rewind(double seconds) {
        double before = distance;
        shiftDistance(-nominalSpeed() * seconds);
        return before - distance;
    }

    /**
     * Holds the enemy in place for {@code duration} while pulling it along its route toward
     * {@code atDistance} at {@code pullSpeed} px/s (Gemini vortex). Does nothing for control-immune enemies.
     */
    public boolean hold(double duration, double atDistance, double pullSpeed) {
        if (duration <= 0 || !alive || isControlImmune()) return false;
        holdRemaining = Math.max(holdRemaining, duration);
        holdDistance = atDistance;
        holdPull = pullSpeed;
        return true;
    }
    public boolean isHeld() { return holdRemaining > 0; }

    /**
     * Makes the enemy walk backwards for {@code duration}, dealing {@code contactDps} to every other enemy it
     * touches (Cohere Command Override). The caller is responsible for excluding bosses.
     */
    public void startReverse(double duration, double contactDps, Object source) {
        reverseRemaining = Math.max(reverseRemaining, duration);
        reverseDps = contactDps;
        reverseSource = source;
    }
    public boolean isReversed() { return reverseRemaining > 0; }
    public double getReverseDps() { return reverseDps; }
    public Object getReverseSource() { return reverseSource; }

    // ---------------- per-tick upkeep ----------------
    /** Ticks statuses and regrow. Returns DOT damage to be applied by the caller. */
    public double tickStatuses(double dt) {
        double dot = 0;
        for (int k = 0; k < statuses.length; k++) {
            ArrayList<StatusEffect> list = statuses[k];
            if (list == null) continue;
            for (int i = list.size() - 1; i >= 0; i--) {
                StatusEffect s = list.get(i);
                s.remaining -= dt;
                if (s.kind == StatusEffect.Kind.DOT) dot += s.magnitude * dt;
                if (s.remaining <= 0) list.remove(i);
            }
        }
        if (holdRemaining > 0) holdRemaining = Math.max(0, holdRemaining - dt);
        if (reverseRemaining > 0) reverseRemaining = Math.max(0, reverseRemaining - dt);
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
