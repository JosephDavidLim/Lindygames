package entities;

/**
 * An enemy that rubs out status effects: it ignores slows. Stuns still work on it.
 */
public class ImmuneWalker extends Enemy {
    /**
     * Creates an immune walker.
     *
     * @param type the enemy type
     * @param row lane index
     * @param x starting position in cell units
     */
    public ImmuneWalker(EnemyType type, int row, double x) {
        super(type, row, x);
    }

    @Override
    public void applySlow(int ticks) {
    }
}
