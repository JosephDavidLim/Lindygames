package lindefense.ui.sprites;

import acm.graphics.GLine;
import acm.graphics.GOval;
import lindefense.enemy.Enemy;

/**
 * Shell Sort: a chunky segmented nautilus shell (four nested, offset rings
 * with radiating ribs) with a little foot and two eyes on stalks peeking out
 * the front. Faint "gap" ghosts fade in ahead of it as the next gap-jump
 * approaches, spaced by the current gap from {@link Enemy#getShellJumpIndex()}.
 * Rings are lost from the outside in as HP drops.
 */
@SuppressWarnings("serial")
public final class ShellSortSprite extends EnemySprite {
    private final GOval[] rings = new GOval[4];
    private final GLine[] ribs = new GLine[3];
    private final GOval foot;
    private final GOval[] eye;
    private final GLine[] stalks = new GLine[2];
    private final GOval[] ghosts = new GOval[3];
    private int shown = -1;

    /** Builds the shell for the given enemy. */
    public ShellSortSprite(Enemy e) {
        super(e);
        for (int i = 0; i < 3; i++) {
            ghosts[i] = Art.ovalAt(0, 0, 1.1 * r, 1.1 * r);
            ghosts[i].setFilled(true);
            ((Thick.Oval) ghosts[i]).setLineWidth(1.5);
            add(ghosts[i]);
        }
        foot = part(Art.ovalAt(0.75 * r, 0.5 * r, 1.1 * r, 0.65 * r), Art.SHELL_DARK);
        for (int i = 0; i < 2; i++) stalks[i] = thin(stroke(Art.line(0, 0, 1, 1), Art.SHELL_DARK));
        for (int i = 0; i < 4; i++) {
            double d = (2.0 - i * 0.46) * r;
            rings[i] = part(Art.ovalAt(-0.05 * r + i * 0.13 * r, -0.05 * r + i * 0.09 * r, d, d),
                i % 2 == 0 ? Art.SHELL : Art.shade(Art.SHELL, 0.25));
            if (i > 0) thin(rings[i]);
        }
        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(200 + i * 45);
            ribs[i] = thin(stroke(Art.line(0.3 * r, 0.2 * r, 0.3 * r + Math.cos(a) * r, 0.2 * r + Math.sin(a) * r),
                Art.shade(Art.SHELL_DARK, 0.35)));
        }
        gloss(-0.05 * r, -0.05 * r, 2.0 * r, 2.0 * r);
        eye = eyes(1.3 * r, -0.05 * r, 0.36 * r, 0.3 * r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        int layers = layersLeft(e, 5);
        int show = layers >= 5 ? 4 : Math.max(1, Math.min(4, layers - 1));
        if (show != shown) {
            shown = show;
            for (int i = 0; i < 4; i++) rings[i].setVisible(i >= 4 - show);
            for (int i = 0; i < 3; i++) ribs[i].setVisible(show >= 3);
        }
        double creep = Math.sin(t * 6 + e.getId()) * r * 0.08;
        Art.center(foot, 0.75 * r + creep, 0.5 * r);
        for (int i = 0; i < 2; i++) {
            double ex = 1.25 * r + creep + (i == 0 ? -0.18 : 0.18) * r, ey = -0.05 * r + Math.sin(t * 5 + i) * r * 0.04;
            stalks[i].setStartPoint(1.1 * r + creep, 0.35 * r);
            stalks[i].setEndPoint(ex, ey + 0.1 * r);
            Art.center(eye[2 * i], ex, ey);
            Art.center(eye[2 * i + 1], ex + 0.04 * r, ey + 0.03 * r);
        }
        double cyc = (t + e.getId() * 0.5) % 3.0;
        double antic = Math.max(0, (cyc - 1.8) / 1.2);
        double gap = Math.min(Enemy.SHELL_GAPS[Math.min(e.getShellJumpIndex(), Enemy.SHELL_GAPS.length - 1)], 70) * 0.5;
        for (int i = 0; i < 3; i++) {
            double d = gap * (i + 1) / 3.0 + r * 0.8;
            Art.center(ghosts[i], dirX * d, dirY * d);
            int a = (int) (110 * antic * (1 - i * 0.25));
            ghosts[i].setFillColor(Art.alpha(Art.SHELL, a / 2));
            ghosts[i].setColor(Art.alpha(Art.OUTLINE, a));
        }
    }
}
