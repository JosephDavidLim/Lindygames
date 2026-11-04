package lindefense.ui.sprites.towers;

import acm.graphics.GCompound;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.awt.Color;

/**
 * Cartoon drawing helpers shared by every tower, projectile and effect sprite.
 *
 * <p>The ACM library has no stroke width, so the thick dark outline of the
 * Bloons-style look is produced by drawing an "ink" copy of each shape that is
 * {@link #OUT} logical pixels larger, then the coloured fill on top. Helpers
 * that take a {@link GCompound} add both pieces to it and return the fill
 * object so callers can mutate it later.</p>
 */
public final class Art {
    private Art() { }

    /** Outline ink colour (a very dark purple reads warmer than pure black). */
    public static final Color INK = new Color(0x1d1430);
    /** Outline thickness in logical pixels. */
    public static final double OUT = 2.2;
    /** Superclass accent gold. */
    public static final Color GOLD = new Color(0xffd34d);
    /** Darker gold for shading. */
    public static final Color GOLD_DARK = new Color(0xd29a12);
    /** Pale gold for highlights. */
    public static final Color GOLD_LIGHT = new Color(0xfff1a8);
    /** Neutral plate slate. */
    public static final Color SLATE = new Color(0x3a4256);
    /** Lighter slate. */
    public static final Color SLATE_LIGHT = new Color(0x5a647c);
    /** Dark metal. */
    public static final Color METAL = new Color(0x6f7a90);
    /** Light metal. */
    public static final Color METAL_LIGHT = new Color(0xb8c2d6);

    // ------------------------------------------------------------ colours

    /** Opaque colour from a 0xRRGGBB int. */
    public static Color rgb(int v) { return new Color(v); }

    /** Copy of {@code c} with the given alpha (0-255). */
    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    /** Colour with alpha from a 0xRRGGBB int. */
    public static Color alpha(int v, int a) { return alpha(new Color(v), a); }

    /**
     * Shades a colour: {@code k < 1} darkens multiplicatively, {@code k > 1}
     * lightens toward white by {@code k - 1}.
     */
    public static Color shade(Color c, double k) {
        if (k <= 1) {
            return new Color(clamp(c.getRed() * k), clamp(c.getGreen() * k), clamp(c.getBlue() * k), c.getAlpha());
        }
        double t = Math.min(1, k - 1);
        return new Color(clamp(c.getRed() + (255 - c.getRed()) * t), clamp(c.getGreen() + (255 - c.getGreen()) * t),
            clamp(c.getBlue() + (255 - c.getBlue()) * t), c.getAlpha());
    }

    /** Linear mix of two colours. */
    public static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(clamp(a.getRed() + (b.getRed() - a.getRed()) * t),
            clamp(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            clamp(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
            clamp(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    private static int clamp(double v) { return (int) Math.max(0, Math.min(255, Math.round(v))); }

    // ------------------------------------------------------------ outlined primitives

    /** Adds an outlined filled circle centred at (cx, cy); returns the fill oval. */
    public static GOval disc(GCompound g, double cx, double cy, double rad, Color fill) {
        return ellipse(g, cx, cy, rad, rad, fill);
    }

    /** Adds an outlined filled ellipse; returns the fill oval. */
    public static GOval ellipse(GCompound g, double cx, double cy, double rx, double ry, Color fill) {
        GOval ink = new GOval(cx - rx - OUT, cy - ry - OUT, 2 * (rx + OUT), 2 * (ry + OUT));
        ink.setFilled(true); ink.setFillColor(INK); ink.setColor(INK);
        g.add(ink);
        GOval o = new GOval(cx - rx, cy - ry, 2 * rx, 2 * ry);
        o.setFilled(true); o.setFillColor(fill); o.setColor(shade(fill, 0.75));
        g.add(o);
        return o;
    }

    /** Adds an outlined filled rectangle; returns the fill rect. */
    public static GRect box(GCompound g, double x, double y, double w, double h, Color fill) {
        GRect ink = new GRect(x - OUT, y - OUT, w + 2 * OUT, h + 2 * OUT);
        ink.setFilled(true); ink.setFillColor(INK); ink.setColor(INK);
        g.add(ink);
        GRect r = new GRect(x, y, w, h);
        r.setFilled(true); r.setFillColor(fill); r.setColor(shade(fill, 0.75));
        g.add(r);
        return r;
    }

    /** Adds an outlined filled polygon from flat {x0,y0,x1,y1,...}; returns the fill polygon. */
    public static GPolygon poly(GCompound g, double[] xy, Color fill) {
        g.add(rawPoly(expand(xy, OUT), INK, INK));
        GPolygon p = rawPoly(xy, fill, shade(fill, 0.75));
        g.add(p);
        return p;
    }

    /** Adds an un-outlined polygon (for glows, glosses, shadows). */
    public static GPolygon plain(GCompound g, double[] xy, Color fill) {
        GPolygon p = rawPoly(xy, fill, fill);
        g.add(p);
        return p;
    }

    /** Builds a filled polygon whose origin is (0,0) so it can be rotated about that pivot. */
    public static GPolygon rawPoly(double[] xy, Color fill, Color outline) {
        GPolygon p = new GPolygon();
        for (int i = 0; i + 1 < xy.length; i += 2) p.addVertex(xy[i], xy[i + 1]);
        p.setFilled(true);
        p.setFillColor(fill);
        p.setColor(outline);
        return p;
    }

    // ------------------------------------------------------------ decorations

    /** Big glossy highlight: a soft white ellipse. */
    public static GOval gloss(GCompound g, double cx, double cy, double rx, double ry) {
        GOval o = new GOval(cx - rx, cy - ry, 2 * rx, 2 * ry);
        o.setFilled(true); o.setFillColor(alpha(Color.WHITE, 120)); o.setColor(alpha(Color.WHITE, 0));
        g.add(o);
        return o;
    }

    /** Small four-point white sparkle. */
    public static GPolygon sparkle(GCompound g, double cx, double cy, double s) {
        GPolygon p = rawPoly(star(cx, cy, s, s * 0.3, 4, 0), alpha(Color.WHITE, 230), alpha(Color.WHITE, 0));
        g.add(p);
        return p;
    }

    /** Soft semi-transparent ground shadow. */
    public static GOval shadow(GCompound g, double cx, double cy, double rx, double ry) {
        GOval o = new GOval(cx - rx, cy - ry, 2 * rx, 2 * ry);
        o.setFilled(true); o.setFillColor(alpha(0x08061a, 70)); o.setColor(alpha(0x08061a, 0));
        g.add(o);
        return o;
    }

    /** Soft glow disc without outline. */
    public static GOval glow(GCompound g, double cx, double cy, double rad, Color c, int a) {
        GOval o = new GOval(cx - rad, cy - rad, 2 * rad, 2 * rad);
        o.setFilled(true); o.setFillColor(alpha(c, a)); o.setColor(alpha(c, 0));
        g.add(o);
        return o;
    }

    /**
     * Two-tone cartoon body: outline, dark tone, lighter tone offset to the
     * upper-left, and a glossy highlight. Returns the light-tone oval.
     */
    public static GOval body(GCompound g, double cx, double cy, double rad, Color fill) {
        ellipse(g, cx, cy, rad, rad, shade(fill, 0.78));
        GOval light = new GOval(cx - rad * 0.86 - rad * 0.1, cy - rad * 0.86 - rad * 0.1, rad * 1.72, rad * 1.72);
        light.setFilled(true); light.setFillColor(fill); light.setColor(fill);
        g.add(light);
        gloss(g, cx - rad * 0.38, cy - rad * 0.42, rad * 0.32, rad * 0.2);
        return light;
    }

    /** Two-tone rectangular body with gloss. Returns the light rect. */
    public static GRect bodyBox(GCompound g, double x, double y, double w, double h, Color fill) {
        box(g, x, y, w, h, shade(fill, 0.78));
        GRect light = new GRect(x, y, w * 0.85, h * 0.85);
        light.setFilled(true); light.setFillColor(fill); light.setColor(fill);
        g.add(light);
        gloss(g, x + w * 0.28, y + h * 0.22, w * 0.18, h * 0.1);
        return light;
    }

    // ------------------------------------------------------------ vertex builders

    /** Circle approximated by {@code n} vertices. */
    public static double[] circle(double cx, double cy, double rad, int n) { return regular(cx, cy, rad, n, 0); }

    /** Regular polygon; {@code rotDeg} rotates it (0 puts a vertex at the top). */
    public static double[] regular(double cx, double cy, double rad, int n, double rotDeg) {
        double[] v = new double[n * 2];
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(rotDeg - 90) + i * 2 * Math.PI / n;
            v[2 * i] = cx + Math.cos(a) * rad;
            v[2 * i + 1] = cy + Math.sin(a) * rad;
        }
        return v;
    }

    /** Star with {@code n} points. */
    public static double[] star(double cx, double cy, double ro, double ri, int n, double rotDeg) {
        double[] v = new double[n * 4];
        for (int i = 0; i < 2 * n; i++) {
            double a = Math.toRadians(rotDeg - 90) + i * Math.PI / n;
            double rad = (i % 2 == 0) ? ro : ri;
            v[2 * i] = cx + Math.cos(a) * rad;
            v[2 * i + 1] = cy + Math.sin(a) * rad;
        }
        return v;
    }

    /**
     * Ring (annulus) with a real hole: the outer loop followed by the inner
     * loop, which the even-odd fill rule of {@code fillPolygon} leaves empty.
     */
    public static double[] ring(double cx, double cy, double ro, double ri, int n) {
        double[] outer = circle(cx, cy, ro, n);
        double[] inner = circle(cx, cy, ri, n);
        return join(outer, inner);
    }

    /** Spiky ring: star outline outside, round hole inside. */
    public static double[] gearRing(double cx, double cy, double ro, double rs, double ri, int n) {
        return join(star(cx, cy, ro, rs, n, 0), circle(cx, cy, ri, n * 2));
    }

    private static double[] join(double[] outer, double[] inner) {
        int no = outer.length / 2, ni = inner.length / 2;
        double[] v = new double[(no + 1 + ni + 1) * 2];
        int k = 0;
        for (int i = 0; i < no; i++) { v[k++] = outer[2 * i]; v[k++] = outer[2 * i + 1]; }
        v[k++] = outer[0]; v[k++] = outer[1];
        for (int i = 0; i <= ni; i++) {
            int j = (ni - i) % ni;
            v[k++] = inner[2 * j]; v[k++] = inner[2 * j + 1];
        }
        return v;
    }

    /** Axis-aligned rectangle vertices. */
    public static double[] rect(double x, double y, double w, double h) {
        return new double[] { x, y, x + w, y, x + w, y + h, x, y + h };
    }

    /** Capsule / rounded bar from (x0,y0) to (x1,y1) with half-width {@code hw}. */
    public static double[] bar(double x0, double y0, double x1, double y1, double hw) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy);
        if (len < 1e-6) { dx = 1; dy = 0; len = 1; }
        double base = Math.atan2(dy / len, dx / len);
        int seg = 6;
        double[] v = new double[(seg + 1) * 4];
        int k = 0;
        for (int i = 0; i <= seg; i++) {
            double a = base - Math.PI / 2 + i * Math.PI / seg;
            v[k++] = x1 + Math.cos(a) * hw;
            v[k++] = y1 + Math.sin(a) * hw;
        }
        for (int i = 0; i <= seg; i++) {
            double a = base + Math.PI / 2 + i * Math.PI / seg;
            v[k++] = x0 + Math.cos(a) * hw;
            v[k++] = y0 + Math.sin(a) * hw;
        }
        return v;
    }

    /** Isosceles arrow / wedge pointing along +x: base at x0 of half-width hw, tip at x1. */
    public static double[] wedge(double x0, double x1, double hw, double cy) {
        return new double[] { x0, cy - hw, x1, cy, x0, cy + hw };
    }

    /** Lightning zigzag along +x from x0 to x1 with amplitude amp and thickness th. */
    public static double[] bolt(double x0, double x1, double amp, double th) {
        double m = (x0 + x1) / 2, q = (x0 + m) / 2, q3 = (m + x1) / 2;
        return new double[] {
            x0, 0, q, -amp, m - th * 0.5, -amp * 0.2, q3, -amp, x1, 0,
            q3 - th, amp * 0.1, m + th * 0.5, amp * 0.9, q - th, amp * 0.3 };
    }

    /** Translates vertices. */
    public static double[] shift(double[] xy, double dx, double dy) {
        double[] v = xy.clone();
        for (int i = 0; i + 1 < v.length; i += 2) { v[i] += dx; v[i + 1] += dy; }
        return v;
    }

    /** Rotates vertices about (0,0) by degrees (screen-space, clockwise positive). */
    public static double[] spin(double[] xy, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        double[] v = new double[xy.length];
        for (int i = 0; i + 1 < xy.length; i += 2) {
            v[i] = xy[i] * c - xy[i + 1] * s;
            v[i + 1] = xy[i] * s + xy[i + 1] * c;
        }
        return v;
    }

    /** Scales vertices about (0,0). */
    public static double[] scaled(double[] xy, double k) {
        double[] v = new double[xy.length];
        for (int i = 0; i < xy.length; i++) v[i] = xy[i] * k;
        return v;
    }

    /** Mirrors vertices horizontally about x = 0. */
    public static double[] mirror(double[] xy) {
        double[] v = xy.clone();
        for (int i = 0; i + 1 < v.length; i += 2) v[i] = -v[i];
        return v;
    }

    /** Expands a polygon away from its centroid by {@code by} pixels (approximate outline). */
    public static double[] expand(double[] xy, double by) {
        int n = xy.length / 2;
        double cx = 0, cy = 0;
        for (int i = 0; i < n; i++) { cx += xy[2 * i]; cy += xy[2 * i + 1]; }
        cx /= n; cy /= n;
        double[] v = new double[xy.length];
        for (int i = 0; i < n; i++) {
            double dx = xy[2 * i] - cx, dy = xy[2 * i + 1] - cy;
            double d = Math.hypot(dx, dy);
            // vertices at sharp angles need a little extra push to keep a visible rim
            double k = d < 1e-6 ? 0 : (d + by * 1.15) / d;
            v[2 * i] = cx + dx * k;
            v[2 * i + 1] = cy + dy * k;
        }
        return v;
    }
}
