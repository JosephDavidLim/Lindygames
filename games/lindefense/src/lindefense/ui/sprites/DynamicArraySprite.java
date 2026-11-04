package lindefense.ui.sprites;

import acm.graphics.GLine;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import lindefense.enemy.Enemy;

/**
 * Dynamic Array: a container with three filled cells and an open right end
 * where a translucent "capacity" cell keeps growing and shrinking, with a plus
 * sign inside it. A thin capacity bracket spans the whole thing.
 */
@SuppressWarnings("serial")
public final class DynamicArraySprite extends EnemySprite {
    private final GRect[] cells = new GRect[3];
    private final GRect growth;
    private final GLine plusH, plusV, bracket;
    private final GPolygon[] cracks;
    private final double w, h, left;
    private int shownCells = -1;

    /** Builds the container for the given enemy. */
    public DynamicArraySprite(Enemy e) {
        super(e);
        w = 2.1 * r; h = 1.35 * r; left = -w / 2 - 0.3 * r;
        bodyRect(left, -h / 2, w, h, Art.DYNAMIC_ARRAY);
        thin(part(Art.rect(left, -h / 2, w * 0.12, h), Art.shade(Art.DYNAMIC_ARRAY, 0.5)));
        double cw = w * 0.88 / 3;
        for (int i = 0; i < 3; i++) {
            double cx = left + w * 0.12 + cw * (i + 0.5);
            cells[i] = thin(part(Art.rect(cx - cw * 0.32, -h * 0.28, cw * 0.64, h * 0.56), Art.tint(Art.DYNAMIC_ARRAY, 0.55)));
        }
        growth = thin(fixed(Art.rect(left + w, -h / 2, 0.6 * r, h),
            Art.alpha(Art.DYNAMIC_ARRAY, 90), Art.alpha(Art.outline(Art.DYNAMIC_ARRAY), 220)));
        plusH = thin(fixedStroke(Art.line(0, 0, 1, 0), Art.shade(Art.DYNAMIC_ARRAY, 0.5)));
        plusV = thin(fixedStroke(Art.line(0, 0, 0, 1), Art.shade(Art.DYNAMIC_ARRAY, 0.5)));
        bracket = thin(fixedStroke(Art.line(left, -h / 2 - 0.28 * r, left + w, -h / 2 - 0.28 * r), Art.shade(Art.DYNAMIC_ARRAY, 0.4)));
        gloss(left + w / 2, -h * 0.12, w, h * 0.9);
        cracks = makeCracks(3, r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        double gw = r * (0.45 + 0.4 * (0.5 + 0.5 * Math.sin(t * 2.2 + e.getId())));
        double gx = left + w;
        growth.setBounds(gx, -h / 2, gw, h);
        double cx = gx + gw / 2, s = Math.min(gw * 0.3, r * 0.25);
        plusH.setStartPoint(cx - s, 0); plusH.setEndPoint(cx + s, 0);
        plusV.setStartPoint(cx, -s); plusV.setEndPoint(cx, s);
        bracket.setEndPoint(gx + gw, -h / 2 - 0.28 * r);
        int show = Math.max(0, (int) Math.ceil(hpFrac * 3 - 1e-6));
        if (show != shownCells) {
            shownCells = show;
            for (int i = 0; i < 3; i++) cells[i].setVisible(i < show);
        }
        showCracks(cracks, dmg);
    }
}
