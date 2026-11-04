package lindefense.ui.sprites;

import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import lindefense.enemy.Enemy;

/**
 * SW Resource Consultant (BFB-class): a broad teal-grey double hull ribbed
 * with five steel reinforcement ribs, twin side engine pods with orange
 * exhausts, a bridge dome, an armoured wedge nose and a large (invented)
 * orange SWRC monogram badge on its side. Ribs crack away and pods start smoking as it is worn down.
 */
@SuppressWarnings("serial")
public final class SwResourceConsultantSprite extends BossSprite {
    private final GPolygon[] ribs = new GPolygon[5];
    private final GPolygon[] cracks = new GPolygon[5];
    private final GOval[] exhaust = new GOval[2];
    private int shownRibs = -1;

    /** Builds the BFB-class airship for the given boss. */
    public SwResourceConsultantSprite(Enemy e) {
        super(e);
        double rx = 1.0 * r, ry = 0.62 * r;
        Color dark = Art.shade(Art.SW, 0.35);
        // fins and pods behind the hull
        fin(dark, -0.55 * r, -1, 0.3 * r, 0.3 * r, ry);
        fin(dark, -0.55 * r, 1, 0.3 * r, 0.3 * r, ry);
        plate(dark, -0.85 * r, -0.1 * r, -1.2 * r, -0.05 * r, -1.2 * r, 0.05 * r, -0.85 * r, 0.1 * r);
        for (int i = 0; i < 2; i++) {
            double side = i == 0 ? -1 : 1;
            hull(Art.STEEL_DARK, -0.25 * r, side * 0.72 * r, 0.5 * r, 0.16 * r);
            exhaust[i] = glow(new Color(255, 150, 50, 210), -0.78 * r, side * 0.72 * r, 0.2 * r);
            glow(Art.STEEL, -0.05 * r, side * 0.72 * r, 0.1 * r);
        }
        // outer reinforced hull then inner hull
        hull(dark, 0, 0, rx, ry);
        hull(Art.SW, 0.02 * r, 0, rx * 0.92, ry * 0.86);
        hullShine(0, 0, rx, ry);
        // belt line
        plate(Art.shade(Art.SW, 0.5), -0.9 * r, -0.03 * r, 0.9 * r, -0.03 * r, 0.9 * r, 0.03 * r, -0.9 * r, 0.03 * r);
        // ribs
        for (int i = 0; i < 5; i++) {
            double x = (i < 2 ? -0.85 + i * 0.22 : 0.52 + (i - 2) * 0.2) * r;
            ribs[i] = band(Art.STEEL, x, 0.09 * r, rx * 1.01, ry * 1.03);
        }
        // bridge dome
        hull(Art.tint(Art.SW, 0.2), 0.3 * r, -0.5 * r, 0.22 * r, 0.14 * r);
        glow(new Color(255, 240, 200, 230), 0.36 * r, -0.52 * r, 0.08 * r);
        // nose wedge
        plate(Art.STEEL_DARK, 0.9 * r, -0.3 * r, 1.22 * r, -0.06 * r, 1.22 * r, 0.06 * r, 0.9 * r, 0.3 * r);
        for (int i = 0; i < 5; i++) {
            double a = 10 + i * 72;
            cracks[i] = Art.crack(Math.cos(Math.toRadians(a)) * 0.35 * r, Math.sin(Math.toRadians(a)) * 0.25 * r, 0.5 * r, a + 40, i + 11);
            rotating(cracks[i]);
            fixed(cracks[i], Art.OUTLINE, Art.OUTLINE).setVisible(false);
        }
        damageDressing(new double[][] { { -0.3 * r, -0.72 * r }, { -0.3 * r, 0.72 * r }, { 0.2 * r, -0.1 * r } }, 0.5, 0.78);
        acm.graphics.GCompound logo = Logo.swrc(r);
        add(logo);
        orbit(logo, -0.05 * r, 0.0);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        setFacing(facingDeg);
        int show = Math.max(0, Math.min(5, (int) Math.ceil(hpFrac * 5 - 1e-6)));
        if (show != shownRibs) {
            shownRibs = show;
            int[] order = { 2, 0, 4, 1, 3 };
            for (int i = 0; i < 5; i++) ribs[order[i]].setVisible(i < show);
        }
        showCracks(cracks, dmg);
        for (int i = 0; i < 2; i++) {
            exhaust[i].setFillColor(new Color(255, 150, 50, 150 + (int) (90 * Math.sin(t * 11 + i * 1.7))));
        }
        updateDressing(t);
    }
}
