package entities;

import game.Board;

/**
 * Anything on the board that has health and acts every tick. Positions are in
 * cell units: x = 0.0 is the left edge of column 0, x = 9.0 is the right edge
 * of the board.
 */
public abstract class Entity {
    /** Horizontal position in cell units. */
    protected double x;
    /** Current health. The entity is dead at zero or below. */
    protected double health;
    /** Health the entity started with. */
    protected double maxHealth;

    /**
     * Creates an entity.
     *
     * @param x starting horizontal position in cell units
     * @param health starting (and maximum) health
     */
    protected Entity(double x, double health) {
        this.x = x;
        this.health = health;
        this.maxHealth = health;
    }

    /**
     * Advances this entity by one game tick.
     *
     * @param board the board this entity lives on
     */
    public abstract void update(Board board);

    /**
     * Subtracts health.
     *
     * @param amount damage to take
     */
    public void takeDamage(double amount) {
        health -= amount;
    }

    /**
     * Restores health, up to the maximum.
     *
     * @param amount health to restore
     */
    public void heal(double amount) {
        health = Math.min(maxHealth, health + amount);
    }

    /**
     * Tells whether this entity has run out of health.
     *
     * @return true if health is zero or below
     */
    public boolean isDead() {
        return health <= 0;
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
     * Gets the current health.
     *
     * @return current health
     */
    public double getHealth() {
        return health;
    }

    /**
     * Gets the starting health.
     *
     * @return maximum health
     */
    public double getMaxHealth() {
        return maxHealth;
    }
}
