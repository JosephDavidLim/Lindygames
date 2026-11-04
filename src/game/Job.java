package game;

import entities.DefenderType;

/**
 * The second construct a classmate can be fitted with at level 10, like a
 * MapleStory job advancement. Each chooses between the two for their role.
 */
public enum Job {
    /** Shooter path. */
    PITCHER("Railthrower", "Throws 20% faster", 0, 0.20, 0, 0, 0, false),
    /** Shooter path. */
    CATAPULT("Mortar Arm", "+10% power, and shots splash nearby enemies", 0.10, 0, 0, 0, 0, true),
    /** Sniper path. */
    MARKSMAN("Deadeye", "+25% crit chance", 0, 0, 0, 0, 0.25, false),
    /** Sniper path. */
    SPOTTER("Overwatch", "Shoots 20% faster", 0, 0.20, 0, 0, 0, false),
    /** Close blast path. */
    BRUISER("Breaker", "+25% power", 0.25, 0, 0, 0, 0, false),
    /** Close blast path. */
    BRAWLER("Reflex Rig", "Attacks 20% faster", 0, 0.20, 0, 0, 0, false),
    /** Generator path. */
    TUTOR("Overclocker", "Pays out 20% faster", 0, 0.20, 0, 0, 0, false),
    /** Generator path. */
    SCHOLAR("Deep Miner", "+25% power", 0.25, 0, 0, 0, 0, false),
    /** Support path. */
    NURSE("Medic Array", "+30% healing", 0.30, 0, 0, 0, 0, false),
    /** Support path. */
    COUNSELOR("Calm Field", "Acts 25% faster, with +25% health", 0, 0.25, 0, 0.25, 0, false),
    /** Wall path. */
    BULWARK("Plate Frame", "+50% health", 0, 0, 0, 0.50, 0, false),
    /** Wall path. */
    SPIKED("Razor Frame", "+40% power, +15% health", 0.40, 0, 0, 0.15, 0, false),
    /** One-use special path. */
    DEMOLISHER("Demo Core", "+25% power", 0.25, 0, 0, 0, 0, false),
    /** One-use special path. */
    QUICK_STUDY("Fast Cycle", "Card recharges 50% faster", 0, 0, 0.50, 0, 0, false);

    /** The level at which a classmate may advance. */
    public static final int ADVANCE_LEVEL = 10;

    private final String displayName;
    private final String description;
    private final double power;
    private final double speed;
    private final double recharge;
    private final double health;
    private final double crit;
    private final boolean splash;

    Job(String displayName, String description, double power, double speed, double recharge, double health,
        double crit, boolean splash) {
        this.displayName = displayName;
        this.description = description;
        this.power = power;
        this.speed = speed;
        this.recharge = recharge;
        this.health = health;
        this.crit = crit;
        this.splash = splash;
    }

    /**
     * Gets the two paths open to a classmate.
     *
     * @param type the classmate
     * @return exactly two jobs
     */
    public static Job[] optionsFor(DefenderType type) {
        switch (type.role()) {
            case SHOOTER:
                return new Job[] {PITCHER, CATAPULT};
            case SNIPER:
                return new Job[] {MARKSMAN, SPOTTER};
            case POINT_BLANK:
                return new Job[] {BRUISER, BRAWLER};
            case GENERATOR:
                return new Job[] {TUTOR, SCHOLAR};
            case SUPPORT:
                return new Job[] {NURSE, COUNSELOR};
            case WALL:
                return new Job[] {BULWARK, SPIKED};
            default:
                return new Job[] {DEMOLISHER, QUICK_STUDY};
        }
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
     * Describes what this job does.
     *
     * @return a short line
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the bonus to power.
     *
     * @return added multiplier
     */
    public double getPower() {
        return power;
    }

    /**
     * Gets the bonus to action speed.
     *
     * @return added multiplier
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Gets the bonus to card recharge.
     *
     * @return added multiplier
     */
    public double getRecharge() {
        return recharge;
    }

    /**
     * Gets the bonus to health.
     *
     * @return added multiplier
     */
    public double getHealth() {
        return health;
    }

    /**
     * Gets the bonus to crit chance.
     *
     * @return added chance
     */
    public double getCrit() {
        return crit;
    }

    /**
     * Tells whether this job's shots splash.
     *
     * @return true if shots also hit nearby enemies
     */
    public boolean hasSplash() {
        return splash;
    }
}
