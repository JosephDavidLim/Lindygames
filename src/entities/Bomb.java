package entities;

import game.Board;

/**
 * Explodes after a short fuse, damaging every enemy in the 3x3 area around
 * it, then removes itself.
 */
public class Bomb extends Defender {
    private int fuse;

    /**
     * Creates a bomb.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    public Bomb(DefenderType type, int row, int col) {
        super(type, row, col);
        this.fuse = Board.seconds(type.getIntervalSec());
    }

    @Override
    public void onPlaced(Board board) {
        fuse = actionTicks(board);
    }

    @Override
    public void update(Board board) {
        fuse--;
        if (fuse <= 0) {
            board.damageArea(row, col, 1, power(), critChance());
            board.addEffect(new Effect(Effect.Kind.BLAST, row, col, 1, 16));
            health = 0;
        }
    }

    @Override
    public boolean isBlocking() {
        return false;
    }

    @Override
    public void takeDamage(double amount) {
        // A lit bomb cannot be destroyed; it always goes off.
    }

    /**
     * Gets the ticks left before the blast, for drawing a fuse effect.
     *
     * @return ticks remaining
     */
    public int getFuse() {
        return fuse;
    }
}
