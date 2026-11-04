package entities;

import game.Gear;

/**
 * Something the view should float over the board for a moment: a damage
 * number, a MISS, damage to the avatar, or a coin or item being picked up. It holds no game logic.
 */
public class Popup {
    /** What the popup shows. */
    public enum Kind { DAMAGE, CRIT, MISS, HURT, COIN, GEAR, SHOUT }

    private final Kind kind;
    private final int row;
    private final double x;
    private final int amount;
    private final Gear gear;
    private final String text;

    /**
     * Creates a popup.
     *
     * @param kind what it shows
     * @param row lane index
     * @param x position in cell units
     * @param amount the damage or Credits shown, or 0
     * @param gear the item dropped, or null
     */
    public Popup(Kind kind, int row, double x, int amount, Gear gear) {
        this.kind = kind;
        this.row = row;
        this.x = x;
        this.amount = amount;
        this.gear = gear;
        this.text = null;
    }

    /**
     * Creates a popup for something a boss shouts.
     *
     * @param row lane index
     * @param x position in cell units
     * @param text what it shouts
     */
    public Popup(int row, double x, String text) {
        this.kind = Kind.SHOUT;
        this.row = row;
        this.x = x;
        this.amount = 0;
        this.gear = null;
        this.text = text;
    }

    /**
     * Gets the shouted text.
     *
     * @return the text, or null
     */
    public String getText() {
        return text;
    }

    /**
     * Gets what the popup shows.
     *
     * @return the kind
     */
    public Kind getKind() {
        return kind;
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
     * Gets the number shown.
     *
     * @return damage or Credits
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Gets the item dropped.
     *
     * @return the item, or null
     */
    public Gear getGear() {
        return gear;
    }
}
