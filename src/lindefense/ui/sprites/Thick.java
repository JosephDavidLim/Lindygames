package lindefense.ui.sprites;

import acm.graphics.GArc;
import acm.graphics.GLine;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * ACM primitives that paint with a configurable, round-joined outline width
 * (and anti-aliasing) instead of the fixed one-pixel stroke. The cartoon art
 * direction wants a thick dark outline on every shape; these are the only
 * primitives the sprites use.
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

    /** A shape whose outline width can be set. */
    public interface Outlined {
        /** Sets the outline width in logical px (0 hides the outline). */
        void setLineWidth(double w);
        /** Current outline width, or negative if never set. */
        double getLineWidth();
    }

    private static Stroke stroke(double w) {
        return new BasicStroke((float) Math.max(0.1, w), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    private static Stroke begin(Graphics g, Stroke s) {
        Graphics2D g2 = (Graphics2D) g;
        Stroke old = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(s);
        return old;
    }

    /** Oval with a thick outline. */
    public static class Oval extends GOval implements Outlined {
        private static final long serialVersionUID = 1L;
        private double width = -1;
        private transient Stroke s;
        /** See {@link GOval#GOval(double, double, double, double)}. */
        public Oval(double x, double y, double w, double h) { super(x, y, w, h); }
        @Override public void setLineWidth(double w) { width = w; s = stroke(w); }
        @Override public double getLineWidth() { return width; }
        @Override public void paint(Graphics g) {
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
        private transient Stroke s;
        /** See {@link GRect#GRect(double, double, double, double)}. */
        public Rect(double x, double y, double w, double h) { super(x, y, w, h); }
        @Override public void setLineWidth(double w) { width = w; s = stroke(w); }
        @Override public double getLineWidth() { return width; }
        @Override public void paint(Graphics g) {
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
        private transient Stroke s;
        /** Empty polygon at the origin. */
        public Poly() { super(); }
        @Override public void setLineWidth(double w) { width = w; s = stroke(w); }
        @Override public double getLineWidth() { return width; }
        @Override public void paint(Graphics g) {
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
        private transient Stroke s;
        /** See {@link GArc#GArc(double, double, double, double, double, double)}. */
        public Arc(double x, double y, double w, double h, double start, double sweep) { super(x, y, w, h, start, sweep); }
        @Override public void setLineWidth(double w) { width = w; s = stroke(w); }
        @Override public double getLineWidth() { return width; }
        @Override public void paint(Graphics g) {
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
        private transient Stroke s;
        /** See {@link GLine#GLine(double, double, double, double)}. */
        public Line(double x0, double y0, double x1, double y1) { super(x0, y0, x1, y1); }
        @Override public void setLineWidth(double w) { width = w; s = stroke(w); }
        @Override public double getLineWidth() { return width; }
        @Override public void paint(Graphics g) {
            if (s == null) s = stroke(width < 0 ? 1 : width);
            Stroke old = begin(g, s);
            super.paint(g);
            ((Graphics2D) g).setStroke(old);
        }
    }
}
