package lindefense.ui.sprites;

import acm.graphics.GArc;
import acm.graphics.GLine;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import lindefense.enemy.Enemy;

/**
 * Heap Sort: a chunky riveted steel heater-shield carrying a seven-node
 * binary heap (root, two children, four leaves). A bright ring "sifts" down
 * the tree, and a translucent green aura trails behind it to show the zone it
 * shields. Leaves are lost from the bottom as HP drops; at one layer only the
 * root remains.
 */
@SuppressWarnings("serial")
public final class HeapSortSprite extends EnemySprite {
    private static final double[][] NODE = {
        { 0, -0.5 }, { -0.42, 0.0 }, { 0.42, 0.0 },
        { -0.62, 0.5 }, { -0.22, 0.5 }, { 0.22, 0.5 }, { 0.62, 0.5 } };
    private final GOval[] nodes = new GOval[7];
    private final GLine[] edges = new GLine[6];
    private final GOval hop;
    private final GArc aura;
    private final GPolygon[] cracks;
    private int shown = -1;

    /** Builds the armoured heap for the given enemy. */
    public HeapSortSprite(Enemy e) {
        super(e);
        aura = fixed(Art.arc(-1.4 * r, -1.4 * r, 2.8 * r, 2.8 * r, 90, 180),
            Art.alpha(Art.HEAP_NODE, 50), Art.alpha(Art.HEAP_NODE, 0));
        ((Thick.Arc) aura).setLineWidth(0);
        bodyPoly(Art.HEAP_ARMOR, -1.05 * r, -0.9 * r, 1.05 * r, -0.9 * r, 1.05 * r, 0.3 * r, 0, 1.12 * r, -1.05 * r, 0.3 * r);
        for (int i = 0; i < 4; i++) {
            double x = (i % 2 == 0 ? -0.86 : 0.86) * r, y = (i < 2 ? -0.72 : 0.2) * r;
            thin(part(Art.ovalAt(x, y, 0.2 * r, 0.2 * r), Art.STEEL, Art.OUTLINE));
        }
        int[][] E = { { 0, 1 }, { 0, 2 }, { 1, 3 }, { 1, 4 }, { 2, 5 }, { 2, 6 } };
        for (int i = 0; i < 6; i++) {
            edges[i] = lineWidth(stroke(Art.line(NODE[E[i][0]][0] * r, NODE[E[i][0]][1] * r, NODE[E[i][1]][0] * r, NODE[E[i][1]][1] * r),
                Art.shade(Art.HEAP_ARMOR, 0.6)), 2.0);
        }
        for (int i = 0; i < 7; i++) {
            double d = (i == 0 ? 0.46 : (i < 3 ? 0.36 : 0.3)) * r;
            nodes[i] = thin(part(Art.ovalAt(NODE[i][0] * r, NODE[i][1] * r, d, d), i == 0 ? Art.tint(Art.HEAP_NODE, 0.2) : Art.HEAP_NODE));
        }
        highlight(-0.08 * r, -0.58 * r, 0.18 * r, 0.12 * r);
        hop = lineWidth(fixedStroke(Art.ovalAt(0, -0.5 * r, 0.6 * r, 0.6 * r), Art.alpha(Art.tint(Art.HEAP_NODE, 0.6), 230)), 2.0);
        gloss(0, -0.3 * r, 2.0 * r, 1.6 * r);
        cracks = makeCracks(3, r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        int layers = layersLeft(e, 8);
        int show = Math.max(1, Math.min(7, layers - 1));
        if (show != shown) {
            shown = show;
            for (int i = 0; i < 7; i++) nodes[i].setVisible(i < show);
            for (int i = 0; i < 6; i++) edges[i].setVisible(i + 1 < show);
        }
        int[] path = { 0, 1 + ((int) (t / 1.5) % 2), 3 + ((int) (t / 1.5) % 4) };
        int k = (int) ((t * 2) % 3);
        int n = Math.min(path[k], show - 1);
        Art.center(hop, NODE[n][0] * r, NODE[n][1] * r);
        aura.setStartAngle(facingDeg + 90);
        showCracks(cracks, dmg);
    }
}
