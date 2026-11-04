package lindefense.effects;

import java.util.List;
import lindefense.combat.AttackContext;
import lindefense.enemy.Enemy;
import lindefense.map.Path;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Targeting;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * An orbiting, patrolling or stationary shooter with its own stats and lifetime: the Student+AI companion and
 * study-buddy drones, Qwen formation drones, MiniMax clones and other helpers. It fires the same kinds of
 * attacks as a tower ({@code PROJECTILE}, {@code INSTANT}, {@code CHAIN}) from its own position using
 * {@link TowerStats} (typically built with {@link #statsFor}), credited to its owner tower.
 *
 * <ul>
 *   <li>ORBIT: circles the owner at {@link #orbitRadius}. PATROL: walks back and forth along a route between two
 *       distances. STATIC: stays at its position.</li>
 *   <li>{@link #mirrorOwner}: use the owner's current stats instead of its own (MiniMax clones copy the base attack).</li>
 *   <li>{@link #detection}: camo detection of the drone itself (independent of the owner).</li>
 *   <li>Drone hits do not receive the owner's flat damage buff.</li>
 * </ul>
 */
public final class Drone {
    public enum Mode { ORBIT, PATROL, STATIC }

    public final Tower owner;
    public final Mode mode;
    public TowerStats stats;
    public boolean mirrorOwner;
    public boolean detection;
    /** Targeting priority used by the drone. */
    public Targeting targeting = Targeting.FIRST;
    /** Seconds of life (&le; 0 = lives as long as the owner). */
    public double lifetime;
    public int rgb = 0xffffff;
    /** Free-form label for the renderer / tests ("companion", "buddy", "qwen", "clone"). */
    public String kind = "drone";

    // orbit
    public double orbitRadius = 40;
    /** Radians per second. */
    public double orbitSpeed = 2.0;
    private double angle;
    // patrol
    private Path route;
    private double from, to, patrolSpeed, patrolPos;
    private int patrolDir = 1;

    private Vec pos;
    private double cooldown;
    private double age;
    private boolean dead;

    private Drone(Tower owner, Mode mode, TowerStats stats, Vec pos) {
        this.owner = owner; this.mode = mode; this.stats = stats; this.pos = pos;
    }

    /** An orbiting drone around the owner. {@code startAngle} spreads several drones around the circle. */
    public static Drone orbit(Tower owner, TowerStats stats, double radius, double startAngle) {
        Drone d = new Drone(owner, Mode.ORBIT, stats, owner.getPosition());
        d.orbitRadius = radius; d.angle = startAngle;
        d.pos = owner.getPosition().add(new Vec(Math.cos(startAngle) * radius, Math.sin(startAngle) * radius));
        return d;
    }

    /** A drone patrolling a route between two distances (anywhere on the map). */
    public static Drone patrol(Tower owner, TowerStats stats, Path route, double from, double to, double speed) {
        Drone d = new Drone(owner, Mode.PATROL, stats, route.positionAt(from));
        d.route = route; d.from = Math.min(from, to); d.to = Math.max(from, to); d.patrolSpeed = speed; d.patrolPos = from;
        return d;
    }

    /** A stationary drone. */
    public static Drone fixed(Tower owner, TowerStats stats, Vec at) { return new Drone(owner, Mode.STATIC, stats, at); }

    /**
     * Builds drone stats: an attack of the given kind with the given range / interval / damage / pierce / speed.
     * The hit spec (type, marks...) can be edited on the returned stats.
     */
    public static TowerStats statsFor(lindefense.tower.AttackKind kind, double range, double interval, double damage,
                                      lindefense.combat.DamageType type, int pierce, double projectileSpeed) {
        TowerStats s = new TowerStats();
        s.kind = kind; s.range = range; s.interval(interval); s.hit.damage = damage; s.hit.type = type;
        s.pierce = pierce; s.projectileSpeed = projectileSpeed;
        return s;
    }

    public Vec getPosition() { return pos; }
    public boolean isAlive() { return !dead; }
    public void kill() { dead = true; }
    public double getAge() { return age; }
    public TowerStats effectiveStats() { return mirrorOwner ? owner.getStats() : stats; }

    void update(Game game, double dt) {
        if (dead) return;
        if (!owner.isAlive()) { dead = true; return; }
        age += dt;
        if (lifetime > 0 && age >= lifetime) { dead = true; return; }
        switch (mode) {
            case ORBIT:
                angle += orbitSpeed * dt;
                pos = owner.getPosition().add(new Vec(Math.cos(angle) * orbitRadius, Math.sin(angle) * orbitRadius));
                break;
            case PATROL:
                patrolPos += patrolDir * patrolSpeed * dt;
                if (patrolPos >= to) { patrolPos = to; patrolDir = -1; }
                if (patrolPos <= from) { patrolPos = from; patrolDir = 1; }
                pos = route.positionAt(patrolPos);
                break;
            default: break;
        }
        cooldown -= dt;
        if (cooldown > 0) return;
        TowerStats s = effectiveStats();
        int want = Math.max(s.targets, s.kind == lindefense.tower.AttackKind.INSTANT ? s.projectilesPerShot : 1);
        List<Enemy> targets = game.getCombat().acquireTargetsFrom(pos, s.range, detection, true, targeting, Math.max(1, want), null);
        if (targets.isEmpty()) return;
        AttackContext c = new AttackContext(owner, s, targets);
        c.damageMult = owner.damageMultiplier(game);
        c.pierce = Math.max(1, s.pierce);
        c.bounces = s.bounces; c.clones = s.clones; c.splash = s.splashRadius; c.speed = s.projectileSpeed;
        game.getCombat().executeAttack(c, pos, detection, false);
        cooldown = 1.0 / Math.max(0.05, s.rate);
    }
}
