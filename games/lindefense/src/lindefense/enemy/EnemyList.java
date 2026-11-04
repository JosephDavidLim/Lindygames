package lindefense.enemy;

import java.util.ArrayList;

/**
 * The live enemy list owned by {@link EnemyManager} ({@link EnemyManager#getEnemies()}). It is an ordinary
 * {@link ArrayList} that additionally exposes its structural modification count, so the {@link EnemyGrid} built
 * over it can tell cheaply and reliably whether enemies were added or removed since it was last built (however the
 * change was made), and it carries a reference to that grid so shared helpers such as
 * {@link lindefense.combat.Clusters} can use the grid whenever they are handed this list.
 */
public final class EnemyList extends ArrayList<Enemy> {
    private static final long serialVersionUID = 1L;

    private transient EnemyGrid grid;

    EnemyList() { super(256); }

    /** Structural modification count (changes on every add / remove / clear / sort, never on reads). */
    int version() { return modCount; }

    void setGrid(EnemyGrid g) { grid = g; }

    /** The spatial grid over this list (rebuilt lazily; always consistent when queried). */
    public EnemyGrid grid() { return grid; }
}
