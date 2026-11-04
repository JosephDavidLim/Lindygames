package lindefense.map;

import java.util.ArrayList;
import java.util.List;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;

/**
 * Map 7: Abandoned Data Center - Server Graveyard (Beginner).
 * A long serpentine path up and down dead server aisles, under the central cooling facility and out through the
 * maintenance corridors.
 *
 * Mechanics:
 * <ul>
 *   <li><b>Malfunctioning Infrastructure</b>: four hazard sections lie on the track. Every {@link #HAZARD_INTERVAL} s
 *       one of them (never the same one twice in a row) flashes a {@link #HAZARD_WARNING} s warning and then activates
 *       for {@link #HAZARD_DURATION} s. Enemies inside an active section move {@link #HAZARD_SLOW} slower; bosses take
 *       a third of that slow (the global boss slow rule).</li>
 * </ul>
 */
public final class AbandonedDataCenter extends GameMap {
    public static final double HAZARD_INTERVAL = 16;
    public static final double HAZARD_WARNING = 2;
    public static final double HAZARD_DURATION = 7;
    public static final double HAZARD_SLOW = 0.45;

    /** A slowing section of the track, [from, to] in distance along the route. */
    public static final class Hazard {
        public final String name, kind;
        public final int rgb;
        public final double from, to;
        Hazard(String name, String kind, int rgb, double from, double to) { this.name = name; this.kind = kind; this.rgb = rgb; this.from = from; this.to = to; }
        public boolean covers(double d) { return d >= from && d <= to; }
    }

    private final List<Hazard> hazards = new ArrayList<>();
    private int active = -1, warned = -1, last = -1;
    private double timer = HAZARD_INTERVAL - HAZARD_WARNING;
    private double remaining;
    private String banner;
    private double bannerTime;

    public AbandonedDataCenter() {
        super("Abandoned Data Center", "Beginner",
              "The Server Graveyard. A long path twists through dead server aisles, past failing cooling plants and sparking cables.");
        Path p = new Path("Maintenance Route",
            0, 90, 130, 90, 130, 780, 290, 780, 290, 160, 450, 160, 450, 700, 750, 700,
            750, 160, 910, 160, 910, 780, 1070, 780, 1070, 90, 1200, 90);
        routes.add(p);
        hazard(p, "Aisle 2 Live Cables", "SPARKS", 0xffd23b, new Vec(290, 640), new Vec(290, 330));
        hazard(p, "Coolant Leak", "COOLANT", 0x5fe8ff, new Vec(480, 700), new Vec(720, 700));
        hazard(p, "Aisle 5 Fan Failure", "TURBINE", 0x9cff6a, new Vec(910, 330), new Vec(910, 640));
        hazard(p, "Exit Vent Blowback", "VENT", 0xff7a3b, new Vec(1070, 640), new Vec(1070, 330));
        // dead server rows in the aisles, the cooling plant and the outer racks
        obstacles.add(new Obstacle(500, 300, 200, 240, false, "COOLING PLANT"));
        obstacles.add(new Obstacle(180, 260, 60, 150, false, "RACK"));
        obstacles.add(new Obstacle(340, 520, 60, 150, false, "RACK"));
        obstacles.add(new Obstacle(800, 260, 60, 150, false, "RACK"));
        obstacles.add(new Obstacle(960, 520, 60, 150, false, "RACK"));
        obstacles.add(new Obstacle(20, 220, 66, 200, false, "RACK"));
        obstacles.add(new Obstacle(1114, 470, 66, 200, false, "RACK"));
        obstacles.add(new Obstacle(520, 20, 160, 70, false, "UPS BANK"));
        for (Obstacle o : obstacles) {
            addDecoration(Decoration.rect(o.x, o.y, o.w, o.h, 0x1b1f24));
            addDecoration(Decoration.frame(o.x, o.y, o.w, o.h, 0x6a6f78));
        }
    }

    private void hazard(Path p, String name, String kind, int rgb, Vec a, Vec b) {
        double da = p.nearestDistanceAlong(a), db = p.nearestDistanceAlong(b);
        hazards.add(new Hazard(name, kind, rgb, Math.min(da, db), Math.max(da, db)));
    }

    public List<Hazard> getHazards() { return hazards; }
    /** Index of the active hazard, or -1. */
    public int getActiveHazard() { return remaining > 0 ? active : -1; }
    /** Index of the hazard about to activate (warning phase), or -1. */
    public int getWarnedHazard() { return warned; }
    public double getHazardRemaining() { return Math.max(0, remaining); }

    @Override public int getBackgroundRgb() { return 0x0b0d10; }
    @Override public int getTrackRgb() { return 0x9aa4b0; }
    @Override public String getMechanicSummary() {
        return "Malfunctioning Infrastructure: every 16 s one of four marked track sections sparks, leaks coolant or blasts air, slowing enemies there by 45% for 7 s.";
    }

    @Override public void reset() {
        active = -1; warned = -1; last = -1; remaining = 0; timer = HAZARD_INTERVAL - HAZARD_WARNING; banner = null;
    }

    @Override public void update(Game game, double dt) {
        bannerTime -= dt;
        if (bannerTime <= 0) banner = null;
        if (remaining > 0) {
            remaining -= dt;
            if (remaining <= 0) active = -1;
        }
        timer -= dt;
        if (warned < 0 && timer <= 0) {
            int pick;
            if (last < 0) pick = game.getRng().nextInt(hazards.size());
            else { pick = game.getRng().nextInt(hazards.size() - 1); if (pick >= last) pick++; }
            warned = pick;
            timer = HAZARD_WARNING;
            banner = "Warning: " + hazards.get(pick).name + " is failing...";
            bannerTime = HAZARD_WARNING;
        } else if (warned >= 0 && timer <= 0) {
            active = warned;
            last = warned;
            warned = -1;
            remaining = HAZARD_DURATION;
            timer = HAZARD_INTERVAL - HAZARD_WARNING;
            Hazard h = hazards.get(active);
            Vec mid = routes.get(0).positionAt((h.from + h.to) / 2);
            game.emitText(mid, h.kind + "!", h.rgb);
            banner = null;
        }
    }

    @Override public double enemySpeedMultiplier(Enemy e) {
        if (remaining <= 0 || active < 0 || e.isStored() || e.getRoute() != routes.get(0)) return 1.0;
        if (!hazards.get(active).covers(e.getDistance())) return 1.0;
        return 1.0 - (e.isBoss() ? HAZARD_SLOW / 3 : HAZARD_SLOW);
    }

    @Override public String getEventText() {
        if (banner != null) return banner;
        int a = getActiveHazard();
        return a >= 0 ? String.format("%s: enemies slowed (%.0fs)", hazards.get(a).name, remaining) : null;
    }
}
