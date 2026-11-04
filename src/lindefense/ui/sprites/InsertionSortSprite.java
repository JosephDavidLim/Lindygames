package lindefense.ui.sprites;

import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import lindefense.enemy.Enemy;

/**
 * Insertion Sort: a hand of four sorted slabs on a chunky base plate, plus a
 * bright fifth slab with eyes that keeps lifting out, sliding sideways and
 * dropping back into a gap that opens for it. Slabs are lost from the tallest
 * end as HP drops.
 */
@SuppressWarnings("serial")
public final class InsertionSortSprite extends EnemySprite {
    private final GRect[] slabs = new GRect[4];
    private final GRect active;
    private final GOval activeShine;
    private final GOval[] eye;
    private final GPolygon[] cracks;
    private final double slabW, baseY;
    private int shown = -1;

    /** Builds the slab hand for the given enemy. */
    public InsertionSortSprite(Enemy e) {
        super(e);
        slabW = r * 0.42;
        baseY = r * 0.8;
        bodyRect(-1.25 * r, baseY - 0.05 * r, 2.5 * r, 0.42 * r, Art.shade(Art.INSERTION, 0.3));
        for (int i = 0; i < 4; i++) {
            double h = r * (0.5 + 0.28 * i);
            slabs[i] = thin(part(Art.rect(0, 0, slabW, h), Art.mix(Art.INSERTION, Art.tint(Art.INSERTION, 0.5), i / 4.0)));
        }
        active = part(Art.rect(0, 0, slabW, r * 0.9), Art.tint(Art.INSERTION, 0.2));
        activeShine = highlight(0, 0, slabW * 0.5, r * 0.25);
        eye = eyes(0, 0, slabW * 0.5, slabW * 0.36);
        gloss(-0.4 * r, baseY - 0.2 * r, 1.8 * r, 1.2 * r);
        cracks = makeCracks(2, r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        double cyc = (t * 0.6 + e.getId() * 0.21) % 1.0;
        double lift = cyc < 0.25 ? cyc / 0.25 : (cyc < 0.65 ? 1 : (cyc < 0.8 ? 1 - (cyc - 0.65) / 0.15 : 0));
        double slide = cyc < 0.25 ? 0 : (cyc < 0.65 ? (cyc - 0.25) / 0.4 : 1);
        double gap = slabW * 1.05;
        double startX = -1.1 * r;
        for (int i = 0; i < 4; i++) {
            double x = startX + i * slabW * 1.1 + (i >= 1 ? gap * (cyc >= 0.8 ? 1 : slide) : 0) + slabW * 0.05;
            slabs[i].setLocation(x, baseY - slabs[i].getHeight());
        }
        double fromX = startX + 4 * slabW * 1.1 + slabW * 0.05;
        double toX = startX + 1 * slabW * 1.1 + slabW * 0.05;
        double ax = fromX + (toX - fromX) * slide;
        double ay = baseY - active.getHeight() - lift * r * 0.75;
        active.setLocation(ax, ay);
        Art.center(activeShine, ax + slabW * 0.35, ay + r * 0.2);
        for (int i = 0; i < 2; i++) {
            double ex = ax + slabW * 0.5 + (i == 0 ? -1 : 1) * slabW * 0.25, ey = ay + r * 0.5;
            Art.center(eye[2 * i], ex, ey);
            Art.center(eye[2 * i + 1], ex + (slide < 1 ? -0.04 * r : 0.03 * r), ey + 0.03 * r);
        }
        int show = Math.max(1, layersLeft(e, 4));
        if (show != shown) {
            shown = show;
            for (int i = 0; i < 4; i++) slabs[i].setVisible(i < show);
        }
        showCracks(cracks, dmg);
    }
}
