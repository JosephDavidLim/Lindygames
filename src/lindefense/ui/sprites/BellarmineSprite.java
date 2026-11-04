package lindefense.ui.sprites;

import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import lindefense.enemy.Enemy;

/**
 * Bellarmine College Prep (MOAB-class): a fat navy blimp strapped with three
 * riveted steel armour bands, a steel nose cone, twin tail fins, a gondola
 * with lit windows, and the school crest (shield, bell and "B") over a
 * "BELLARMINE" banner across its broad side. Bands shear off one by one as armour
 * is lost; smoke and fire follow.
 */
@SuppressWarnings("serial")
public final class BellarmineSprite extends BossSprite {
    private final GPolygon[] bands = new GPolygon[3];
    private final GOval[] rivets = new GOval[6];
    private final GPolygon[] cracks = new GPolygon[4];
    private final GOval engine;
    private int shownBands = -1;

    /** Builds the MOAB-class airship for the given boss. */
    public BellarmineSprite(Enemy e) {
        super(e);
        double rx = 1.0 * r, ry = 0.55 * r;
        Color steel = Art.STEEL, steelD = Art.STEEL_DARK;
        // tail fins, tail cone, engine glow (behind hull)
        fin(Art.shade(Art.BELLARMINE, 0.2), -0.6 * r, -1, 0.35 * r, 0.3 * r, ry);
        fin(Art.shade(Art.BELLARMINE, 0.2), -0.6 * r, 1, 0.35 * r, 0.3 * r, ry);
        plate(steelD, -0.95 * r, -0.2 * r, -1.25 * r, -0.12 * r, -1.25 * r, 0.12 * r, -0.95 * r, 0.2 * r);
        engine = glow(new Color(255, 170, 60, 200), -1.3 * r, 0, 0.22 * r);
        // gondola
        hull(Art.shade(Art.BELLARMINE, 0.45), 0.05 * r, 0.56 * r, 0.38 * r, 0.14 * r);
        for (int i = 0; i < 3; i++) glow(new Color(255, 235, 170, 230), -0.1 * r + i * 0.15 * r, 0.56 * r, 0.07 * r);
        // hull
        hull(Art.BELLARMINE, 0, 0, rx, ry);
        hullShine(0, 0, rx, ry);
        // armour bands
        double[] bx = { -0.72 * r, -0.45 * r, 0.75 * r };
        for (int i = 0; i < 3; i++) bands[i] = band(i % 2 == 0 ? steel : Art.tint(steel, 0.15), bx[i], 0.2 * r, rx * 1.02, ry * 1.06);
        for (int i = 0; i < 6; i++) {
            double x = bx[i % 3], y = (i < 3 ? -0.3 : 0.3) * ry;
            rivets[i] = glow(steelD, x, y, 0.08 * r);
        }
        // nose cone
        plate(steelD, 0.82 * r, -0.32 * r, 1.18 * r, -0.08 * r, 1.18 * r, 0.08 * r, 0.82 * r, 0.32 * r);
        for (int i = 0; i < 4; i++) {
            double a = 20 + i * 95;
            cracks[i] = Art.crack(Math.cos(Math.toRadians(a)) * 0.3 * r, Math.sin(Math.toRadians(a)) * 0.2 * r, 0.5 * r, a + 30, i + 7);
            rotating(cracks[i]);
            fixed(cracks[i], Art.OUTLINE, Art.OUTLINE).setVisible(false);
        }
        damageDressing(new double[][] { { -0.4 * r, -0.25 * r }, { 0.3 * r, 0.2 * r } }, 0.55, 0.8);
        // school crest + wordmark banner, large on the broad side, always upright
        acm.graphics.GCompound logo = Logo.bellarmine(r);
        add(logo);
        orbit(logo, 0.12 * r, -0.08 * r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        setFacing(facingDeg);
        int show = dmg < 0.3 ? 3 : (dmg < 0.55 ? 2 : (dmg < 0.8 ? 1 : 0));
        if (show != shownBands) {
            shownBands = show;
            int[] order = { 1, 0, 2 };
            for (int i = 0; i < 3; i++) {
                boolean v = i < show;
                bands[order[i]].setVisible(v);
                rivets[order[i]].setVisible(v); rivets[order[i] + 3].setVisible(v);
            }
        }
        showCracks(cracks, dmg);
        engine.setFillColor(new Color(255, 170, 60, 150 + (int) (90 * Math.sin(t * 9))));
        updateDressing(t);
    }
}
