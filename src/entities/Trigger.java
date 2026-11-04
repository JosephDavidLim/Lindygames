package entities;

import game.Board;

import java.util.List;

/**
 * A defender that waits, arms after a delay, and then goes off the first time
 * an enemy steps into its cell. It is used up when it fires. Until it is armed,
 * enemies treat it like any other defender and bite it.
 */
public abstract class Trigger extends Defender {
    private int armTicks;

    /**
     * Creates a trigger defender.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     * @param armSeconds time before it can go off
     */
    protected Trigger(DefenderType type, int row, int col, double armSeconds) {
        super(type, row, col);
        this.armTicks = Board.seconds(armSeconds);
    }

    @Override
    public void onPlaced(Board board) {
        armTicks = actionTicks(board);
    }

    @Override
    public void update(Board board) {
        if (armTicks > 0) {
            armTicks--;
            return;
        }
        List<Enemy> hit = board.enemiesInCell(row, col);
        if (!hit.isEmpty()) {
            fire(board, hit);
            health = 0;
        }
    }

    /**
     * Tells whether this defender is ready to go off, for drawing an indicator.
     *
     * @return true once armed
     */
    public boolean isArmed() {
        return armTicks <= 0;
    }

    /**
     * Does the trigger's effect.
     *
     * @param board the board
     * @param hit the enemies standing in this cell, never empty
     */
    protected abstract void fire(Board board, List<Enemy> hit);
}
