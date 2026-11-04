package lindefense.ui.sprites.towers;

import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import lindefense.ui.sprites.Thick;

/**
 * The plain ACM shapes used by tower, projectile and attack-effect art ({@link Art}): an {@link GOval},
 * {@link GRect} and {@link GPolygon} that paint exactly like the ACM originals (fill, then the default one-pixel
 * outline) but skip work that cannot change a pixel or that the current level of detail drops:
 * <ul>
 *   <li>a fully transparent fill or outline is not drawn at all (glows, gloss, shadows and effect bars use
 *       transparent outlines; each skipped outline is one anti-aliased rasterization saved per frame);</li>
 *   <li>a shape with a {@link #setDetail detail} tier is skipped while the world view paints at that
 *       {@link Thick#level() level of detail} or above (decorative shadow, gloss, sparkles, glows);</li>
 *   <li>a shape with an {@link #setOutlineDetail outline tier} keeps its fill but drops the one-pixel outline at that
 *       level (in very large fights).</li>
 * </ul>
 * The tiers are {@link Thick#DETAIL_DECOR} and {@link Thick#DETAIL_FINE}, shared with the enemy sprites.
 */
public final class Flat {
    private Flat() { }

    /** A shape with level-of-detail tiers. */
    public interface Detailed {
        /** Tier at which the whole shape is skipped (0 = always drawn). */
        void setDetail(int tier);
        /** Tier at which only the outline is dropped (0 = always drawn). */
        void setOutlineDetail(int tier);
    }

    private static boolean skipped(int tier) { return tier != 0 && tier <= Thick.level(); }

    private static boolean visible(Color c) { return c == null || c.getAlpha() != 0; }

    /** Tags any shape that supports it (other GObjects are left alone); returns it for chaining. */
    public static <T> T detail(T g, int tier) {
        if (g instanceof Detailed) ((Detailed) g).setDetail(tier);
        return g;
    }

    /** {@link GOval} with transparent-part skipping and detail tiers. */
    public static class Oval extends GOval implements Detailed {
        private static final long serialVersionUID = 1L;
        private int detail, outlineDetail;
        /** See {@link GOval#GOval(double, double, double, double)}. */
        public Oval(double x, double y, double w, double h) { super(x, y, w, h); }
        @Override public void setDetail(int tier) { detail = tier; }
        @Override public void setOutlineDetail(int tier) { outlineDetail = tier; }
        @Override public void paint(Graphics g) {
            if (skipped(detail)) return;
            boolean fill = isFilled() && visible(getFillColor());
            boolean line = visible(getColor()) && !skipped(outlineDetail);
            if (fill && line) { super.paint(g); return; }
            if (!fill && !line) return;
            Rectangle r = getAWTBounds();
            if (fill) { g.setColor(getFillColor()); g.fillOval(r.x, r.y, r.width, r.height); }
            else { g.setColor(getColor()); g.drawOval(r.x, r.y, r.width, r.height); }
        }
    }

    /** {@link GRect} with transparent-part skipping and detail tiers. */
    public static class Rect extends GRect implements Detailed {
        private static final long serialVersionUID = 1L;
        private int detail, outlineDetail;
        /** See {@link GRect#GRect(double, double, double, double)}. */
        public Rect(double x, double y, double w, double h) { super(x, y, w, h); }
        @Override public void setDetail(int tier) { detail = tier; }
        @Override public void setOutlineDetail(int tier) { outlineDetail = tier; }
        @Override public void paint(Graphics g) {
            if (skipped(detail)) return;
            boolean fill = isFilled() && visible(getFillColor());
            boolean line = visible(getColor()) && !skipped(outlineDetail);
            if (fill && line) { super.paint(g); return; }
            if (!fill && !line) return;
            Rectangle r = getAWTBounds();
            if (fill) { g.setColor(getFillColor()); g.fillRect(r.x, r.y, r.width, r.height); }
            else { g.setColor(getColor()); g.drawRect(r.x, r.y, r.width, r.height); }
        }
    }

    /** {@link GPolygon} with transparent-part skipping and detail tiers. */
    public static class Poly extends GPolygon implements Detailed {
        private static final long serialVersionUID = 1L;
        private int detail, outlineDetail;
        /** Empty polygon at the origin. */
        public Poly() { super(); }
        @Override public void setDetail(int tier) { detail = tier; }
        @Override public void setOutlineDetail(int tier) { outlineDetail = tier; }
        @Override public void paint(Graphics g) {
            if (skipped(detail)) return;
            boolean fill = isFilled() && visible(getFillColor());
            boolean line = visible(getColor()) && !skipped(outlineDetail);
            if (fill && line) { super.paint(g); return; }
            if (!fill && !line) return;
            Polygon p = getPolygon();
            if (p.npoints == 0) return;
            if (fill) { g.setColor(getFillColor()); g.fillPolygon(p.xpoints, p.ypoints, p.npoints); }
            else { g.setColor(getColor()); g.drawPolygon(p.xpoints, p.ypoints, p.npoints); }
        }
    }
}
