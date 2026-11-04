package lindefense.ui.sprites;

import acm.graphics.GOval;
import acm.graphics.GRect;
import lindefense.enemy.Enemy;

/**
 * Merge Run: one half of a Merge Sort. A chunky lavender half-hexagon with a
 * jagged cut edge on its flat side, two sorted bars (two layers) and a single
 * eye, since it only got half the face.
 */
@SuppressWarnings("serial")
public final class MergeRunSprite extends EnemySprite {
    private final GRect[] bars = new GRect[2];
    private final GOval pupil;
    private int shown = -1;

    /** Builds the half-hexagon for the given enemy. */
    public MergeRunSprite(Enemy e) {
        super(e);
        double R = r * 1.1;
        bodyPoly(Art.MERGE_RUN, -0.45 * R, -R * 0.87, 0.3 * R, -R * 0.87, 0.85 * R, 0, 0.3 * R, R * 0.87, -0.45 * R, R * 0.87);
        for (int i = 0; i < 2; i++) {
            bars[i] = thin(part(Art.rect(-0.25 * R + i * 0.34 * R, 0.45 * R - r * (0.45 + 0.3 * i), r * 0.22, r * (0.45 + 0.3 * i)),
                Art.tint(Art.MERGE_RUN, 0.4 + 0.2 * i)));
        }
        // the torn seam
        thin(stroke(Art.line(-0.45 * R, -R * 0.6, -0.3 * R, -R * 0.3), Art.tint(Art.MERGE, 0.5)));
        thin(stroke(Art.line(-0.45 * R, 0.0, -0.3 * R, 0.3 * R), Art.tint(Art.MERGE, 0.5)));
        gloss(0.15 * R, 0, 1.2 * R, 1.6 * R);
        Thick.Oval white = Art.ovalAt(0.25 * R, -0.3 * R, 0.38 * r, 0.42 * r);
        white.setLineWidth(Thick.DETAIL);
        fixed(white, java.awt.Color.WHITE, Art.OUTLINE);
        pupil = fixed(Art.ovalAt(0.28 * R, -0.28 * R, 0.17 * r, 0.2 * r), Art.OUTLINE, Art.OUTLINE);
        ((Thick.Oval) pupil).setLineWidth(0);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        int show = Math.max(1, layersLeft(e, 2));
        if (show != shown) {
            shown = show;
            bars[1].setVisible(show >= 2);
        }
        Art.center(pupil, 0.275 * r * 1.1 + dirX * 0.05 * r, -0.3 * r * 1.1 + dirY * 0.05 * r + 0.02 * r);
    }
}
