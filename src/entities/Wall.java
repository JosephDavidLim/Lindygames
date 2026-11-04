package entities;

import game.Board;

/**
 * Blocks enemies with its health. Some walls have thorns that hurt biters.
 */
public class Wall extends Defender {

    /**
     * Creates a wall.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    public Wall(DefenderType type, int row, int col) {
        super(type, row, col);
    }

    @Override
    public void update(Board board) {
    }

    @Override
    public void onBitten(Enemy attacker) {
        if (type.getPower() > 0) {
            attacker.takeDamage(power() / Board.TICKS_PER_SECOND);
        }
    }
}
