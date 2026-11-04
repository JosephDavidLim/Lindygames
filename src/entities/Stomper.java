package entities;

import game.Board;

import java.util.List;

/**
 * A sturdy melee fighter that stomps everything in its own cell and the next.
 */
public class Stomper extends Defender {
    private static final double RANGE = 2.0;

    private int timer;

    /**
     * Creates a stomper.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    public Stomper(DefenderType type, int row, int col) {
        super(type, row, col);
    }

    @Override
    public void update(Board board) {
        if (timer > 0) {
            timer--;
            return;
        }
        List<Enemy> near = board.enemiesAhead(row, col, RANGE);
        if (!near.isEmpty()) {
            for (Enemy e : near) {
                board.strike(e, power(), critChance());
            }
            board.addEffect(new Effect(Effect.Kind.SWIPE, row, col, 1, 8));
            timer = actionTicks(board);
            actionDone();
        }
    }
}
