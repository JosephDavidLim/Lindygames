package lindefense.map;

import java.util.ArrayList;
import java.util.List;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Faction;
import lindefense.tower.Tower;

/**
 * Map 1: The Silicon Circuit (Beginner).
 * Dual-lane printed circuit board with gold traces.
 *
 * Mechanics:
 * <ul>
 *   <li><b>Capacitor Pads</b>: three pads. A pad charges for {@link #PAD_CHARGE_TIME} s;
 *       when a Support-faction tower or hero stands on a fully charged pad the pad
 *       discharges, paying {@link #PAD_CASH} and giving every tower within
 *       {@link #PAD_BOOST_RADIUS} px a +40% attack-speed boost for 6 s.</li>
 *   <li><b>Trace Short-Circuit</b>: every {@link #SHORT_INTERVAL} s one lane shorts for
 *       2 s. Non-boss enemies on that lane take 2 damage; towers within 60 px of that
 *       lane's trace gain +30% damage for 8 s.</li>
 * </ul>
 */
public final class SiliconCircuit extends GameMap {
    public static final double PAD_CHARGE_TIME = 20;
    public static final double PAD_CASH = 25;
    public static final double PAD_BOOST_RADIUS = 110;
    public static final double SHORT_INTERVAL = 25;

    private final List<Vec> pads = new ArrayList<>();
    private final double[] padCharge = new double[3];
    private double shortTimer = SHORT_INTERVAL;
    private int shortedLane = -1;
    private double shortRemaining = 0;
    private String banner;
    private double bannerTime;

    public SiliconCircuit() {
        super("The Silicon Circuit", "Beginner",
              "A dual-lane printed circuit board. Gold traces carry two independent streams of data.");
        routes.add(new Path("Trace A",
            0, 230, 230, 230, 230, 120, 540, 120, 540, 380, 830, 380, 830, 200, 1200, 200));
        routes.add(new Path("Trace B",
            0, 650, 330, 650, 330, 800, 650, 800, 650, 580, 930, 580, 930, 730, 1200, 730));
        pads.add(new Vec(150, 440));
        pads.add(new Vec(430, 520));
        pads.add(new Vec(1040, 440));
        obstacles.add(new Obstacle(900, 30, 170, 60, false, "CPU"));
        obstacles.add(new Obstacle(60, 790, 150, 80, false, "RAM"));
        obstacles.add(new Obstacle(1000, 810, 160, 60, false, "I/O"));
        for (Obstacle o : obstacles) {
            addDecoration(Decoration.rect(o.x, o.y, o.w, o.h, 0x1f2a33));
            addDecoration(Decoration.frame(o.x, o.y, o.w, o.h, 0x8a929a));
            addDecoration(Decoration.label(o.x + 10, o.y + o.h / 2 + 5, o.label, 0xc0c8d0));
        }
        for (Vec p : pads) {
            addDecoration(Decoration.oval(p.x - 42, p.y - 42, 84, 84, 0x2d4a3e, true));
            addDecoration(Decoration.oval(p.x - 42, p.y - 42, 84, 84, 0x7fd8a8, false));
            addDecoration(Decoration.label(p.x - 36, p.y + 58, "Capacitor Pad", 0x7fd8a8));
        }
        // Little via dots to sell the PCB look.
        for (int i = 0; i < 40; i++) {
            double x = 40 + (i * 173) % 1120, y = 60 + (i * 389) % 780;
            addDecoration(Decoration.oval(x, y, 6, 6, 0x3b4b3b, true));
        }
    }

    @Override public int getBackgroundRgb() { return 0x11331f; }
    @Override public int getTrackRgb() { return 0xd4a62a; }
    @Override public String getMechanicSummary() {
        return "Capacitor Pads charge Support towers (cash + speed). Trace Short-Circuits periodically zap one lane and boost towers along it.";
    }

    public List<Vec> getPads() { return pads; }
    public double getPadCharge(int i) { return padCharge[i] / PAD_CHARGE_TIME; }
    public int getShortedLane() { return shortRemaining > 0 ? shortedLane : -1; }

    @Override public void reset() {
        for (int i = 0; i < padCharge.length; i++) padCharge[i] = 0;
        shortTimer = SHORT_INTERVAL; shortedLane = -1; shortRemaining = 0; banner = null;
    }

    /** Returns the index of the pad the tower stands on, or -1. */
    public int padIndexFor(Tower t) {
        for (int i = 0; i < pads.size(); i++) {
            if (pads.get(i).dist(t.getPosition()) <= 44) return i;
        }
        return -1;
    }

    @Override public void update(Game game, double dt) {
        bannerTime -= dt;
        if (bannerTime <= 0) banner = null;
        // Capacitor pads
        for (int i = 0; i < pads.size(); i++) {
            padCharge[i] = Math.min(PAD_CHARGE_TIME, padCharge[i] + dt);
            if (padCharge[i] >= PAD_CHARGE_TIME) {
                Tower occupant = null;
                for (Tower t : game.getTowerManager().getTowers()) {
                    if (!t.isTemporary() && pads.get(i).dist(t.getPosition()) <= 44
                        && (t.getType().faction == Faction.SUPPORT || t.getType().faction == Faction.HERO)) {
                        occupant = t; break;
                    }
                }
                if (occupant != null) {
                    padCharge[i] = 0;
                    game.addCash(PAD_CASH, pads.get(i));
                    for (Tower t : game.getTowerManager().getTowers()) {
                        if (t.getPosition().dist(pads.get(i)) <= PAD_BOOST_RADIUS) t.addBuff(1.4, 1.0, 1.0, 6);
                    }
                    game.emitText(pads.get(i), "Capacitor discharge!", 0x7fd8a8);
                }
            }
        }
        // Short circuits
        if (shortRemaining > 0) {
            shortRemaining -= dt;
        } else {
            shortTimer -= dt;
            if (shortTimer <= 0) {
                shortTimer = SHORT_INTERVAL;
                shortedLane = game.getRng().nextInt(routes.size());
                shortRemaining = 2.0;
                Path lane = routes.get(shortedLane);
                for (Enemy e : game.getEnemyManager().getEnemies()) {
                    if (e.getRoute() == lane && !e.isBoss()) {
                        game.getCombat().applyMapDamage(e, 2);
                    }
                }
                for (Tower t : game.getTowerManager().getTowers()) {
                    if (lane.distanceToPoint(t.getPosition()) <= 60) t.addBuff(1.0, 1.3, 1.0, 8);
                }
                banner = "Short-circuit on " + lane.getName() + "!";
                bannerTime = 3;
            }
        }
    }

    @Override public String getEventText() { return banner; }
}
