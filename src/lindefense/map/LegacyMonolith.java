package lindefense.map;

import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Tower;

/**
 * Map 2: The Legacy Monolith (Intermediate).
 * A multi-tiered server rack: enemies enter at the top and snake down four
 * floors to the exit at the bottom-left.
 *
 * Mechanics:
 * <ul>
 *   <li><b>Server chassis</b> between floors block line of sight: towers cannot
 *       shoot through them.</li>
 *   <li><b>Thermal Overload Venting</b>: every 10th wave a random floor vents for
 *       the whole wave. Enemies on that floor take 1 heat damage every 2 s and move
 *       10% slower; towers on that floor overheat and fire 25% slower.</li>
 * </ul>
 */
public final class LegacyMonolith extends GameMap {
    /** Floor bands [top, bottom). */
    private static final double[][] FLOORS = { {50, 215}, {215, 425}, {425, 635}, {635, 850} };

    private int ventedFloor = -1;
    private double heatTimer;
    private String banner;
    private double bannerTime;

    public LegacyMonolith() {
        super("The Legacy Monolith", "Intermediate",
              "A towering server rack. Data spirals down four floors past rows of humming chassis.");
        routes.add(new Path("Rack Spiral",
            80, 0, 80, 110, 1120, 110, 1120, 320, 80, 320, 80, 530, 1120, 530, 1120, 740, 0, 740));
        double[][] chassis = { {300, 175}, {700, 175}, {300, 385}, {700, 385}, {300, 595}, {700, 595} };
        int n = 1;
        for (double[] c : chassis) {
            obstacles.add(new Obstacle(c[0], c[1], 200, 80, true, "SRV-0" + n++));
        }
        for (Obstacle o : obstacles) {
            addDecoration(Decoration.rect(o.x, o.y, o.w, o.h, 0x2a2f3a));
            addDecoration(Decoration.frame(o.x, o.y, o.w, o.h, 0x6f7a8c));
            for (int i = 0; i < 4; i++) addDecoration(Decoration.rect(o.x + 12, o.y + 10 + i * 17, o.w - 24, 8, 0x3b4454));
            addDecoration(Decoration.oval(o.x + o.w - 22, o.y + 8, 8, 8, 0x5ce07a, true));
            addDecoration(Decoration.label(o.x + 12, o.y + o.h - 6, o.label, 0xa6b0c4));
        }
        for (double[] f : FLOORS) addDecoration(Decoration.line(0, f[0], WIDTH, f[0], 0x3a4150));
        addDecoration(Decoration.label(1130, 70, "FLOOR 1", 0x6f7a8c));
        addDecoration(Decoration.label(1130, 280, "FLOOR 2", 0x6f7a8c));
        addDecoration(Decoration.label(1130, 490, "FLOOR 3", 0x6f7a8c));
        addDecoration(Decoration.label(1130, 700, "FLOOR 4", 0x6f7a8c));
    }

    @Override public int getBackgroundRgb() { return 0x161a22; }
    @Override public int getTrackRgb() { return 0x4f6f8f; }
    @Override public String getMechanicSummary() {
        return "Server chassis block line of sight. Every 10 waves a random floor vents heat, hurting enemies and slowing towers there.";
    }

    public int floorOf(Vec p) {
        for (int i = 0; i < FLOORS.length; i++) if (p.y >= FLOORS[i][0] && p.y < FLOORS[i][1]) return i;
        return -1;
    }

    public int getVentedFloor() { return ventedFloor; }
    public double[][] getFloors() { return FLOORS; }

    @Override public void reset() { ventedFloor = -1; banner = null; }

    @Override public void onWaveStart(Game game, int wave) {
        if (wave % 10 == 0) {
            ventedFloor = game.getRng().nextInt(FLOORS.length);
            banner = "Thermal Overload Venting on Floor " + (ventedFloor + 1) + "!";
            bannerTime = 5;
        } else {
            ventedFloor = -1;
        }
    }

    @Override public void update(Game game, double dt) {
        bannerTime -= dt;
        if (bannerTime <= 0) banner = null;
        if (ventedFloor < 0) return;
        heatTimer += dt;
        if (heatTimer >= 2.0) {
            heatTimer = 0;
            for (Enemy e : game.getEnemyManager().getEnemies()) {
                if (floorOf(e.getPosition()) == ventedFloor) game.getCombat().applyMapDamage(e, 1);
            }
        }
    }

    @Override public double enemySpeedMultiplier(Enemy e) {
        return ventedFloor >= 0 && floorOf(e.getPosition()) == ventedFloor ? 0.9 : 1.0;
    }

    @Override public double towerRateMultiplier(Game game, Tower t) {
        return ventedFloor >= 0 && floorOf(t.getPosition()) == ventedFloor ? 0.75 : 1.0;
    }

    @Override public String getEventText() {
        if (banner != null) return banner;
        return ventedFloor >= 0 ? "Floor " + (ventedFloor + 1) + " is venting heat" : null;
    }
}
