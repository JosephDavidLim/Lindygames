package lindefense.map;

import java.util.ArrayList;
import java.util.List;
import lindefense.sim.Vec;

/** Route-building helpers for map authors. */
public final class Routes {
    private Routes() { }

    /**
     * A polyline through the given corner points ({@code x0, y0, x1, y1, ...}) whose interior corners are rounded with
     * a fillet of up to {@code radius} px (never more than half of either adjacent leg). Each fillet is a quadratic
     * curve sampled into short straight pieces, so enemies follow a smooth road.
     */
    public static List<Vec> rounded(double radius, double... xy) {
        List<Vec> pts = new ArrayList<>();
        for (int i = 0; i + 1 < xy.length; i += 2) pts.add(new Vec(xy[i], xy[i + 1]));
        List<Vec> out = new ArrayList<>();
        out.add(pts.get(0));
        for (int i = 1; i + 1 < pts.size(); i++) {
            Vec a = pts.get(i - 1), b = pts.get(i), c = pts.get(i + 1);
            double r = Math.min(radius, Math.min(a.dist(b), b.dist(c)) / 2);
            if (r < 1) { add(out, b); continue; }
            Vec s = b.add(a.sub(b).normalized().scale(r)), e = b.add(c.sub(b).normalized().scale(r));
            int steps = Math.max(3, (int) Math.ceil(r / 9));
            for (int k = 0; k <= steps; k++) {
                double t = k / (double) steps, u = 1 - t;
                add(out, new Vec(u * u * s.x + 2 * u * t * b.x + t * t * e.x, u * u * s.y + 2 * u * t * b.y + t * t * e.y));
            }
        }
        add(out, pts.get(pts.size() - 1));
        return out;
    }

    private static void add(List<Vec> out, Vec p) {
        if (out.isEmpty() || out.get(out.size() - 1).dist(p) > 0.5) out.add(p);
    }
}
