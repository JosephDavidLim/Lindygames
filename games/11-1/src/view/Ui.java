package view;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GRect;
import acm.graphics.GRoundRect;

import java.awt.Color;

/**
 * Small helpers and the neon color palette used by every screen.
 */
public final class Ui {
    /** Chalk white text. */
    public static final Color CHALK = new Color(226, 238, 255);
    /** Chalkboard green. */
    public static final Color BOARD = new Color(8, 8, 24);
    /** Light wood. */
    public static final Color WOOD = new Color(30, 26, 66);
    /** Dark wood. */
    public static final Color WOOD_DARK = new Color(16, 14, 40);
    /** Dark brown for text on light backgrounds. */
    public static final Color INK = new Color(226, 238, 255);
    /** Highlight yellow. */
    public static final Color GOLD = new Color(70, 230, 255);
    /** Paper cream for cards. */
    public static final Color PAPER = new Color(30, 32, 72);

    /** Window frame, in the style of a MapleStory menu. */
    public static final Color FRAME = new Color(20, 20, 54);
    /** Window title bar. */
    public static final Color FRAME_TOP = new Color(66, 30, 116);
    /** Window interior. */
    public static final Color WINDOW = new Color(12, 12, 34);
    /** Health bar red. */
    public static final Color HP_RED = new Color(232, 70, 70);
    /** EXP bar yellow. */
    public static final Color EXP_YELLOW = new Color(250, 214, 60);

    /** Fill of a chosen tile, dark enough for light text. */
    public static final Color SELECTED = new Color(16, 104, 138);
    /** Quiet text on dark panels. */
    public static final Color MUTED = new Color(140, 150, 190);
    /** Warm accent for money. */
    public static final Color MONEY = new Color(255, 200, 90);
    /** Hot pink accent. */
    public static final Color PINK = new Color(255, 80, 170);
    /** Good news green. */
    public static final Color GOOD = new Color(90, 255, 170);
    /** Fill of a button that can't be pressed. */
    public static final Color DISABLED = new Color(44, 44, 66);

    private Ui() {
    }

    /**
     * Makes a label whose left baseline is at a point.
     *
     * @param s text
     * @param font font in ACM form, for example "SansSerif-bold-16"
     * @param c text color
     * @param x left edge
     * @param y baseline
     * @return the label
     */
    public static GLabel text(String s, String font, Color c, double x, double y) {
        GLabel l = new GLabel(s, x, y);
        l.setFont(font);
        l.setColor(c);
        return l;
    }

    /**
     * Makes a label centered horizontally on a point.
     *
     * @param s text
     * @param font font in ACM form
     * @param c text color
     * @param cx horizontal center
     * @param y baseline
     * @return the label
     */
    public static GLabel centered(String s, String font, Color c, double cx, double y) {
        GLabel l = text(s, font, c, 0, y);
        l.setLocation(cx - l.getWidth() / 2, y);
        return l;
    }

    /**
     * Changes a label's text and keeps it centered.
     *
     * @param l the label
     * @param s new text
     * @param cx horizontal center
     */
    public static void recenter(GLabel l, String s, double cx) {
        l.setLabel(s);
        l.setLocation(cx - l.getWidth() / 2, l.getY());
    }

    /**
     * Makes a window with a title bar, in the style of a MapleStory menu.
     *
     * @param title text in the title bar
     * @param x left
     * @param y top
     * @param w width
     * @param h height
     * @return the window, located at (x, y)
     */
    public static GCompound window(String title, double x, double y, double w, double h) {
        GCompound c = new GCompound();
        c.add(round(0, 0, w, h, 14, FRAME));
        acm.graphics.GRoundRect edge = new acm.graphics.GRoundRect(0, 0, w, h, 14);
        edge.setColor(new Color(70, 230, 255, 170));
        c.add(edge);
        c.add(round(3, 3, w - 6, 26, 10, FRAME_TOP));
        c.add(text(title, "SansSerif-bold-14", GOLD, 12, 21));
        c.add(round(6, 32, w - 12, h - 38, 8, WINDOW));
        c.setLocation(x, y);
        return c;
    }

    /**
     * Splits text into lines that fit.
     *
     * @param text the text
     * @param width most characters per line
     * @return the lines
     */
    public static java.util.List<String> wrap(String text, int width) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        lines.add(line.toString());
        return lines;
    }

    /**
     * Makes a labelled progress bar.
     *
     * @param x left
     * @param y top
     * @param w width
     * @param h height
     * @param fraction how full, 0 to 1
     * @param fill bar color
     * @param caption text on the bar, may be empty
     * @return the bar
     */
    public static GCompound bar(double x, double y, double w, double h, double fraction, Color fill, String caption) {
        GCompound c = new GCompound();
        c.add(round(0, 0, w, h, h / 2, new Color(6, 6, 18)));
        double f = Math.max(0, Math.min(1, fraction));
        if (f > 0) {
            c.add(round(1, 1, Math.max(h / 2, (w - 2) * f), h - 2, h / 2, fill));
        }
        if (!caption.isEmpty()) {
            c.add(centered(caption, "SansSerif-bold-10", Color.WHITE, w / 2, h - 2));
        }
        c.setLocation(x, y);
        return c;
    }

    /**
     * Makes a filled rectangle.
     *
     * @param x left
     * @param y top
     * @param w width
     * @param h height
     * @param fill fill color (alpha allowed)
     * @return the rectangle
     */
    public static GRect rect(double x, double y, double w, double h, Color fill) {
        GRect r = new GRect(x, y, w, h);
        r.setFilled(true);
        r.setColor(fill);
        return r;
    }

    /**
     * Makes a filled rounded rectangle.
     *
     * @param x left
     * @param y top
     * @param w width
     * @param h height
     * @param arc corner size
     * @param fill fill color (alpha allowed)
     * @return the rectangle
     */
    public static GRoundRect round(double x, double y, double w, double h, double arc, Color fill) {
        GRoundRect r = new GRoundRect(x, y, w, h, arc);
        r.setFilled(true);
        r.setColor(fill);
        return r;
    }
}
