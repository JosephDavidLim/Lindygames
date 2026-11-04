package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Owen, the Drone Operator: an engineer in a gunmetal flight jacket with tangerine stripes, a backwards cap and a
 * headset mic, steering his squadron with a twin-stick controller and an uplink dish; a drone dock pad sits behind him.
 * His drones are drawn separately (quad-rotor scouts and delta-wing swarm drones), never as copies of him.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class OwenArt extends TowerArt {
    private static final long serialVersionUID = 1L;
    private static final Color JACKET = new Color(0x3a404d);

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public OwenArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        // dock pad on the back
        poly(Art.regular(0, r * 0.55, r * 0.42, 6, 0), Art.shade(JACKET, 1.2));
        plain(Art.ring(0, r * 0.55, r * 0.3, r * 0.22, 16), pri);
        plain(Art.rect(-r * 0.05, r * 0.38, r * 0.1, r * 0.34), Art.alpha(pri, 160));
        // uplink dish + controller
        Turret tu = turret();
        tu.poly(Art.rect(r * 0.2, -r * 0.18, r * 0.45, r * 0.36), Art.METAL);
        tu.plain(Art.circle(r * 0.32, -r * 0.08, r * 0.06, 8), pri);
        tu.plain(Art.circle(r * 0.5, r * 0.08, r * 0.06, 8), Art.rgb(0x5fd6ff));
        tu.poly(Art.rect(r * 0.62, -r * 0.04, r * 0.4, r * 0.08), Art.METAL_LIGHT);
        tu.poly(new double[] { r * 1.0, -r * 0.34, r * 1.18, -r * 0.2, r * 1.18, r * 0.2, r * 1.0, r * 0.34, r * 1.08, 0 }, pri);
        tu.setFlash(tu.plain(Art.star(r * 1.35, 0, r * 0.26, r * 0.1, 4, 0), Art.alpha(Art.rgb(0x5fd6ff), 220)));
        double hy = HeroFigure.draw(this, r, HeroFigure.SKIN_LIGHT, new Color(0x2b2f3a), HeroFigure.Hair.CAP, JACKET, pri);
        // jacket stripes
        plain(Art.rect(-r * 0.7, r * 0.12, r * 0.16, r * 0.06), pri);
        plain(Art.rect(r * 0.54, r * 0.12, r * 0.16, r * 0.06), pri);
        // headset
        poly(Art.bar(-r * 0.4, hy - r * 0.1, -r * 0.12, hy + r * 0.32, r * 0.035), Art.INK);
        disc(-r * 0.42, hy, r * 0.1, pri);
        plain(Art.circle(-r * 0.1, hy + r * 0.32, r * 0.05, 6), Art.INK);
        antenna(r * 0.42, -r * 0.2, -r * 0.62, Art.rgb(0x5fd6ff));
        pulsingGlow(r * 0.42, -r * 0.62, r * 0.18, Art.rgb(0x5fd6ff), 70, 60, 2);
        sparkle(-r * 0.5, -r * 0.55, r * 0.08);
    }
}
