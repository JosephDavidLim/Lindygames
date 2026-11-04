package entities;

import game.Board;

import java.util.List;

/**
 * Explodes for heavy damage under the first enemy to step on it.
 */
public class Mine extends Trigger {
    /**
     * Creates a mine.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    public Mine(DefenderType type, int row, int col) {
        super(type, row, col, type.getIntervalSec());
    }

    @Override
    protected void fire(Board board, List<Enemy> hit) {
        board.damageArea(row, col, 0, power(), critChance());
        board.addEffect(new Effect(Effect.Kind.BLAST, row, col, 0, 14));
    }
}
