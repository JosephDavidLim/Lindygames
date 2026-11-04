package lindefense.map;

import lindefense.sim.Vec;

/**
 * A rectangular non-buildable region on a map. Obstacles may optionally block
 * line of sight (server chassis on The Legacy Monolith).
 */
public class Obstacle {
    public final double x, y, w, h;
    public final boolean blocksLineOfSight;
    public final String label;

    public Obstacle(double x, double y, double w, double h, boolean blocksLineOfSight, String label) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.blocksLineOfSight = blocksLineOfSight;
        this.label = label;
    }

    public boolean contains(Vec p, double margin) {
        return p.x >= x - margin && p.x <= x + w + margin && p.y >= y - margin && p.y <= y + h + margin;
    }

    /** True if the segment a-b crosses the rectangle (used for line-of-sight). */
    public boolean intersectsSegment(Vec a, Vec b) {
        if (contains(a, 0) || contains(b, 0)) return true;
        return segHits(a, b, new Vec(x, y), new Vec(x + w, y))
            || segHits(a, b, new Vec(x + w, y), new Vec(x + w, y + h))
            || segHits(a, b, new Vec(x + w, y + h), new Vec(x, y + h))
            || segHits(a, b, new Vec(x, y + h), new Vec(x, y));
    }

    private static boolean segHits(Vec p1, Vec p2, Vec p3, Vec p4) {
        double d1 = cross(p3, p4, p1), d2 = cross(p3, p4, p2), d3 = cross(p1, p2, p3), d4 = cross(p1, p2, p4);
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
    }

    private static double cross(Vec a, Vec b, Vec c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }
}
