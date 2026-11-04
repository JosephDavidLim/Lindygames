package game;

import entities.DefenderType;

/**
 * A shop card for one defender type, with its own recharge timer.
 */
public class Card {
    private final DefenderType type;
    private final double rechargeSpeed;
    private int rechargeLeft;

    /**
     * Creates a card that is ready to use.
     *
     * @param type the defender this card places
     */
    public Card(DefenderType type) {
        this(type, 1.0);
    }

    /**
     * Creates a card whose recharge is sped up by the classmate's INT.
     *
     * @param type the defender this card places
     * @param rechargeSpeed recharge multiplier, 1.0 for normal
     */
    public Card(DefenderType type, double rechargeSpeed) {
        this.type = type;
        this.rechargeSpeed = rechargeSpeed;
    }

    /**
     * Counts the recharge down by one tick.
     */
    public void update() {
        if (rechargeLeft > 0) {
            rechargeLeft--;
        }
    }

    /**
     * Starts the recharge after the defender is placed.
     */
    public void startRecharge() {
        rechargeLeft = totalRecharge();
    }

    /**
     * Tells whether the card has recharged.
     *
     * @return true if it can be used
     */
    public boolean isReady() {
        return rechargeLeft == 0;
    }

    /**
     * Gets how much of the recharge is left, for shading the card.
     *
     * @return 1.0 just after use, 0.0 when ready
     */
    public double getRechargeFraction() {
        int total = totalRecharge();
        return total == 0 ? 0 : (double) rechargeLeft / total;
    }

    /**
     * Gets the defender type.
     *
     * @return the type
     */
    public DefenderType getType() {
        return type;
    }

    private int totalRecharge() {
        return (int) Math.round(Board.seconds(type.getRechargeSec()) / rechargeSpeed);
    }
}
