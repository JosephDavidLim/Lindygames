package entities;

import game.Board;
import game.Gear;

/**
 * Credits or an item lying on the floor where an enemy was defeated. The
 * avatar picks it up by walking over it. Coins vanish after a while, items
 * stay, and whatever is left is collected when the level ends.
 */
public class Drop {
    private static final int COIN_LIFE = Board.seconds(12);

    private final int row;
    private final double x;
    private final int credits;
    private final Gear gear;
    private int age;

    /**
     * Creates a drop.
     *
     * @param row lane index
     * @param x position in cell units
     * @param credits Credits inside, 0 for an item
     * @param gear the item, or null for coins
     */
    public Drop(int row, double x, int credits, Gear gear) {
        this.row = row;
        this.x = Math.max(0.2, Math.min(Board.COLS - 0.2, x));
        this.credits = credits;
        this.gear = gear;
    }

    /**
     * Ages the drop by one tick.
     */
    public void update() {
        age++;
    }

    /**
     * Tells whether coins have vanished.
     *
     * @return true once the drop should disappear
     */
    public boolean isExpired() {
        return gear == null && age >= COIN_LIFE;
    }

    /**
     * Gets how long the drop has been on the floor.
     *
     * @return ticks
     */
    public int getAge() {
        return age;
    }

    /**
     * Gets the lane.
     *
     * @return lane index
     */
    public int getRow() {
        return row;
    }

    /**
     * Gets the position.
     *
     * @return position in cell units
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the Credits inside.
     *
     * @return Credits
     */
    public int getCredits() {
        return credits;
    }

    /**
     * Gets the item.
     *
     * @return the item, or null for coins
     */
    public Gear getGear() {
        return gear;
    }
}
