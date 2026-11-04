package lindefense.map;

import java.util.ArrayList;
import java.util.List;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Faction;
import lindefense.tower.Tower;

/**
 * Map 3: The Hallucination Canyon (Advanced).
 * A fragmented neural-network canyon. Two routes cross at a central attention
 * hub; bridge nodes link the fragments.
 *
 * Mechanics:
 * <ul>
 *   <li><b>Attention Drift</b>: the first time an enemy passes through the centre
 *       hub it has a 25% chance to teleport to a random other bridge node. The node
 *       may be ahead or behind, on either route; path progress is recomputed from
 *       the node's distance along its route.</li>
 *   <li><b>Noise Storm</b>: every {@link #STORM_INTERVAL} s a storm rages for
 *       {@link #STORM_DURATION} s and halves the range of every tower, unless a
 *       Logic Specialist (Military faction: DeepSeek or Copilot) is within
 *       {@link #SPECIALIST_RADIUS} px of that tower.</li>
 * </ul>
 */
public final class HallucinationCanyon extends GameMap {
    public static final double STORM_INTERVAL = 32;
    public static final double STORM_DURATION = 9;
    public static final double SPECIALIST_RADIUS = 140;
    public static final double DRIFT_CHANCE = 0.25;

    /** Bridge node: a position and the route/distance it sits on. */
    public static final class BridgeNode {
        public final Vec pos; public final int routeIndex; public final double distance;
        BridgeNode(Vec pos, int routeIndex, double distance) { this.pos = pos; this.routeIndex = routeIndex; this.distance = distance; }
    }

    private final Vec center = new Vec(600, 450);
    private final List<BridgeNode> nodes = new ArrayList<>();
    private double stormTimer = STORM_INTERVAL;
    private double stormRemaining = 0;

    public HallucinationCanyon() {
        super("The Hallucination Canyon", "Advanced",
              "A fractured neural canyon. Attention drifts at the hub; noise storms cloud every sensor.");
        routes.add(new Path("Dendrite Descent",
            600, 0, 600, 140, 440, 140, 440, 260, 600, 260, 600, 450, 600, 640, 760, 640, 760, 780, 600, 780, 600, 900));
        routes.add(new Path("Axon Crossing",
            0, 450, 160, 450, 160, 320, 380, 320, 380, 450, 600, 450, 820, 450, 820, 580, 1040, 580, 1040, 450, 1200, 450));
        addNode(600, 260, 0);
        addNode(600, 640, 0);
        addNode(380, 450, 1);
        addNode(820, 450, 1);
        // Chasms (non-buildable gaps)
        double[][] gaps = { {60, 60, 260, 180}, {880, 60, 260, 220}, {60, 660, 300, 200}, {900, 700, 240, 160}, {60, 300, 60, 120}, {1100, 240, 80, 160} };
        for (double[] g : gaps) {
            obstacles.add(new Obstacle(g[0], g[1], g[2], g[3], false, "Gap"));
            addDecoration(Decoration.rect(g[0], g[1], g[2], g[3], 0x0e0a1a));
            addDecoration(Decoration.frame(g[0], g[1], g[2], g[3], 0x3f2d6b));
        }
        for (BridgeNode n : nodes) {
            addDecoration(Decoration.oval(n.pos.x - 26, n.pos.y - 26, 52, 52, 0x3f2d6b, true));
            addDecoration(Decoration.oval(n.pos.x - 26, n.pos.y - 26, 52, 52, 0xb48cff, false));
        }
        addDecoration(Decoration.oval(center.x - 40, center.y - 40, 80, 80, 0x5a3fa0, true));
        addDecoration(Decoration.oval(center.x - 40, center.y - 40, 80, 80, 0xe0c8ff, false));
        addDecoration(Decoration.label(center.x - 48, center.y - 48, "Attention Hub", 0xe0c8ff));
        // "Synapse" dots
        for (int i = 0; i < 60; i++) {
            double x = 30 + (i * 211) % 1140, y = 30 + (i * 157) % 840;
            addDecoration(Decoration.oval(x, y, 4, 4, 0x4a3a7a, true));
        }
    }

    private void addNode(double x, double y, int route) {
        Vec p = new Vec(x, y);
        nodes.add(new BridgeNode(p, route, routes.get(route).nearestDistanceAlong(p)));
    }

    @Override public int getBackgroundRgb() { return 0x1b1430; }
    @Override public int getTrackRgb() { return 0x8f7fcf; }
    @Override public String getMechanicSummary() {
        return "Attention Drift: 25% of enemies crossing the hub teleport to another bridge node. Noise Storms halve tower range unless a Logic Specialist is nearby.";
    }

    public boolean isStormActive() { return stormRemaining > 0; }
    public double getStormRemaining() { return stormRemaining; }
    public List<BridgeNode> getNodes() { return nodes; }
    public Vec getCenter() { return center; }

    @Override public void reset() { stormTimer = STORM_INTERVAL; stormRemaining = 0; }

    @Override public void update(Game game, double dt) {
        if (stormRemaining > 0) {
            stormRemaining -= dt;
        } else {
            stormTimer -= dt;
            if (stormTimer <= 0) {
                stormTimer = STORM_INTERVAL;
                stormRemaining = STORM_DURATION;
                game.emitText(center, "Noise Storm!", 0xff8cf0);
            }
        }
    }

    @Override public void onEnemyMoved(Game game, Enemy e) {
        if (e.hasDrifted() || e.isStored()) return;
        if (e.getPosition().dist(center) <= 18) {
            e.setDrifted(true);
            if (game.getRng().chance(DRIFT_CHANCE)) {
                BridgeNode n = nodes.get(game.getRng().nextInt(nodes.size()));
                e.teleportTo(routes.get(n.routeIndex), n.routeIndex, n.distance);
                game.emitTeleport(e.getPosition());
            }
        }
    }

    @Override public double towerRangeMultiplier(Game game, Tower t) {
        if (stormRemaining <= 0) return 1.0;
        if (t == null) return 0.5; // placement preview (no tower yet): show the storm penalty
        for (Tower o : game.getTowerManager().getTowers()) {
            if (o.getType().faction == Faction.MILITARY && o.getPosition().dist(t.getPosition()) <= SPECIALIST_RADIUS) return 1.0;
        }
        return 0.5;
    }

    @Override public String getEventText() {
        return stormRemaining > 0 ? String.format("Noise Storm: tower range halved (%.0fs)", stormRemaining) : null;
    }
}
