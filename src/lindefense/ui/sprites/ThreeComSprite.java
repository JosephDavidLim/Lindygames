package lindefense.ui.sprites;

import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import lindefense.enemy.Enemy;

/**
 * 3Com (ZOMG-class): a massive dark-olive hull divided into three sections by
 * two wide structural rings, studded with blinking green network ports, four
 * engine nacelles with green exhausts, hex armour plates, an antenna mast with
 * a red beacon and a big white "3Com" wordmark badge (with its swoosh arcs)
 * on the hull. Plates drop, ports go dark, cracks spread
 * and the hull smokes and burns as it is destroyed.
 */
@SuppressWarnings("serial")
public final class ThreeComSprite extends BossSprite {
    private final GPolygon[] plates = new GPolygon[3];
    private final GOval[] ports = new GOval[8];
    private final GOval[] exhaust = new GOval[4];
    private final GOval beacon;
    private final GPolygon[] cracks = new GPolygon[6];
    private int shownPlates = -1;

    /** Builds the ZOMG-class airship for the given boss. */
    public ThreeComSprite(Enemy e) {
        super(e);
        double rx = 1.0 * r, ry = 0.74 * r;
        Color dark = Art.shade(Art.THREECOM, 0.4), mid = Art.tint(Art.THREECOM, 0.12);
        // antenna mast + beacon (behind hull, sticks out the top)
        plate(Art.STEEL_DARK, 0.1 * r, -0.6 * r, 0.14 * r, -0.6 * r, 0.13 * r, -1.0 * r, 0.11 * r, -1.0 * r);
        plate(Art.STEEL_DARK, -0.02 * r, -0.86 * r, 0.26 * r, -0.86 * r, 0.26 * r, -0.83 * r, -0.02 * r, -0.83 * r);
        beacon = glow(new Color(255, 60, 60, 230), 0.12 * r, -1.03 * r, 0.1 * r);
        // fins
        fin(dark, -0.5 * r, -1, 0.28 * r, 0.35 * r, ry);
        fin(dark, -0.5 * r, 1, 0.28 * r, 0.35 * r, ry);
        plate(dark, -0.9 * r, -0.14 * r, -1.2 * r, -0.08 * r, -1.2 * r, 0.08 * r, -0.9 * r, 0.14 * r);
        // nacelles
        for (int i = 0; i < 4; i++) {
            double side = i % 2 == 0 ? -1 : 1, x = i < 2 ? -0.45 * r : 0.2 * r;
            hull(Art.STEEL_DARK, x, side * 0.84 * r, 0.32 * r, 0.11 * r);
            exhaust[i] = glow(Art.alpha(Art.THREECOM_GLOW, 220), x - 0.34 * r, side * 0.84 * r, 0.15 * r);
        }
        // hull with two structural rings
        hull(Art.THREECOM, 0, 0, rx, ry);
        hullShine(0, 0, rx, ry);
        band(mid, -0.33 * r, 0.14 * r, rx * 1.02, ry * 1.05);
        band(mid, 0.33 * r, 0.14 * r, rx * 1.02, ry * 1.05);
        // network ports along the belt
        for (int i = 0; i < 8; i++) {
            double x = (-0.78 + i * 0.22) * r;
            if (Math.abs(x + 0.33 * r) < 0.1 * r || Math.abs(x - 0.33 * r) < 0.1 * r) x += 0.08 * r;
            ports[i] = glow(Art.alpha(Art.THREECOM_GLOW, 240), x, (i % 2 == 0 ? -0.28 : 0.28) * r, 0.09 * r);
        }
        // hex armour plates
        double[][] px = { { -0.62, -0.42 }, { 0.05, 0.6 }, { 0.64, -0.32 } };
        for (int i = 0; i < 3; i++) {
            GPolygon p = new Thick.Poly();
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3;
                p.addVertex(px[i][0] * r + Math.cos(a) * 0.17 * r, px[i][1] * r + Math.sin(a) * 0.17 * r);
            }
            rotating(p);
            plates[i] = part(p, Art.STEEL);
        }
        // nose
        plate(Art.STEEL_DARK, 0.9 * r, -0.28 * r, 1.15 * r, -0.1 * r, 1.15 * r, 0.1 * r, 0.9 * r, 0.28 * r);
        for (int i = 0; i < 6; i++) {
            double a = 15 + i * 60;
            cracks[i] = Art.crack(Math.cos(Math.toRadians(a)) * 0.4 * r, Math.sin(Math.toRadians(a)) * 0.3 * r, 0.55 * r, a + 35, i + 5);
            rotating(cracks[i]);
            fixed(cracks[i], Art.OUTLINE, Art.OUTLINE).setVisible(false);
        }
        damageDressing(new double[][] { { -0.5 * r, -0.3 * r }, { 0.4 * r, 0.3 * r }, { -0.1 * r, 0.5 * r }, { 0.1 * r, -0.5 * r } }, 0.45, 0.75);
        acm.graphics.GCompound logo = Logo.threeCom(r);
        add(logo);
        orbit(logo, 0.0, 0.02 * r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        setFacing(facingDeg);
        int show = dmg < 0.25 ? 3 : (dmg < 0.5 ? 2 : (dmg < 0.75 ? 1 : 0));
        if (show != shownPlates) {
            shownPlates = show;
            for (int i = 0; i < 3; i++) plates[i].setVisible(i < show);
        }
        int lit = (int) Math.ceil(hpFrac * 8 - 1e-6);
        for (int i = 0; i < 8; i++) {
            boolean on = i < lit && (((int) (t * 6 + i * 2.3)) % 7 != 0);
            ports[i].setFillColor(on ? Art.alpha(Art.THREECOM_GLOW, 240) : new Color(30, 40, 30, 240));
        }
        beacon.setFillColor(new Color(255, 60, 60, ((int) (t * 2)) % 2 == 0 ? 240 : 90));
        for (int i = 0; i < 4; i++) {
            exhaust[i].setFillColor(Art.alpha(Art.THREECOM_GLOW, 140 + (int) (100 * Math.sin(t * 8 + i))));
        }
        showCracks(cracks, dmg);
        updateDressing(t);
    }
}
