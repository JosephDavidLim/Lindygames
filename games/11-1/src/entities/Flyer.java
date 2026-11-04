package entities;

import game.Board;

/**
 * Hops over the first defender it meets, then walks and bites normally.
 */
public class Flyer extends Enemy {
    private boolean hasJumped;

    /**
     * Creates a flyer.
     *
     * @param type the enemy type
     * @param row lane index
     * @param x starting position in cell units
     */
    public Flyer(EnemyType type, int row, double x) {
        super(type, row, x);
    }

    @Override
    protected void act(Board board) {
        if (!hasJumped && blockingDefender(board) != null) {
            hasJumped = true;
            x -= 1.2;
            return;
        }
        walkOrBite(board);
    }
}
