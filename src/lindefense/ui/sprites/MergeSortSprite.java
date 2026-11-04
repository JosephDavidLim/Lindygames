package lindefense.ui.sprites;

import acm.graphics.GLine;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import lindefense.enemy.Enemy;

/**
 * Merge Sort: a chunky purple hexagon already scored down the middle into two
 * mirrored halves, each holding three sorted bars (six bars = six layers) and
 * one eye, so the split visibly divides the face too. The halves breathe
 * apart, and the seam opens wider the more damage it takes, telegraphing the
 * split into two Merge Runs.
 */
@SuppressWarnings("serial")
public final class MergeSortSprite extends EnemySprite {
    private final GPolygon[] left, right;
    private final GOval shineL, shineR;
    private final GOval[] eye;
    private final GRect[] bars = new GRect[6];
    private final GLine[] seam = new GLine[3];
    private int shown = -1;

    /** Builds the halved hexagon for the given enemy. */
    public MergeSortSprite(Enemy e) {
        super(e);
        double R = r * 1.0, h = R * 0.866;
        left = bodyPolyParts(Art.MERGE, 0, -h, -R * 0.5, -h, -R, 0, -R * 0.5, h, 0, h);
        right = bodyPolyParts(Art.shade(Art.MERGE, 0.12), 0, -h, R * 0.5, -h, R, 0, R * 0.5, h, 0, h);
        for (int i = 0; i < 6; i++) {
            int k = i % 3;
            bars[i] = thin(part(Art.rect(0, 0, r * 0.2, r * (0.3 + 0.22 * k)), Art.tint(Art.MERGE, 0.45 + 0.15 * k)));
        }
        for (int i = 0; i < 3; i++) {
            seam[i] = thin(fixedStroke(Art.line(0, -h + i * h * 0.7, 0, -h + i * h * 0.7 + h * 0.4), Art.tint(Art.MERGE, 0.6)));
        }
        shineL = highlight(-0.5 * r, -0.5 * r, 0.42 * r, 0.26 * r);
        shineR = highlight(0.3 * r, -0.55 * r, 0.22 * r, 0.14 * r);
        eye = eyes(0, -0.25 * r, 0.7 * r, 0.36 * r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        double gap = r * (0.06 + 0.05 * Math.sin(t * 3 + e.getId()) + 0.3 * dmg);
        left[0].setLocation(-gap, 0); left[1].setLocation(-gap, 0);
        right[0].setLocation(gap, 0); right[1].setLocation(gap, 0);
        Art.center(shineL, -gap - 0.5 * r, -0.5 * r);
        Art.center(shineR, gap + 0.3 * r, -0.55 * r);
        for (int i = 0; i < 6; i++) {
            int side = i / 3, k = i % 3;
            double x = side == 0 ? -gap - r * 0.78 + k * r * 0.24 : gap + r * 0.12 + k * r * 0.24;
            bars[i].setLocation(x, r * 0.5 - bars[i].getHeight());
        }
        for (int i = 0; i < 2; i++) {
            double ex = (i == 0 ? -gap - 0.35 * r : gap + 0.35 * r), ey = -0.25 * r;
            Art.center(eye[2 * i], ex, ey);
            Art.center(eye[2 * i + 1], ex + dirX * 0.06 * r + 0.03 * r, ey + dirY * 0.06 * r + 0.03 * r);
        }
        int show = layersLeft(e, 6);
        if (show != shown) {
            shown = show;
            int[] order = { 5, 2, 4, 1, 3, 0 };
            for (int i = 0; i < 6; i++) bars[order[i]].setVisible(i >= 6 - show);
        }
    }
}
