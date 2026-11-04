package lindefense.map;

import java.util.ArrayList;
import java.util.List;
import lindefense.sim.Vec;

/**
 * An ordered list of waypoints from an entrance to an exit.
 * Positions along the path are addressed by distance travelled, which keeps
 * progress consistent when enemies are slowed, knocked back or teleported.
 */
public class Path {
    private final List<Vec> waypoints = new ArrayList<>();
    private final List<Double> cumulative = new ArrayList<>();
    private double totalLength;
    private final String name;

    public Path(String name, double... coords) {
        this.name = name;
        for (int i = 0; i + 1 < coords.length; i += 2) {
            waypoints.add(new Vec(coords[i], coords[i + 1]));
        }
        rebuild();
    }

    public Path(String name, List<Vec> pts) {
        this.name = name;
        waypoints.addAll(pts);
        rebuild();
    }

    private void rebuild() {
        cumulative.clear();
        double d = 0;
        cumulative.add(0.0);
        for (int i = 1; i < waypoints.size(); i++) {
            d += waypoints.get(i).dist(waypoints.get(i - 1));
            cumulative.add(d);
        }
        totalLength = d;
    }

    public String getName() { return name; }
    public double getTotalLength() { return totalLength; }
    public List<Vec> getWaypoints() { return waypoints; }
    public Vec getStart() { return waypoints.get(0); }
    public Vec getEnd() { return waypoints.get(waypoints.size() - 1); }

    /** World position at the given travelled distance (clamped to the path). */
    public Vec positionAt(double dist) {
        if (dist <= 0) return waypoints.get(0);
        if (dist >= totalLength) return getEnd();
        int seg = segmentIndex(dist);
        double segStart = cumulative.get(seg);
        double segLen = cumulative.get(seg + 1) - segStart;
        double t = segLen <= 0 ? 0 : (dist - segStart) / segLen;
        return Vec.lerp(waypoints.get(seg), waypoints.get(seg + 1), t);
    }

    /** Unit direction of travel at the given distance. */
    public Vec directionAt(double dist) {
        int seg = segmentIndex(Math.max(0, Math.min(dist, totalLength - 0.001)));
        return waypoints.get(seg + 1).sub(waypoints.get(seg)).normalized();
    }

    private int segmentIndex(double dist) {
        int lo = 0, hi = waypoints.size() - 2;
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (cumulative.get(mid) <= dist) lo = mid; else hi = mid - 1;
        }
        return Math.max(0, Math.min(lo, waypoints.size() - 2));
    }

    /** Shortest distance from a point to the path's centre line. */
    public double distanceToPoint(Vec p) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i + 1 < waypoints.size(); i++) {
            best = Math.min(best, pointSegmentDistance(p, waypoints.get(i), waypoints.get(i + 1)));
        }
        return best;
    }

    /** Distance along the path of the point on the path closest to p. */
    public double nearestDistanceAlong(Vec p) {
        double best = Double.MAX_VALUE;
        double bestD = 0;
        for (int i = 0; i + 1 < waypoints.size(); i++) {
            Vec a = waypoints.get(i), b = waypoints.get(i + 1);
            Vec ab = b.sub(a);
            double len2 = ab.x * ab.x + ab.y * ab.y;
            double t = len2 <= 0 ? 0 : Math.max(0, Math.min(1, ((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2));
            Vec q = Vec.lerp(a, b, t);
            double d = q.dist(p);
            if (d < best) { best = d; bestD = cumulative.get(i) + t * Math.sqrt(len2); }
        }
        return bestD;
    }

    public static double pointSegmentDistance(Vec p, Vec a, Vec b) {
        Vec ab = b.sub(a);
        double len2 = ab.x * ab.x + ab.y * ab.y;
        double t = len2 <= 0 ? 0 : Math.max(0, Math.min(1, ((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2));
        return Vec.lerp(a, b, t).dist(p);
    }
}
