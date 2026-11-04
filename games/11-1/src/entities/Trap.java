package entities;

import game.Board;

import java.util.List;

/**
 * Freezes the first enemy that steps on it in place for a few seconds.
 */
public class Trap extends Trigger {
    /**
     * Creates a trap.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    public Trap(DefenderType type, int row, int col) {
        super(type, row, col, type.getIntervalSec());
    }

    @Override
    protected void fire(Board board, List<Enemy> hit) {
        hit.get(0).stun(Board.seconds(softPower(type.getPower())));
        board.addEffect(new Effect(Effect.Kind.STUN, row, col, 0, 30));
    }
}
