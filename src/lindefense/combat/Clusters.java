package lindefense.combat;

import java.util.Collection;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyGrid;
import lindefense.enemy.EnemyList;
import lindefense.sim.Vec;

/**
 * The single definition of "densest cluster" used by Grok's targeting mode, Copilot's drone, Brad's barrage
 * and any other "aim at the densest cluster" effect.
 *
 * <p><b>Definition:</b> an enemy's <i>density</i> is the number of OTHER enemies (alive, on the track, not
 * stored in a bucket) whose position is within {@link #RADIUS} px of it. The densest enemy is the one with the
 * highest density; ties go to the enemy furthest along the track (First, i.e. smallest remaining distance).
 * The cluster's aim point is that enemy's position.</p>
 *
 * <p><b>Cost:</b> when the population is the live enemy list ({@code game.getEnemyManager().getEnemies()}, an
 * {@link EnemyList}) the neighbours are counted from its {@link EnemyGrid} (only the cells within {@link #RADIUS}),
 * so {@link #densest} is about O(n) instead of O(n&sup2;). Any other collection is scanned in full. The counts are the
 * same either way.</p>
 */
public final class Clusters {
    /** Neighbour radius in px that defines a cluster. */
    public static final double RADIUS = 60;

    private Clusters() { }

    /** Number of enemies in {@code population} (other than {@code e}) within {@link #RADIUS} px of {@code e}. */
    public static int density(Enemy e, Collection<Enemy> population) {
        int n = 0;
        double r2 = RADIUS * RADIUS;
        Vec p = e.getPosition();
        if (population instanceof EnemyList && ((EnemyList) population).grid() != null) {
            EnemyGrid.Scan scan = ((EnemyList) population).grid().scan(p.x, p.y, RADIUS);
            try {
                for (Enemy o = scan.next(); o != null; o = scan.next()) {
                    if (o == e || !o.isAlive() || o.isStored()) continue;
                    if (o.getPosition().distSq(p) <= r2) n++;
                }
            } finally {
                scan.close();
            }
            return n;
        }
        for (Enemy o : population) {
            if (o == e || !o.isAlive() || o.isStored()) continue;
            if (o.getPosition().distSq(p) <= r2) n++;
        }
        return n;
    }

    /**
     * The densest enemy among {@code candidates}, with density counted against {@code population}
     * (which may be a superset, e.g. all enemies on the map). Null if there are no candidates.
     */
    public static Enemy densest(Collection<Enemy> candidates, Collection<Enemy> population) {
        Enemy best = null;
        int bestN = -1;
        for (Enemy c : candidates) {
            if (!c.isAlive() || c.isStored()) continue;
            int n = density(c, population);
            if (n > bestN || (n == bestN && c.getRemainingDistance() < best.getRemainingDistance())) { best = c; bestN = n; }
        }
        return best;
    }

    /** Aim point of the densest cluster among all given enemies, or null if there are none. */
    public static Vec densestPoint(Collection<Enemy> enemies) {
        Enemy e = densest(enemies, enemies);
        return e == null ? null : e.getPosition();
    }
}
