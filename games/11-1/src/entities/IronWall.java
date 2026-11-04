package entities;

import game.Board;

/**
 * A tank that repairs itself whenever nothing has bitten it for a moment.
 */
public class IronWall extends Defender {
    private static final int CALM_TICKS = Board.seconds(2);

    private int sinceBitten = CALM_TICKS;

    /**
     * Creates an iron wall.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    public IronWall(DefenderType type, int row, int col) {
        super(type, row, col);
    }

    @Override
    public void update(Board board) {
        if (sinceBitten < CALM_TICKS) {
            sinceBitten++;
        } else {
            heal(power() / Board.TICKS_PER_SECOND);
        }
    }

    @Override
    public void onBitten(Enemy attacker) {
        sinceBitten = 0;
    }
}
