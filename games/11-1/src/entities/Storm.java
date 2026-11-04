package entities;

import game.Board;

import java.util.List;

/**
 * Has two abilities on one timer: it generates Focus Points, and it calls
 * lightning on the three enemies closest to the left edge, damaging them and
 * stunning them for a moment.
 */
public class Storm extends Defender {
    /** Focus Points made each time. */
    public static final int PAYOUT = 50;
    private static final int TARGETS = 3;
    private static final int STUN_TICKS = Board.seconds(1);

    private int timer;

    /**
     * Creates a storm.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    public Storm(DefenderType type, int row, int col) {
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
        if (timer > 0) {
            return;
        }
        board.addFocusPoints((int) Math.round(softPower(PAYOUT)));
        List<Enemy> targets = board.frontmost(TARGETS);
        for (Enemy e : targets) {
            board.strike(e, power(), critChance());
            e.stun(STUN_TICKS);
            board.addEffect(new Effect(Effect.Kind.BOLT, e.getRow(), (int) Math.floor(e.getX()), 0, 12));
        }
        timer = actionTicks(board);
        actionDone();
    }
}
