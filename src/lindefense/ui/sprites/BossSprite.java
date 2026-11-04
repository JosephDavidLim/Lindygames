package lindefense.ui.sprites;

import acm.graphics.GLabel;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import java.awt.Font;
import lindefense.enemy.Boss;
import lindefense.enemy.Enemy;

/**
 * Shared scaffolding for the corporate airships. Boss hulls are built from
 * polygons whose vertices are expressed in ship-local coordinates (nose at +x)
 * around the sprite origin, so the whole ship rotates with {@link #setFacing}.
 * Round details (lights, windows) are {@link #orbit}ed instead.
 *
 * <p>Also provides the damage dressing every boss shares: smoke puffs that
 * start rising once the hull is badly hurt and fire licks near the end.</p>
 */
@SuppressWarnings("serial")
public abstract class BossSprite extends EnemySprite {
    private GOval[] smoke;
    private GOval[] fire;
    private double[][] smokeAnchor;
    private double smokeThreshold = 0.5, fireThreshold = 0.78;

    /** Base constructor. */
    protected BossSprite(Enemy e) { super(e); }

    /** Returns the enemy as a Boss, or null for plain enemies passed by mistake. */
    protected static Boss boss(Enemy e) { return e instanceof Boss ? (Boss) e : null; }

    /** Elliptical hull (polygon, 28 vertices) centred at local (cx,cy). */
    protected GPolygon hull(Color fill, double cx, double cy, double rx, double ry) {
        GPolygon p = Art.ellipse(cx, cy, rx, ry, 28);
        rotating(p);
        return part(p, fill);
    }

    /** Quad plate from four local points. */
    protected GPolygon plate(Color fill, double... xy) {
        return rpoly(fill, xy);
    }

    /** Horizontal armour band across an elliptical hull at local x. */
    protected GPolygon band(Color fill, double x, double w, double rx, double ry) {
        double h1 = ry * Math.sqrt(Math.max(0, 1 - (x - w / 2) * (x - w / 2) / (rx * rx)));
        double h2 = ry * Math.sqrt(Math.max(0, 1 - (x + w / 2) * (x + w / 2) / (rx * rx)));
        return plate(fill, x - w / 2, -h1, x + w / 2, -h2, x + w / 2, h2, x - w / 2, h1);
    }

    /** Tail fin on one side (sign = -1 top, +1 bottom in screen terms). */
    protected GPolygon fin(Color fill, double rootX, double side, double len, double sweep, double ry) {
        return plate(fill, rootX, side * ry * 0.6, rootX - sweep, side * (ry + len), rootX - sweep - len * 0.6, side * (ry + len),
            rootX - len * 0.9, side * ry * 0.6);
    }

    /** Glowing round light at local position (not damage-tinted). */
    protected GOval glow(Color c, double lx, double ly, double d) {
        GOval o = Art.ovalAt(0, 0, d, d);
        fixed(o, c, Art.alpha(c, 0));
        orbit(o, lx, ly);
        return o;
    }

    /** Text emblem anchored at local position; stays upright. */
    protected GLabel emblem(String text, Color c, double lx, double ly, double size) {
        GLabel l = new GLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, (int) Math.round(size))));
        fixedStroke(l, c);
        orbit(l, lx, ly);
        return l;
    }

    /** Polygon hull highlight (top-left light) that rotates with the ship. */
    protected GPolygon hullShine(double cx, double cy, double rx, double ry) {
        GPolygon p = Art.ellipse(cx - rx * 0.25, cy - ry * 0.45, rx * 0.45, ry * 0.22, 14);
        rotating(p);
        return fixed(p, Art.HIGHLIGHT, new Color(255, 255, 255, 0));
    }

    /**
     * Creates smoke and fire dressing. Anchors are ship-local points (x,y) the
     * smoke rises from. Thresholds are damage fractions at which each starts.
     */
    protected void damageDressing(double[][] anchors, double smokeAt, double fireAt) {
        smokeAnchor = anchors;
        smokeThreshold = smokeAt; fireThreshold = fireAt;
        smoke = new GOval[anchors.length * 2];
        for (int i = 0; i < smoke.length; i++) {
            smoke[i] = Art.ovalAt(0, 0, r * 0.3, r * 0.3);
            smoke[i].setFilled(true);
            smoke[i].setVisible(false);
            add(smoke[i]);
        }
        fire = new GOval[anchors.length];
        for (int i = 0; i < fire.length; i++) {
            fire[i] = Art.ovalAt(0, 0, r * 0.22, r * 0.3);
            fire[i].setFilled(true);
            fire[i].setVisible(false);
            add(fire[i]);
        }
    }

    /** Animates the smoke and fire for the current damage level. Call from updateBody. */
    protected void updateDressing(double t) {
        if (smoke == null) return;
        boolean smoking = dmg >= smokeThreshold;
        boolean burning = dmg >= fireThreshold;
        for (int i = 0; i < smoke.length; i++) {
            if (!smoking) { if (smoke[i].isVisible()) smoke[i].setVisible(false); continue; }
            double[] a = smokeAnchor[i % smokeAnchor.length];
            double ph = (t * 0.55 + (double) i / smoke.length) % 1.0;
            double[] p = local(a[0], a[1]);
            double s = r * (0.18 + 0.5 * ph);
            double x = p[0] + Math.sin(ph * 6 + i) * r * 0.1, y = p[1] - ph * r * 1.1;
            smoke[i].setBounds(x - s / 2, y - s / 2, s, s);
            int alpha = (int) (150 * (1 - ph) * Math.min(1, (dmg - smokeThreshold) / 0.25 + 0.3));
            Color c = new Color(70, 70, 78, Math.max(0, alpha));
            smoke[i].setFillColor(c);
            smoke[i].setColor(c);
            smoke[i].setVisible(true);
        }
        for (int i = 0; i < fire.length; i++) {
            if (!burning) { if (fire[i].isVisible()) fire[i].setVisible(false); continue; }
            double[] a = smokeAnchor[i];
            double[] p = local(a[0], a[1]);
            double flick = 0.6 + 0.4 * Math.sin(t * 17 + i * 2.3);
            double w = r * 0.22 * flick, h = r * 0.34 * flick;
            fire[i].setBounds(p[0] - w / 2, p[1] - h * 0.8, w, h);
            Color c = i % 2 == 0 ? new Color(255, 150, 40, 220) : new Color(255, 80, 30, 220);
            fire[i].setFillColor(c);
            fire[i].setColor(new Color(255, 220, 120, 200));
            fire[i].setVisible(true);
        }
    }
}
