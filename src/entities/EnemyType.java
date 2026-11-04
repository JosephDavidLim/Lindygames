package entities;

import game.Board;

/**
 * Every school enemy. Stats live here as data, and
 * {@link #create(int, double)} builds the matching behavior object.
 * Speed is in cells per second and bite is damage per second.
 */
public enum EnemyType {
    /** Basic walker. */
    HOMEWORK_SHEET("Homework Sheet", 100, 0.12, 10, 1, Kind.WALKER, 1, 0),
    /** Fast and fragile. */
    SHARP_PENCIL("Sharp Pencil", 60, 0.25, 8, 1, Kind.WALKER, 1, 0),
    /** Grade drops as it takes damage; panics at F. */
    REPORT_CARD("Report Card", 250, 0.12, 10, 3, Kind.REPORT_CARD, 1, 0),
    /** Jumps over the first defender it meets. */
    PAPER_AIRPLANE("Paper Airplane", 120, 0.20, 10, 2, Kind.FLYER, 1, 0),
    /** Heavy biter that ignores slows. */
    ERASER("Eraser", 150, 0.10, 30, 3, Kind.IMMUNE, 1, 0),
    /** Mini-boss: calls in pencils. */
    MIDTERM_EXAM("Midterm Exam", 600, 0.08, 25, 0, Kind.BOSS, 1, 15),
    /** Steals Focus Points while alive. */
    APPLICATION_FEE("Application Fee", 120, 0.12, 10, 3, Kind.DRAINER, 1, 10),
    /** Soaks up damage and stops to revise itself at half health. */
    PERSONAL_ESSAY("Personal Essay", 400, 0.10, 15, 4, Kind.REVISER, 1, 0),
    /** Fast and brutal. */
    REJECTION_LETTER("Rejection Letter", 250, 0.16, 45, 4, Kind.WALKER, 1, 0),
    /** Always arrives in a group of three. */
    SCANTRON_SWARM("Scantron Swarm", 60, 0.20, 8, 3, Kind.WALKER, 3, 0),
    /** Final boss: calls in pencils. */
    THE_DEADLINE("The Deadline", 3000, 0.06, 40, 0, Kind.BOSS, 1, 10),
    /** Hallway regular: a steady biter that writes you up. */
    HALL_MONITOR("Hall Monitor", 180, 0.12, 14, 2, Kind.WALKER, 1, 0),
    /** Cafeteria regular: slow and very hard to get rid of. */
    MYSTERY_MEAT("Mystery Meat", 300, 0.08, 10, 3, Kind.WALKER, 1, 0),
    /** Library regular: flutters over the first defender it meets. */
    OVERDUE_NOTICE("Overdue Notice", 130, 0.16, 10, 2, Kind.FLYER, 1, 0),
    /** Gym regular: fast and fragile. */
    DODGEBALL("Dodgeball", 90, 0.34, 12, 2, Kind.WALKER, 1, 0),
    /** Classroom boss: hands out extra homework. */
    THE_SUBSTITUTE("The Substitute", 220, 0.08, 18, 0, Kind.BOSS, 1, 14),
    /** Hallway boss: sends classmates to detention. */
    VICE_PRINCIPAL("The Vice Principal", 600, 0.08, 25, 0, Kind.BOSS, 1, 12),
    /** Cafeteria boss: flings food at classmates anywhere on the map. */
    LUNCH_LADY("The Lunch Lady", 700, 0.08, 25, 0, Kind.BOSS, 1, 11),
    /** Library boss: silences whole lanes. */
    LIBRARIAN("The Librarian", 720, 0.07, 28, 0, Kind.BOSS, 1, 14),
    /** Gym boss: speeds up every enemy and throws dodgeballs. */
    COACH("The Coach", 820, 0.09, 30, 0, Kind.BOSS, 1, 13);

    /** Behavior families. */
    public enum Kind { WALKER, REPORT_CARD, FLYER, DRAINER, BOSS, IMMUNE, REVISER }

    private final String displayName;
    private final int health;
    private final double speed;
    private final double bite;
    private final int points;
    private final Kind kind;
    private final int groupSize;
    private final double specialSec;

    EnemyType(String displayName, int health, double speed, double bite, int points,
              Kind kind, int groupSize, double specialSec) {
        this.displayName = displayName;
        this.health = health;
        this.speed = speed;
        this.bite = bite;
        this.points = points;
        this.kind = kind;
        this.groupSize = groupSize;
        this.specialSec = specialSec;
    }

    /**
     * Builds the behavior object for this type.
     *
     * @param row lane index
     * @param x starting position in cell units
     * @return a new enemy
     */
    public Enemy create(int row, double x) {
        switch (kind) {
            case WALKER:
                return new Walker(this, row, x);
            case IMMUNE:
                return new ImmuneWalker(this, row, x);
            case REVISER:
                return new Reviser(this, row, x);
            case REPORT_CARD:
                return new ReportCard(this, row, x);
            case FLYER:
                return new Flyer(this, row, x);
            case DRAINER:
                return new Drainer(this, row, x);
            case BOSS:
                return new Boss(this, row, x);
            default:
                throw new IllegalStateException("No behavior for " + this);
        }
    }

    /**
     * Gets the EXP and Credits this enemy is worth before its level bonus.
     *
     * @return reward points; tougher enemies pay more, and bosses pay double
     */
    public int getReward() {
        int base = Math.max(1, health / 20);
        return kind == Kind.BOSS ? base * 2 : base;
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
     * Gets starting health.
     *
     * @return health points
     */
    public int getHealth() {
        return health;
    }

    /**
     * Gets walking speed per tick.
     *
     * @return cells per tick
     */
    public double getSpeedPerTick() {
        return speed / Board.TICKS_PER_SECOND;
    }

    /**
     * Gets bite damage per tick.
     *
     * @return damage per tick
     */
    public double getBitePerTick() {
        return bite / Board.TICKS_PER_SECOND;
    }

    /**
     * Gets the wave-budget cost. Bosses cost zero because they are placed by hand.
     *
     * @return points
     */
    public int getPoints() {
        return points;
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
     * Gets how many enemies arrive together for one purchase.
     *
     * @return group size
     */
    public int getGroupSize() {
        return groupSize;
    }

    /**
     * Gets the interval of the enemy's special ability, if it has one.
     *
     * @return seconds between uses, 0 for none
     */
    public double getSpecialSec() {
        return specialSec;
    }
}
