package game;

import entities.DefenderType;
import entities.EnemyType;

/**
 * A favor a classmate asks of the player: defeat a number of one enemy. It's a
 * small kindness in a place with very few of them.
 */
public enum Quest {
    /** Starter quest. */
    BUSYWORK("Busywork", DefenderType.AARAV, "My Miner bills me for every sheet I look at. Thin them out?",
            EnemyType.HOMEWORK_SHEET, 20, 2, 100, null),
    /** Pencil quest. */
    SHARP_TURN("Sharp Turn", DefenderType.LUIS, "Those pencils have my blood type on file.",
            EnemyType.SHARP_PENCIL, 20, 3, 150, Gear.LUCKY_PENCIL),
    /** Swarm quest. */
    SHORTCUT("Bad Answers", DefenderType.SAM, "Scantrons keep marking my chest as a wrong answer.",
            EnemyType.SCANTRON_SWARM, 18, 10, 500, Gear.BEANIE),
    /** Substitute's follow-up. */
    PATROL("Hall Patrol", DefenderType.KAI, "Hall Monitors wrote me up for breathing. Get them.",
            EnemyType.HALL_MONITOR, 15, 6, 250, Gear.WORN_SNEAKERS),
    /** Report card quest. */
    BAD_GRADES("Grade Inflation", DefenderType.IMRAAN, "My report cards know where I sleep.",
            EnemyType.REPORT_CARD, 15, 5, 250, Gear.WOODEN_RULER),
    /** Boss quest. */
    DETENTION("Out of Detention", DefenderType.AUSTIN, "I lost a month in detention. Beat the Vice Principal for me.",
            EnemyType.VICE_PRINCIPAL, 1, 6, 400, Gear.THINKING_CAP),
    /** Cafeteria quest. */
    MYSTERY("Mystery Solved", DefenderType.JOSEPH, "Nobody knows what that meat is. I think it knows me.",
            EnemyType.MYSTERY_MEAT, 15, 9, 400, null),
    /** Plane quest. */
    NO_FLY_ZONE("No-Fly Zone", DefenderType.MATT, "Paper planes are the Ledger's drones. Ground them.",
            EnemyType.PAPER_AIRPLANE, 20, 8, 400, Gear.TRACK_SPIKES),
    /** Library quest. */
    OVERDUE("Long Overdue", DefenderType.NIK, "My lens reads overdue fees on everyone. Mine are the worst.",
            EnemyType.OVERDUE_NOTICE, 20, 10, 600, Gear.STEEL_RULER),
    /** Boss quest. */
    SILENCE("Break the Silence", DefenderType.HANBO, "One shush from the Librarian and my spear locked up. Never again.",
            EnemyType.LIBRARIAN, 1, 13, 800, Gear.FOUR_LEAF_BOOKMARK),
    /** Gym quest. */
    DODGE("Dodge This", DefenderType.JUSTIN, "Those dodgeballs are tracking me now. Help.",
            EnemyType.DODGEBALL, 25, 13, 800, Gear.HALL_PASS_RUNNERS),
    /** Late quest. */
    FINAL_DRAFT("Final Draft", DefenderType.TANISH, "Destroy every essay before the Tower reads mine.",
            EnemyType.PERSONAL_ESSAY, 20, 17, 1200, Gear.GRADUATION_CAP);

    private final String title;
    private final DefenderType npc;
    private final String line;
    private final EnemyType target;
    private final int count;
    private final int minLevel;
    private final int credits;
    private final Gear gear;

    Quest(String title, DefenderType npc, String line, EnemyType target, int count, int minLevel, int credits,
          Gear gear) {
        this.title = title;
        this.npc = npc;
        this.line = line;
        this.target = target;
        this.count = count;
        this.minLevel = minLevel;
        this.credits = credits;
        this.gear = gear;
    }

    /**
     * Describes what must be done.
     *
     * @return for example "Defeat 20 Sharp Pencils"
     */
    public String describeGoal() {
        return count == 1 ? "Defeat " + target.getDisplayName() : "Defeat " + count + " x " + target.getDisplayName();
    }

    /**
     * Describes the reward.
     *
     * @return a short line
     */
    public String describeReward() {
        return credits + " Credits" + (gear == null ? "" : " + " + gear.getDisplayName());
    }

    /**
     * Gets the quest's name.
     *
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets who gives this quest.
     *
     * @return the classmate
     */
    public DefenderType getNpc() {
        return npc;
    }

    /**
     * Gets what the classmate says.
     *
     * @return one line of dialogue
     */
    public String getLine() {
        return line;
    }

    /**
     * Gets the enemy to defeat.
     *
     * @return the enemy
     */
    public EnemyType getTarget() {
        return target;
    }

    /**
     * Gets how many are needed.
     *
     * @return the goal count
     */
    public int getCount() {
        return count;
    }

    /**
     * Gets the highest-unlocked level needed before this quest appears.
     *
     * @return level number
     */
    public int getMinLevel() {
        return minLevel;
    }

    /**
     * Gets the Credits paid.
     *
     * @return Credits
     */
    public int getCredits() {
        return credits;
    }

    /**
     * Gets the item paid.
     *
     * @return the item, or null
     */
    public Gear getGear() {
        return gear;
    }
}
