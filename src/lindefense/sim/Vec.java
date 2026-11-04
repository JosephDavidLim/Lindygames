package lindefense.sim;

/**
 * Immutable 2D vector / point used throughout the simulation.
 * Kept independent of java.awt / acm so the simulation can run headless.
 */
public final class Vec {
    public final double x;
    public final double y;

    public Vec(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec add(Vec o) { return new Vec(x + o.x, y + o.y); }
    public Vec sub(Vec o) { return new Vec(x - o.x, y - o.y); }
    public Vec scale(double s) { return new Vec(x * s, y * s); }
    public double length() { return Math.sqrt(x * x + y * y); }
    public double dist(Vec o) { return Math.hypot(x - o.x, y - o.y); }
    public double distSq(Vec o) { double dx = x - o.x, dy = y - o.y; return dx * dx + dy * dy; }

    /** Unit vector in the same direction (or zero vector if this is zero). */
    public Vec normalized() {
        double len = length();
        return len < 1e-9 ? new Vec(0, 0) : new Vec(x / len, y / len);
    }

    /** Linear interpolation between a and b. */
    public static Vec lerp(Vec a, Vec b, double t) {
        return new Vec(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
    }

    @Override public String toString() { return String.format("(%.1f, %.1f)", x, y); }
}
