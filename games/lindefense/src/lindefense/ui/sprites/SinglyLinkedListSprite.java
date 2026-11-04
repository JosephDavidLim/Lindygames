package lindefense.ui.sprites;

import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import lindefense.enemy.Enemy;

/**
 * Singly Linked List: three glossy round nodes strung along the direction of
 * travel, joined by one-way arrows that point forward, with a small null
 * terminator trailing the tail. The head node has eyes. The chain snakes as
 * it moves; nodes drop off from the tail as HP falls.
 */
@SuppressWarnings("serial")
public final class SinglyLinkedListSprite extends EnemySprite {
    private final GOval[] rims = new GOval[3];
    private final GOval[] faces = new GOval[3];
    private final GOval[] shine = new GOval[3];
    private final GPolygon[] arrows = new GPolygon[2];
    private final GRect nul;
    private final GOval[] eye;
    private final double[] diam = new double[3];
    private int shownNodes = -1;

    /** Builds the chain for the given enemy. */
    public SinglyLinkedListSprite(Enemy e) {
        super(e);
        for (int i = 0; i < 2; i++) {
            double len = r * 0.6;
            arrows[i] = rotating(Art.poly(-len, -r * 0.1, len * 0.25, -r * 0.1, len * 0.25, -r * 0.3, len, 0,
                len * 0.25, r * 0.3, len * 0.25, r * 0.1, -len, r * 0.1));
            thin(part(arrows[i], Art.shade(Art.SLL, 0.35)));
        }
        nul = thin(part(Art.rect(0, 0, r * 0.34, r * 0.34), Art.OUTLINE, Art.OUTLINE));
        for (int i = 2; i >= 0; i--) {
            diam[i] = (i == 0 ? 1.1 : 0.9) * r;
            rims[i] = part(Art.ovalAt(0, 0, diam[i], diam[i]), Art.shade(Art.SLL, 0.3));
            Thick.Oval f = Art.ovalAt(0, 0, diam[i] * 0.82, diam[i] * 0.8);
            f.setLineWidth(0);
            faces[i] = part(f, i == 0 ? Art.tint(Art.SLL, 0.1) : Art.SLL, Art.SLL);
            shine[i] = highlight(0, 0, diam[i] * 0.4, diam[i] * 0.28);
        }
        eye = eyes(0, 0, 0.42 * r, 0.3 * r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        setFacing(facingDeg);
        double px = -dirY, py = dirX;
        double spacing = r * 1.0;
        double[] cx = new double[3], cy = new double[3];
        for (int i = 0; i < 3; i++) {
            double along = (1 - i) * spacing;
            double wob = Math.sin(t * 7 - i * 1.3 + e.getId()) * r * 0.22;
            cx[i] = dirX * along + px * wob;
            cy[i] = dirY * along + py * wob;
            Art.center(rims[i], cx[i], cy[i]);
            Art.center(faces[i], cx[i] - diam[i] * 0.04, cy[i] - diam[i] * 0.06);
            Art.center(shine[i], cx[i] - diam[i] * 0.2, cy[i] - diam[i] * 0.22);
        }
        for (int i = 0; i < 2; i++) arrows[i].setLocation((cx[i] + cx[i + 1]) / 2, (cy[i] + cy[i + 1]) / 2);
        // eyes on the head, looking forward
        for (int i = 0; i < 2; i++) {
            double ex = cx[0] + px * (i == 0 ? -1 : 1) * 0.22 * r + dirX * 0.12 * r;
            double ey = cy[0] + py * (i == 0 ? -1 : 1) * 0.22 * r + dirY * 0.12 * r;
            Art.center(eye[2 * i], ex, ey);
            Art.center(eye[2 * i + 1], ex + dirX * 0.07 * r, ey + dirY * 0.07 * r);
        }
        int layers = layersLeft(e, 4);               // 4,3 -> 3 nodes; 2 -> 2; 1 -> 1
        int show = layers >= 3 ? 3 : Math.max(1, layers);
        if (show != shownNodes) {
            shownNodes = show;
            for (int i = 0; i < 3; i++) { rims[i].setVisible(i < show); faces[i].setVisible(i < show); shine[i].setVisible(i < show); }
            for (int i = 0; i < 2; i++) arrows[i].setVisible(i + 1 < show);
        }
        int last = show - 1;
        nul.setLocation(cx[last] - dirX * spacing * 0.78 - r * 0.17, cy[last] - dirY * spacing * 0.78 - r * 0.17);
    }
}
