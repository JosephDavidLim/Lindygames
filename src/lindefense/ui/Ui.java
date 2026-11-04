package lindefense.ui;

import acm.graphics.GLabel;
import acm.graphics.GOval;
import acm.graphics.GRect;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/** Shared UI constants and small helpers (fonts, colours, text wrapping). */
public final class Ui {
    private Ui() { }

    /** Logical canvas size; everything is laid out in these units and scaled to fit. */
    public static final int W = 1600;
    public static final int H = 1000;
    public static final int TOP_H = 48;
    public static final int WORLD_X = 0;
    public static final int WORLD_Y = 48;
    public static final int WORLD_W = 1200;
    public static final int WORLD_H = 900;
    public static final int BOTTOM_Y = 948;
    public static final int PANEL_X = 1200;
    public static final int PANEL_W = 400;

    public static final Font F_TITLE = new Font("SansSerif", Font.BOLD, 64);
    public static final Font F_H1 = new Font("SansSerif", Font.BOLD, 26);
    public static final Font F_H2 = new Font("SansSerif", Font.BOLD, 18);
    public static final Font F_BODY = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font F_BOLD = new Font("SansSerif", Font.BOLD, 14);
    public static final Font F_SMALL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font F_TINY = new Font("SansSerif", Font.PLAIN, 10);

    public static final Color BG = new Color(0x0f1218);
    public static final Color PANEL = new Color(0x171c26);
    public static final Color PANEL_LIGHT = new Color(0x222a38);
    public static final Color TEXT = new Color(0xe8ecf2);
    public static final Color MUTED = new Color(0x97a3b6);
    public static final Color ACCENT = new Color(0x7fd1ff);
    public static final Color GOLD = new Color(0xffd166);
    public static final Color GREEN = new Color(0x7fd8a8);
    public static final Color RED = new Color(0xff5a5a);

    public static Color c(int rgb) { return new Color(rgb); }
    public static Color c(int rgb, int alpha) { return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha); }
    public static Color withAlpha(Color col, int alpha) { return new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha); }

    public static GLabel label(String text, double x, double y, Font f, Color col) {
        GLabel l = new GLabel(text, x, y);
        l.setFont(f);
        l.setColor(col);
        return l;
    }

    public static GRect rect(double x, double y, double w, double h, Color fill, Color outline) {
        GRect r = new GRect(x, y, w, h);
        if (fill != null) { r.setFilled(true); r.setFillColor(fill); }
        r.setColor(outline != null ? outline : (fill != null ? fill : TEXT));
        return r;
    }

    public static GOval oval(double x, double y, double w, double h, Color fill, Color outline) {
        GOval o = new GOval(x, y, w, h);
        if (fill != null) { o.setFilled(true); o.setFillColor(fill); }
        o.setColor(outline != null ? outline : (fill != null ? fill : TEXT));
        return o;
    }

    /** Greedy word wrap using GLabel measurement. */
    public static List<String> wrap(String text, Font f, double maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null) return lines;
        GLabel probe = new GLabel("");
        probe.setFont(f);
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String trial = cur.length() == 0 ? word : cur + " " + word;
            probe.setLabel(trial);
            if (probe.getWidth() > maxWidth && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(trial);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    public static String money(double v) { return "$" + (long) Math.floor(v); }

    public static String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.valueOf((long) Math.rint(v));
        return String.format("%.1f", v);
    }
}
