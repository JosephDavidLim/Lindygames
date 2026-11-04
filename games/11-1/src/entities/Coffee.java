package entities;

import game.Board;

/**
 * A collectible coffee that appears on the lawn and is worth Focus Points.
 */
public class Coffee {
    /** Focus Points a coffee is worth. */
    public static final int VALUE = 25;
    /** How long a coffee stays before it disappears. */
    public static final int LIFETIME_TICKS = Board.seconds(8);

    private final double x;
    private final double y;
    private int life = LIFETIME_TICKS;

    /**
     * Creates a coffee.
     *
     * @param x horizontal position in cell units
     * @param y vertical position in cell units, 0 is the top of the lawn
     */
    public Coffee(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Ages the coffee by one tick.
     */
    public void update() {
        life--;
    }

    /**
     * Tells whether the coffee has timed out.
     *
     * @return true when its life is used up
     */
    public boolean isExpired() {
        return life <= 0;
    }

    /**
     * Gets the horizontal position.
     *
     * @return x in cell units
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the vertical position.
     *
     * @return y in cell units
     */
    public double getY() {
        return y;
    }
}
