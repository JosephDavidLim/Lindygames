package lindefense.effects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lindefense.combat.Combat;
import lindefense.combat.HitContext;
import lindefense.combat.HitSpec;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyGrid;
import lindefense.map.Path;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Tower;

/**
 * A summon that walks along a route between two track distances, forwards or backwards, dealing contact damage
 * to the enemies it touches. Used by DALL-E illustrated creatures (walk backward), Llama charges, and the Open
 * Titan Stampede herd (exit to entrance).
 *
 * <ul>
 *   <li>Contact: an enemy is touched when the distance between centres is at most {@code radius + enemy.radius}.</li>
 *   <li>{@link #pierce}: the number of DISTINCT enemies it can hit before it expires.</li>
 *   <li>{@link #perEnemyCooldown}: 0 hits each enemy once; &gt; 0 lets the walker hit the same enemy again after that
 *       many seconds (herds).</li>
 *   <li>{@link #bossHit}: optional alternative spec used against bosses (Stampede: 60 dmg + 50 px knockback).</li>
 * </ul>
 * The walker is removed when it reaches its end distance, runs out of pierce, or its lifetime elapses.
 */
public final class TrackWalker {
    public final Path route;
    public double distance;
    public final double endDistance;
    /** Speed in px/s along the route. */
    public final double speed;
    public final Tower owner;
    public HitSpec hit;
    public HitSpec bossHit;
    public double radius = 14;
    public int pierce = 1;
    public double perEnemyCooldown;
    /** Seconds before it disappears regardless (&le; 0 = unlimited). */
    public double lifetime;
    public boolean detection;
    /** B3 engine addition: when true, undetected Camo enemies are not touched (Llama before Illusion Pack). */
    public boolean respectCamo;
    /** Damage multiplier (usually 1: walker damage comes from its spec). */
    public double damageMult = 1;
    public int rgb = 0xffffff;
    /** Free-form label for the renderer / tests ("creature", "llama", "herd"). */
    public String kind = "walker";

    private final Set<Integer> hitOnce = new HashSet<>();
    private final Map<Integer, Double> lastHit = new HashMap<>();
    private boolean dead;
    private double age;

    public TrackWalker(Path route, double startDistance, double endDistance, double speed, Tower owner, HitSpec hit) {
        this.route = route; this.distance = startDistance; this.endDistance = endDistance;
        this.speed = speed; this.owner = owner; this.hit = hit;
    }

    public Vec getPosition() { return route.positionAt(distance); }
    public boolean isForward() { return endDistance >= distance; }
    public boolean isAlive() { return !dead; }
    public int getDistinctHits() { return hitOnce.size(); }
    public void kill() { dead = true; }

    void update(Game game, double dt) {
        if (dead) return;
        age += dt;
        if (lifetime > 0 && age >= lifetime) { dead = true; return; }
        double dir = endDistance >= distance ? 1 : -1;
        distance += dir * speed * dt;
        boolean finished = dir > 0 ? distance >= endDistance : distance <= endDistance;
        if (finished) distance = endDistance;
        Vec pos = getPosition();
        Combat combat = game.getCombat();
        // only the grid cells around the walker; hits may knock enemies around, so use a self-refreshing scan
        EnemyGrid.Scan scan = game.getEnemyManager().getGrid().scan(pos.x, pos.y, radius);
        try {
            for (Enemy e = scan.next(); e != null && !dead; e = scan.next()) {
                if (!e.isAlive() || e.isStored()) continue;
                if (respectCamo && !detection && e.isCamo() && !e.isRevealed()) continue; // B3 engine addition
                double rr = radius + e.getRadius();
                if (e.getPosition().distSq(pos) > rr * rr) continue;
                int id = e.getId();
                boolean first = !hitOnce.contains(id);
                if (!first) {
                    if (perEnemyCooldown <= 0) continue;
                    Double last = lastHit.get(id);
                    if (last != null && game.getTime() - last < perEnemyCooldown) continue;
                } else if (hitOnce.size() >= pierce) {
                    continue;
                }
                hitOnce.add(id);
                lastHit.put(id, game.getTime());
                HitContext h = new HitContext(e, e.isBoss() && bossHit != null ? bossHit : hit, owner);
                h.mult = damageMult * (game.getGlobalDamageMultiplier()); h.detection = detection;
                combat.applyHit(h);
                if (hitOnce.size() >= pierce && perEnemyCooldown <= 0) dead = true;
            }
        } finally {
            scan.close();
        }
        if (finished) dead = true;
    }
}
