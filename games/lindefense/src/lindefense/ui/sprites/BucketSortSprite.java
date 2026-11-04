package lindefense.ui.sprites;

import acm.graphics.GArc;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.awt.Color;
import java.util.List;
import lindefense.enemy.Enemy;

/**
 * Bucket Sort: a chunky tin bucket with a wooden rim, a swinging handle and a
 * pair of eyes on its front. Enemies it has absorbed show through as coloured
 * dots bobbing in a liquid whose level rises with
 * {@link Enemy#getStoredEnemies()}. Dents and a leaking drip appear as HP drops.
 */
@SuppressWarnings("serial")
public final class BucketSortSprite extends EnemySprite {
    private static final Color WATER = new Color(70, 150, 240);
    private final GRect liquid;
    private final GOval[] dots = new GOval[6];
    private final GArc handle;
    private final GOval drip;
    private final GOval[] eye;
    private final GPolygon[] cracks;
    private final double top, bottom;
    private int shownCount = -1;

    /** Builds the bucket for the given enemy. */
    public BucketSortSprite(Enemy e) {
        super(e);
        top = -0.75 * r; bottom = 1.0 * r;
        double halfTop = 1.0 * r, halfBot = 0.74 * r;
        bodyPoly(Art.BUCKET, -halfTop, top, halfTop, top, halfBot, bottom, -halfBot, bottom);
        liquid = fixed(Art.rect(-halfBot * 0.95, bottom - 0.1 * r, halfBot * 1.9, 0.1 * r), Art.alpha(WATER, 130), Art.alpha(WATER, 0));
        ((Thick.Rect) liquid).setLineWidth(0);
        for (int i = 0; i < 6; i++) {
            dots[i] = Art.ovalAt(0, 0, 0.32 * r, 0.32 * r);
            dots[i].setFilled(true);
            ((Thick.Oval) dots[i]).setLineWidth(Thick.DETAIL);
            dots[i].setVisible(false);
            add(dots[i]);
        }
        // side band and rim
        thin(part(Art.rect(-0.9 * r, 0.3 * r, 1.8 * r, 0.16 * r), Art.shade(Art.BUCKET, 0.4)));
        part(Art.rect(-halfTop - 0.1 * r, top - 0.2 * r, 2 * halfTop + 0.2 * r, 0.34 * r), Art.BUCKET_WOOD);
        handle = lineWidth(stroke(Art.arc(-0.85 * r, -1.6 * r, 1.7 * r, 1.5 * r, 0, 180), Art.BUCKET_WOOD), 2.6);
        drip = fixed(Art.ovalAt(0.5 * r, bottom + 0.25 * r, 0.2 * r, 0.32 * r), WATER, Art.OUTLINE);
        ((Thick.Oval) drip).setLineWidth(Thick.DETAIL);
        drip.setVisible(false);
        gloss(0, 0.1 * r, 1.9 * r, 1.7 * r);
        eye = eyes(0, -0.2 * r, 0.6 * r, 0.36 * r);
        cracks = makeCracks(3, r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        List<Enemy> stored = e.getStoredEnemies();
        int n = Math.min(6, stored.size());
        double level = (0.1 + 0.6 * n / 6.0) * (bottom - top);
        liquid.setBounds(-0.7 * r, bottom - level, 1.4 * r, level);
        if (n != shownCount) {
            shownCount = n;
            for (int i = 0; i < 6; i++) {
                dots[i].setVisible(i < n);
                if (i < n) {
                    Color c = Art.colorOf(stored.get(i).getType());
                    dots[i].setFillColor(c);
                    dots[i].setColor(Art.OUTLINE);
                }
            }
        }
        for (int i = 0; i < n; i++) {
            double x = (-0.42 + 0.42 * (i % 3)) * r;
            double y = bottom - level * (0.3 + 0.45 * (i / 3)) + Math.sin(t * 3 + i) * r * 0.06;
            Art.center(dots[i], x, y);
        }
        handle.setLocation(-0.85 * r + Math.sin(t * 3 + e.getId()) * r * 0.08, -1.6 * r);
        for (int i = 0; i < 2; i++) {
            Art.center(eye[2 * i + 1], eye[2 * i].getX() + eye[2 * i].getWidth() / 2 + dirX * 0.05 * r + 0.02 * r,
                eye[2 * i].getY() + eye[2 * i].getHeight() / 2 + dirY * 0.05 * r + 0.03 * r);
        }
        showCracks(cracks, dmg);
        boolean leak = dmg > 0.45;
        drip.setVisible(leak);
        if (leak) Art.center(drip, 0.5 * r, bottom + 0.1 * r + ((t * 1.5) % 1.0) * r * 0.6);
    }
}
