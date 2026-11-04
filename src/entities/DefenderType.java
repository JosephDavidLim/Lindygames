package entities;

/**
 * Every defender the player can place. Stats live here as data, and
 * {@link #create(int, int)} builds the matching behavior object. Each
 * constant is named after the classmate; the display name is the character's
 * in-game title.
 */
public enum DefenderType {
    /** Generator: makes Focus Points. */
    AARAV("Sage Aarav", 50, 60, 5, Kind.GENERATOR, 25, 8, 1, false, 0),
    /** Two abilities: generates Focus Points and strikes the three most advanced enemies with lightning. */
    TANISH("Tanimbus", 150, 80, 7, Kind.STORM, 40, 15, 0, false, 0),
    /** Support: heals the eight cells around him. */
    JOSEPH("Sunny Joe", 100, 70, 7, Kind.SUPPORT, 10, 3, 0, false, 0),
    /** Basic shooter. */
    LUIS("Lobber Luis", 100, 60, 5, Kind.SHOOTER, 20, 1.5, 1, false, 0),
    /** Ricochet shooter: each hit bounces into a nearby enemy in an adjacent lane. */
    ABHINAV("Abhi-Ace", 125, 60, 6, Kind.RICOCHET, 15, 1.2, 1, false, 0),
    /** Fan shooter: fires down his own lane and both neighbors. */
    MATT("Zippy Matt", 175, 50, 7, Kind.FAN, 10, 1.0, 1, false, 0),
    /** Close-range blast: hits every enemy within two cells ahead. */
    JUSTIN("Justin Time", 150, 80, 8, Kind.POINT_BLANK, 60, 2, 0, false, 0),
    /** Piercing shooter: hits every enemy in the lane. */
    HANBO("Spear Hanbo", 175, 60, 7, Kind.SHOOTER, 25, 2, 1, true, 0),
    /** Slowing shooter. */
    AUSTIN("Chillstin", 125, 60, 6, Kind.SHOOTER, 15, 1.5, 1, false, 3),
    /** Sniper: instantly hits the toughest enemy on screen, in any lane. */
    NIK("Nik-Scope", 175, 50, 10, Kind.SNIPER, 60, 3.5, 0, false, 0),
    /** Two abilities: a tank that also repairs itself when nothing is biting it. */
    IMRAAN("Iron Imraan", 75, 400, 12, Kind.FORTRESS, 8, 0, 0, false, 0),
    /** Two abilities: a cheap wall that also stomps everything in his own cell and the next. */
    KAI("Kaiju Kai", 50, 150, 8, Kind.STOMPER, 10, 1.5, 0, false, 0),
    /** Thorn tank: hurts whatever bites it. */
    TUHIN("Thorn Tuhin", 100, 600, 12, Kind.WALL, 10, 0, 0, false, 0),
    /** Area bomb: explodes after a fuse. */
    SAM("Boom Sam", 150, 1, 25, Kind.BOMB, 150, 1.5, 0, false, 0),
    /** Mine: arms after a delay, then explodes under the first enemy to step on it. */
    CAMILO("Cami-Mine", 25, 1, 20, Kind.MINE, 300, 8, 0, false, 0),
    /** Trap: stuns the first enemy that steps on it. Power is stun seconds, interval is arm time. */
    JAMES("Trapper Che", 75, 1, 15, Kind.TRAP, 3, 2, 0, false, 0),
    /** Lane clear: hits every enemy in his lane once. */
    ARNAV("Blitz Arnav", 200, 1, 40, Kind.LANE_CLEAR, 1000, 0, 0, false, 0);

    /** Behavior families. */
    public enum Kind {
        GENERATOR, SHOOTER, RICOCHET, FAN, SNIPER, POINT_BLANK, STORM, WALL, FORTRESS, STOMPER,
        BOMB, SUPPORT, MINE, TRAP, LANE_CLEAR
    }

    private final String displayName;
    private final int cost;
    private final int health;
    private final double rechargeSec;
    private final Kind kind;
    private final double power;
    private final double intervalSec;
    private final int burst;
    private final boolean piercing;
    private final double slowSec;

    DefenderType(String displayName, int cost, int health, double rechargeSec, Kind kind,
                 double power, double intervalSec, int burst, boolean piercing, double slowSec) {
        this.displayName = displayName;
        this.cost = cost;
        this.health = health;
        this.rechargeSec = rechargeSec;
        this.kind = kind;
        this.power = power;
        this.intervalSec = intervalSec;
        this.burst = burst;
        this.piercing = piercing;
        this.slowSec = slowSec;
    }

    /**
     * Builds the behavior object for this type.
     *
     * @param row lane the defender is placed in
     * @param col column the defender is placed in
     * @return a new defender
     */
    public Defender create(int row, int col) {
        switch (kind) {
            case GENERATOR:
                return new Generator(this, row, col);
            case SHOOTER:
            case RICOCHET:
            case FAN:
                return new Shooter(this, row, col);
            case SNIPER:
                return new Sniper(this, row, col);
            case POINT_BLANK:
                return new PointBlank(this, row, col);
            case STORM:
                return new Storm(this, row, col);
            case WALL:
                return new Wall(this, row, col);
            case FORTRESS:
                return new IronWall(this, row, col);
            case STOMPER:
                return new Stomper(this, row, col);
            case BOMB:
                return new Bomb(this, row, col);
            case SUPPORT:
                return new Support(this, row, col);
            case MINE:
                return new Mine(this, row, col);
            case TRAP:
                return new Trap(this, row, col);
            case LANE_CLEAR:
                return new LaneClear(this, row, col);
            default:
                throw new IllegalStateException("No behavior for " + this);
        }
    }

    /**
     * Gets the broad job of this defender, grouping shooter variants together
     * and wall variants together.
     *
     * @return SHOOTER, WALL, or this type's own kind
     */
    public Kind role() {
        switch (kind) {
            case RICOCHET:
            case FAN:
                return Kind.SHOOTER;
            case FORTRESS:
            case STOMPER:
                return Kind.WALL;
            case STORM:
                return Kind.GENERATOR;
            default:
                return kind;
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
     * Gets the Focus Point cost.
     *
     * @return cost to place
     */
    public int getCost() {
        return cost;
    }

    /**
     * Gets starting health.
     *
     * @return health points
     */
    public int getHealth() {
        return health;
    }

    /**
     * Gets the recharge time before the card can be used again.
     *
     * @return recharge in seconds
     */
    public double getRechargeSec() {
        return rechargeSec;
    }

    /**
     * Gets the behavior family.
     *
     * @return the kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Gets the main number: shot damage, Focus Points made, thorn damage per
     * second, blast damage, heal amount, or stun seconds depending on the kind.
     *
     * @return the power value
     */
    public double getPower() {
        return power;
    }

    /**
     * Gets the action interval: seconds between shots, payouts or heals, the
     * bomb fuse, or the arming time of a mine or trap.
     *
     * @return interval in seconds
     */
    public double getIntervalSec() {
        return intervalSec;
    }

    /**
     * Gets how many projectiles one shot makes.
     *
     * @return projectiles per burst
     */
    public int getBurst() {
        return burst;
    }

    /**
     * Tells whether shots pass through enemies.
     *
     * @return true if piercing
     */
    public boolean isPiercing() {
        return piercing;
    }

    /**
     * Gets how long a hit slows an enemy.
     *
     * @return slow time in seconds, 0 for none
     */
    public double getSlowSec() {
        return slowSec;
    }

    /**
     * Describes what this defender does in a few words, for the UI.
     *
     * @return a short stat line such as "20 dmg/1.5s"
     */
    public String describe() {
        switch (kind) {
            case GENERATOR:
                return "+" + (int) power + " FP/" + plain(intervalSec) + "s";
            case SHOOTER:
                String s = (int) power + " dmg/" + plain(intervalSec) + "s";
                if (burst > 1) {
                    s = burst + "x " + s;
                }
                if (piercing) {
                    s += " pierce";
                }
                if (slowSec > 0) {
                    s += " slow";
                }
                return s;
            case RICOCHET:
                return (int) power + " dmg, ricochets";
            case FAN:
                return (int) power + " dmg in 3 lanes";
            case SNIPER:
                return (int) power + " dmg, any lane";
            case POINT_BLANK:
                return (int) power + " dmg close blast";
            case STORM:
                return "+" + Storm.PAYOUT + " FP + lightning/" + plain(intervalSec) + "s";
            case FORTRESS:
                return health + " HP, self-repairs";
            case STOMPER:
                return health + " HP + " + (int) power + " dmg stomp";
            case WALL:
                return health + " HP" + (power > 0 ? " thorns" : "");
            case SUPPORT:
                return "heals " + (int) power + "/" + plain(intervalSec) + "s";
            case MINE:
                return (int) power + " dmg mine";
            case TRAP:
                return "stun " + plain(power) + "s";
            case LANE_CLEAR:
                return (int) power + " dmg lane";
            default:
                return (int) power + " dmg blast";
        }
    }

    /**
     * Names the implant the Institute fitted this classmate with.
     *
     * @return a short line for the UI
     */
    public String construct() {
        switch (this) {
            case AARAV:
                return "Focus Miner: turns attention into currency";
            case TANISH:
                return "Storm Coil: mines Focus and calls lightning";
            case JOSEPH:
                return "Medic Array: heals others automatically";
            case LUIS:
                return "Rail Arm: throws hard and straight";
            case ABHINAV:
                return "Vector Arm: shots find a second target";
            case MATT:
                return "Spread Rig: fires down three lanes";
            case JUSTIN:
                return "Close Coil: works only when it's nearly too late";
            case HANBO:
                return "Lance Frame: drives through a whole line";
            case AUSTIN:
                return "Cryo Blood: chills what it touches, and its owner";
            case NIK:
                return "Target Lens: reads anyone's grade from a mile away";
            case IMRAAN:
                return "Plate Spine: rebuilt in steel, repairs itself";
            case KAI:
                return "Quake Legs: stomps everything nearby";
            case TUHIN:
                return "Thorn Skin: nothing can hold on";
            case SAM:
                return "Chest Charge: a demolition scholarship";
            case CAMILO:
                return "Mine Hands: good at waiting";
            case JAMES:
                return "Snare Rig: stops whoever steps on it";
            default:
                return "One Shot: built to be used once";
        }
    }

    /**
     * Describes how this classmate attacks when the player controls them.
     *
     * @return a short line for the UI
     */
    public String heroMove() {
        switch (kind) {
            case GENERATOR:
                return "Close swing that also earns 3 FP";
            case STORM:
                return "Close swing that briefly stuns";
            case SUPPORT:
                return "Close swing that heals nearby classmates";
            case SHOOTER:
                if (piercing) {
                    return "Long spear thrust through the whole line";
                }
                return slowSec > 0 ? "Long throw that slows" : "Long throw at one enemy";
            case RICOCHET:
                return "Medium throw that bounces to the next lane";
            case FAN:
                return "Medium throw that also hits both nearby lanes";
            case SNIPER:
                return "Slow, heavy shot at any range in the lane";
            case POINT_BLANK:
                return "Short, strong blast on everything in reach";
            case STOMPER:
                return "Stomp that hits both directions";
            case WALL:
            case FORTRESS:
                return "Close swing, with 50% more health";
            case BOMB:
                return "Slow throw that explodes in a 3x3";
            case MINE:
                return "Short, heavy hit on one enemy";
            case TRAP:
                return "Slow swing that stuns for 1 second";
            case LANE_CLEAR:
                return "Very slow strike on the whole lane";
            default:
                return "Close swing";
        }
    }

    private static String plain(double v) {
        return v == (int) v ? String.valueOf((int) v) : String.valueOf(v);
    }
}
