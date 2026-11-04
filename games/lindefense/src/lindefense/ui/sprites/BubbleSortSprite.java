package lindefense.ui.sprites;

import acm.graphics.GOval;
import lindefense.enemy.Enemy;

/**
 * Bubble Sort: a cluster of three glossy translucent bubbles of different
 * sizes, the biggest wearing a pair of eyes. The two largest periodically swap
 * places along a little arc (the swap mechanic); the smaller bubbles pop away
 * as HP is lost.
 */
@SuppressWarnings("serial")
public final class BubbleSortSprite extends EnemySprite {
    private final GOval[] bubbles = new GOval[3];
    private final GOval[] shine = new GOval[3];
    private final GOval[] sparkle = new GOval[3];
    private final GOval[] eye;
    private final double[] diam;
    private int shown = -1;

    /** Builds the bubble cluster for the given enemy. */
    public BubbleSortSprite(Enemy e) {
        super(e);
        diam = new double[] { 1.45 * r, 1.0 * r, 0.62 * r };
        for (int i = 2; i >= 0; i--) {
            bubbles[i] = part(Art.ovalAt(0, 0, diam[i], diam[i]),
                Art.alpha(Art.tint(Art.BUBBLE, 0.1 * i), 165), Art.outline(Art.BUBBLE));
            if (i == 2) thin(bubbles[i]);
            shine[i] = highlight(0, 0, diam[i] * 0.42, diam[i] * 0.3);
            sparkle[i] = highlight(0, 0, diam[i] * 0.12, diam[i] * 0.12);
        }
        eye = eyes(0, 0, 0.5 * r, 0.34 * r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        double bob = Math.sin(t * 4 + e.getId()) * r * 0.1;
        double[][] home = { { -0.25 * r, 0.1 * r }, { 0.6 * r, -0.5 * r }, { -0.75 * r, 0.65 * r } };
        double cyc = (t + e.getId() * 0.37) % 3.0;
        double p = Math.max(0, Math.min(1, cyc / 0.7));
        double s = p * p * (3 - 2 * p);
        boolean swapped = ((int) ((t + e.getId() * 0.37) / 3.0) % 2) == 1;
        double[] a = swapped ? home[1] : home[0], b = swapped ? home[0] : home[1];
        double lift = Math.sin(s * Math.PI) * r * 0.5;
        double[][] pos = {
            { a[0] + (b[0] - a[0]) * s, a[1] + (b[1] - a[1]) * s - lift + bob },
            { b[0] + (a[0] - b[0]) * s, b[1] + (a[1] - b[1]) * s + lift + bob },
            { home[2][0], home[2][1] + bob * 1.5 } };
        for (int i = 0; i < 3; i++) {
            Art.center(bubbles[i], pos[i][0], pos[i][1]);
            Art.center(shine[i], pos[i][0] - diam[i] * 0.2, pos[i][1] - diam[i] * 0.24);
            Art.center(sparkle[i], pos[i][0] - diam[i] * 0.34, pos[i][1] + diam[i] * 0.02);
        }
        for (int i = 0; i < 2; i++) {
            double ex = pos[0][0] + (i == 0 ? -1 : 1) * 0.25 * r, ey = pos[0][1] - 0.05 * r;
            Art.center(eye[2 * i], ex, ey);
            Art.center(eye[2 * i + 1], ex + dirX * 0.08 * r + 0.03 * r, ey + dirY * 0.08 * r + 0.03 * r);
        }
        int show = Math.max(1, layersLeft(e, 3));
        if (show != shown) {
            shown = show;
            for (int i = 0; i < 3; i++) {
                bubbles[i].setVisible(i < show); shine[i].setVisible(i < show); sparkle[i].setVisible(i < show);
            }
        }
    }
}
