package entities;

import game.Board;

/**
 * A school enemy walking right to left along one lane. It stops to bite the
 * first defender in its way. Subclasses add special abilities.
 */
public abstract class Enemy extends Entity {
    /** Which enemy this is. */
    protected final EnemyType type;
    /** Lane the enemy walks in. */
    protected final int row;
    /** Ticks of slow left. A slowed enemy moves at half speed. */
    protected int slowTicks;
    /** Ticks of stun left. A stunned enemy does nothing. */
    protected int stunTicks;
    /** True during a tick in which the enemy bit a defender. */
    protected boolean biting;
    private static final double HEALTH_PER_LEVEL = 0.34;
    private static final double BITE_PER_LEVEL = 0.14;
    private static final double SPEED_PER_LEVEL = 0.03;

    private double biteScale = 1;
    private int hasteTicks;
    private double speedScale = 1;
    private boolean noReward;
    private int enemyLevel = 1;

    /**
     * Creates an enemy.
     *
     * @param type the enemy type
     * @param row lane index
     * @param x starting position in cell units
     */
    protected Enemy(EnemyType type, int row, double x) {
        super(x, type.getHealth());
        this.type = type;
        this.row = row;
    }

    @Override
    public void update(Board board) {
        if (slowTicks > 0) {
            slowTicks--;
        }
        if (hasteTicks > 0) {
            hasteTicks--;
        }
        biting = false;
        if (stunTicks > 0) {
            stunTicks--;
            return;
        }
        act(board);
    }

    /**
     * Runs this enemy's behavior for one tick when it is not stunned.
     * The default is to walk and bite.
     *
     * @param board the board
     */
    protected void act(Board board) {
        walkOrBite(board);
    }

    /**
     * Sets this enemy's level, which scales its health, bite and speed.
     *
     * @param level the level
     */
    public void applyLevel(int level) {
        enemyLevel = level;
        double scale = 1 + HEALTH_PER_LEVEL * (level - 1);
        maxHealth = type.getHealth() * scale;
        health = maxHealth;
        biteScale = 1 + BITE_PER_LEVEL * (level - 1);
        speedScale = 1 + SPEED_PER_LEVEL * (level - 1);
    }

    /**
     * Makes this enemy move faster for a while.
     *
     * @param ticks how long
     */
    public void haste(int ticks) {
        hasteTicks = Math.max(hasteTicks, ticks);
    }

    /**
     * Gets the multiplier on this enemy's bite from its level.
     *
     * @return the multiplier
     */
    protected double biteMultiplier() {
        return biteScale;
    }

    /**
     * Gets the damage this enemy does to the avatar by touching it.
     *
     * @return one second's worth of its bite
     */
    public double getTouchDamage() {
        return type.getBitePerTick() * Board.TICKS_PER_SECOND * biteScale;
    }

    /**
     * Gets this enemy's level.
     *
     * @return the level
     */
    public int getEnemyLevel() {
        return enemyLevel;
    }

    /**
     * Defeats this enemy with no EXP or Credits, for the Highlighter sweep.
     */
    public void removeWithoutReward() {
        noReward = true;
        health = 0;
    }

    /**
     * Tells whether this defeat should pay out.
     *
     * @return true if it earns EXP and Credits
     */
    public boolean givesReward() {
        return !noReward;
    }

    /**
     * Gets the speed this tick after slow effects.
     *
     * @return cells per tick
     */
    protected double currentSpeed() {
        double speed = type.getSpeedPerTick() * speedScale;
        if (hasteTicks > 0) {
            speed *= 1.6;
        }
        return slowTicks > 0 ? speed * 0.5 : speed;
    }

    /**
     * Finds the defender this enemy is currently blocked by.
     *
     * @param board the board
     * @return the defender in this enemy's cell, or null if the way is clear
     */
    protected Defender blockingDefender(Board board) {
        int col = (int) Math.floor(x);
        if (col < 0 || col >= Board.COLS || x > col + 0.9) {
            return null;
        }
        Defender d = board.defenderAt(row, col);
        return d != null && d.isBlocking() ? d : null;
    }

    /**
     * Bites a blocking defender, or walks if nothing is in the way.
     *
     * @param board the board
     */
    protected void walkOrBite(Board board) {
        Defender d = blockingDefender(board);
        if (d != null) {
            biting = true;
            d.receiveBite(board, type.getBitePerTick() * biteScale);
            d.onBitten(this);
        } else {
            x -= currentSpeed() * board.terrainAt(row, (int) Math.floor(x)).getEnemySpeed();
        }
    }

    /**
     * Slows this enemy. A longer slow replaces a shorter one.
     *
     * @param ticks length of the slow in ticks
     */
    public void applySlow(int ticks) {
        slowTicks = Math.max(slowTicks, ticks);
    }

    /**
     * Stuns this enemy.
     *
     * @param ticks length of the stun in ticks
     */
    public void stun(int ticks) {
        stunTicks = Math.max(stunTicks, ticks);
    }

    /**
     * Gets the enemy type.
     *
     * @return the type
     */
    public EnemyType getType() {
        return type;
    }

    /**
     * Gets the lane.
     *
     * @return row index
     */
    public int getRow() {
        return row;
    }

    /**
     * Tells whether the enemy is slowed.
     *
     * @return true while slowed
     */
    public boolean isSlowed() {
        return slowTicks > 0;
    }

    /**
     * Tells whether the enemy is frozen in place by a trap.
     *
     * @return true while stunned
     */
    public boolean isStunned() {
        return stunTicks > 0;
    }

    /**
     * Tells whether the enemy bit a defender this tick, for drawing a lunge.
     *
     * @return true if it is chewing on something
     */
    public boolean isBiting() {
        return biting;
    }
}
