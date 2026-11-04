package lindefense.ui.sprites;

import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import lindefense.enemy.EnemyType;

/**
 * Shared palette and colour / geometry helpers that keep every sprite in one
 * visual universe: a single dark outline colour, top-left lighting (highlights
 * are white with alpha, shadows are black with alpha) and one hue family per
 * enemy class.
 */
public final class Art {
    private Art() { }

    /** Universal outline colour. */
    public static final Color OUTLINE = new Color(24, 22, 40);
    /** Colour that damaged parts are mixed towards. */
    public static final Color DAMAGE = new Color(60, 48, 52);
    /** Frost tint for slowed enemies. */
    public static final Color FROST = new Color(150, 215, 255);
    /** Highlight colour (top-left light). */
    public static final Color HIGHLIGHT = new Color(255, 255, 255, 120);
    /** Soft shadow colour. */
    public static final Color SHADOW = new Color(0, 0, 0, 55);
    /** Fortified plating colours. */
    public static final Color STEEL = new Color(165, 172, 185);
    public static final Color STEEL_DARK = new Color(92, 98, 112);
    /** Regrow glow. */
    public static final Color REGROW = new Color(110, 240, 130);
    /** Status colours. */
    public static final Color MARK = new Color(255, 72, 72);
    public static final Color STUN = new Color(255, 225, 70);
    public static final Color DISRUPT = new Color(240, 70, 230);
    public static final Color REVEAL = new Color(255, 255, 255);

    // ---- Data-structure family: cool blues / violets ----
    public static final Color PRIMITIVE = new Color(60, 190, 255);
    public static final Color STATIC_ARRAY = new Color(70, 140, 240);
    public static final Color DYNAMIC_ARRAY = new Color(40, 215, 165);
    public static final Color SLL = new Color(140, 100, 255);
    public static final Color DLL = new Color(245, 70, 160);

    // ---- Sorting family: warm oranges / purples ----
    public static final Color BUBBLE = new Color(110, 225, 250);
    public static final Color SELECTION = new Color(255, 195, 40);
    public static final Color SELECTION_BODY = new Color(90, 100, 135);
    public static final Color INSERTION = new Color(255, 125, 45);
    public static final Color SHELL = new Color(240, 190, 110);
    public static final Color SHELL_DARK = new Color(170, 105, 55);
    public static final Color MERGE = new Color(150, 70, 225);
    public static final Color MERGE_RUN = new Color(190, 140, 245);
    public static final Color QUICK = new Color(240, 55, 70);
    public static final Color QUICK_PIVOT = new Color(255, 215, 60);
    public static final Color QUICK_PARTITION = new Color(255, 120, 110);
    public static final Color HEAP_ARMOR = new Color(125, 140, 170);
    public static final Color HEAP_NODE = new Color(70, 225, 110);
    public static final Color COUNTING = new Color(190, 230, 50);
    public static final Color RADIX = new Color(225, 130, 60);
    public static final Color BUCKET = new Color(120, 170, 215);
    public static final Color BUCKET_WOOD = new Color(175, 115, 60);
    public static final Color TIMSORT = new Color(170, 80, 230);

    // ---- Bosses ----
    public static final Color BELLARMINE = new Color(35, 70, 170);
    public static final Color SW = new Color(50, 150, 165);
    public static final Color SW_ACCENT = new Color(255, 140, 40);
    public static final Color THREECOM = new Color(75, 95, 70);
    public static final Color THREECOM_GLOW = new Color(90, 255, 120);
    public static final Color LOCKHEED = new Color(45, 45, 62);
    public static final Color LOCKHEED_GLOW = new Color(255, 60, 60);
    public static final Color PALANTIR = new Color(55, 50, 80);
    public static final Color PALANTIR_EYE0 = new Color(0, 210, 255);
    public static final Color PALANTIR_EYE1 = new Color(170, 90, 255);
    public static final Color PALANTIR_EYE2 = new Color(255, 70, 60);

    /** Signature colour for a type (used by effects and the bucket's stored dots). */
    public static Color colorOf(EnemyType t) {
        switch (t) {
            case PRIMITIVE_VARIABLE: return PRIMITIVE;
            case STATIC_ARRAY: return STATIC_ARRAY;
            case DYNAMIC_ARRAY: return DYNAMIC_ARRAY;
            case SINGLY_LINKED_LIST: return SLL;
            case DOUBLY_LINKED_LIST: return DLL;
            case BUBBLE_SORT: return BUBBLE;
            case SELECTION_SORT: return SELECTION;
            case INSERTION_SORT: return INSERTION;
            case SHELL_SORT: return SHELL;
            case MERGE_SORT: return MERGE;
            case MERGE_RUN: return MERGE_RUN;
            case QUICK_SORT: return QUICK;
            case QUICK_PARTITION: return QUICK_PARTITION;
            case HEAP_SORT: return HEAP_NODE;
            case COUNTING_SORT: return COUNTING;
            case RADIX_SORT: return RADIX;
            case BUCKET_SORT: return BUCKET;
            case TIMSORT: return TIMSORT;
            case BELLARMINE_COLLEGE_PREP: return BELLARMINE;
            case SW_RESOURCE_CONSULTANT: return SW;
            case THREE_COM: return THREECOM_GLOW;
            case LOCKHEED_MARTIN: return LOCKHEED_GLOW;
            case PALANTIR: return PALANTIR_EYE0;
            default: return new Color(180, 180, 190);
        }
    }

    /** Linear mix of two colours; keeps {@code a}'s alpha. */
    public static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
            a.getAlpha());
    }

    /** Same colour with the given alpha (0-255). */
    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    /** Darker version (f in 0..1, 0.3 = 30% towards black). */
    public static Color shade(Color c, double f) { return mix(c, Color.BLACK, f); }

    /** Lighter version (f in 0..1). */
    public static Color tint(Color c, double f) { return mix(c, Color.WHITE, f); }

    /** Outline colour matched to a fill: the universal outline, slightly pulled towards the hue. */
    public static Color outline(Color fill) { return mix(OUTLINE, fill, 0.18); }

    /** Regular polygon centred on the polygon origin. */
    public static Thick.Poly regular(int sides, double radius, double rotationDeg) {
        Thick.Poly p = new Thick.Poly();
        for (int i = 0; i < sides; i++) {
            double a = Math.toRadians(rotationDeg) + i * 2 * Math.PI / sides;
            p.addVertex(radius * Math.cos(a), radius * Math.sin(a));
        }
        return p;
    }

    /** Polygon from flat x,y pairs (relative to the polygon origin). */
    public static Thick.Poly poly(double... xy) {
        Thick.Poly p = new Thick.Poly();
        for (int i = 0; i + 1 < xy.length; i += 2) p.addVertex(xy[i], xy[i + 1]);
        return p;
    }

    /** Ellipse approximated by a polygon so it can be rotated (centre cx,cy). */
    public static Thick.Poly ellipse(double cx, double cy, double rx, double ry, int n) {
        Thick.Poly p = new Thick.Poly();
        for (int i = 0; i < n; i++) {
            double a = i * 2 * Math.PI / n;
            p.addVertex(cx + rx * Math.cos(a), cy + ry * Math.sin(a));
        }
        return p;
    }

    /** Five-point star centred on the origin. */
    public static Thick.Poly star(double outer, double inner) {
        Thick.Poly p = new Thick.Poly();
        for (int i = 0; i < 10; i++) {
            double rad = (i % 2 == 0) ? outer : inner;
            double a = -Math.PI / 2 + i * Math.PI / 5;
            p.addVertex(rad * Math.cos(a), rad * Math.sin(a));
        }
        return p;
    }

    /**
     * Jagged crack: a thin filled sliver that zig-zags from (x,y) in direction
     * {@code deg} for {@code length} units and tapers to a point.
     */
    public static Thick.Poly crack(double x, double y, double length, double deg, long seed) {
        int segs = 3;
        double[] xs = new double[segs + 1], ys = new double[segs + 1];
        xs[0] = x; ys[0] = y;
        double a = Math.toRadians(deg);
        for (int i = 1; i <= segs; i++) {
            double jitter = (((seed * 31 + i * 17) % 7) - 3) * 0.22;
            xs[i] = xs[i - 1] + Math.cos(a + jitter) * length / segs;
            ys[i] = ys[i - 1] + Math.sin(a + jitter) * length / segs;
        }
        double nx = -Math.sin(a), ny = Math.cos(a);
        Thick.Poly p = new Thick.Poly();
        for (int i = 0; i <= segs; i++) {
            double w = 0.9 * (1.0 - (double) i / segs) + 0.15;
            p.addVertex(xs[i] + nx * w, ys[i] + ny * w);
        }
        for (int i = segs; i >= 0; i--) {
            double w = 0.9 * (1.0 - (double) i / segs) + 0.15;
            p.addVertex(xs[i] - nx * w, ys[i] - ny * w);
        }
        return p;
    }

    /** Oval whose centre is (cx,cy). */
    public static Thick.Oval ovalAt(double cx, double cy, double w, double h) {
        return new Thick.Oval(cx - w / 2, cy - h / 2, w, h);
    }

    /** Rectangle with a thick outline. */
    public static Thick.Rect rect(double x, double y, double w, double h) { return new Thick.Rect(x, y, w, h); }

    /** Line with a thick stroke. */
    public static Thick.Line line(double x0, double y0, double x1, double y1) { return new Thick.Line(x0, y0, x1, y1); }

    /** Arc with a thick outline. */
    public static Thick.Arc arc(double x, double y, double w, double h, double start, double sweep) {
        return new Thick.Arc(x, y, w, h, start, sweep);
    }

    /** Moves an oval so its centre is (cx,cy). */
    public static void center(GOval o, double cx, double cy) {
        o.setLocation(cx - o.getWidth() / 2, cy - o.getHeight() / 2);
    }
}
