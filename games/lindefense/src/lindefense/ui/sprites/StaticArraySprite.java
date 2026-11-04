package lindefense.ui.sprites;

import acm.graphics.GPolygon;
import acm.graphics.GRect;
import lindefense.enemy.Enemy;

/**
 * Static Array: a rigid, orderly container with three fixed cells and heavy
 * end-caps (its size never changes). Nothing moves; as HP drops the cells
 * empty from the right and the casing cracks.
 */
@SuppressWarnings("serial")
public final class StaticArraySprite extends EnemySprite {
    private final GRect[] cells = new GRect[3];
    private final GPolygon[] cracks;
    private int shownCells = -1;

    /** Builds the container for the given enemy. */
    public StaticArraySprite(Enemy e) {
        super(e);
        double w = 2.4 * r, h = 1.35 * r;
        bodyRect(-w / 2, -h / 2, w, h, Art.STATIC_ARRAY);
        // heavy end caps: "fixed size"
        thin(part(Art.rect(-w / 2, -h / 2, w * 0.12, h), Art.shade(Art.STATIC_ARRAY, 0.5)));
        thin(part(Art.rect(w / 2 - w * 0.12, -h / 2, w * 0.12, h), Art.shade(Art.STATIC_ARRAY, 0.5)));
        double inner = w * 0.76, cw = inner / 3;
        for (int i = 0; i < 3; i++) {
            double cx = -inner / 2 + cw * (i + 0.5);
            cells[i] = thin(part(Art.rect(cx - cw * 0.32, -h * 0.28, cw * 0.64, h * 0.56), Art.tint(Art.STATIC_ARRAY, 0.55)));
        }
        thin(stroke(Art.line(-inner / 6, -h / 2 + 2, -inner / 6, h / 2 - 2), Art.shade(Art.STATIC_ARRAY, 0.45)));
        thin(stroke(Art.line(inner / 6, -h / 2 + 2, inner / 6, h / 2 - 2), Art.shade(Art.STATIC_ARRAY, 0.45)));
        gloss(0, -h * 0.12, w, h * 0.9);
        cracks = makeCracks(2, r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        int show = Math.max(0, (int) Math.ceil(hpFrac * 3 - 1e-6));
        if (show != shownCells) {
            shownCells = show;
            for (int i = 0; i < 3; i++) cells[i].setVisible(i < show);
        }
        showCracks(cracks, dmg);
    }
}
