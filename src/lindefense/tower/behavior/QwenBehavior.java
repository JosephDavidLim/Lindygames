package lindefense.tower.behavior;

import lindefense.combat.DamageType;
import lindefense.combat.HitSpec;
import lindefense.combat.VisualEvent;
import lindefense.effects.Drone;
import lindefense.enemy.Boss;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyType;
import lindefense.map.Path;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * Qwen (spec 3.6 #14). Multi-threaded discs (3 per attack at U1) and Sovereign Processing's x3 vs HP &ge; 5 are
 * data-driven. Cosmic Sovereign Burst is implemented (it demonstrates {@code Enemy.stripFortified} and
 * {@code Boss.disablePhaseShield}); Drone Formation (U2) keeps patrol drones on other track segments.
 */
public final class QwenBehavior extends TowerBehavior {
    private static final String DRONES = "qwenDrones";
    private static final String TIER = "qwenDroneTier";

    /** Drone Formation (U2): keeps {@code droneCount} patrol drones alive on track segments away from the tower. */
    @Override public void update(Game game, Tower t, double dt) {
        TowerStats s = t.getStats();
        if (!s.has("droneCount")) return;
        int n = s.pi("droneCount");
        Drone[] ds = (Drone[]) t.state().get(DRONES);
        if (ds == null || ds.length != n) {
            Drone[] bigger = new Drone[n];
            if (ds != null) System.arraycopy(ds, 0, bigger, 0, Math.min(ds.length, n));
            ds = bigger;
            t.state().put(DRONES, ds);
        }
        boolean retier = t.counter(TIER) != t.getTier() + 1;
        TowerStats dstats = null;
        for (int i = 0; i < n; i++) {
            Drone d = ds[i];
            if (d == null || !d.isAlive()) {
                d = spawn(game, t, i, n);
                ds[i] = d;
            } else if (retier) {
                if (dstats == null) dstats = droneStats(t);
                d.stats = dstats;
            }
        }
        if (retier) t.setCounter(TIER, t.getTier() + 1);
    }

    /** "Same disc stats" as the tower at its current tier, with the drone interval. */
    private static TowerStats droneStats(Tower t) {
        TowerStats ds = t.getStats().copy();
        ds.interval(t.getStats().p("droneInterval"));
        return ds;
    }

    /** Spawns drone {@code index} of {@code count} on a segment away from the tower, spread evenly over the candidates. */
    private static Drone spawn(Game game, Tower t, int index, int count) {
        TowerStats s = t.getStats();
        double seg = s.p("droneSegment");
        java.util.List<Path> routes = game.getMap().getRoutes();
        for (int pass = 0; pass < 2; pass++) {
            boolean outsideOnly = pass == 0;
            int total = 0;
            for (Path r : routes) total += countSegments(r, seg, t, outsideOnly);
            if (total == 0) continue;
            int pick = Math.min(total - 1, (int) ((index + 0.5) * total / count));
            for (Path r : routes) {
                int c = countSegments(r, seg, t, outsideOnly);
                if (pick >= c) { pick -= c; continue; }
                for (double from = 0; from + seg <= r.getTotalLength(); from += seg) {
                    if (outsideOnly && !outside(r, from, seg, t)) continue;
                    if (pick-- > 0) continue;
                    Drone d = Drone.patrol(t, droneStats(t), r, from, from + seg, s.p("droneSpeed"));
                    d.kind = "qwen";
                    d.rgb = t.getType().primaryRgb;
                    game.getEffects().addDrone(d);
                    return d;
                }
            }
        }
        // degenerate map: a route shorter than one segment
        Path r = routes.get(0);
        Drone d = Drone.patrol(t, droneStats(t), r, 0, r.getTotalLength(), s.p("droneSpeed"));
        d.kind = "qwen";
        d.rgb = t.getType().primaryRgb;
        game.getEffects().addDrone(d);
        return d;
    }

    private static boolean outside(Path r, double from, double seg, Tower t) {
        return r.positionAt(from + seg / 2).dist(t.getPosition()) > t.getStats().range + seg / 2;
    }

    private static int countSegments(Path r, double seg, Tower t, boolean outsideOnly) {
        int c = 0;
        for (double from = 0; from + seg <= r.getTotalLength(); from += seg) if (!outsideOnly || outside(r, from, seg, t)) c++;
        return c;
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        HitSpec h = new HitSpec(a.p("damage"), DamageType.ENERGY);
        for (Enemy e : new java.util.ArrayList<>(game.getEnemyManager().getEnemies())) {
            if (!e.isAlive() || e.isStored()) continue;
            if (e.isBoss() && e.getType() != EnemyType.PALANTIR) e.stripFortified();
            if (e instanceof Boss && e.getType() == EnemyType.PALANTIR) ((Boss) e).disablePhaseShield(a.p("palantirShieldOff"));
            game.getCombat().applyHit(e, h, t, 1, 0);
        }
        game.getCombat().emit(VisualEvent.banner("SOVEREIGN BURST", t.getType().primaryRgb, 1.2));
        return true;
    }
}
