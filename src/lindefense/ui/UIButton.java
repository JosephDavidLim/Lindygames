package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import java.awt.Color;
import java.awt.Font;

/**
 * Cyberpunk button: chamfered dark-glass frame with a neon accent border, an optional vector icon, and
 * eased hover / selected / press transitions (call {@link #animate(double)} once per frame). Hit testing is done in
 * logical coordinates by the owning screen ({@link #hit(double, double)}), so the button works at any window scale.
 * Styles: normal, primary (solid accent fill), flat (segmented controls, no frame until active).
 */
public final class UIButton extends GCompound {
    private static final long serialVersionUID = 1L;

    private static final Color FILL = new Color(10, 18, 38, 225);
    private static final Color DISABLED_TEXT = new Color(0x62768f);
    private static final Color DISABLED_LINE = new Color(0x2c4260);

    private final Cyber.Chamfer frame;
    private final GLabel text;
    private final Cyber.Icon icon;
    private final double w, h;
    private Runnable action;
    private boolean enabled = true;
    private boolean selected;
    private boolean hover;
    private boolean primary;
    private boolean flat;
    private Color accent = Cyber.CYAN;
    private Color textColor = Cyber.TEXT;
    private Color textOverride;
    private double hoverT, selT, pressT, pulse;
    private double pulseAmount;
    private boolean hasIcon;

    public UIButton(double x, double y, double w, double h, String label, Font font, Runnable action) {
        this.w = w; this.h = h; this.action = action;
        frame = new Cyber.Chamfer(0, 0, w, h, Math.min(10, h * 0.28)).scan(false);
        add(frame);
        icon = new Cyber.Icon(Cyber.Kind.PLAY, 14, Cyber.TEXT);
        icon.setVisible(false);
        add(icon);
        text = new Cyber.Text(label);
        text.setFont(font);
        add(text);
        setLocation(x, y);
        setText(label);
        apply();
    }

    public UIButton(double x, double y, double w, double h, String label, Runnable action) {
        this(x, y, w, h, label, Cyber.display(Font.BOLD, 16), action);
    }

    // ------------------------------------------------------------------ configuration
    public void setText(String s) {
        text.setLabel(s);
        layoutContent();
    }

    private void layoutContent() {
        double tw = text.getWidth();
        double iw = hasIcon ? 14 : 0, gap = hasIcon && tw > 0 ? 7 : 0;
        double total = iw + gap + tw;
        double x0 = (w - total) / 2;
        icon.setLocation(x0, (h - 14) / 2);
        text.setLocation(x0 + iw + gap, (h + text.getAscent()) / 2 - 2);
    }

    public String getText() { return text.getLabel(); }
    public void setAction(Runnable r) { action = r; }
    public void setEnabled(boolean e) { enabled = e; }
    public boolean isEnabled() { return enabled; }
    public void setSelected(boolean s) { selected = s; }
    public boolean isSelected() { return selected; }
    public void setHover(boolean h) { hover = h; }
    public void setAccent(Color c) { accent = c; }
    public void setPrimary(boolean p) { primary = p; }
    public void setFlat(boolean f) { flat = f; frame.size(w, h); }
    public void setTextColor(Color c) { textOverride = c; }
    /** Breathing glow amount (0 = none); used by the primary Start Wave button while idle. */
    public void setPulse(double amount) { pulseAmount = amount; }
    public void setIcon(Cyber.Kind k) {
        hasIcon = k != null;
        icon.setVisible(hasIcon);
        if (k != null) icon.setKind(k);
        layoutContent();
    }
    /** Legacy colour setter: the fill hue becomes the accent colour. */
    public void setColors(Color fill, Color text) { if (fill != null && fill.getAlpha() > 0) { accent = fill; } textOverride = text; }
    public void setSelectedColor(Color c) { accent = c; }
    public double getW() { return w; }
    public double getH() { return h; }

    // ------------------------------------------------------------------ animation
    /** Eases hover / selected / press blends and repaints the frame colours. */
    public void animate(double dt) {
        double k = Math.min(1, dt * 14);
        hoverT += ((hover && enabled ? 1 : 0) - hoverT) * k;
        selT += ((selected ? 1 : 0) - selT) * k;
        pressT = Math.max(0, pressT - dt * 5);
        pulse += dt;
        apply();
    }

    private void apply() {
        Color acc = enabled ? accent : DISABLED_LINE;
        double act = Math.max(hoverT * 0.55, selT);
        Color fill;
        Color border;
        Color tc;
        if (!enabled) {
            fill = flat ? Cyber.alpha(FILL, 0) : new Color(8, 14, 28, 200);
            border = flat ? Cyber.alpha(DISABLED_LINE, 0) : DISABLED_LINE;
            tc = DISABLED_TEXT;
        } else if (primary) {
            Color base = Cyber.lerp(Cyber.alpha(accent, 205), Cyber.alpha(Cyber.lerp(accent, Color.WHITE, 0.35), 240), hoverT);
            fill = Cyber.lerp(base, Color.WHITE, pressT * 0.4);
            border = Cyber.lerp(accent, Color.WHITE, 0.4 + 0.3 * hoverT);
            tc = textOverride != null ? textOverride : new Color(0x031219);
        } else {
            fill = flat ? Cyber.alpha(acc, (int) (70 * selT + 40 * hoverT))
                : Cyber.lerp(FILL, Cyber.alpha(Cyber.lerp(new Color(10, 18, 38), acc, 0.28), 235), Math.min(1, act + pressT * 0.6));
            border = flat ? Cyber.alpha(acc, (int) (255 * selT)) : Cyber.lerp(Cyber.alpha(acc, 150), acc, Math.min(1, 0.45 + act));
            tc = textOverride != null ? textOverride : Cyber.lerp(Cyber.TEXT, Color.WHITE, hoverT);
            if (flat && selT < 0.5 && textOverride == null) tc = Cyber.lerp(Cyber.MUTED, Color.WHITE, hoverT);
        }
        double glow = enabled ? Math.min(1, hoverT * 0.75 + selT * 0.55 + (primary ? 0.45 : 0)) : 0;
        if (pulseAmount > 0 && enabled) glow = Math.min(1.3, glow + pulseAmount * (0.5 + 0.5 * Math.sin(pulse * 3.2)));
        frame.fill(fill).border(border).glow(glow).ticks(selected && !flat && !primary);
        Color tcol = tc;
        Cyber.setColor(text, tcol);
        icon.setIconColor(tcol);
    }

    /** True if logical point (lx, ly) lies inside the button (absolute coordinates). */
    public boolean hit(double lx, double ly) {
        return isVisible() && lx >= getX() && lx <= getX() + w && ly >= getY() && ly <= getY() + h;
    }

    /** Runs the action if enabled. Returns true if consumed. */
    public boolean click() {
        if (!enabled || !isVisible()) return false;
        pressT = 1;
        if (action != null) action.run();
        return true;
    }
}
