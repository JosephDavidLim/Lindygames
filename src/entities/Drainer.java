package entities;

import game.Board;

/**
 * Steals Focus Points on a timer for as long as it is alive.
 */
public class Drainer extends Enemy {
    /** Focus Points stolen each time. */
    public static final int STEAL = 25;

    private final int intervalTicks;
    private int timer;

    /**
     * Creates a drainer.
     *
     * @param type the enemy type
     * @param row lane index
     * @param x starting position in cell units
     */
    public Drainer(EnemyType type, int row, double x) {
        super(type, row, x);
        this.intervalTicks = Board.seconds(type.getSpecialSec());
        this.timer = intervalTicks;
    }

    @Override
    protected void act(Board board) {
        timer--;
        if (timer <= 0) {
            board.addFocusPoints(-STEAL);
            timer = intervalTicks;
        }
        walkOrBite(board);
    }
}
