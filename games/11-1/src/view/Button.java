package view;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GRoundRect;

import java.awt.Color;

/**
 * A clickable rounded button made of a rectangle and a centered label.
 */
public class Button {
    private final String id;
    private final double x;
    private final double y;
    private final double w;
    private final double h;
    private final GCompound shape = new GCompound();
    private final GRoundRect bg;
    private final GLabel label;
    private final GRoundRect glow;
    private boolean enabled = true;

    /**
     * Creates a button.
     *
     * @param id name the screen uses to tell which button was clicked
     * @param text text on the button
     * @param x left edge in pixels
     * @param y top edge in pixels
     * @param w width
     * @param h height
     * @param fill background color
     * @param textColor text color
     * @param fontSize size of the label font
     */
    public Button(String id, String text, double x, double y, double w, double h,
                  Color fill, Color textColor, int fontSize) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.bg = Ui.round(0, 0, w, h, 14, fill);
        shape.add(bg);
        this.label = Ui.centered(text, "SansSerif-bold-" + fontSize, textFor(fill, textColor), w / 2,
                h / 2 + fontSize * 0.36);
        shape.add(label);
        this.glow = Ui.round(0, 0, w, h, 14, new Color(255, 255, 255, 60));
        glow.setVisible(false);
        shape.add(glow);
    }

    private static Color textFor(Color fill, Color wanted) {
        boolean lightFill = fill.getRed() * 0.3 + fill.getGreen() * 0.59 + fill.getBlue() * 0.11 > 140;
        return lightFill ? new Color(8, 8, 24) : wanted;
    }

    /**
     * Lights the button while the mouse is over it.
     *
     * @param on true while hovered
     */
    public void setHover(boolean on) {
        glow.setVisible(on && enabled);
    }

    /**
     * Tells whether a point is on this button.
     *
     * @param px x in pixels
     * @param py y in pixels
     * @return true if the point is inside
     */
    public boolean contains(double px, double py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    /**
     * Enables or disables the button. A disabled button is grayed and ignores clicks.
     *
     * @param on true to enable
     * @param fill background color to use when enabled
     */
    public void setEnabled(boolean on, Color fill) {
        enabled = on;
        Color c = on ? fill : Ui.DISABLED;
        bg.setFillColor(c);
        bg.setColor(c);
        label.setColor(on ? textFor(fill, Ui.INK) : Ui.MUTED);
    }

    /**
     * Changes the button's background color, used for on/off toggles.
     *
     * @param c the new background color
     */
    public void setFill(Color c) {
        bg.setFillColor(c);
        bg.setColor(c);
        label.setColor(textFor(c, Ui.INK));
    }

    /**
     * Changes the button's text.
     *
     * @param text new text
     */
    public void setText(String text) {
        Ui.recenter(label, text, w / 2);
    }

    /**
     * Gets the button id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Tells whether the button accepts clicks.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the graphics object to add to a canvas at {@link #getX()}, {@link #getY()}.
     *
     * @return the compound
     */
    public GCompound getShape() {
        return shape;
    }

    /**
     * Gets the left edge.
     *
     * @return x in pixels
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the top edge.
     *
     * @return y in pixels
     */
    public double getY() {
        return y;
    }
}
