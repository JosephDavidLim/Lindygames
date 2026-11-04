package lindefense.ui.sprites;

import acm.graphics.GArc;
import acm.graphics.GLine;
import acm.graphics.GMath;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * ACM primitives that paint with a configurable, round-joined outline width
 * (and anti-aliasing) instead of the fixed one-pixel stroke. The cartoon art
 * direction wants a thick dark outline on every shape; these are the only
 * primitives the sprites use.
 *
 * <h3>Paint cost</h3>
 * Every visible shape is an anti-aliased rasterization (two when it has an outline), and in a big fight there are
 * thousands per frame, so these primitives avoid work that cannot change a pixel:
 * <ul>
 *   <li>an outline of width 0 or with a fully transparent colour is not stroked (the fill is drawn alone, exactly as
 *       ACM would draw it); a fully transparent fill is not filled;</li>
 *   <li>anti-aliasing is switched on only if the {@link Graphics2D} does not already have it (the game's root layer
 *       sets all quality hints once per frame), instead of re-setting the hint for every shape;</li>
 *   <li><b>level of detail</b>: each shape has a {@link Outlined#setDetail detail} level (0 = essential). While
 *       {@link #level()} is at or above a shape's level the shape is skipped at paint time. The world view raises the
 *       level when many enemies are on screen; sprites tag gloss, shadows, eyes, cracks and similar small parts. Skipping
 *       at paint time leaves visibility, z-order and every sprite's own logic untouched. A shape can also have an
 *       {@link Outlined#setOutlineDetail outline detail} tier at which it keeps its fill but drops its outline.</li>
 * </ul>
 */
public final class Thick {
    private Thick() { }

    /** Default outline width for ordinary enemies, in logical px. */
    public static final double NORMAL = 2.2;
    /** Default outline width for boss hull parts. */
    public static final double BOSS = 3.4;
    /** Outline width for small detail parts (bars, cells, rivets). */
    public static final double DETAIL = 1.4;
    /** Width used for stroke-only accents (seams, edges, rings). */
    public static final double LINE = 1.8;

    /** Detail level of decorative parts dropped first in crowds (gloss, shadows, cracks, overlay sub-details). */
    public static final int DETAIL_DECOR = 1;
    /** Detail level of small features dropped in very large crowds (eyes, rivets, thin detail parts). */
    public static final int DETAIL_FINE = 2;

    /** Current level of detail being painted (0 = everything). Painting happens on the event thread only. */
    private static int level;

    /**
     * Sets the level of detail for subsequent paints and returns the previous one (restore it when done).
     * Shapes whose detail level is non-zero and at most {@code lod} are skipped.
     */
    public static int setLevel(int lod) {
        int old = level;
        level = lod;
        return old;
    }

    /** The level of detail currently being painted. */
    public static int level() { return level; }

    /** A shape whose outline width can be set. */
    public interface Outlined {
        /** Sets the outline width in logical px (0 hides the outline). */
        void setLineWidth(double w);
        /** Current outline width, or negative if never set. */
        double getLineWidth();
        /** Sets the level-of-detail tier of this shape (0 = always drawn, see {@link Thick#setLevel}). */
        void setDetail(int detail);
        /** The level-of-detail tier of this shape. */
        int getDetail();
        /** Sets the tier at which only the outline is dropped (the fill is still drawn); 0 = never. */
        void setOutlineDetail(int detail);
    }

    private static Stroke stroke(double w) {
        return new BasicStroke((float) Math.max(0.1, w), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    private static Stroke begin(Graphics g, Stroke s) {
        Graphics2D g2 = (Graphics2D) g;
        Stroke old = g2.getStroke();
        if (g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING) != RenderingHints.VALUE_ANTIALIAS_ON) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        g2.setStroke(s);
        return old;
    }

    /** True if a shape of this detail tier is skipped at the current level. */
    private static boolean skipped(int detail) { return detail != 0 && detail <= level; }

    /** True if an outline of this width and colour would change pixels. */
    private static boolean strokes(double width, Color c) { return width != 0 && (c == null || c.getAlpha() != 0); }

    private static boolean visible(Color c) { return c == null || c.getAlpha() != 0; }

    /** Turns anti-aliasing on for a fill-only paint if the context does not have it yet. */
    private static void aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING) != RenderingHints.VALUE_ANTIALIAS_ON) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    /** Oval with a thick outline. */
    public static class Oval extends GOval implements Outlined {
        private static final long serialVersionUID = 1L;
        private double width = -1;
        private int detail, outlineDetail;
        private transient Stroke s;
        /** See {@link GOval#GOval(double, double, double, double)}. */
        public Oval(double x, double y, double w, double h) { super(x, y, w, h); }
        @Override public void setLineWidth(double w) { width = w; s = stroke(w); }
        @Override public double getLineWidth() { return width; }
        @Override public void setDetail(int d) { detail = d; }
        @Override public int getDetail() { return detail; }
        @Override public void setOutlineDetail(int d) { outlineDetail = d; }
        @Override public void paint(Graphics g) {
            if (skipped(detail)) return;
            if (!strokes(width, getColor()) || skipped(outlineDetail)) {
                if (isFilled() && visible(getFillColor())) {
                    aa(g);
                    Rectangle r = getAWTBounds();
                    g.setColor(getFillColor());
                    g.fillOval(r.x, r.y, r.width, r.height);
                }
                return;
            }
            if (s == null) s = stroke(width < 0 ? 1 : width);
            Stroke old = begin(g, s);
            super.paint(g);
            ((Graphics2D) g).setStroke(old);
        }
    }

    /** Rectangle with a thick outline. */
    public static class Rect extends GRect implements Outlined {
        private static final long serialVersionUID = 1L;
        private double width = -1;
        private int detail, outlineDetail;
        private transient Stroke s;
        /** See {@link GRect#GRect(double, double, double, double)}. */
        public Rect(double x, double y, double w, double h) { super(x, y, w, h); }
        @Override public void setLineWidth(double w) { width = w; s = stroke(w); }
        @Override public double getLineWidth() { return width; }
        @Override public void setDetail(int d) { detail = d; }
        @Override public int getDetail() { return detail; }
        @Override public void setOutlineDetail(int d) { outlineDetail = d; }
        @Override public void paint(Graphics g) {
            if (skipped(detail)) return;
            if (!strokes(width, getColor()) || skipped(outlineDetail)) {
                if (isFilled() && visible(getFillColor())) {
                    aa(g);
                    Rectangle r = getAWTBounds();
                    g.setColor(getFillColor());
                    g.fillRect(r.x, r.y, r.width, r.height);
                }
                return;
            }
            if (s == null) s = stroke(width < 0 ? 1 : width);
            Stroke old = begin(g, s);
            super.paint(g);
            ((Graphics2D) g).setStroke(old);
        }
    }

    /** Polygon with a thick outline. */
    public static class Poly extends GPolygon implements Outlined {
        private static final long serialVersionUID = 1L;
        private double width = -1;
        private int detail, outlineDetail;
        private transient Stroke s;
        /** Empty polygon at the origin. */
        public Poly() { super(); }
        @Override public void setLineWidth(double w) { width = w; s = stroke(w); }
        @Override public double getLineWidth() { return width; }
        @Override public void setDetail(int d) { detail = d; }
        @Override public int getDetail() { return detail; }
        @Override public void setOutlineDetail(int d) { outlineDetail = d; }
        @Override public void paint(Graphics g) {
            if (skipped(detail)) return;
            if (!strokes(width, getColor()) || skipped(outlineDetail)) {
                if (isFilled() && visible(getFillColor())) {
                    Polygon p = getPolygon();
                    if (p.npoints == 0) return;
                    aa(g);
                    g.setColor(getFillColor());
                    g.fillPolygon(p.xpoints, p.ypoints, p.npoints);
                }
                return;
            }
            if (s == null) s = stroke(width < 0 ? 1 : width);
            Stroke old = begin(g, s);
            super.paint(g);
            ((Graphics2D) g).setStroke(old);
        }
    }

    /** Arc with a thick outline. */
    public static class Arc extends GArc implements Outlined {
        private static final long serialVersionUID = 1L;
        private double width = -1;
        private int detail, outlineDetail;
        private transient Stroke s;
        /** See {@link GArc#GArc(double, double, double, double, double, double)}. */
        public Arc(double x, double y, double w, double h, double start, double sweep) { super(x, y, w, h, start, sweep); }
        @Override public void setLineWidth(double w) { width = w; s = stroke(w); }
        @Override public double getLineWidth() { return width; }
        @Override public void setDetail(int d) { detail = d; }
        @Override public int getDetail() { return detail; }
        @Override public void setOutlineDetail(int d) { outlineDetail = d; }
        @Override public void paint(Graphics g) {
            if (skipped(detail)) return;
            if (!strokes(width, getColor()) || skipped(outlineDetail)) {
                if (isFilled() && visible(getFillColor())) {
                    aa(g);
                    Rectangle r = getAWTBounds();
                    g.setColor(getFillColor());
                    g.fillArc(r.x, r.y, r.width, r.height, GMath.round(getStartAngle()), GMath.round(getSweepAngle()));
                }
                return;
            }
            if (s == null) s = stroke(width < 0 ? 1 : width);
            Stroke old = begin(g, s);
            super.paint(g);
            ((Graphics2D) g).setStroke(old);
        }
    }

    /** Line with a thick stroke. */
    public static class Line extends GLine implements Outlined {
        private static final long serialVersionUID = 1L;
        private double width = -1;
        private int detail, outlineDetail;
        private transient Stroke s;
        /** See {@link GLine#GLine(double, double, double, double)}. */
        public Line(double x0, double y0, double x1, double y1) { super(x0, y0, x1, y1); }
        @Override public void setLineWidth(double w) { width = w; s = stroke(w); }
        @Override public double getLineWidth() { return width; }
        @Override public void setDetail(int d) { detail = d; }
        @Override public int getDetail() { return detail; }
        @Override public void setOutlineDetail(int d) { outlineDetail = d; }
        @Override public void paint(Graphics g) {
            if (skipped(detail) || skipped(outlineDetail) || !strokes(width, getColor())) return;
            if (s == null) s = stroke(width < 0 ? 1 : width);
            Stroke old = begin(g, s);
            super.paint(g);
            ((Graphics2D) g).setStroke(old);
        }
    }
}
