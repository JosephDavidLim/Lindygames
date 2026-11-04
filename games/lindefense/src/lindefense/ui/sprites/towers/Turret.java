package lindefense.ui.sprites.towers;

import acm.graphics.GCompound;
import acm.graphics.GPolygon;
import java.awt.Color;
import lindefense.ui.sprites.Thick;
import java.util.ArrayList;
import java.util.List;

/**
 * A rotatable group of polygons that pivots about its own origin. Parts are
 * authored pointing along +x ("forward"); {@link #setAngle} rotates every
 * registered polygon to the requested screen-space heading and
 * {@link #setRecoil} slides the whole group backwards along that heading.
 * Only {@link GPolygon}s can rotate in the ACM library, so every part of a
 * turret is a polygon (circles are 20-gons from {@link Art#circle}).
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class Turret extends GCompound {
    private static final long serialVersionUID = 1L;

    private final List<GPolygon> parts = new ArrayList<>();
    private final List<GPolygon> spinners = new ArrayList<>();
    private double angle;
    private double spinDegPerSec = 180;
    private GPolygon flash;

    /** Creates an empty turret at the compound origin. */
    public Turret() {
        setLocation(0, 0);
    }

    /** Adds an outlined polygon part (authored pointing +x). Returns the fill polygon. */
    public GPolygon poly(double[] xy, Color fill) {
        GPolygon ink = Art.rawPoly(Art.expand(xy, Art.OUT), Art.INK, Art.INK);
        add(ink); parts.add(ink);
        GPolygon p = Art.rawPoly(xy, fill, Art.shade(fill, 0.75));
        add(p); parts.add(p);
        return p;
    }

    /** Adds an un-outlined polygon part (gloss, glow). */
    public GPolygon plain(double[] xy, Color fill) {
        GPolygon p = Art.rawPoly(xy, fill, fill);
        add(p); parts.add(p);
        return p;
    }

    /** Adds a small white gloss ellipse (as a polygon) at the given local position. */
    public GPolygon gloss(double cx, double cy, double rx, double ry) {
        double[] v = new double[32];
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8;
            v[2 * i] = cx + Math.cos(a) * rx;
            v[2 * i + 1] = cy + Math.sin(a) * ry;
        }
        return Flat.detail(plain(v, Art.alpha(Color.WHITE, 120)), Thick.DETAIL_DECOR);
    }

    /**
     * Registers a part that also spins continuously about its own centre
     * (e.g. a drill, fan or ring). The part must be centred at (0,0) before
     * being shifted; pass the centre so it can be spun in place.
     */
    public GPolygon spinner(double[] xyAtOrigin, double cx, double cy, Color fill) {
        GPolygon ink = Art.rawPoly(Art.expand(xyAtOrigin, Art.OUT), Art.INK, Art.INK);
        ink.setLocation(cx, cy);
        add(ink); spinners.add(ink);
        GPolygon p = Art.rawPoly(xyAtOrigin, fill, Art.shade(fill, 0.75));
        p.setLocation(cx, cy);
        add(p); spinners.add(p);
        return p;
    }

    /** Adds an outlined spinning ring (real hole) centred at (cx, cy) in turret space. */
    public GPolygon ringSpinner(double ro, double ri, int n, double cx, double cy, Color fill) {
        GPolygon ink = Art.rawPoly(Art.ring(0, 0, ro + Art.OUT * 0.7, Math.max(1, ri - Art.OUT * 0.7), n), Art.INK, Art.INK);
        ink.setLocation(cx, cy);
        add(ink); spinners.add(ink);
        GPolygon p = Art.rawPoly(Art.ring(0, 0, ro, ri, n), fill, Art.shade(fill, 0.75));
        p.setLocation(cx, cy);
        add(p); spinners.add(p);
        return p;
    }

    /** Sets the muzzle flash polygon (hidden by default, shown briefly on fire). */
    public void setFlash(GPolygon p) {
        flash = p;
        p.setVisible(false);
    }

    /** Shows or hides the muzzle flash. */
    public void showFlash(boolean on) {
        if (flash != null && flash.isVisible() != on) flash.setVisible(on);
    }

    /** Spin speed for spinner parts, degrees per second. */
    public void setSpinSpeed(double degPerSec) { spinDegPerSec = degPerSec; }

    /** Current heading in radians (screen space, 0 = +x, positive = clockwise on screen). */
    public double getAngle() { return angle; }

    /** Rotates all parts to the given heading. */
    public void setAngle(double a) {
        double d = a - angle;
        if (Math.abs(d) < 1e-5) return;
        double deg = -Math.toDegrees(d);
        for (GPolygon p : parts) p.rotate(deg);
        angle = a;
        // spinners orbit the pivot like regular parts but keep their own spin
        double c = Math.cos(d), s = Math.sin(d);
        for (GPolygon p : spinners) {
            double x = p.getX(), y = p.getY();
            p.setLocation(x * c - y * s, x * s + y * c);
        }
    }

    /** Advances spinner rotation. */
    public void spin(double dt) {
        if (spinners.isEmpty()) return;
        double deg = spinDegPerSec * dt;
        for (GPolygon p : spinners) p.rotate(deg);
    }

    /** Slides the turret back along its heading by {@code px}. */
    public void setRecoil(double px) {
        setLocation(-Math.cos(angle) * px, -Math.sin(angle) * px);
    }
}
