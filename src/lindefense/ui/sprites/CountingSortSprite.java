package lindefense.ui.sprites;

import acm.graphics.GLabel;
import acm.graphics.GPolygon;
import java.awt.Color;
import java.awt.Font;
import lindefense.enemy.Enemy;

/**
 * Counting Sort: a glossy lime outer ring (the single outer layer) holding
 * four white counter boxes in a 2x2 grid, each with a coloured tab and a digit
 * that keeps ticking upwards. The ring cracks as its one layer is worn down.
 */
@SuppressWarnings("serial")
public final class CountingSortSprite extends EnemySprite {
    private final GLabel[] digits = new GLabel[4];
    private final int[] lastDigit = { -1, -1, -1, -1 };
    private final GPolygon[] cracks;

    /** Builds the counter cluster for the given enemy. */
    public CountingSortSprite(Enemy e) {
        super(e);
        bodyOval(0, 0, 2.1 * r, 2.1 * r, Art.COUNTING);
        thin(part(Art.ovalAt(0, 0, 1.6 * r, 1.6 * r), Art.tint(Art.COUNTING, 0.75), Art.shade(Art.COUNTING, 0.4)));
        double s = r * 0.62;
        Font f = new Font("SansSerif", Font.BOLD, Math.max(7, (int) Math.round(r * 0.52)));
        for (int i = 0; i < 4; i++) {
            double cx = (i % 2 == 0 ? -0.36 : 0.36) * r, cy = (i < 2 ? -0.36 : 0.36) * r;
            thin(part(Art.rect(cx - s / 2, cy - s / 2, s, s), new Color(252, 252, 245), Art.OUTLINE));
            Thick.Rect tab = Art.rect(cx - s / 2, cy - s / 2, s, s * 0.24);
            tab.setLineWidth(0);
            part(tab, Art.mix(Art.COUNTING, Art.PRIMITIVE, i / 3.0), Art.mix(Art.COUNTING, Art.PRIMITIVE, i / 3.0));
            digits[i] = fixedStroke(new GLabel("0"), Art.OUTLINE);
            digits[i].setFont(f);
            digits[i].setLocation(cx - digits[i].getWidth() / 2, cy + s * 0.12 + digits[i].getAscent() / 2);
        }
        gloss(0, 0, 2.1 * r, 2.1 * r);
        cracks = makeCracks(3, r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        for (int i = 0; i < 4; i++) {
            int d = (int) ((t * (1.0 + i * 0.4) + e.getId() + i * 3) % 10);
            if (d != lastDigit[i]) {
                lastDigit[i] = d;
                digits[i].setLabel(Integer.toString(d));
            }
        }
        showCracks(cracks, dmg);
    }
}
