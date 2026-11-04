package lindefense.ui.sprites;

import acm.graphics.GLine;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import lindefense.enemy.Enemy;

/**
 * TimSort: the hybrid. A chunky hexagon split down the seam like Merge Sort,
 * but its left half is purple with sorted merge bars and its right half is
 * orange with Insertion Sort slabs (one of which keeps lifting and sliding
 * into place). One eye per half, and two swept speed fins trail behind it.
 * Elements are lost two per layer.
 */
@SuppressWarnings("serial")
public final class TimSortSprite extends EnemySprite {
    private final GPolygon[] left, right;
    private final GRect[] bars = new GRect[2];
    private final GRect[] slabs = new GRect[3];
    private final GPolygon[] fins = new GPolygon[2];
    private final GOval shineL, shineR;
    private final GOval[] eye;
    private final GPolygon[] cracks;
    private int shown = -1;

    /** Builds the hybrid body for the given enemy. */
    public TimSortSprite(Enemy e) {
        super(e);
        double R = r * 1.0, h = R * 0.866;
        for (int i = 0; i < 2; i++) {
            double side = i == 0 ? -1 : 1;
            fins[i] = rotating(Art.poly(-0.4 * r, side * 0.35 * r, -1.5 * r, side * 0.95 * r, -0.75 * r, side * 0.25 * r));
            thin(part(fins[i], Art.shade(i == 0 ? Art.TIMSORT : Art.INSERTION, 0.3)));
        }
        left = bodyPolyParts(Art.TIMSORT, 0, -h, -R * 0.5, -h, -R, 0, -R * 0.5, h, 0, h);
        right = bodyPolyParts(Art.INSERTION, 0, -h, R * 0.5, -h, R, 0, R * 0.5, h, 0, h);
        for (int i = 0; i < 2; i++) {
            bars[i] = thin(part(Art.rect(0, 0, r * 0.22, r * (0.4 + 0.3 * i)), Art.tint(Art.TIMSORT, 0.45 + 0.2 * i)));
        }
        for (int i = 0; i < 3; i++) {
            slabs[i] = thin(part(Art.rect(0, 0, r * 0.2, r * (0.35 + 0.2 * i)), Art.tint(Art.INSERTION, 0.3 + 0.2 * i)));
        }
        for (int i = 0; i < 2; i++) {
            thin(fixedStroke(Art.line(0, -0.7 * R + i * 0.8 * R, 0, -0.7 * R + i * 0.8 * R + 0.5 * R), Art.tint(Art.TIMSORT, 0.6)));
        }
        shineL = highlight(-0.5 * r, -0.5 * r, 0.42 * r, 0.26 * r);
        shineR = highlight(0.3 * r, -0.55 * r, 0.22 * r, 0.14 * r);
        eye = eyes(0, -0.25 * r, 0.7 * r, 0.34 * r);
        cracks = makeCracks(3, r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        setFacing(facingDeg);
        double gap = r * (0.05 + 0.04 * Math.sin(t * 3.5 + e.getId()) + 0.3 * dmg);
        left[0].setLocation(-gap, 0); left[1].setLocation(-gap, 0);
        right[0].setLocation(gap, 0); right[1].setLocation(gap, 0);
        Art.center(shineL, -gap - 0.5 * r, -0.5 * r);
        Art.center(shineR, gap + 0.3 * r, -0.55 * r);
        for (int i = 0; i < 2; i++) bars[i].setLocation(-gap - 0.72 * r + i * 0.3 * r, 0.48 * r - bars[i].getHeight());
        double cyc = (t * 0.9 + e.getId() * 0.3) % 1.0;
        double lift = cyc < 0.3 ? cyc / 0.3 : (cyc < 0.6 ? 1 : Math.max(0, 1 - (cyc - 0.6) / 0.2));
        double slide = cyc < 0.3 ? 0 : (cyc < 0.6 ? (cyc - 0.3) / 0.3 : 1);
        for (int i = 0; i < 3; i++) {
            double x = gap + 0.12 * r + i * 0.26 * r;
            double y = 0.48 * r - slabs[i].getHeight();
            if (i == 2) { x = gap + 0.12 * r + 0.26 * r * (2 - slide); y -= lift * r * 0.5; }
            slabs[i].setLocation(x, y);
        }
        for (int i = 0; i < 2; i++) {
            double ex = (i == 0 ? -gap - 0.35 * r : gap + 0.35 * r), ey = -0.25 * r;
            Art.center(eye[2 * i], ex, ey);
            Art.center(eye[2 * i + 1], ex + dirX * 0.06 * r + 0.03 * r, ey + dirY * 0.06 * r + 0.03 * r);
        }
        int show = Math.max(1, (int) Math.ceil(layersLeft(e, 10) / 2.0));
        if (show != shown) {
            shown = show;
            bars[1].setVisible(show >= 5); slabs[2].setVisible(show >= 4);
            bars[0].setVisible(show >= 3); slabs[1].setVisible(show >= 2);
        }
        showCracks(cracks, dmg);
    }
}
