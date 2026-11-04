package lindefense.ui.sprites;

import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import lindefense.enemy.Enemy;

/**
 * Quick Sort: a chunky crimson plate carrying seven bars with a golden pivot
 * in the middle (it has the eyes and a little marker above it) and a dashed
 * partition line through it. Shorter bars stand left of the pivot, taller
 * bars right; the outer bars keep shuffling heights as they are partitioned.
 * Bars vanish from the outside in as HP drops, leaving the pivot last.
 */
@SuppressWarnings("serial")
public final class QuickSortSprite extends EnemySprite {
    private final GRect[] bars = new GRect[7];
    private final double[] baseH = new double[7];
    private final GPolygon marker;
    private final GOval[] eye;
    private final GOval pivotShine;
    private final double plateY;
    private int shown = -1;

    /** Builds the partition plate for the given enemy. */
    public QuickSortSprite(Enemy e) {
        super(e);
        plateY = r * 0.72;
        bodyRect(-1.25 * r, plateY, 2.5 * r, 0.42 * r, Art.shade(Art.QUICK, 0.25));
        double bw = r * 0.27;
        for (int i = 0; i < 7; i++) {
            int k = i - 3;
            baseH[i] = k == 0 ? r * 1.0 : (k < 0 ? r * (0.35 + 0.15 * (3 + k)) : r * (1.05 + 0.15 * (k - 1)));
            double x = k * bw * 1.25 - bw / 2;
            bars[i] = part(Art.rect(x, plateY - baseH[i], bw, baseH[i]),
                k == 0 ? Art.QUICK_PIVOT : (k < 0 ? Art.tint(Art.QUICK, 0.3) : Art.QUICK));
            if (k != 0) thin(bars[i]);
        }
        thin(fixedStroke(Art.line(0, plateY + 0.42 * r, 0, plateY + 0.7 * r), Art.tint(Art.QUICK_PIVOT, 0.4)));
        thin(fixedStroke(Art.line(0, plateY - 1.35 * r, 0, plateY - 1.1 * r), Art.tint(Art.QUICK_PIVOT, 0.4)));
        marker = thin(fixed(Art.poly(-0.17 * r, -1.6 * r + plateY, 0.17 * r, -1.6 * r + plateY, 0, -1.36 * r + plateY),
            Art.QUICK_PIVOT, Art.OUTLINE));
        pivotShine = highlight(-bw * 0.15, plateY - 0.85 * r, bw * 0.45, 0.2 * r);
        eye = eyes(0, plateY - 0.55 * r, bw * 0.55, bw * 0.4);
        gloss(-0.5 * r, plateY + 0.1 * r, 1.4 * r, 0.6 * r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        int step = (int) ((t + e.getId() * 0.3) * 2);
        for (int i = 0; i < 7; i++) {
            if (i == 3) continue;
            int side = i < 3 ? 0 : 1;
            int j = side == 0 ? (step + i) % 3 : 4 + (step + i) % 3;
            double h = baseH[j] * (0.92 + 0.08 * Math.sin(t * 6 + i));
            bars[i].setBounds(bars[i].getX(), plateY - h, bars[i].getWidth(), h);
        }
        marker.setLocation(0, Math.sin(t * 4) * r * 0.08);
        for (int i = 0; i < 2; i++) Art.center(eye[2 * i + 1], eye[2 * i].getX() + eye[2 * i].getWidth() / 2 + dirX * 0.04 * r + 0.02 * r,
            eye[2 * i].getY() + eye[2 * i].getHeight() / 2 + dirY * 0.04 * r + 0.02 * r);
        int show = layersLeft(e, 7);
        if (show != shown) {
            shown = show;
            int[] order = { 0, 6, 1, 5, 2, 4, 3 };
            for (int i = 0; i < 7; i++) bars[order[i]].setVisible(i >= 7 - show);
        }
    }
}
