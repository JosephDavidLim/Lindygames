package entities;

import game.Board;

import java.util.ArrayList;

/**
 * A shot travelling right along one lane.
 */
public class Projectile {
    /** Cells travelled per tick. */
    public static final double SPEED = 0.15;
    private static final double HIT_RANGE = 0.35;
    private static final double RICOCHET_RANGE = 2.0;
    private static final double SPLASH_RANGE = 0.7;

    private final int row;
    private double x;
    private final double damage;
    private final boolean piercing;
    private final int slowTicks;
    private final DefenderType source;
    private double critChance;
    private boolean splash;
    private boolean spent;
    private final ArrayList<Enemy> alreadyHit = new ArrayList<>();

    /**
     * Creates a projectile.
     *
     * @param row lane index
     * @param x starting position in cell units
     * @param damage damage dealt on hit
     * @param piercing true if it keeps going after a hit
     * @param slowTicks ticks of slow applied to what it hits, 0 for none
     */
    public Projectile(int row, double x, double damage, boolean piercing, int slowTicks) {
        this(row, x, damage, piercing, slowTicks, null);
    }

    /**
     * Creates a projectile that remembers who threw it, so the view can draw
     * the right kind of shot.
     *
     * @param row lane index
     * @param x starting position in cell units
     * @param damage damage dealt on hit
     * @param piercing true if it keeps going after a hit
     * @param slowTicks ticks of slow applied to what it hits, 0 for none
     * @param source the defender that threw it, or null
     */
    public Projectile(int row, double x, double damage, boolean piercing, int slowTicks,
                      DefenderType source) {
        this.source = source;
        this.row = row;
        this.x = x;
        this.damage = damage;
        this.piercing = piercing;
        this.slowTicks = slowTicks;
    }

    /**
     * Moves the shot and hits the first enemy it reaches.
     *
     * @param board the board
     */
    public void update(Board board) {
        x += SPEED;
        if (x > Board.COLS + 1) {
            spent = true;
            return;
        }
        for (Enemy e : board.getEnemies()) {
            if (e.getRow() != row || e.isDead() || alreadyHit.contains(e)) {
                continue;
            }
            if (Math.abs(e.getX() - x) <= HIT_RANGE) {
                board.strike(e, damage, critChance);
                if (splash) {
                    for (Enemy near : board.enemiesAhead(row, e.getX() - SPLASH_RANGE, SPLASH_RANGE * 2)) {
                        if (near != e) {
                            board.strike(near, damage / 2, critChance);
                        }
                    }
                }
                if (slowTicks > 0) {
                    e.applySlow(slowTicks);
                }
                alreadyHit.add(e);
                if (source != null && source.getKind() == DefenderType.Kind.RICOCHET) {
                    Enemy bounce = board.nearestInAdjacentLanes(row, x, RICOCHET_RANGE);
                    if (bounce != null) {
                        board.strike(bounce, damage, critChance);
                        board.addEffect(new Effect(Effect.Kind.SNIPE, bounce.getRow(),
                                (int) Math.floor(bounce.getX()), 0, 8));
                    }
                }
                if (!piercing) {
                    spent = true;
                    return;
                }
            }
        }
    }

    /**
     * Sets whether this shot also hits nearby enemies for half damage.
     *
     * @param on true for a splash
     * @return this shot
     */
    public Projectile withSplash(boolean on) {
        splash = on;
        return this;
    }

    /**
     * Sets the chance that this shot does double damage.
     *
     * @param chance chance from 0 to 1
     * @return this shot
     */
    public Projectile withCrit(double chance) {
        critChance = chance;
        return this;
    }

    /**
     * Gets the defender type that threw this shot.
     *
     * @return the source, or null
     */
    public DefenderType getSource() {
        return source;
    }

    /**
     * Tells whether this shot should be removed.
     *
     * @return true once it hit something or left the board
     */
    public boolean isSpent() {
        return spent;
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
     * Gets the position.
     *
     * @return x in cell units
     */
    public double getX() {
        return x;
    }

    /**
     * Tells whether this shot passes through enemies.
     *
     * @return true if piercing
     */
    public boolean isPiercing() {
        return piercing;
    }

    /**
     * Tells whether this shot slows what it hits.
     *
     * @return true if it applies a slow
     */
    public boolean isSlowing() {
        return slowTicks > 0;
    }
}
