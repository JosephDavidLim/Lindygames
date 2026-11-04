package entities;

import game.Board;

import java.util.List;

/**
 * Waits for enemies to get close, then blasts every enemy within two cells ahead.
 */
public class PointBlank extends Defender {
    private static final double RANGE = 2.0;

    private int timer;

    /**
     * Creates a point-blank attacker.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    public PointBlank(DefenderType type, int row, int col) {
        super(type, row, col);
    }

    @Override
    public void update(Board board) {
        if (timer > 0) {
            timer--;
            return;
        }
        List<Enemy> near = board.enemiesAhead(row, x, RANGE);
        if (!near.isEmpty()) {
            for (Enemy e : near) {
                board.strike(e, power(), critChance());
            }
            board.addEffect(new Effect(Effect.Kind.SWIPE, row, col, 2, 8));
            timer = actionTicks(board);
            actionDone();
        }
    }
}
