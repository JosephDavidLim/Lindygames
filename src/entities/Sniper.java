package entities;

import game.Board;

/**
 * Instantly hits the toughest enemy on screen, in any lane.
 */
public class Sniper extends Defender {
    private int timer;

    /**
     * Creates a sniper.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    public Sniper(DefenderType type, int row, int col) {
        super(type, row, col);
    }

    @Override
    public void update(Board board) {
        if (timer > 0) {
            timer--;
            return;
        }
        Enemy target = board.strongestOnScreen();
        if (target != null) {
            board.strike(target, power(), critChance());
            board.addEffect(new Effect(Effect.Kind.SNIPE, target.getRow(), (int) Math.floor(target.getX()), 0, 10));
            timer = actionTicks(board);
            actionDone();
        }
    }
}
