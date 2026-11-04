package lindefense.ui;

import acm.graphics.GCompound;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Root compound that renders its children in a fixed logical coordinate space
 * ({@link Ui#W} x {@link Ui#H}) scaled uniformly to fit the current canvas size,
 * centred with letterboxing. Mouse coordinates are mapped back with
 * {@link #toLogicalX(double)} / {@link #toLogicalY(double)}.
 */
public class ScaledLayer extends GCompound {
    private static final long serialVersionUID = 1L;

    private double scale = 1;
    private double offX, offY;
    private int canvasW = Ui.W, canvasH = Ui.H;

    /** Recomputes scale and offsets for a canvas of the given pixel size. */
    public void fit(int w, int h) {
        canvasW = Math.max(1, w);
        canvasH = Math.max(1, h);
        scale = Math.min(canvasW / (double) Ui.W, canvasH / (double) Ui.H);
        offX = (canvasW - Ui.W * scale) / 2;
        offY = (canvasH - Ui.H * scale) / 2;
    }

    public double getScale() { return scale; }
    public double toLogicalX(double px) { return (px - offX) / scale; }
    public double toLogicalY(double py) { return (py - offY) / scale; }

    @Override public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Cyber.quality(g2);
            g2.translate(offX, offY);
            g2.scale(scale, scale);
            g2.clipRect(0, 0, Ui.W, Ui.H);
            super.paint(g2);
        } finally {
            g2.dispose();
        }
    }
}
