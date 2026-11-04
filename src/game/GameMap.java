package game;

/**
 * The six floors of the Meridian Institute. Each has its own art, its own
 * rule and three levels.
 */
public enum GameMap {
    /** Where every student wakes up. No special tiles. */
    CLASSROOM("Homeroom", "Intake. Nothing here but you and the work.", "world1", 10),
    /** Lockers take away building spots. */
    HALLWAY("Locker Corridor", "Sealed lockers block spots where you could build.", "world1", 10),
    /** Coolant speeds enemies up, but coffee is everywhere. */
    CAFETERIA("Nutrition Hall", "Spilled coolant speeds enemies up. Coffee falls faster.", "world1", 7),
    /** Damping fields slow enemies down, but coffee is banned. */
    LIBRARY("The Archive", "Damping fields slow enemies down. Coffee is scarce.", "world2", 13),
    /** Pads make classmates act faster. */
    GYM("Compliance Gym", "Stand on an overclock pad to act 25% faster.", "world2", 10),
    /** Everything at once. */
    ADMISSIONS("Admissions Tower", "Every hazard in the Institute, all at once.", "world2", 10);

    private final String displayName;
    private final String rule;
    private final String music;
    private final double coffeeSec;

    GameMap(String displayName, String rule, String music, double coffeeSec) {
        this.displayName = displayName;
        this.rule = rule;
        this.music = music;
        this.coffeeSec = coffeeSec;
    }

    /**
     * Gets the name shown in the UI.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the one-line description of this map's rule.
     *
     * @return the rule
     */
    public String getRule() {
        return rule;
    }

    /**
     * Gets the soundtrack.
     *
     * @return the music track name
     */
    public String getMusic() {
        return music;
    }

    /**
     * Gets the seconds between falling coffees.
     *
     * @return the interval
     */
    public double getCoffeeSec() {
        return coffeeSec;
    }

    /**
     * Gets the file name of this floor's art.
     *
     * @return the name without a path or extension
     */
    public String getArtName() {
        return name().toLowerCase();
    }
}
