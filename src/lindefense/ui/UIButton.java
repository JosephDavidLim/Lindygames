package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GRect;
import java.awt.Color;
import java.awt.Font;

/**
 * A clickable rectangle with a centred label. Hit testing is done in logical
 * coordinates by the owning screen (see {@link #hit(double, double)}), so the
 * button works at any window scale.
 */
public final class UIButton extends GCompound {
    private static final long serialVersionUID = 1L;

    private final GRect box;
    private final GLabel text;
    private final double w, h;
    private Runnable action;
    private boolean enabled = true;
    private boolean selected;
    private boolean hover;
    private Color fill = Ui.PANEL_LIGHT;
    private Color fillHover = new Color(0x2f3a4d);
    private Color fillSelected = new Color(0x3a5a7a);
    private Color fillDisabled = new Color(0x1a1f29);
    private Color textColor = Ui.TEXT;
    private Color textDisabled = new Color(0x5b6578);

    public UIButton(double x, double y, double w, double h, String label, Font font, Runnable action) {
        this.w = w; this.h = h; this.action = action;
        box = new GRect(0, 0, w, h);
        box.setFilled(true);
        add(box);
        text = new GLabel(label);
        text.setFont(font);
        add(text);
        setLocation(x, y);
        setText(label);
        refresh();
    }

    public UIButton(double x, double y, double w, double h, String label, Runnable action) {
        this(x, y, w, h, label, Ui.F_BOLD, action);
    }

    public void setText(String s) {
        text.setLabel(s);
        text.setLocation((w - text.getWidth()) / 2, (h + text.getAscent()) / 2 - 2);
    }

    public String getText() { return text.getLabel(); }
    public void setAction(Runnable r) { action = r; }
    public void setEnabled(boolean e) { enabled = e; refresh(); }
    public boolean isEnabled() { return enabled; }
    public void setSelected(boolean s) { selected = s; refresh(); }
    public void setHover(boolean h) { if (hover != h) { hover = h; refresh(); } }
    public void setColors(Color fill, Color text) { this.fill = fill; this.textColor = text; refresh(); }
    public void setSelectedColor(Color c) { fillSelected = c; refresh(); }
    public double getW() { return w; }
    public double getH() { return h; }

    private void refresh() {
        Color f = !enabled ? fillDisabled : selected ? fillSelected : hover ? fillHover : fill;
        box.setFillColor(f);
        box.setColor(selected ? Ui.ACCENT : new Color(0x3b4556));
        text.setColor(enabled ? textColor : textDisabled);
    }

    /** True if logical point (lx, ly) lies inside the button (absolute coordinates). */
    public boolean hit(double lx, double ly) {
        return isVisible() && lx >= getX() && lx <= getX() + w && ly >= getY() && ly <= getY() + h;
    }

    /** Runs the action if enabled. Returns true if consumed. */
    public boolean click() {
        if (!enabled || !isVisible()) return false;
        if (action != null) action.run();
        return true;
    }
}
