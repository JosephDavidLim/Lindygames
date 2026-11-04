package entities;

import game.Board;

/**
 * The first time it drops below half health, it stops to revise itself and
 * heals. Interrupt it with a big hit before it recovers.
 */
public class Reviser extends Enemy {
    private static final int REVISE_TICKS = Board.seconds(3);
    private static final double REVISE_HEAL_FRACTION = 0.375;

    private boolean revised;

    /**
     * Creates a reviser.
     *
     * @param type the enemy type
     * @param row lane index
     * @param x starting position in cell units
     */
    public Reviser(EnemyType type, int row, double x) {
        super(type, row, x);
    }

    @Override
    protected void act(Board board) {
        if (!revised && health < maxHealth / 2) {
            revised = true;
            heal(maxHealth * REVISE_HEAL_FRACTION);
            stun(REVISE_TICKS);
            board.addEffect(new Effect(Effect.Kind.SNIPE, row, (int) Math.floor(x), 0, 20));
            return;
        }
        walkOrBite(board);
    }
}
