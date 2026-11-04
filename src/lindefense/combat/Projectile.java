package lindefense.combat;

import java.util.HashSet;
import java.util.Set;
import lindefense.enemy.Enemy;
import lindefense.sim.Vec;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * A travelling projectile. Projectiles home gently on their target while it is
 * alive (so that displayed shots really land), collide with any enemy whose
 * radius they enter, and support pierce, splash, bounce and clone behaviour.
 */
public class Projectile {
    private Vec pos;
    private Vec vel;
    private Enemy target;
    private final Tower source;
    private final TowerStats stats;
    private final double damageMult;
    private int pierceLeft;
    private int bouncesLeft;
    private boolean canClone;
    private final Set<Integer> hitIds = new HashSet<>(4);
    private double travelled;
    private final double maxTravel;
    private boolean alive = true;
    private double stalled;
    private final double speed;
    private final int rgb;
    private final double size;

    public Projectile(Tower source, TowerStats stats, Vec start, Enemy target, double damageMult, boolean canClone, int rgb) {
        this.source = source;
        this.stats = stats;
        this.pos = start;
        this.target = target;
        this.damageMult = damageMult;
        this.speed = stats.projectileSpeed;
        this.pierceLeft = Math.max(1, stats.pierce);
        this.bouncesLeft = stats.bounces;
        this.canClone = canClone && stats.clones > 0;
        this.maxTravel = stats.range * 1.4 + 80;
        this.rgb = rgb;
        this.size = stats.splashRadius > 0 ? 8 : (stats.hit.damage >= 5 ? 7 : 5);
        Vec dir = target.getPosition().sub(start).normalized();
        if (dir.length() < 0.5) dir = new Vec(1, 0);
        this.vel = dir.scale(speed);
    }

    public Vec getPosition() { return pos; }
    public Vec getVelocity() { return vel; }
    public boolean isAlive() { return alive; }
    public Tower getSource() { return source; }
    public TowerStats getStats() { return stats; }
    public double getDamageMult() { return damageMult; }
    public int getRgb() { return rgb; }
    public double getSize() { return size; }
    public Set<Integer> getHitIds() { return hitIds; }
    public Enemy getTarget() { return target; }
    public int getBouncesLeft() { return bouncesLeft; }
    public boolean canClone() { return canClone; }
    public void consumeClone() { canClone = false; }
    public void kill() { alive = false; }

    public void retarget(Enemy e) {
        target = e;
        if (e != null) vel = e.getPosition().sub(pos).normalized().scale(speed);
    }

    public void consumeBounce() { bouncesLeft--; }

    /** Decrements pierce; returns true if the projectile is spent. */
    public boolean consumePierce() {
        pierceLeft--;
        return pierceLeft <= 0;
    }

    /** Moves the projectile; returns false if it expired. */
    public boolean move(double dt) {
        if (!alive) return false;
        if (target != null && target.isAlive() && !target.isStored()) {
            Vec want = target.getPosition().sub(pos);
            double d = want.length();
            if (d > 1) {
                // gentle homing: blend current velocity with desired direction
                Vec desired = want.scale(speed / d);
                double k = Math.min(1, dt * 10);
                vel = new Vec(vel.x + (desired.x - vel.x) * k, vel.y + (desired.y - vel.y) * k).normalized().scale(speed);
            }
        } else if (target != null && !target.isAlive()) {
            target = null;
        }
        Vec step = vel.scale(dt);
        pos = pos.add(step);
        travelled += step.length();
        if (travelled > maxTravel) alive = false;
        // BUGFIX (found via Difficulty calibration): a zero-velocity clone never travels and would stall wave completion forever.
        if (step.length() < 1e-6 && (stalled += dt) > 1.0) alive = false;
        return alive;
    }
}
