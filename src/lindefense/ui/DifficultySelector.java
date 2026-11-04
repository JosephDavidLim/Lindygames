package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GRect;
import java.awt.Color;
import java.awt.Font;
import lindefense.sim.Difficulty;

/**
 * Compact "Difficulty Growth" picker: four mode buttons (O(1), O(N), O(N&sup2;), O(N&#7482;)) plus a
 * one-line description of the selected mode. Selection is stored in {@link Difficulty#setSelected}
 * so {@code LindefenseProgram.startGame} can read it. Self-contained and deliberately plain so the
 * UI restyle can swap the look without touching the logic. Hit-testing uses logical coordinates.
 */
public final class DifficultySelector extends GCompound {
    private static final long serialVersionUID = 1L;
    private static final Font NAME_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private static final Font SUB_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    private static final Font DESC_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Color[] COLORS = { new Color(0x3bff8a), new Color(0x35e0ff), new Color(0xff8c3b), new Color(0xff3bd0) };

    private final double bx, by, bw, bh, gap;
    private final GRect[] boxes = new GRect[Difficulty.values().length];
    private final GLabel desc;

    /** Builds the selector with its top-left corner at (x, y); {@code width} is the total row width. */
    public DifficultySelector(double x, double y, double width) {
        Difficulty[] ds = Difficulty.values();
        gap = 10;
        bw = (width - gap * (ds.length - 1)) / ds.length;
        bh = 44;
        bx = x; by = y;
        for (int i = 0; i < ds.length; i++) {
            double px = bx + i * (bw + gap);
            GRect r = Ui.rect(px, by, bw, bh, null, COLORS[i]);
            boxes[i] = r;
            add(r);
            GLabel n = Ui.label(display(ds[i], NAME_FONT), 0, 0, NAME_FONT, COLORS[i]);
            n.setLocation(px + (bw - n.getWidth()) / 2, by + 20);
            add(n);
            GLabel s = Ui.label(ds[i].subtitle.replaceAll(" \\(.*", ""), 0, 0, SUB_FONT, Ui.MUTED);
            s.setLocation(px + (bw - s.getWidth()) / 2, by + 36);
            add(s);
        }
        desc = Ui.label("", bx, by + bh + 18, DESC_FONT, Ui.TEXT);
        add(desc);
        refresh();
    }

    /** Label for the mode: Unicode (O(N&sup2;), O(N&#7482;)) if the font can draw it, else the ASCII fallback (O(N^2), O(N^N)). */
    public static String display(Difficulty d, Font f) {
        return f.canDisplayUpTo(d.label) == -1 ? d.label : d.asciiLabel;
    }

    /** Label usable anywhere with the default UI font (HUD, result screens). */
    public static String display(Difficulty d) { return display(d, Ui.F_BOLD); }

    private void refresh() {
        Difficulty sel = Difficulty.getSelected();
        Difficulty[] ds = Difficulty.values();
        for (int i = 0; i < ds.length; i++) {
            boolean on = ds[i] == sel;
            boxes[i].setFilled(on);
            boxes[i].setFillColor(Ui.withAlpha(COLORS[i], 90));
            boxes[i].setColor(on ? Color.WHITE : COLORS[i]);
        }
        desc.setLabel(display(sel) + "  " + sel.subtitle + "  -  " + sel.description);
        desc.setColor(COLORS[sel.ordinal()]);
    }

    /** Handles a click in logical coordinates; returns true if it changed the selection. */
    public boolean mousePressed(double lx, double ly) {
        double ox = getX(), oy = getY();   // compound offset (children are placed in absolute coordinates, so this is 0 unless moved)
        Difficulty[] ds = Difficulty.values();
        for (int i = 0; i < ds.length; i++) {
            double px = bx + i * (bw + gap) + ox;
            if (lx >= px && lx <= px + bw && ly >= by + oy && ly <= by + bh + oy) {
                Difficulty.setSelected(ds[i]);
                refresh();
                return true;
            }
        }
        return false;
    }
}
