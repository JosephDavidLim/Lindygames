package view;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GRoundRect;

import java.awt.Color;

/**
 * A labelled bar that stays on the canvas and only changes when its value
 * does. Rebuilding a bar every frame makes it flicker.
 */
public class StatBar extends GCompound {
    private static final long serialVersionUID = 1L;

    private final double barWidth;
    private final double barHeight;
    private final transient GRoundRect fill;
    private final transient GLabel caption;
    private int shownWidth = -1;

    /**
     * Creates a bar.
     *
     * @param x left
     * @param y top
     * @param w width
     * @param h height
     * @param color bar color
     */
    public StatBar(double x, double y, double w, double h, Color color) {
        this.barWidth = w;
        this.barHeight = h;
        add(Ui.round(0, 0, w, h, h / 2, new Color(6, 6, 18)));
        fill = Ui.round(1, 1, w - 2, h - 2, h / 2, color);
        add(fill);
        caption = Ui.centered("", "SansSerif-bold-10", Color.WHITE, w / 2, h - 2);
        add(caption);
        setLocation(x, y);
    }

    /**
     * Updates the bar.
     *
     * @param fraction how full, 0 to 1
     * @param text the caption
     */
    public void show(double fraction, String text) {
        double f = Math.max(0, Math.min(1, fraction));
        int width = (int) Math.round((barWidth - 2) * f);
        if (width != shownWidth) {
            shownWidth = width;
            fill.setVisible(width > 0);
            if (width > 0) {
                fill.setSize(Math.max(barHeight / 2, width), barHeight - 2);
            }
        }
        if (!text.equals(caption.getLabel())) {
            Ui.recenter(caption, text, barWidth / 2);
        }
    }
}
