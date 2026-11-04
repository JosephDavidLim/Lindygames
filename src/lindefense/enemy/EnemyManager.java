package lindefense.enemy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lindefense.combat.VisualEvent;
import lindefense.map.Path;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Tower;

/**
 * Owns every live enemy. Handles spawning, movement, status upkeep, the
 * special mechanics that need neighbour information (swaps, jumps, shields,
 * buckets), leaks, deaths, rewards and splitting. Additions during iteration go
 * through a pending list so no ConcurrentModificationException can occur.
 *
 * <h3>Precise mechanic rules</h3>
 * <ul>
 *   <li>Bubble Sort: every 3 s, swaps distance with the nearest enemy ahead within 45 px on its route.</li>
 *   <li>Selection Sort: every 2.5 s, if any enemy is ahead within 140 px, jumps 6 px in front of the furthest one.</li>
 *   <li>Insertion Sort: every 2 s, leapfrogs 6 px past the nearest enemy ahead within 70 px.</li>
 *   <li>Shell Sort: every 3 s, jumps forward by its current gap (140, 70, 35, then 18 px forever).</li>
 *   <li>Heap Sort: enemies on its route from 90 px behind it up to its position take 50% damage (not itself).</li>
 *   <li>Bucket Sort: every 1 s stores non-boss enemies of base HP &le; 5 within 30 px (max 6). Stored enemies are
 *       off the track until the bucket dies (released at its position, keeping their HP) or leaks (they leak too).</li>
 *   <li>Disrupted enemies perform none of the above; their timers keep running.</li>
 *   <li>Splitting: children spawn at the parent's distance (staggered 6 px behind each other), on the parent's route,
 *       with the parent's HP/speed scale. Camo and Regrow are inherited; Fortified is inherited only from bosses.</li>
 *   <li>Rewards: each entity pays its own reward exactly once, when its own body is destroyed. Leaks pay nothing.
 *       Rewards are multiplied by {@link lindefense.wave.WaveData#rewardScale(int)}.</li>
 * </ul>
 */
public class EnemyManager {
    private final Game game;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Enemy> pending = new ArrayList<>();
    private final Map<Path, List<Enemy>> byRoute = new HashMap<>();
    private int totalKilled;
    private int totalLeaked;
    private int totalSpawned;

    public EnemyManager(Game game) { this.game = game; }

    /** Active (on-track, alive) enemies. Do not modify. */
    public List<Enemy> getEnemies() { return enemies; }
    public int getTotalKilled() { return totalKilled; }
    public int getTotalLeaked() { return totalLeaked; }
    public int getTotalSpawned() { return totalSpawned; }
    public boolean isEmpty() { return enemies.isEmpty() && pending.isEmpty(); }
    public int getAliveCount() {
        int n = enemies.size() + pending.size();
        for (Enemy e : enemies) n += e.getStoredEnemies().size();
        return n;
    }

    /** Spawns an enemy of the given type at the start of a route (routeIndex &lt; 0 = random). */
    public Enemy spawn(EnemyType type, EnumSet<Modifier> mods, int routeIndex, double distance) {
        return spawnScaled(type, mods, routeIndex, distance, game.getWaveHpScale(), game.getWaveSpeedScale());
    }

    public Enemy spawnScaled(EnemyType type, EnumSet<Modifier> mods, int routeIndex, double distance,
                             double hpScale, double speedScale) {
        List<Path> routes = game.getMap().getRoutes();
        int idx = routeIndex < 0 || routeIndex >= routes.size() ? game.getMap().pickRoute(game) : routeIndex;
        Path route = routes.get(idx);
        Enemy e = type.isBoss()
            ? new Boss(type, mods, route, idx, distance, hpScale, speedScale)
            : new Enemy(type, mods, route, idx, distance, hpScale, speedScale);
        e.setSpawnTime(game.getTime());
        pending.add(e);
        totalSpawned++;
        return e;
    }

    public void clear() { enemies.clear(); pending.clear(); byRoute.clear(); }

    // ------------------------------------------------------------------

    public void update(double dt) {
        flushPending();
        double now = game.getTime();
        // 1. statuses, DOT, regrow, boss logic
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            double dot = e.tickStatuses(dt);
            if (dot > 0) e.takeDamage(dot, now);
            if (e instanceof Boss) ((Boss) e).updateBoss(game, dt);
        }
        // 2. movement + leaks
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.isAlive()) continue;
            double ext = game.getMap().enemySpeedMultiplier(e) * game.getGlobalEnemySpeedMultiplier();
            boolean reached = e.advance(dt, ext);
            if (reached) {
                leak(e);
            } else {
                game.getMap().onEnemyMoved(game, e);
            }
        }
        // 3. neighbour-based mechanics
        rebuildRouteIndex();
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.isAlive() || e.isStored()) continue;
            if (e.getType().mechanic != Mechanic.NONE && e.getType().mechanic != Mechanic.PALANTIR) runMechanic(e, dt);
        }
        // 4. deaths
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.isAlive() && e.getHp() <= 0) kill(e);
        }
        // 5. sweep
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (!e.isAlive() || e.isStored()) {
                int last = enemies.size() - 1;
                enemies.set(i, enemies.get(last));
                enemies.remove(last);
            }
        }
        flushPending();
    }

    private void flushPending() {
        if (!pending.isEmpty()) { enemies.addAll(pending); pending.clear(); }
    }

    private void rebuildRouteIndex() {
        for (List<Enemy> l : byRoute.values()) l.clear();
        for (Enemy e : enemies) {
            if (!e.isAlive() || e.isStored()) continue;
            byRoute.computeIfAbsent(e.getRoute(), k -> new ArrayList<>()).add(e);
        }
        for (List<Enemy> l : byRoute.values()) l.sort(Comparator.comparingDouble(Enemy::getDistance));
    }

    private List<Enemy> routeMates(Enemy e) {
        List<Enemy> l = byRoute.get(e.getRoute());
        return l == null ? new ArrayList<>() : l;
    }

    private void runMechanic(Enemy e, double dt) {
        Mechanic m = e.getType().mechanic;
        if (m == Mechanic.HEAP_SHIELD) {
            if (e.isDisrupted()) return;
            for (Enemy o : routeMates(e)) {
                if (o == e) continue;
                double gap = e.getDistance() - o.getDistance();
                if (gap > 0 && gap <= 90) o.setShieldedTick(game.getTick());
            }
            return;
        }
        if (m == Mechanic.ACCELERATE_ON_DAMAGE || m == Mechanic.RADIX_PHASES) return; // handled in Enemy
        e.setMechanicTimer(e.getMechanicTimer() + dt);
        double period = m == Mechanic.BUBBLE_SWAP ? 3.0 : m == Mechanic.SELECTION_FRONT ? 2.5
            : m == Mechanic.INSERTION_LEAPFROG ? 2.0 : m == Mechanic.SHELL_GAP_JUMP ? 3.0 : 1.0;
        if (e.getMechanicTimer() < period) return;
        e.setMechanicTimer(0);
        if (e.isDisrupted() || e.isStunned()) return;
        switch (m) {
            case BUBBLE_SWAP: {
                Enemy ahead = nearestAhead(e, 45);
                if (ahead != null) {
                    double d = ahead.getDistance();
                    ahead.setDistance(e.getDistance());
                    e.setDistance(d);
                    game.getEvents().add(VisualEvent.split(e.getPosition(), 14, 0x9be7ff));
                }
                break;
            }
            case SELECTION_FRONT: {
                double best = -1;
                for (Enemy o : routeMates(e)) {
                    double gap = o.getDistance() - e.getDistance();
                    if (o != e && gap > 0 && gap <= 140 && o.getDistance() > best) best = o.getDistance();
                }
                if (best >= 0) { e.setDistance(best + 6); game.getEvents().add(VisualEvent.teleport(e.getPosition())); }
                break;
            }
            case INSERTION_LEAPFROG: {
                Enemy ahead = nearestAhead(e, 70);
                if (ahead != null) { e.setDistance(ahead.getDistance() + 6); game.getEvents().add(VisualEvent.split(e.getPosition(), 10, 0xffe066)); }
                break;
            }
            case SHELL_GAP_JUMP: {
                double gap = e.currentShellGap();
                Vec from = e.getPosition();
                e.shiftDistance(gap);
                e.setShellJumpIndex(e.getShellJumpIndex() + 1);
                game.getEvents().add(VisualEvent.chain(from, e.getPosition(), 0xc2b280));
                break;
            }
            case BUCKET_STORE: {
                if (e.getStoredEnemies().size() >= 6) break;
                for (Enemy o : routeMates(e)) {
                    if (o == e || !o.isAlive() || o.isStored() || o.isBoss()) continue;
                    if (o.getType().baseHp > 5 || o.getType().mechanic == Mechanic.BUCKET_STORE) continue;
                    if (Math.abs(o.getDistance() - e.getDistance()) <= 30) {
                        o.setStored(true);
                        e.getStoredEnemies().add(o);
                        game.getEvents().add(VisualEvent.split(o.getPosition(), 12, 0x8ad8ff));
                        if (e.getStoredEnemies().size() >= 6) break;
                    }
                }
                break;
            }
            default: break;
        }
    }

    private Enemy nearestAhead(Enemy e, double maxGap) {
        Enemy best = null;
        double bestGap = maxGap;
        for (Enemy o : routeMates(e)) {
            if (o == e) continue;
            double gap = o.getDistance() - e.getDistance();
            if (gap > 0 && gap <= bestGap) { bestGap = gap; best = o; }
        }
        return best;
    }

    // ------------------------------------------------------------------

    private void leak(Enemy e) {
        if (!e.isAlive()) return;
        int lives = e.getType().lives;
        for (Enemy s : e.getStoredEnemies()) { lives += s.getType().lives; s.markLeaked(); }
        e.markLeaked();
        totalLeaked++;
        game.loseLives(lives, e);
    }

    /** Destroys the enemy's body: pays its reward and releases children / stored enemies. */
    public void kill(Enemy e) {
        if (!e.isAlive()) return;
        e.markDead();
        totalKilled++;
        Object src = e.getLastHitBy();
        if (src instanceof Tower) ((Tower) src).addKill();
        double reward = Math.round(e.getType().reward * game.getRewardScale());
        if (reward > 0) game.addCash(reward, e.getPosition());
        game.onEnemyKilled(e);

        EnemyType t = e.getType();
        if (t == EnemyType.PALANTIR) return; // victory sequence; no children
        EnumSet<Modifier> inherited = EnumSet.noneOf(Modifier.class);
        if (e.has(Modifier.CAMO)) inherited.add(Modifier.CAMO);
        if (e.has(Modifier.REGROW)) inherited.add(Modifier.REGROW);
        if (e.has(Modifier.FORTIFIED) && e.isBoss()) inherited.add(Modifier.FORTIFIED);
        int k = 0;
        EnemyType[] ct = t.getChildTypes();
        int[] cc = t.getChildCounts();
        for (int i = 0; i < ct.length; i++) {
            for (int j = 0; j < cc[i]; j++) {
                double d = e.getDistance() - 6 * k++;
                Enemy child = spawnScaled(ct[i], inherited, e.getRouteIndex(), Math.max(0, d), e.getHpScale(), e.getSpeedScale());
                child.setDrifted(e.hasDrifted());
            }
        }
        if (ct.length > 0) game.getEvents().add(VisualEvent.split(e.getPosition(), e.getRadius() * 1.6, 0xffffff));
        else game.getEvents().add(VisualEvent.death(e.getPosition(), e.getRadius(), 0xffffff));
        if (!e.getStoredEnemies().isEmpty()) {
            int i = 0;
            for (Enemy s : e.getStoredEnemies()) {
                s.setStored(false);
                s.teleportTo(e.getRoute(), e.getRouteIndex(), Math.max(0, e.getDistance() - 8 * i++));
                pending.add(s);
            }
            e.getStoredEnemies().clear();
        }
    }

    /** Debug/test helper: counts enemies of a type (active + pending + stored). */
    public int countOfType(EnemyType t) {
        int n = 0;
        for (Enemy e : enemies) { if (e.getType() == t) n++; for (Enemy s : e.getStoredEnemies()) if (s.getType() == t) n++; }
        for (Enemy e : pending) if (e.getType() == t) n++;
        return n;
    }
}
