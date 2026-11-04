package lindefense.test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import lindefense.combat.DamageType;
import lindefense.combat.HitSpec;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyType;
import lindefense.enemy.Modifier;
import lindefense.map.MapCatalog;
import lindefense.map.Path;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Tower;
import lindefense.tower.TowerType;

/**
 * Shared helpers for the headless test classes: the pass/fail counters and {@link #check}, plus builders for
 * games, towers and enemies. Test conventions: each group is a class with a static {@code runAll()} that calls
 * {@code run("name", Class::method)}; use {@code check(cond, message)} for assertions; build scenes with
 * {@link #newGame}, {@link #towerNearTrack} and {@link #spawnAt} on a frozen game
 * ({@code g.setGlobalEnemySpeedMultiplier(0)}) so positions are exact.
 */
public final class TestKit {
    private static int passed, failed;
    private static final List<String> failures = new ArrayList<>();
    private static String current = "";

    private TestKit() { }

    // ------------------------------------------------------------------ infra
    /** Runs one named test, catching exceptions as failures. */
    public static void run(String name, Runnable r) {
        current = name;
        int before = failed;
        try {
            r.run();
        } catch (Throwable t) {
            failed++;
            failures.add(name + ": exception " + t);
            t.printStackTrace(System.out);
        }
        System.out.println((failed == before ? "PASS " : "FAIL ") + name);
    }

    /** Records an assertion. */
    public static void check(boolean cond, String msg) {
        if (cond) passed++;
        else { failed++; failures.add(current + ": " + msg); }
    }

    public static int passed() { return passed; }
    public static int failed() { return failed; }
    public static List<String> failures() { return failures; }

    // ------------------------------------------------------------------ builders
    public static Game newGame(int mapIdx) { return new Game(MapCatalog.create(mapIdx), 42); }

    /** Finds a buildable point within radius of the target. */
    public static Vec buildableNear(Game g, Vec target, double radius) {
        for (double r = 0; r <= radius; r += 10) {
            for (int a = 0; a < 24; a++) {
                double ang = a * Math.PI / 12;
                Vec p = new Vec(target.x + Math.cos(ang) * r, target.y + Math.sin(ang) * r);
                if (g.getMap().isBuildable(p, 20) && g.getTowerManager().placementProblem(TowerType.GPT, p) == null
                    || g.getMap().isBuildable(p, 20) && "Not enough cash".equals(g.getTowerManager().placementProblem(TowerType.GPT, p))) return p;
            }
        }
        throw new IllegalStateException("No buildable point near " + target);
    }

    /** Places a tower with unlimited cash and buys {@code tier} upgrades. */
    public static Tower forcePlace(Game g, TowerType type, Vec p, int tier) {
        g.addCash(1_000_000, null);
        Tower t = g.getTowerManager().place(type, p);
        if (t == null) throw new IllegalStateException("Could not place " + type + " at " + p + ": " + g.getTowerManager().placementProblem(type, p));
        for (int i = 0; i < tier; i++) g.getTowerManager().upgrade(t);
        return t;
    }

    /** Places a tower about 70 px beside route 0 at the given track distance. */
    public static Tower towerNearTrack(Game g, TowerType type, int tier, double trackDistance) {
        Path r = g.getMap().getRoutes().get(0);
        Vec p = buildableNear(g, r.positionAt(trackDistance).add(new Vec(0, -70)), 120);
        return forcePlace(g, type, p, tier);
    }

    /** Spawns an enemy on a route and flushes it into the active list without moving anything. */
    public static Enemy spawnAt(Game g, EnemyType type, EnumSet<Modifier> mods, int route, double dist) {
        Enemy e = g.getEnemyManager().spawn(type, mods, route, dist);
        g.getEnemyManager().update(0);
        return e;
    }

    /** Destroys an enemy through the normal damage path (pays reward, spawns children). */
    public static void killOutright(Game g, Enemy e) {
        g.getCombat().applyHit(e, new HitSpec(1e9, DamageType.TRUE), null, 1, 0);
        g.getEnemyManager().update(0);
    }

    /** Runs the simulation for the given game seconds. */
    public static void runSeconds(Game g, double seconds) { g.run((int) Math.round(seconds / Game.DT)); }

    /** Convenience: a hit spec of the given damage and type. */
    public static HitSpec hit(double dmg, DamageType type) { return new HitSpec(dmg, type); }

    /** Applies a hit with no tower and returns the HP removed. */
    public static double hitFor(Game g, Enemy e, double dmg, DamageType type) {
        return g.getCombat().applyHit(e, new HitSpec(dmg, type), null, 1, 0);
    }
}
