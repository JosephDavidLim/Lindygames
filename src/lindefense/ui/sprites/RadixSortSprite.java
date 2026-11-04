package lindefense.ui.sprites;

import acm.graphics.GArc;
import acm.graphics.GLabel;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import java.awt.Font;
import lindefense.enemy.Enemy;

/**
 * Radix Sort: a chunky copper octagon core wearing eight digit plates (eight
 * layers). The plates change armour with {@link Enemy#radixPhase()}:
 * <ol start="0">
 *   <li>plain copper plates,</li>
 *   <li>steel plates with bolts (Physical-immune),</li>
 *   <li>violet rune plates with an arcane glow (Energy/Magic-resistant),</li>
 *   <li>black-and-yellow blast shielding (Explosive-immune).</li>
 * </ol>
 * The centre label shows the current place value and a thin ring arc spins in
 * the phase colour. Plates fall off as layers are lost.
 */
@SuppressWarnings("serial")
public final class RadixSortSprite extends EnemySprite {
    private static final Color[] PHASE = {
        Art.RADIX, Art.STEEL, new Color(160, 95, 240), new Color(255, 205, 40) };
    private static final Color[] PHASE_ALT = {
        Art.shade(Art.RADIX, 0.15), Art.STEEL_DARK, new Color(115, 60, 200), new Color(40, 40, 44) };
    private static final String[] PLACE = { "1", "10", "100", "1000" };

    private final GPolygon[] plates = new GPolygon[8];
    private final GOval[] bolts = new GOval[4];
    private final GOval glow;
    private final GArc spin;
    private final GLabel place;
    private int shownPhase = -1;
    private int shown = -1;

    /** Builds the digit-armoured octagon for the given enemy. */
    public RadixSortSprite(Enemy e) {
        super(e);
        glow = fixed(Art.ovalAt(0, 0, 2.6 * r, 2.6 * r), Art.alpha(PHASE[2], 55), Art.alpha(PHASE[2], 0));
        ((Thick.Oval) glow).setLineWidth(0);
        glow.setVisible(false);
        double[] oct = new double[16];
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(22.5 + i * 45);
            oct[2 * i] = 0.82 * r * Math.cos(a); oct[2 * i + 1] = 0.82 * r * Math.sin(a);
        }
        bodyPoly(Art.shade(Art.RADIX, 0.1), oct);
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            double c = Math.cos(a), s = Math.sin(a);
            double in = 0.74 * r, out = 1.1 * r, hw = 0.34 * r;
            GPolygon p = Art.poly(c * in - s * hw * 0.8, s * in + c * hw * 0.8,
                c * out - s * hw, s * out + c * hw,
                c * out + s * hw, s * out - c * hw,
                c * in + s * hw * 0.8, s * in - c * hw * 0.8);
            plates[i] = thin(part(p, PHASE[0]));
        }
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(i * 90 + 45);
            bolts[i] = thin(fixed(Art.ovalAt(Math.cos(a) * 0.92 * r, Math.sin(a) * 0.92 * r, 0.16 * r, 0.16 * r), Art.STEEL_DARK, Art.OUTLINE));
            bolts[i].setVisible(false);
        }
        spin = lineWidth(fixedStroke(Art.arc(-1.3 * r, -1.3 * r, 2.6 * r, 2.6 * r, 0, 80), PHASE[0]), 2.0);
        gloss(0, 0, 1.7 * r, 1.7 * r);
        place = fixedStroke(new GLabel("1"), Color.WHITE);
        place.setFont(new Font("SansSerif", Font.BOLD, Math.max(7, (int) Math.round(r * 0.5))));
        finishOverlays();
        centreLabel();
    }

    private void centreLabel() {
        place.setLocation(-place.getWidth() / 2, place.getAscent() * 0.38);
    }

    @Override protected void updateBody(Enemy e, double t) {
        int ph = Math.max(0, Math.min(3, e.radixPhase()));
        if (ph != shownPhase) {
            shownPhase = ph;
            for (int i = 0; i < 8; i++) {
                Color f = (ph == 3 && i % 2 == 1) ? PHASE_ALT[3] : PHASE[ph];
                recolor(plates[i], f, Art.OUTLINE);
            }
            for (GOval b : bolts) b.setVisible(ph == 1);
            glow.setVisible(ph == 2);
            spin.setColor(ph == 3 ? PHASE_ALT[3] : PHASE[ph]);
            place.setLabel(PLACE[ph]);
            centreLabel();
        }
        spin.setStartAngle((t * 90 + e.getId() * 30) % 360);
        if (ph == 2) glow.setFillColor(Art.alpha(PHASE[2], 35 + (int) (30 * Math.sin(t * 5))));
        int show = layersLeft(e, 8);
        if (show != shown) {
            shown = show;
            for (int i = 0; i < 8; i++) plates[i].setVisible(((i * 3) % 8) < show);
        }
    }
}
