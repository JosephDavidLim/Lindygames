package lindefense.ui.sprites;

import acm.graphics.GPolygon;
import java.awt.Color;
import java.util.List;
import lindefense.enemy.Enemy;

/**
 * Split animation for an enemy whose outer body has just been destroyed and
 * is releasing children. Identity-specific:
 * <ul>
 *   <li>Merge Sort: the hexagon visibly halves along its seam.</li>
 *   <li>Quick Sort: the partition line flashes and the two bar groups fly off
 *       to either side while the pivot pops upward.</li>
 *   <li>TimSort: its purple merge half and orange insertion half separate.</li>
 *   <li>Counting Sort: the four counters fly out diagonally through the ring.</li>
 *   <li>Static / Dynamic Array: the container walls fall away.</li>
 *   <li>Bucket Sort: the bucket tips and its stored contents spill out.</li>
 *   <li>Anything else: a ring and two puffs.</li>
 * </ul>
 */
@SuppressWarnings("serial")
public final class SplitEffect extends Effect {
    /** Builds the split animation at the parent's current position. */
    public SplitEffect(Enemy parent) {
        super(parent.getPosition().x, parent.getPosition().y, 0.8);
        double r = Math.max(4, parent.getRadius());
        long seed = parent.getId();
        switch (parent.getType()) {
            case MERGE_SORT: {
                double R = r * 0.95;
                GPolygon l = Art.poly(0, -R * 0.87, -R * 0.5, -R * 0.87, -R, 0, -R * 0.5, R * 0.87, 0, R * 0.87);
                GPolygon rt = Art.poly(0, -R * 0.87, R * 0.5, -R * 0.87, R, 0, R * 0.5, R * 0.87, 0, R * 0.87);
                ((Thick.Poly) l).setLineWidth(Thick.NORMAL);
                ((Thick.Poly) rt).setLineWidth(Thick.NORMAL);
                piece(l, 0, 0, -r * 2.2, -r * 0.6, r * 3, Art.MERGE, 0, 0.7, -90, true);
                piece(rt, 0, 0, r * 2.2, -r * 0.6, r * 3, Art.shade(Art.MERGE, 0.15), 0, 0.7, 90, true);
                ring(0, 0, r * 0.1, r * 1.6, Art.alpha(Art.tint(Art.MERGE, 0.5), 230), 0, 0.35, false);
                shards(6, 0, 0, r * 0.25, r * 3, r * 6, Art.tint(Art.MERGE, 0.5), 1, 0, 0.5, seed);
                break;
            }
            case QUICK_SORT: {
                GPolygon line = Art.poly(-r * 0.08, -r * 1.4, r * 0.08, -r * 1.4, r * 0.08, r * 1.4, -r * 0.08, r * 1.4);
                piece(line, 0, 0, 0, 0, 0, Art.tint(Art.QUICK_PIVOT, 0.5), 0, 0.3, 0, true);
                for (int i = 0; i < 3; i++) {
                    double h = r * (0.4 + 0.2 * i);
                    GPolygon b = Art.poly(-r * 0.12, -h / 2, r * 0.12, -h / 2, r * 0.12, h / 2, -r * 0.12, h / 2);
                    piece(b, -r * 0.3 - i * r * 0.3, r * 0.2, -r * (2.5 + i * 0.6), -r * (1 + i * 0.4), r * 6, Art.tint(Art.QUICK, 0.25), 0.03 * i, 0.6, -120, true);
                    GPolygon b2 = Art.poly(-r * 0.12, -h / 2, r * 0.12, -h / 2, r * 0.12, h / 2, -r * 0.12, h / 2);
                    piece(b2, r * 0.3 + i * r * 0.3, 0, r * (2.5 + i * 0.6), -r * (1 + i * 0.4), r * 6, Art.QUICK, 0.03 * i, 0.6, 120, true);
                }
                GPolygon pivot = Art.poly(-r * 0.14, -r * 0.5, r * 0.14, -r * 0.5, r * 0.14, r * 0.5, -r * 0.14, r * 0.5);
                piece(pivot, 0, -r * 0.2, 0, -r * 3.5, r * 9, Art.QUICK_PIVOT, 0, 0.7, 200, true);
                ring(0, 0, r * 0.2, r * 1.8, Art.alpha(Art.QUICK_PIVOT, 220), 0, 0.35, false);
                break;
            }
            case TIMSORT: {
                double R = r * 0.95;
                GPolygon l = Art.poly(0, -R * 0.87, -R * 0.5, -R * 0.87, -R, 0, -R * 0.5, R * 0.87, 0, R * 0.87);
                GPolygon rt = Art.poly(0, -R * 0.87, R * 0.5, -R * 0.87, R, 0, R * 0.5, R * 0.87, 0, R * 0.87);
                ((Thick.Poly) l).setLineWidth(Thick.NORMAL);
                ((Thick.Poly) rt).setLineWidth(Thick.NORMAL);
                piece(l, 0, 0, -r * 2.4, -r * 0.8, r * 3, Art.TIMSORT, 0, 0.7, -60, true);
                piece(rt, 0, 0, r * 2.4, -r * 0.8, r * 3, Art.INSERTION, 0, 0.7, 60, true);
                ring(0, 0, r * 0.1, r * 1.8, Art.alpha(Color.WHITE, 220), 0, 0.3, false);
                shards(4, 0, 0, r * 0.25, r * 3, r * 6, Art.tint(Art.TIMSORT, 0.5), 1, 0, 0.5, seed);
                shards(4, 0, 0, r * 0.25, r * 3, r * 6, Art.tint(Art.INSERTION, 0.5), 1, 0.02, 0.5, seed + 1);
                break;
            }
            case COUNTING_SORT: {
                double s = r * 0.6;
                for (int i = 0; i < 4; i++) {
                    double sx = i % 2 == 0 ? -1 : 1, sy = i < 2 ? -1 : 1;
                    GPolygon box = Art.poly(-s / 2, -s / 2, s / 2, -s / 2, s / 2, s / 2, -s / 2, s / 2);
                    ((Thick.Poly) box).setLineWidth(Thick.DETAIL);
                    piece(box, sx * r * 0.4, sy * r * 0.4, sx * r * 3, sy * r * 3 - r, r * 6, new Color(250, 250, 240), 0, 0.65, sx * 150, false);
                }
                ring(0, 0, r * 0.9, r * 2.0, Art.alpha(Art.COUNTING, 230), 0, 0.4, false);
                shards(8, 0, 0, r * 0.3, r * 5, r * 10, Art.COUNTING, 0, 0, 0.5, seed);
                break;
            }
            case STATIC_ARRAY: case DYNAMIC_ARRAY: {
                Color c = Art.colorOf(parent.getType());
                double w = 2.2 * r, h = 1.25 * r;
                GPolygon topW = Art.poly(-w / 2, -h / 2, w / 2, -h / 2, w / 2, -h / 2 + r * 0.18, -w / 2, -h / 2 + r * 0.18);
                GPolygon botW = Art.poly(-w / 2, h / 2 - r * 0.18, w / 2, h / 2 - r * 0.18, w / 2, h / 2, -w / 2, h / 2);
                GPolygon lW = Art.poly(-w / 2, -h / 2, -w / 2 + w * 0.1, -h / 2, -w / 2 + w * 0.1, h / 2, -w / 2, h / 2);
                GPolygon rW = Art.poly(w / 2 - w * 0.1, -h / 2, w / 2, -h / 2, w / 2, h / 2, w / 2 - w * 0.1, h / 2);
                piece(topW, 0, 0, 0, -r * 3, r * 6, Art.tint(c, 0.15), 0, 0.6, -40, true);
                piece(botW, 0, 0, 0, r * 1.5, r * 6, Art.tint(c, 0.15), 0, 0.6, 40, true);
                piece(lW, 0, 0, -r * 3, -r, r * 6, Art.shade(c, 0.35), 0, 0.6, -200, true);
                piece(rW, 0, 0, r * 3, -r, r * 6, Art.shade(c, 0.35), 0, 0.6, 200, true);
                ring(0, 0, r * 0.3, r * 1.6, Art.alpha(c, 200), 0, 0.35, false);
                break;
            }
            case BUCKET_SORT: {
                GPolygon b = Art.poly(-r, -0.75 * r, r, -0.75 * r, 0.72 * r, 0.95 * r, -0.72 * r, 0.95 * r);
                ((Thick.Poly) b).setLineWidth(Thick.NORMAL);
                piece(b, 0, 0, -r * 0.8, -r * 0.5, r * 5, Art.BUCKET, 0, 0.75, 160, false);
                List<Enemy> stored = parent.getStoredEnemies();
                int n = Math.max(3, Math.min(6, stored.size()));
                for (int i = 0; i < n; i++) {
                    Color c = i < stored.size() ? Art.colorOf(stored.get(i).getType()) : new Color(80, 140, 230);
                    Thick.Oval d = Art.ovalAt(0, 0, r * 0.3, r * 0.3);
                    d.setLineWidth(Thick.DETAIL);
                    piece(d, r * 0.3, -r * 0.3, r * (0.8 + 0.5 * i), -r * (1.5 + 0.3 * (i % 3)), r * 12, c, 0.04 * i, 0.7, 0, false);
                }
                shards(6, 0, 0, r * 0.25, r * 3, r * 10, new Color(80, 140, 230, 200), 2, 0.05, 0.5, seed);
                break;
            }
            default:
                ring(0, 0, r * 0.3, r * 1.8, Art.alpha(Art.colorOf(parent.getType()), 200), 0, 0.4, false);
                shards(2, 0, 0, r * 0.8, r * 2, 0, Art.alpha(Color.WHITE, 160), 2, 0, 0.5, seed);
                break;
        }
    }
}
