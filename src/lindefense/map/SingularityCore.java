package lindefense.map;

import java.util.ArrayList;
import java.util.List;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Tower;

/**
 * Map 4: The Singularity Core (Expert).
 * A circular arena with four entry portals. Each portal's route spirals inward
 * to the central vortex, which is the exit.
 *
 * Mechanics:
 * <ul>
 *   <li><b>Gravitational Pull</b>: enemy speed is multiplied by
 *       1 + 0.6 * (1 - r / R) where r is the distance to the centre.</li>
 *   <li><b>Event Horizon Collapse</b>: {@link #COLLAPSE_DELAY} s after Wave 50
 *       begins, every tower ability cooldown is reset and every live enemy is
 *       scattered to a random portal route at the same fractional progress.</li>
 * </ul>
 */
public final class SingularityCore extends GameMap {
    public static final double ARENA_RADIUS = 430;
    public static final double COLLAPSE_DELAY = 15;
    public static final int COLLAPSE_WAVE = 50;

    private final Vec center = new Vec(600, 450);
    private boolean collapsePending;
    private double collapseTimer;
    private boolean collapsed;
    private String banner;
    private double bannerTime;

    public SingularityCore() {
        super("The Singularity Core", "Expert",
              "Four portals feed a central vortex. Gravity accelerates everything that falls inward.");
        String[] names = { "North Portal", "East Portal", "South Portal", "West Portal" };
        for (int i = 0; i < 4; i++) {
            routes.add(new Path(names[i], spiral(-90 + 90 * i)));
        }
        addDecoration(Decoration.oval(center.x - ARENA_RADIUS, center.y - ARENA_RADIUS, ARENA_RADIUS * 2, ARENA_RADIUS * 2, 0x1a1030, true));
        addDecoration(Decoration.oval(center.x - ARENA_RADIUS, center.y - ARENA_RADIUS, ARENA_RADIUS * 2, ARENA_RADIUS * 2, 0x6a4fcf, false));
        for (double r = 360; r > 60; r -= 110) {
            addDecoration(Decoration.oval(center.x - r, center.y - r, r * 2, r * 2, 0x2c2050, false));
        }
        addDecoration(Decoration.oval(center.x - 48, center.y - 48, 96, 96, 0x000000, true));
        addDecoration(Decoration.oval(center.x - 48, center.y - 48, 96, 96, 0xb090ff, false));
        addDecoration(Decoration.oval(center.x - 30, center.y - 30, 60, 60, 0x40208a, false));
        for (Path p : routes) {
            Vec s = p.getStart();
            addDecoration(Decoration.oval(s.x - 24, s.y - 24, 48, 48, 0x4020a0, true));
            addDecoration(Decoration.oval(s.x - 24, s.y - 24, 48, 48, 0xc0a0ff, false));
        }
    }

    private List<Vec> spiral(double startDeg) {
        List<Vec> pts = new ArrayList<>();
        double a = Math.toRadians(startDeg);
        pts.add(polar(ARENA_RADIUS, a));
        pts.add(polar(360, a));
        double ang = startDeg;
        for (int k = 1; k <= 10; k++) pts.add(polar(360, Math.toRadians(ang + k * 10)));
        ang += 100;
        pts.add(polar(250, Math.toRadians(ang)));
        for (int k = 1; k <= 10; k++) pts.add(polar(250, Math.toRadians(ang + k * 10)));
        ang += 100;
        pts.add(polar(140, Math.toRadians(ang)));
        for (int k = 1; k <= 8; k++) pts.add(polar(140, Math.toRadians(ang + k * 10)));
        pts.add(center);
        return pts;
    }

    private Vec polar(double r, double angRad) {
        return new Vec(center.x + r * Math.cos(angRad), center.y + r * Math.sin(angRad));
    }

    @Override public int getBackgroundRgb() { return 0x07050e; }
    @Override public int getTrackRgb() { return 0x7a5fe0; }
    @Override public String getMechanicSummary() {
        return "Gravitational Pull speeds enemies up near the core. Event Horizon Collapse at Wave 50 resets ability cooldowns and scatters enemies across all portals.";
    }

    public Vec getCenter() { return center; }
    public boolean hasCollapsed() { return collapsed; }

    @Override protected boolean isInsideArena(Vec p) {
        double r = p.dist(center);
        return r <= ARENA_RADIUS - 20 && r >= 70;
    }

    @Override public void reset() { collapsePending = false; collapsed = false; banner = null; }

    @Override public void onWaveStart(Game game, int wave) {
        if (wave == COLLAPSE_WAVE) {
            collapsePending = true;
            collapseTimer = COLLAPSE_DELAY;
            banner = "Warning: the Event Horizon is destabilising...";
            bannerTime = 6;
        }
    }

    @Override public void update(Game game, double dt) {
        bannerTime -= dt;
        if (bannerTime <= 0) banner = null;
        if (collapsePending) {
            collapseTimer -= dt;
            if (collapseTimer <= 0) {
                collapsePending = false;
                collapsed = true;
                triggerCollapse(game);
            }
        }
    }

    /** Executes the Event Horizon Collapse. Public so tests can trigger it directly. */
    public void triggerCollapse(Game game) {
        for (Tower t : game.getTowerManager().getTowers()) t.resetAbilityCooldown();
        for (Enemy e : game.getEnemyManager().getEnemies()) {
            if (e.isStored()) continue;
            double frac = e.getDistance() / e.getRoute().getTotalLength();
            int idx = game.getRng().nextInt(routes.size());
            Path np = routes.get(idx);
            e.teleportTo(np, idx, frac * np.getTotalLength());
            game.emitTeleport(e.getPosition());
        }
        banner = "EVENT HORIZON COLLAPSE! Cooldowns reset, enemies scattered.";
        bannerTime = 6;
        game.emitText(center, "EVENT HORIZON COLLAPSE", 0xd0b0ff);
    }

    @Override public double enemySpeedMultiplier(Enemy e) {
        double r = e.getPosition().dist(center);
        return 1.0 + 0.6 * Math.max(0, 1 - r / ARENA_RADIUS);
    }

    @Override public String getEventText() { return banner; }
}
