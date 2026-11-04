package lindefense.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lindefense.combat.HitContext;
import lindefense.combat.HitSpec;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyGrid;
import lindefense.enemy.StatusEffect;
import lindefense.map.Path;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Tower;

/**
 * A GLM-style node network: a set of nodes joined by laser links, plus an optional slowing polygon.
 * <ul>
 *   <li><b>Links</b> damage an enemy that crosses them: an enemy whose centre is within {@link #linkWidth} px
 *       (+ its radius / 2) of a link takes {@link #lineHit}, at most once per {@link #crossCooldown} seconds per
 *       enemy, however many links it touches.</li>
 *   <li><b>Region</b>: enemies inside {@link #region} (a polygon, point-in-polygon test) are slowed by
 *       {@link #regionSlow} (refreshed each tick, strongest-slow rules apply).</li>
 * </ul>
 * Build with {@link #line} for two nodes or {@link #network} for N nodes (links join consecutive nodes plus a
 * closing link, and the region is the polygon of the nodes).
 */
public final class NodeHazard {
    public final Tower owner;
    public final List<Vec> nodes = new ArrayList<>();
    /** Index pairs into {@link #nodes}. */
    public final List<int[]> links = new ArrayList<>();
    public HitSpec lineHit;
    public double crossCooldown = 0.3;
    public double linkWidth = 6;
    public List<Vec> region;
    public double regionSlow;
    /** Seconds remaining (&le; 0 with {@link #permanent} = while owner lives). */
    public double duration;
    public boolean permanent = true;
    public boolean detection = true;
    public int rgb = 0xffffff;
    public String kind = "nodes";

    private final Map<Integer, Double> lastHit = new HashMap<>();
    private boolean dead;

    public NodeHazard(Tower owner) { this.owner = owner; }

    /** Two nodes joined by one laser link. */
    public static NodeHazard line(Tower owner, Vec a, Vec b, HitSpec hit, double crossCooldown) {
        NodeHazard h = new NodeHazard(owner);
        h.nodes.add(a); h.nodes.add(b); h.links.add(new int[] { 0, 1 });
        h.lineHit = hit; h.crossCooldown = crossCooldown;
        return h;
    }

    /** N nodes: consecutive links, a closing link when N &gt; 2, and (if {@code slow} &gt; 0) a slowing polygon region. */
    public static NodeHazard network(Tower owner, List<Vec> pts, HitSpec hit, double crossCooldown, double slow) {
        NodeHazard h = new NodeHazard(owner);
        h.nodes.addAll(pts);
        int n = pts.size();
        for (int i = 0; i + 1 < n; i++) h.links.add(new int[] { i, i + 1 });
        if (n > 2) h.links.add(new int[] { n - 1, 0 });
        h.lineHit = hit; h.crossCooldown = crossCooldown;
        if (slow > 0 && n >= 3) { h.region = new ArrayList<>(pts); h.regionSlow = slow; }
        return h;
    }

    public boolean isAlive() { return !dead; }
    public void kill() { dead = true; }

    /** Shortest distance from a point to any link. */
    public double distanceToLinks(Vec p) {
        double best = Double.MAX_VALUE;
        for (int[] l : links) best = Math.min(best, Path.pointSegmentDistance(p, nodes.get(l[0]), nodes.get(l[1])));
        return best;
    }

    /** Point-in-polygon (ray casting) for the slow region. */
    public static boolean polygonContains(List<Vec> poly, Vec p) {
        boolean in = false;
        for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
            Vec a = poly.get(i), b = poly.get(j);
            if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) in = !in;
        }
        return in;
    }

    void update(Game game, double dt) {
        if (dead) return;
        if (owner != null && !owner.isAlive()) { dead = true; return; }
        if (!permanent) {
            duration -= dt;
            if (duration <= 0) { dead = true; return; }
        }
        double now = game.getTime();
        // only the grid cells under the nodes / region (+ link width); a hit may move enemies, so use a scan
        double x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
        for (int pass = 0; pass < 2; pass++) {
            List<Vec> pts = pass == 0 ? nodes : region;
            if (pts == null) continue;
            for (int i = 0; i < pts.size(); i++) {
                Vec v = pts.get(i);
                x0 = Math.min(x0, v.x); y0 = Math.min(y0, v.y); x1 = Math.max(x1, v.x); y1 = Math.max(y1, v.y);
            }
        }
        if (x0 > x1) return;
        EnemyGrid.Scan scan = game.getEnemyManager().getGrid().scanBox(x0 - linkWidth, y0 - linkWidth, x1 + linkWidth, y1 + linkWidth);
        try {
            for (Enemy e = scan.next(); e != null; e = scan.next()) {
                if (!e.isAlive() || e.isStored()) continue;
                if (region != null && regionSlow > 0 && polygonContains(region, e.getPosition())) {
                    e.applyStatus(StatusEffect.Kind.SLOW, regionSlow, TrackZone.REFRESH);
                }
                if (lineHit == null || links.isEmpty()) continue;
                if (distanceToLinks(e.getPosition()) > linkWidth + e.getRadius() * 0.5) continue;
                Double last = lastHit.get(e.getId());
                if (last != null && now - last < crossCooldown) continue;
                lastHit.put(e.getId(), now);
                HitContext h = new HitContext(e, lineHit, owner);
                h.detection = detection;
                h.mult = game.getGlobalDamageMultiplier();
                game.getCombat().applyHit(h);
            }
        } finally {
            scan.close();
        }
        if (lastHit.size() > 400) lastHit.clear();
    }
}
