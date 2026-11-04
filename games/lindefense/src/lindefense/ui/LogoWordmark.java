package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GPolygon;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * The LINDEFENSE wordmark built entirely from GPolygons: angular, cut-corner,
 * slightly italic techno letterforms defined on a 10 x 14 grid, so the logo
 * renders identically on every machine. The compound's origin is the centre
 * of the word, which makes stacked copies at different heights line up for
 * neon glow layers. Letters are grouped so individual ones can flicker.
 */
public final class LogoWordmark extends GCompound {
    private static final long serialVersionUID = 1L;

    public static final String TEXT = "LINDEFENSE";
    private static final double W = 2.7;      // stroke width (grid units)
    private static final double C = 1.0;      // chamfer size
    private static final double GAP = 2.4;    // gap between letters
    private static final double ITALIC = 0.16; // shear per unit of height
    private static final int TL = 1, TR = 2, BR = 4, BL = 8;

    private final List<GCompound> letters = new ArrayList<>();
    private final List<GPolygon> polys = new ArrayList<>();
    private final double height;
    private final double width;
    private Color fill;
    private Color outline;
    private final boolean filled;

    public LogoWordmark(double height, Color fill, Color outline, boolean filled) {
        this.height = height;
        this.fill = fill;
        this.outline = outline;
        this.filled = filled;
        double unit = height / 14.0;
        double total = 0;
        for (char ch : TEXT.toCharArray()) total += advance(ch) + GAP;
        total -= GAP;
        this.width = total * unit + 14 * ITALIC * unit;
        double x = -total / 2.0 - 14 * ITALIC / 2.0;
        for (char ch : TEXT.toCharArray()) {
            GCompound g = new GCompound();
            for (double[][] shape : glyph(ch)) {
                GPolygon p = new GPolygon();
                for (double[] pt : shape) {
                    double lx = pt[0] + x, ly = pt[1] - 7;
                    p.addVertex((lx + (7 - ly) * ITALIC) * unit, ly * unit);
                }
                style(p);
                g.add(p);
                polys.add(p);
            }
            letters.add(g);
            add(g);
            x += advance(ch) + GAP;
        }
    }

    public double getLogicalWidth() { return width; }
    public double getLogicalHeight() { return height; }
    public int getLetterCount() { return letters.size(); }

    private void style(GPolygon p) {
        p.setFilled(filled);
        if (filled) p.setFillColor(fill);
        p.setColor(outline != null ? outline : fill);
    }

    /** Recolours every polygon (used for glow pulsing, glitch tints and flicker). */
    public void setTint(Color fill, Color outline) {
        this.fill = fill;
        this.outline = outline;
        for (GPolygon p : polys) style(p);
    }

    public void setLetterVisible(int i, boolean v) { letters.get(i).setVisible(v); }

    public void setLetterTint(int i, Color fill, Color outline) {
        for (int k = 0; k < letters.get(i).getElementCount(); k++) {
            GPolygon p = (GPolygon) letters.get(i).getElement(k);
            p.setFilled(filled);
            if (filled) p.setFillColor(fill);
            p.setColor(outline != null ? outline : fill);
        }
    }

    /** Letter centre x in local coordinates (for shimmer/flicker effects). */
    public double letterCenterX(int i) {
        double unit = height / 14.0;
        double total = 0;
        for (char ch : TEXT.toCharArray()) total += advance(ch) + GAP;
        total -= GAP;
        double x = -total / 2.0 - 14 * ITALIC / 2.0;
        for (int k = 0; k < i; k++) x += advance(TEXT.charAt(k)) + GAP;
        return (x + advance(TEXT.charAt(i)) / 2.0 + 7 * ITALIC) * unit;
    }

    private static double advance(char ch) { return ch == 'I' ? 5.2 : 10; }

    // ------------------------------------------------------------------ glyphs
    /** A chamfered bar as a polygon point list. */
    private static double[][] bar(double x1, double y1, double x2, double y2, int mask) {
        List<double[]> pts = new ArrayList<>();
        if ((mask & TL) != 0) { pts.add(new double[] { x1, y1 + C }); pts.add(new double[] { x1 + C, y1 }); } else pts.add(new double[] { x1, y1 });
        if ((mask & TR) != 0) { pts.add(new double[] { x2 - C, y1 }); pts.add(new double[] { x2, y1 + C }); } else pts.add(new double[] { x2, y1 });
        if ((mask & BR) != 0) { pts.add(new double[] { x2, y2 - C }); pts.add(new double[] { x2 - C, y2 }); } else pts.add(new double[] { x2, y2 });
        if ((mask & BL) != 0) { pts.add(new double[] { x1 + C, y2 }); pts.add(new double[] { x1, y2 - C }); } else pts.add(new double[] { x1, y2 });
        return pts.toArray(new double[0][]);
    }

    private static List<double[][]> glyph(char ch) {
        List<double[][]> s = new ArrayList<>();
        double m = 7 - W / 2; // middle bar top
        switch (ch) {
            case 'L':
                s.add(bar(0, 0, W, 14, TL));
                s.add(bar(0, 14 - W, 10, 14, BR | BL));
                break;
            case 'I':
                s.add(bar(1.2, 0, 1.2 + W, 14, TR | BL));
                break;
            case 'N':
                s.add(bar(0, 0, W, 14, TL | BL));
                s.add(bar(10 - W, 0, 10, 14, TR | BR));
                s.add(new double[][] { { 0, 0 }, { W + 1.3, 0 }, { 10, 14 }, { 10 - W - 1.3, 14 } });
                break;
            case 'D':
                s.add(bar(0, 0, W, 14, TL | BL));
                s.add(bar(0, 0, 7, W, 0));
                s.add(bar(0, 14 - W, 7, 14, 0));
                s.add(bar(10 - W, 3, 10, 11, 0));
                s.add(new double[][] { { 7, 0 }, { 10, 3 }, { 10 - W, 3.6 }, { 6.4, W } });
                s.add(new double[][] { { 10, 11 }, { 7, 14 }, { 6.4, 14 - W }, { 10 - W, 10.4 } });
                break;
            case 'E':
                s.add(bar(0, 0, W, 14, TL | BL));
                s.add(bar(0, 0, 10, W, TR));
                s.add(bar(0, m, 7.6, m + W, 0));
                s.add(bar(0, 14 - W, 10, 14, BR));
                break;
            case 'F':
                s.add(bar(0, 0, W, 14, TL | BL));
                s.add(bar(0, 0, 10, W, TR));
                s.add(bar(0, m, 7.6, m + W, 0));
                break;
            case 'S':
                s.add(bar(0, 0, 10, W, TR));
                s.add(bar(0, 0, W, m + W, TL));
                s.add(bar(0, m, 10, m + W, 0));
                s.add(bar(10 - W, m, 10, 14, BR));
                s.add(bar(0, 14 - W, 10, 14, BL));
                break;
            default:
                s.add(bar(0, 0, 10, 14, TL | TR | BR | BL));
        }
        return s;
    }
}
