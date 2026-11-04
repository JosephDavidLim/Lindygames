package entities;

/**
 * A short-lived visual effect (an explosion, a lane sweep, a heal pulse). The
 * game logic creates it; the view draws it and fades it out.
 */
public class Effect {
    /** What the effect looks like. */
    public enum Kind { BLAST, LANE, HEAL, STUN, SWIPE, SNIPE, BOLT, LEVEL_UP }

    private final Kind kind;
    private final int row;
    private final int col;
    private final int radius;
    private final int maxLife;
    private int life;

    /**
     * Creates an effect.
     *
     * @param kind the look of the effect
     * @param row lane of the effect's center
     * @param col column of the effect's center
     * @param radius how many cells out it reaches (0 is one cell)
     * @param lifeTicks how long it lasts
     */
    public Effect(Kind kind, int row, int col, int radius, int lifeTicks) {
        this.kind = kind;
        this.row = row;
        this.col = col;
        this.radius = radius;
        this.maxLife = lifeTicks;
        this.life = lifeTicks;
    }

    /**
     * Ages the effect by one tick.
     */
    public void update() {
        life--;
    }

    /**
     * Tells whether the effect has finished.
     *
     * @return true when its time is up
     */
    public boolean isDone() {
        return life <= 0;
    }

    /**
     * Gets how much of its life is left, for fading.
     *
     * @return 1.0 when new, 0.0 when done
     */
    public double getFraction() {
        return Math.max(0, (double) life / maxLife);
    }

    /**
     * Gets the effect kind.
     *
     * @return the kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Gets the center lane.
     *
     * @return row index
     */
    public int getRow() {
        return row;
    }

    /**
     * Gets the center column.
     *
     * @return column index
     */
    public int getCol() {
        return col;
    }

    /**
     * Gets the reach in cells.
     *
     * @return radius, 0 for a single cell
     */
    public int getRadius() {
        return radius;
    }
}
