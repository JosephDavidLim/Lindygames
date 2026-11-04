package lindefense.ui.sprites;

import acm.graphics.GArc;
import acm.graphics.GLine;
import acm.graphics.GOval;
import lindefense.enemy.Enemy;

/**
 * Selection Sort: a chunky slate hexagon with a single big amber "eye" at its
 * centre, wrapped in a targeting reticle whose sweep arm keeps scanning for
 * the front of the group. The eye darts about; the reticle ring breaks into a
 * shorter arc as HP drops.
 */
@SuppressWarnings("serial")
public final class SelectionSortSprite extends EnemySprite {
    private final GArc ring;
    private final GArc sweep;
    private final GOval iris, pupil, glint;
    private final GLine[] ticks = new GLine[4];
    private int shownLayers = -1;

    /** Builds the reticle character for the given enemy. */
    public SelectionSortSprite(Enemy e) {
        super(e);
        double R = 1.1 * r;
        sweep = fixed(Art.arc(-R, -R, 2 * R, 2 * R, 0, 55), Art.alpha(Art.SELECTION, 80), Art.alpha(Art.SELECTION, 0));
        ((Thick.Arc) sweep).setLineWidth(0);
        double[] hex = new double[12];
        for (int i = 0; i < 6; i++) { hex[2 * i] = 0.82 * r * Math.cos(i * Math.PI / 3); hex[2 * i + 1] = 0.82 * r * Math.sin(i * Math.PI / 3); }
        bodyPoly(Art.SELECTION_BODY, hex);
        gloss(0, 0, 1.6 * r, 1.4 * r);
        thin(part(Art.ovalAt(0, 0, 0.9 * r, 0.9 * r), java.awt.Color.WHITE));
        iris = thin(part(Art.ovalAt(0, 0, 0.6 * r, 0.6 * r), Art.SELECTION));
        pupil = fixed(Art.ovalAt(0, 0, 0.28 * r, 0.28 * r), Art.OUTLINE, Art.OUTLINE);
        ((Thick.Oval) pupil).setLineWidth(0);
        glint = highlight(-0.1 * r, -0.12 * r, 0.16 * r, 0.12 * r);
        ring = lineWidth(stroke(Art.arc(-R, -R, 2 * R, 2 * R, 90, 360), Art.SELECTION), 2.2);
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(i * 90);
            ticks[i] = lineWidth(stroke(Art.line(Math.cos(a) * R * 0.85, Math.sin(a) * R * 0.85,
                Math.cos(a) * R * 1.3, Math.sin(a) * R * 1.3), Art.SELECTION), 2.2);
        }
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        sweep.setStartAngle((-t * 160 + e.getId() * 40) % 360);
        double pulse = 1.0 + 0.06 * Math.sin(t * 5);
        double R = 1.1 * r * pulse;
        ring.setFrameRectangle(-R, -R, 2 * R, 2 * R);
        int layers = layersLeft(e, 4);
        if (layers != shownLayers) {
            shownLayers = layers;
            ring.setSweepAngle(90.0 * Math.max(1, layers));
            for (int i = 0; i < 4; i++) ticks[i].setVisible(i < layers);
        }
        double lx = Math.cos(t * 2.3) * r * 0.1 + dirX * 0.08 * r, ly = Math.sin(t * 3.1) * r * 0.1 + dirY * 0.08 * r;
        Art.center(iris, lx, ly);
        Art.center(pupil, lx * 1.3, ly * 1.3);
        Art.center(glint, lx - 0.1 * r, ly - 0.12 * r);
    }
}
