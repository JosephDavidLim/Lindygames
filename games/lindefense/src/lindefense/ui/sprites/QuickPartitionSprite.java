package lindefense.ui.sprites;

import acm.graphics.GPolygon;
import lindefense.enemy.Enemy;

/**
 * Quick Partition: one side of a Quick Sort. A small chunky salmon plate with
 * three slanted bars all leaning the same way (all "less than" the pivot)
 * and half a partition line on one edge.
 */
@SuppressWarnings("serial")
public final class QuickPartitionSprite extends EnemySprite {
    private final GPolygon[] bars = new GPolygon[3];
    private int shown = -1;

    /** Builds the partition fragment for the given enemy. */
    public QuickPartitionSprite(Enemy e) {
        super(e);
        double plateY = r * 0.6;
        bodyPoly(Art.shade(Art.QUICK_PARTITION, 0.25), -1.05 * r, plateY, 1.05 * r, plateY, 0.9 * r, plateY + 0.4 * r, -1.05 * r, plateY + 0.4 * r);
        double bw = r * 0.36;
        for (int i = 0; i < 3; i++) {
            double h = r * (0.6 + 0.3 * i);
            double x = -0.8 * r + i * bw * 1.4;
            bars[i] = thin(part(Art.poly(x, plateY, x + bw, plateY, x + bw + 0.2 * r, plateY - h, x + 0.2 * r, plateY - h),
                Art.mix(Art.QUICK_PARTITION, Art.QUICK, i * 0.2)));
        }
        thin(fixedStroke(Art.line(1.1 * r, plateY - 1.1 * r, 1.1 * r, plateY + 0.5 * r), Art.tint(Art.QUICK_PIVOT, 0.3)));
        gloss(-0.1 * r, plateY - 0.3 * r, 1.6 * r, 1.4 * r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        int show = Math.max(1, layersLeft(e, 3));
        if (show != shown) {
            shown = show;
            for (int i = 0; i < 3; i++) bars[i].setVisible(i < show);
        }
        for (int i = 0; i < 3; i++) bars[i].setLocation(0, Math.sin(t * 5 + i) * r * 0.04);
    }
}
