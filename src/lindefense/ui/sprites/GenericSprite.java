package lindefense.ui.sprites;

import acm.graphics.GLabel;
import acm.graphics.GPolygon;
import java.awt.Font;
import lindefense.enemy.Enemy;

/**
 * Fallback model for any {@link lindefense.enemy.EnemType} constant that has no
 * dedicated sprite yet: a glossy hexagon in the type's signature colour with
 * the first letter of its display name, so a newly added enemy still renders
 * sensibly instead of crashing the renderer.
 */
@SuppressWarnings("serial")
public final class GenericSprite extends EnemySprite {
    private final GPolygon[] cracks;

    /** Builds the placeholder model for the given enemy. */
    public GenericSprite(Enemy e) {
        super(e);
        java.awt.Color c = Art.colorOf(e.getType());
        bodyPoly(c, hexagon(r * 0.95));
        gloss(0, 0, 1.9 * r, 1.9 * r);
        GLabel l = new GLabel(e.getType().displayName.substring(0, 1));
        l.setFont(lindefense.ui.Ui.display(Font.BOLD, Math.max(8, (int) Math.round(r * 0.9))));
        fixedStroke(l, java.awt.Color.WHITE);
        l.setLocation(-l.getWidth() / 2, l.getAscent() * 0.38);
        cracks = makeCracks(3, r);
        finishOverlays();
    }

    private static double[] hexagon(double R) {
        double[] xy = new double[12];
        for (int i = 0; i < 6; i++) {
            xy[2 * i] = R * Math.cos(i * Math.PI / 3);
            xy[2 * i + 1] = R * Math.sin(i * Math.PI / 3);
        }
        return xy;
    }

    @Override protected void updateBody(Enemy e, double t) {
        showCracks(cracks, dmg);
    }
}
