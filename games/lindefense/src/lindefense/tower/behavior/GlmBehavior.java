package lindefense.tower.behavior;

import java.util.ArrayList;
import java.util.List;
import lindefense.combat.DamageType;
import lindefense.combat.HitSpec;
import lindefense.effects.Effects;
import lindefense.effects.NodeHazard;
import lindefense.enemy.Enemy;
import lindefense.enemy.StatusEffect;
import lindefense.map.Path;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * GLM (spec 3.6 #18). Gravity-pebble projectiles (GRAVITY slows on hit) are the data-driven base; Matrix
 * Overload stuns the track. The node network (U1-U3) is a {@link NodeHazard} rebuilt whenever the tier changes:
 * the tower picks the stretch of track nearest to it that lies inside its range and places the nodes on both
 * sides of the track along it ({@code nodeSpread} px off the centre line). Two nodes make a single laser line
 * across the track; 4 and 6 nodes make a ladder whose perimeter and rungs damage crossing enemies (each enemy at
 * most once per {@code lineCooldown}) and whose interior slows by {@code gridSlow}.
 */
public final class GlmBehavior extends TowerBehavior {
    private static final String NET = "net";
    private static final String NET_TIER = "netTier";
    private static final double SCAN_STEP = 10;

    @Override public void onPlaced(Game game, Tower t) { build(game, t); }

    @Override public void onUpgraded(Game game, Tower t) { build(game, t); }

    @Override public void update(Game game, Tower t, double dt) {
        if (!t.getStats().has("nodes")) return;
        NodeHazard cur = (NodeHazard) t.state().get(NET);
        if (cur != null && cur.isAlive() && t.counter(NET_TIER) == t.getTier() + 1) return;
        if (t.periodic("netRetry", 0.25, dt)) build(game, t);
    }

    private static void build(Game game, Tower t) {
        TowerStats s = t.getStats();
        NodeHazard old = (NodeHazard) t.state().remove(NET);
        if (old != null) old.kill();
        t.setCounter(NET_TIER, 0);
        if (!s.has("nodes")) return;
        double range = t.effectiveRange(game);
        Effects.TrackPoint c = game.getEffects().nearestTrackPoint(t.getPosition());
        if (c == null || c.position.dist(t.getPosition()) > range) return;
        Path r = c.route;
        Vec tp = t.getPosition();
        double lo = c.distance, hi = c.distance;
        while (lo - SCAN_STEP >= 0 && r.positionAt(lo - SCAN_STEP).dist(tp) <= range) lo -= SCAN_STEP;
        while (hi + SCAN_STEP <= r.getTotalLength() && r.positionAt(hi + SCAN_STEP).dist(tp) <= range) hi += SCAN_STEP;

        HitSpec h = new HitSpec(s.p("lineDamage"), DamageType.GRAVITY);
        h.multVsFortified = s.p("lineFortifiedMult", 1);
        double cd = s.p("lineCooldown"), spread = s.p("nodeSpread");
        int n = s.pi("nodes");
        NodeHazard net;
        if (n <= 2) {
            double d = (lo + hi) / 2;
            Vec[] ends = sides(r, d, spread);
            net = NodeHazard.line(t, ends[0], ends[1], h, cd);
        } else {
            int k = n / 2;
            List<Vec> left = new ArrayList<>(k), right = new ArrayList<>(k);
            for (int i = 0; i < k; i++) {
                double d = k == 1 ? (lo + hi) / 2 : lo + (hi - lo) * i / (k - 1);
                Vec[] ends = sides(r, d, spread);
                left.add(ends[0]); right.add(ends[1]);
            }
            List<Vec> poly = new ArrayList<>(n);
            poly.addAll(left);
            for (int i = k - 1; i >= 0; i--) poly.add(right.get(i));
            net = NodeHazard.network(t, poly, h, cd, s.p("gridSlow", 0));
            for (int i = 1; i < k - 1; i++) net.links.add(new int[] { i, n - 1 - i }); // interior rungs
        }
        net.kind = "gravity grid";
        net.rgb = t.getType().primaryRgb;
        game.getEffects().addHazard(net);
        t.state().put(NET, net);
        t.setCounter(NET_TIER, t.getTier() + 1);
    }

    /** The two points {@code spread} px either side of the track centre line at distance {@code d}. */
    private static Vec[] sides(Path r, double d, double spread) {
        Vec p = r.positionAt(d), dir = r.directionAt(d);
        Vec nrm = new Vec(-dir.y, dir.x).scale(spread);
        return new Vec[] { p.add(nrm), p.sub(nrm) };
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        for (Enemy e : game.getEnemyManager().getEnemies()) {
            if (!e.isAlive() || e.isStored()) continue;
            if (e.isBoss()) e.applyStatus(StatusEffect.Kind.STUN, 1, a.p("bossStun"), true);
            else e.applyStatus(StatusEffect.Kind.STUN, 1, a.p("stun"), false);
        }
        return true;
    }
}
