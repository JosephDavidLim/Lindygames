package entities;

/**
 * A plain enemy that walks and bites. Homework Sheet, Sharp Pencil, Eraser,
 * Personal Essay, Rejection Letter and Scantron Swarm differ only in stats.
 */
public class Walker extends Enemy {
    /**
     * Creates a walker.
     *
     * @param type the enemy type
     * @param row lane index
     * @param x starting position in cell units
     */
    public Walker(EnemyType type, int row, double x) {
        super(type, row, x);
    }
}
