package entities;

import game.Board;

/**
 * Heals every defender in the eight cells around it on a timer.
 */
public class Support extends Defender {
    private int timer;

    /**
     * Creates a support defender.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    public Support(DefenderType type, int row, int col) {
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
            board.healArea(row, col, 1, power());
            board.addEffect(new Effect(Effect.Kind.HEAL, row, col, 1, 20));
            timer = actionTicks(board);
            actionDone();
        }
    }
}
