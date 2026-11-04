package game;

/**
 * What one tile of a map is made of. A level's layout is a two-dimensional
 * array of these, and each kind changes how the tile plays.
 */
public enum Terrain {
    /** Plain floor. */
    FLOOR('.', true, 1.0, 1.0),
    /** A sealed locker: nobody can be placed here, but enemies squeeze past. */
    LOCKER('L', false, 1.0, 1.0),
    /** Spilled coolant: enemies slide across it faster. */
    MILK('M', true, 1.5, 1.0),
    /** A damping field: enemies drag through it slower. */
    CARPET('C', true, 0.65, 1.0),
    /** An overclock pad: a classmate standing here acts faster. */
    HYPE('H', true, 1.0, 1.25);

    private final char symbol;
    private final boolean buildable;
    private final double enemySpeed;
    private final double defenderSpeed;

    Terrain(char symbol, boolean buildable, double enemySpeed, double defenderSpeed) {
        this.symbol = symbol;
        this.buildable = buildable;
        this.enemySpeed = enemySpeed;
        this.defenderSpeed = defenderSpeed;
    }

    /**
     * Finds the terrain written with a character in a layout.
     *
     * @param c the layout character
     * @return the matching terrain
     */
    public static Terrain fromSymbol(char c) {
        for (Terrain t : values()) {
            if (t.symbol == c) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown terrain: " + c);
    }

    /**
     * Tells whether a classmate can stand here.
     *
     * @return true if the tile can be built on
     */
    public boolean isBuildable() {
        return buildable;
    }

    /**
     * Gets the multiplier on enemy walking speed.
     *
     * @return 1.0 for no change
     */
    public double getEnemySpeed() {
        return enemySpeed;
    }

    /**
     * Gets the multiplier on the action speed of a classmate standing here.
     *
     * @return 1.0 for no change
     */
    public double getDefenderSpeed() {
        return defenderSpeed;
    }
}
