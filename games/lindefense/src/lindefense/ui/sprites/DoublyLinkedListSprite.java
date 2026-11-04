package lindefense.ui.sprites;

import acm.graphics.GLine;
import acm.graphics.GPolygon;
import java.awt.Color;
import lindefense.enemy.Enemy;

/**
 * Doubly Linked List: three chunky square nodes on twin rails with
 * double-headed arrows between them. When it accelerates (hit), the nodes
 * flush redder and speed streaks trail behind. Nodes are lost from the tail
 * as HP falls.
 */
@SuppressWarnings("serial")
public final class DoublyLinkedListSprite extends EnemySprite {
    private final GPolygon[] rims = new GPolygon[3];
    private final GPolygon[] faces = new GPolygon[3];
    private final GPolygon[] arrows = new GPolygon[2];
    private final GLine railA, railB;
    private final GLine[] streaks = new GLine[3];
    private final GPolygon[] cracks;
    private int shownNodes = -1;
    private int accelBucket = -1;

    /** Builds the chain for the given enemy. */
    public DoublyLinkedListSprite(Enemy e) {
        super(e);
        railA = stroke(Art.line(0, 0, 1, 0), Art.shade(Art.DLL, 0.5));
        railB = stroke(Art.line(0, 0, 1, 0), Art.shade(Art.DLL, 0.5));
        for (int i = 0; i < 3; i++) {
            streaks[i] = fixedStroke(Art.line(0, 0, 1, 0), Art.alpha(Art.DLL, 160));
            streaks[i].setVisible(false);
        }
        double len = r * 0.55;
        for (int i = 0; i < 2; i++) {
            arrows[i] = rotating(Art.poly(-len, 0, -len * 0.45, -r * 0.26, -len * 0.45, -r * 0.1,
                len * 0.45, -r * 0.1, len * 0.45, -r * 0.26, len, 0,
                len * 0.45, r * 0.26, len * 0.45, r * 0.1, -len * 0.45, r * 0.1, -len * 0.45, r * 0.26));
            thin(part(arrows[i], Art.shade(Art.DLL, 0.3)));
        }
        for (int i = 2; i >= 0; i--) {
            double R = (i == 0 ? 0.72 : 0.64) * r;
            GPolygon[] pp = bodyPolyParts(i == 0 ? Art.tint(Art.DLL, 0.1) : Art.DLL,
                -R * 0.7, -R * 0.7, R * 0.7, -R * 0.7, R * 0.7, R * 0.7, -R * 0.7, R * 0.7);
            rims[i] = rotating(pp[0]);
            faces[i] = rotating(pp[1]);
            highlight(0, 0, R * 0.5, R * 0.3).setVisible(false);
        }
        cracks = makeCracks(2, r * 0.8);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        setFacing(facingDeg);
        double px = -dirY, py = dirX;
        double spacing = r * 1.05;
        double[] cx = new double[3], cy = new double[3];
        for (int i = 0; i < 3; i++) {
            double along = (1 - i) * spacing;
            double wob = Math.sin(t * 9 - i * 1.1 + e.getId()) * r * 0.12;
            cx[i] = dirX * along + px * wob;
            cy[i] = dirY * along + py * wob;
            rims[i].setLocation(cx[i], cy[i]);
            faces[i].setLocation(cx[i], cy[i]);
        }
        for (int i = 0; i < 2; i++) arrows[i].setLocation((cx[i] + cx[i + 1]) / 2, (cy[i] + cy[i + 1]) / 2);

        int layers = layersLeft(e, 5);               // 5,4 -> 3 nodes; 3,2 -> 2; 1 -> 1
        int show = layers >= 4 ? 3 : (layers >= 2 ? 2 : 1);
        if (show != shownNodes) {
            shownNodes = show;
            for (int i = 0; i < 3; i++) { rims[i].setVisible(i < show); faces[i].setVisible(i < show); }
            for (int i = 0; i < 2; i++) arrows[i].setVisible(i + 1 < show);
        }
        int last = show - 1;
        double off = r * 0.5;
        railA.setStartPoint(cx[0] + dirX * off + px * r * 0.48, cy[0] + dirY * off + py * r * 0.48);
        railA.setEndPoint(cx[last] - dirX * off + px * r * 0.48, cy[last] - dirY * off + py * r * 0.48);
        railB.setStartPoint(cx[0] + dirX * off - px * r * 0.48, cy[0] + dirY * off - py * r * 0.48);
        railB.setEndPoint(cx[last] - dirX * off - px * r * 0.48, cy[last] - dirY * off - py * r * 0.48);

        double accel = e.getAccelBonus();
        int bucket = (int) Math.round(accel / 0.15);
        if (bucket != accelBucket) {
            accelBucket = bucket;
            Color hot = Art.mix(Art.DLL, new Color(255, 60, 40), Math.min(1, accel / 0.9));
            for (int i = 0; i < 3; i++) {
                recolor(rims[i], Art.shade(hot, 0.3), Art.outline(hot));
                recolor(faces[i], i == 0 ? Art.tint(hot, 0.1) : hot, i == 0 ? Art.tint(hot, 0.1) : hot);
            }
            for (GLine s : streaks) s.setVisible(accel > 0);
        }
        if (accel > 0) {
            double len = r * (0.6 + 1.6 * accel) * (0.8 + 0.2 * Math.sin(t * 20));
            for (int i = 0; i < 3; i++) {
                double side = (i - 1) * r * 0.45;
                double sx = cx[last] - dirX * r * 0.5 + px * side, sy = cy[last] - dirY * r * 0.5 + py * side;
                streaks[i].setStartPoint(sx, sy);
                streaks[i].setEndPoint(sx - dirX * len, sy - dirY * len);
            }
        }
        showCracks(cracks, dmg);
    }
}
