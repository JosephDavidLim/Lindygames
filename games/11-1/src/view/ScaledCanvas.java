package view;

import acm.graphics.GCanvas;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A canvas that draws the game at whatever size the window is. Screens always
 * work in the same {@link Layout} coordinates. The picture keeps its shape,
 * with black bars filling any leftover space.
 */
public class ScaledCanvas extends GCanvas {
    private static final long serialVersionUID = 1L;

    /**
     * Gets how much the picture is enlarged.
     *
     * @return the scale, 1.0 at the original size
     */
    public double getScale() {
        return Math.max(0.1, Math.min((double) getWidth() / Layout.W, (double) getHeight() / Layout.H));
    }

    private double offsetX() {
        return (getWidth() - Layout.W * getScale()) / 2;
    }

    private double offsetY() {
        return (getHeight() - Layout.H * getScale()) / 2;
    }

    /**
     * Converts a window position to a game position.
     *
     * @param x x in window pixels
     * @return x in game coordinates
     */
    public double toGameX(double x) {
        return (x - offsetX()) / getScale();
    }

    /**
     * Converts a window position to a game position.
     *
     * @param y y in window pixels
     * @return y in game coordinates
     */
    public double toGameY(double y) {
        return (y - offsetY()) / getScale();
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(offsetX(), offsetY());
        g2.scale(getScale(), getScale());
        g2.clipRect(0, 0, Layout.W, Layout.H);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paint(g2);
        g2.dispose();
    }
}
