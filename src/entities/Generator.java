package entities;

import game.Board;

/**
 * Produces Focus Points on a timer.
 */
public class Generator extends Defender {
    private int timer;

    /**
     * Creates a generator.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    public Generator(DefenderType type, int row, int col) {
        super(type, row, col);
        this.timer = Board.seconds(type.getIntervalSec());
    }

    @Override
    public void onPlaced(Board board) {
        timer = actionTicks(board);
    }

    @Override
    public void update(Board board) {
        timer--;
        if (timer <= 0) {
            board.addFocusPoints((int) Math.round(softPower(type.getPower())));
            timer = actionTicks(board);
            actionDone();
        }
    }
}
