package lindefense.ui.sprites;

import acm.graphics.GArc;
import acm.graphics.GOval;
import lindefense.enemy.Enemy;

/**
 * Primitive Variable: a small glossy data orb with two cartoon eyes and a
 * tiny smile. The eyes glance in the direction of travel. The simplest
 * silhouette in the roster.
 */
@SuppressWarnings("serial")
public final class PrimitiveVariableSprite extends EnemySprite {
    private final GOval[] eye;
    private final GArc smile;
    private final double eyeY;

    /** Builds the orb for the given enemy. */
    public PrimitiveVariableSprite(Enemy e) {
        super(e);
        bodyOval(0, 0, 2 * r, 2 * r, Art.PRIMITIVE);
        gloss(0, 0, 2 * r, 2 * r);
        eyeY = -0.12 * r;
        eye = eyes(0.05 * r, eyeY, 0.75 * r, 0.5 * r);
        smile = Art.arc(-0.3 * r, 0.1 * r, 0.6 * r, 0.5 * r, 200, 140);
        ((Thick.Arc) smile).setLineWidth(Thick.DETAIL);
        fixedStroke(smile, Art.OUTLINE);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        double look = Math.sin(t * 2.0 + e.getId()) * 0.5 + 0.5;
        for (int i = 0; i < 2; i++) {
            double ex = 0.05 * r + (i == 0 ? -1 : 1) * 0.375 * r;
            Art.center(eye[2 * i + 1], ex + dirX * 0.1 * r * look + 0.06 * r, eyeY + dirY * 0.1 * r * look + 0.04 * r);
        }
        // one layer: the base class dims the body; squash the smile when hurt
        smile.setSweepAngle(dmg > 0.5 ? 60 : 140);
        smile.setStartAngle(dmg > 0.5 ? 240 : 200);
    }
}
