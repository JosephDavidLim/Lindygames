package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import java.awt.Color;
import java.awt.Font;
import lindefense.sim.Difficulty;

/**
 * "Difficulty Growth" picker on the map-select screen: four cyberpunk chamfered cards (O(1), O(N), O(N<sup>2</sup>),
 * O(N<sup>N</sup>)), the selected one lit with a glow and a solid accent rail, plus a one-line description of the selected
 * mode. Exponents are drawn as a small raised label next to the base instead of relying on tiny superscript glyphs.
 * Selection is stored in {@link Difficulty#setSelected}; hit-testing uses logical coordinates.
 */
public final class DifficultySelector extends GCompound {
    private static final long serialVersionUID = 1L;
    private static final Font NAME_FONT = Cyber.display(Font.BOLD, 24);
    private static final Font SUP_FONT = Cyber.display(Font.BOLD, 14);
    private static final Font SUB_FONT = Cyber.body(Font.PLAIN, 12);
    private static final Font DESC_FONT = Cyber.body(Font.PLAIN, 14);
    private static final Color[] COLORS = { new Color(0x3bff8a), new Color(0x35e0ff), new Color(0xff8c3b), new Color(0xff3bd0) };
    public static final double HEIGHT = 58;

    private final double bx, by, bw, bh, gap;
    private final Cyber.Chamfer[] boxes = new Cyber.Chamfer[Difficulty.values().length];
    private final GLabel[] subs = new GLabel[Difficulty.values().length];
    private final GCompound[] names = new GCompound[Difficulty.values().length];
    private final GLabel desc;
    private int hover = -1;

    /** Builds the selector with its top-left corner at (x, y); {@code width} is the total row width. */
    public DifficultySelector(double x, double y, double width) {
        Difficulty[] ds = Difficulty.values();
        gap = 12;
        bw = (width - gap * (ds.length - 1)) / ds.length;
        bh = HEIGHT;
        bx = x; by = y;
        for (int i = 0; i < ds.length; i++) {
            double px = bx + i * (bw + gap);
            Cyber.Chamfer c = new Cyber.Chamfer(px, by, bw, bh, 12).scan(false);
            boxes[i] = c;
            add(c);
            GCompound n = mathLabel(ds[i], NAME_FONT, SUP_FONT, COLORS[i]);
            n.setLocation(px + (bw - labelWidth(n)) / 2, by + 28);
            names[i] = n;
            add(n);
            GLabel s = Ui.label(ds[i].subtitle.replaceAll(" \\(.*", "").toUpperCase(), 0, 0, SUB_FONT, Ui.MUTED);
            s.setLocation(px + (bw - s.getWidth()) / 2, by + 47);
            subs[i] = s;
            add(s);
        }
        desc = Ui.label("", bx + 4, by + bh + 22, DESC_FONT, Ui.TEXT);
        add(desc);
        refresh();
    }

    /** Width of a label built by {@link #mathLabel} (its rightmost child's right edge). */
    private static double labelWidth(GCompound c) {
        double w = 0;
        for (int i = 0; i < c.getElementCount(); i++) w = Math.max(w, c.getElement(i).getX() + c.getElement(i).getWidth());
        return w;
    }

    /**
     * "O(N" + small raised exponent + ")" as separate labels. The exponent comes from the ASCII form ("O(N^2)", "O(N^N)"),
     * so it is drawn with the UI font at ~60% size, lifted by a third of the base height. Children are laid out from x = 0
     * with the baseline at y = 0.
     */
    public static GCompound mathLabel(Difficulty d, Font base, Font sup, Color col) {
        GCompound g = new GCompound();
        String ascii = d.asciiLabel;
        int caret = ascii.indexOf('^');
        if (caret < 0) { g.add(Ui.label(ascii, 0, 0, base, col)); return g; }
        GLabel head = Ui.label(ascii.substring(0, caret), 0, 0, base, col);
        g.add(head);
        int close = ascii.indexOf(')', caret);
        String exp = ascii.substring(caret + 1, close < 0 ? ascii.length() : close);
        GLabel up = Ui.label(exp, head.getWidth() + 1, -base.getSize() * 0.38, sup, col);
        g.add(up);
        if (close >= 0) g.add(Ui.label(ascii.substring(close), up.getX() + up.getWidth() + 1, 0, base, col));
        return g;
    }

    /** Label usable anywhere with the given UI font (HUD, result screens): Unicode superscript only where it is legible. */
    public static String display(Difficulty d, Font f) {
        if (d.label.indexOf('ᴺ') >= 0) return d.asciiLabel;   // the tiny modifier-letter N is unreadable: use O(N^N)
        return f.canDisplayUpTo(d.label) == -1 ? d.label : d.asciiLabel;
    }

    /** Label usable anywhere with the default UI font. */
    public static String display(Difficulty d) { return display(d, Ui.F_BOLD); }

    private void refresh() {
        Difficulty sel = Difficulty.getSelected();
        Difficulty[] ds = Difficulty.values();
        for (int i = 0; i < ds.length; i++) {
            boolean on = ds[i] == sel;
            boolean hv = i == hover;
            boxes[i].fill(on ? Cyber.alpha(COLORS[i], 55) : new Color(8, 14, 32, hv ? 235 : 215))
                .border(on ? COLORS[i] : Cyber.alpha(COLORS[i], hv ? 210 : 120)).glow(on ? 1.0 : hv ? 0.4 : 0).lineWidth(on ? 2.4 : 1.4).ticks(on);
            Cyber.setColor(subs[i], on ? Cyber.TEXT : Ui.MUTED);
        }
        desc.setLabel(display(sel) + "   " + sel.subtitle + "  -  " + sel.description);
        desc.setColor(COLORS[sel.ordinal()]);
    }

    private int indexAt(double lx, double ly) {
        double ox = getX(), oy = getY();
        Difficulty[] ds = Difficulty.values();
        for (int i = 0; i < ds.length; i++) {
            double px = bx + i * (bw + gap) + ox;
            if (lx >= px && lx <= px + bw && ly >= by + oy && ly <= by + bh + oy) return i;
        }
        return -1;
    }

    /** Hover highlight; returns whether the cursor is over a card. */
    public boolean mouseMoved(double lx, double ly) {
        int h = indexAt(lx, ly);
        if (h != hover) { hover = h; refresh(); }
        return h >= 0;
    }

    /** Handles a click in logical coordinates; returns true if it hit a card. */
    public boolean mousePressed(double lx, double ly) {
        int i = indexAt(lx, ly);
        if (i < 0) return false;
        Difficulty.setSelected(Difficulty.values()[i]);
        refresh();
        return true;
    }
}
