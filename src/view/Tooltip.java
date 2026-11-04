package view;

import acm.graphics.GCompound;
import acm.graphics.GLabel;

import java.awt.Color;

/**
 * A small dark box that follows the mouse and describes what is under it,
 * like an item tooltip in MapleStory.
 */
public class Tooltip extends GCompound {
    private static final long serialVersionUID = 1L;
    private static final int LINE = 15;
    private static final int PAD = 7;

    /**
     * Creates a hidden tooltip.
     */
    public Tooltip() {
        setVisible(false);
    }

    /**
     * Fills the tooltip and shows it near a point, kept inside the window.
     *
     * @param title first line, in gold
     * @param x mouse x
     * @param y mouse y
     * @param lines further lines
     */
    public void show(String title, double x, double y, String... lines) {
        removeAll();
        GLabel head = Ui.text(title, "SansSerif-bold-13", Ui.GOLD, PAD, 16);
        double width = head.getWidth();
        GLabel[] rows = new GLabel[lines.length];
        for (int i = 0; i < lines.length; i++) {
            rows[i] = Ui.text(lines[i], "SansSerif-plain-12", Color.WHITE, PAD, 16 + LINE * (i + 1));
            width = Math.max(width, rows[i].getWidth());
        }
        double w = width + PAD * 2;
        double h = 22 + LINE * lines.length;
        add(Ui.round(0, 0, w, h, 8, new Color(24, 28, 48, 235)));
        add(head);
        for (GLabel r : rows) {
            add(r);
        }
        setLocation(Math.max(2, Math.min(Layout.W - w - 2, x + 14)), Math.max(2, Math.min(Layout.H - h - 2, y + 14)));
        setVisible(true);
    }

    /**
     * Hides the tooltip.
     */
    public void hide() {
        setVisible(false);
    }
}
