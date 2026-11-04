package lindefense.ui;

import acm.graphics.GObject;
import acm.graphics.GRectangle;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

/**
 * Radial ability-cooldown indicator: a dark track ring, a coloured arc that fills clockwise from 12 o'clock as the
 * cooldown elapses, and (when ready) a pulsing glow ring. Centred on its location, mutated every frame, never rebuilt.
 * Shared by {@link TowerSprite} (around the tower) and {@link AbilityBar} (around each ability icon).
 */
final class RadialCooldown extends GObject {
    private static final long serialVersionUID = 1L;
    private final double radius, width;
    private double progress = 1;      // 0 = just fired, 1 = ready
    private boolean ready, locked;
    private double pulse;
    private Color color = Cyber.MAGENTA;
    private static final Color TRACK = new Color(10, 12, 24, 215);
    private final BasicStroke W_MAIN, W_TRACK, W_GLOW;
    private final Arc2D.Double arc = new Arc2D.Double(Arc2D.OPEN);
    private final Ellipse2D.Double ell = new Ellipse2D.Double();

    RadialCooldown(double radius, double width) { this.radius = radius; this.width = width;
        W_MAIN = new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        W_TRACK = new BasicStroke((float) (width + 2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        W_GLOW = new BasicStroke((float) (width + 3), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    void set(double progress, boolean ready, boolean locked, Color color, double pulse) {
        this.progress = progress; this.ready = ready; this.locked = locked; this.color = color; this.pulse = pulse;
    }

    @Override public GRectangle getBounds() { return new GRectangle(getX() - radius - 6, getY() - radius - 6, 2 * radius + 12, 2 * radius + 12); }

    @Override public void paint(Graphics g) {
        int lod = lindefense.ui.sprites.Thick.level();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            double cx = getX(), cy = getY();
            if (lod >= 2) {   // crowded screen: just the filling arc (or a solid ring when ready), no track or glow
                if (locked) g2.setColor(Cyber.alpha(Cyber.RED, 200));
                else g2.setColor(ready ? Cyber.lerp(color, Color.WHITE, 0.35) : Cyber.alpha(color, 235));
                g2.setStroke(W_MAIN);
                arc.setArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, ready || locked ? -360 : -360 * Math.max(0, Math.min(1, progress)), Arc2D.OPEN);
                g2.draw(arc);
                return;
            }
            Cyber.quality(g2);
            if (ready && lod == 0) {
                double p = 0.5 + 0.5 * Math.sin(pulse * 6);
                double gr = radius + 2 + 3 * p;
                ell.setFrame(cx - gr, cy - gr, gr * 2, gr * 2);
                g2.setStroke(W_GLOW);
                g2.setColor(Cyber.alpha(color, (int) (50 + 80 * p)));
                g2.draw(ell);
            }
            ell.setFrame(cx - radius, cy - radius, radius * 2, radius * 2);
            g2.setStroke(W_TRACK);
            g2.setColor(TRACK);
            g2.draw(ell);
            if (locked) {
                g2.setStroke(W_MAIN);
                g2.setColor(Cyber.alpha(Cyber.RED, 200));
                g2.draw(ell);
                return;
            }
            double sweep = ready ? 360 : 360 * Math.max(0, Math.min(1, progress));
            arc.setArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, -sweep, Arc2D.OPEN);
            g2.setStroke(W_MAIN);
            g2.setColor(ready ? Cyber.lerp(color, Color.WHITE, 0.35) : Cyber.alpha(color, 235));
            g2.draw(arc);
        } finally {
            g2.dispose();
        }
    }
}
