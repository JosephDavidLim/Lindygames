package lindefense.ui.sprites;

import java.awt.Color;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyType;

/**
 * Death burst for an ordinary enemy: a brief flash disc, an expanding ring in
 * the enemy's signature colour and a spray of shards whose shape matches the
 * enemy family (bubbles pop into circles, arrays shatter into squares, sorting
 * algorithms into triangles, linked lists into beads and link slivers).
 */
@SuppressWarnings("serial")
public final class DeathEffect extends Effect {
    /** Builds the burst at the enemy's current position. */
    public DeathEffect(Enemy e) {
        super(e.getPosition().x, e.getPosition().y, 0.7);
        double r = Math.max(4, e.getRadius());
        EnemyType t = e.getType();
        Color c = Art.colorOf(t);
        long seed = e.getId();
        ring(0, 0, r * 0.4, r * 2.2, Art.alpha(c, 220), 0, 0.45, false);
        ring(0, 0, r * 0.2, r * 1.3, Art.alpha(Color.WHITE, 200), 0, 0.18, true);
        switch (t) {
            case BUBBLE_SORT:
                shards(7, 0, 0, r * 0.5, r * 5, r * 2, Art.alpha(c, 200), 2, 0, 0.5, seed);
                ring(0.5 * r, -0.4 * r, r * 0.2, r * 0.9, Art.alpha(Art.tint(c, 0.4), 230), 0.08, 0.3, false);
                ring(-0.6 * r, 0.5 * r, r * 0.1, r * 0.6, Art.alpha(Art.tint(c, 0.4), 230), 0.16, 0.3, false);
                break;
            case STATIC_ARRAY: case DYNAMIC_ARRAY: case COUNTING_SORT: case INSERTION_SORT: case QUICK_SORT: case QUICK_PARTITION:
                shards(8, 0, 0, r * 0.45, r * 7, r * 14, c, 1, 0, 0.55, seed);
                break;
            case SINGLY_LINKED_LIST: case DOUBLY_LINKED_LIST:
                shards(5, 0, 0, r * 0.5, r * 6, r * 12, c, 2, 0, 0.55, seed);
                shards(4, 0, 0, r * 0.4, r * 7, r * 10, Art.shade(c, 0.35), 3, 0, 0.5, seed + 99);
                break;
            case HEAP_SORT:
                shards(6, 0, 0, r * 0.5, r * 6, r * 14, Art.HEAP_ARMOR, 1, 0, 0.6, seed);
                shards(6, 0, 0, r * 0.35, r * 5, r * 8, c, 2, 0.05, 0.5, seed + 7);
                break;
            case RADIX_SORT:
                shards(8, 0, 0, r * 0.4, r * 7, r * 12, Art.RADIX, 1, 0, 0.6, seed);
                shards(4, 0, 0, r * 0.3, r * 4, r * 8, Art.STEEL, 1, 0.04, 0.5, seed + 3);
                break;
            case SHELL_SORT:
                shards(6, 0, 0, r * 0.5, r * 6, r * 14, Art.SHELL, 0, 0, 0.6, seed);
                shards(3, 0, 0, r * 0.35, r * 4, r * 10, Art.SHELL_DARK, 0, 0.03, 0.5, seed + 5);
                break;
            default:
                shards(8, 0, 0, r * 0.45, r * 7, r * 12, c, 0, 0, 0.55, seed);
                break;
        }
        if (e.isFortified()) shards(5, 0, 0, r * 0.35, r * 8, r * 16, Art.STEEL, 1, 0, 0.5, seed + 11);
        if (e.isRegrow()) shards(5, 0, 0, r * 0.3, r * 3, -r * 4, Art.REGROW, 2, 0.05, 0.6, seed + 13);
    }
}
