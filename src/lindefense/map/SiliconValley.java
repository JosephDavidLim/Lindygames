package lindefense.map;

/**
 * Map 5: Silicon Valley - The Innovation District (Intermediate).
 * One long winding road through a futuristic AI campus: headquarters towers, research labs, server farms, a vehicle
 * depot and holo billboards. Rounded corners make arcs that a few well-placed towers can cover twice. Glass skybridges
 * cross the road (cosmetic only: enemies are always drawn above them).
 *
 * No special mechanics: the challenge is the layout.
 */
public final class SiliconValley extends GameMap {
    /** Skybridges as (x, y, w, h) rectangles that span the road (cosmetic). */
    private static final double[][] SKYBRIDGES = { { 340, 262, 80, 26 }, { 1030, 395, 80, 26 }, { 418, 650, 182, 22 } };

    public SiliconValley() {
        super("Silicon Valley", "Intermediate",
              "The Innovation District. A winding campus road snakes between AI labs, glass headquarters and humming server farms.");
        routes.add(new Path("Innovation Drive", Routes.rounded(70,
            0, 150, 380, 150, 380, 400, 170, 400, 170, 760, 560, 760, 560, 520, 800, 520,
            800, 130, 1070, 130, 1070, 760, 1200, 760)));
        obstacles.add(new Obstacle(30, 20, 180, 90, false, "DATA CENTER"));
        obstacles.add(new Obstacle(30, 210, 170, 120, false, "AI RESEARCH LAB"));
        obstacles.add(new Obstacle(20, 470, 110, 250, false, "SERVER FARM"));
        obstacles.add(new Obstacle(250, 470, 150, 130, false, "NEXUS HQ"));
        obstacles.add(new Obstacle(450, 30, 200, 110, false, "GLASS HQ"));
        obstacles.add(new Obstacle(600, 300, 140, 110, false, "ROBOTICS LAB"));
        obstacles.add(new Obstacle(860, 560, 150, 160, false, "SERVER FARM"));
        obstacles.add(new Obstacle(1110, 220, 75, 260, false, "AV DEPOT"));
        obstacles.add(new Obstacle(880, 20, 170, 70, false, "HOLO BOARD"));
        obstacles.add(new Obstacle(600, 610, 170, 100, false, "CHARGING HUB"));
        // thumbnail decorations (the full art is drawn by the renderer)
        for (Obstacle o : obstacles) {
            addDecoration(Decoration.rect(o.x, o.y, o.w, o.h, 0x1c2a3c));
            addDecoration(Decoration.frame(o.x, o.y, o.w, o.h, 0x5fd4ff));
        }
    }

    public double[][] getSkybridges() { return SKYBRIDGES; }

    @Override public int getBackgroundRgb() { return 0x0b1424; }
    @Override public int getTrackRgb() { return 0x3fb8ff; }
    @Override public String getMechanicSummary() {
        return "No special mechanics. One long winding road through the tech campus; buildings block placement, and the rounded bends reward towers that cover two stretches at once.";
    }
}
