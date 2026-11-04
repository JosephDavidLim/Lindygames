package entities;

import game.Board;

/**
 * Hits every enemy in its lane once, then leaves.
 */
public class LaneClear extends Defender {
    /**
     * Creates a lane clear defender.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    public LaneClear(DefenderType type, int row, int col) {
        super(type, row, col);
    }

    @Override
    public void update(Board board) {
        board.damageRow(row, power(), critChance());
        board.addEffect(new Effect(Effect.Kind.LANE, row, col, 0, 18));
        health = 0;
    }
}
