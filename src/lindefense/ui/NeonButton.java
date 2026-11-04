package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GPolygon;
import java.awt.Color;
import java.awt.Font;

/**
 * Cyberpunk menu button: cut-corner neon outline with stacked glow layers that
 * brighten on hover. Same hit/click contract as {@link UIButton}.
 */
public final class NeonButton extends GCompound {
    private static final long serialVersionUID = 1L;

    private final double w, h;
    private final GPolygon fill;
    private final GPolygon edge;
    private final GPolygon[] glow = new GPolygon[3];
    private final GLabel text;
    private final Color neon;
    private final Runnable action;
    private boolean hover;
    private double pulse;

    public NeonButton(double x, double y, double w, double h, String label, Font font, Color neon, Runnable action) {
        this.w = w; this.h = h; this.neon = neon; this.action = action;
        double c = 14;
        for (int i = 2; i >= 0; i--) {
            glow[i] = cut(-(i + 1) * 3, -(i + 1) * 3, w + (i + 1) * 6, h + (i + 1) * 6, c + (i + 1) * 2);
            glow[i].setColor(Ui.withAlpha(neon, 0));
            add(glow[i]);
        }
        fill = cut(0, 0, w, h, c);
        fill.setFilled(true);
        fill.setFillColor(new Color(0x0b1024));
        fill.setColor(neon);
        add(fill);
        edge = cut(3, 3, w - 6, h - 6, c - 2);
        edge.setColor(Ui.withAlpha(neon, 90));
        add(edge);
        text = new GLabel(label);
        text.setFont(font);
        text.setColor(neon);
        text.setLocation((w - text.getWidth()) / 2, (h + text.getAscent()) / 2 - 3);
        add(text);
        setLocation(x, y);
    }

    private static GPolygon cut(double x, double y, double w, double h, double c) {
        GPolygon p = new GPolygon();
        p.addVertex(x + c, y); p.addVertex(x + w, y); p.addVertex(x + w, y + h - c);
        p.addVertex(x + w - c, y + h); p.addVertex(x, y + h); p.addVertex(x, y + c);
        return p;
    }

    public boolean hit(double lx, double ly) {
        return isVisible() && lx >= getX() && lx <= getX() + w && ly >= getY() && ly <= getY() + h;
    }

    public boolean click() {
        if (!isVisible()) return false;
        if (action != null) action.run();
        return true;
    }

    public void setHover(boolean h) { hover = h; }

    private static Color blend(Color a, Color b, double f) {
        return new Color((int) (a.getRed() * f + b.getRed() * (1 - f)), (int) (a.getGreen() * f + b.getGreen() * (1 - f)), (int) (a.getBlue() * f + b.getBlue() * (1 - f)));
    }

    /** Animates glow; t advances pulse. */
    public void animate(double dt) {
        pulse += dt;
        double base = hover ? 1.0 : 0.25 + 0.15 * Math.sin(pulse * 3);
        for (int i = 0; i < 3; i++) glow[i].setColor(Ui.withAlpha(neon, (int) (base * (110 - 30 * i))));
        fill.setFillColor(hover ? blend(neon, new Color(0x0b1024), 0.28) : new Color(0x0b1024));
        text.setColor(hover ? Color.WHITE : neon);
    }
}
