package game;

import entities.DefenderType;

/**
 * One classmate's character sheet: level, EXP, stat points and equipment. The
 * sheet lasts between levels, and every copy of the classmate on the board uses it.
 */
public class Hero {
    /** Highest level a classmate can reach. */
    public static final int MAX_LEVEL = 30;
    /** Stat points earned per level. */
    public static final int AP_PER_LEVEL = 2;
    /** Credits per level to take back every spent stat point. */
    public static final int RESET_COST_PER_LEVEL = 20;

    private static final double MAX_CHANCE = 0.5;

    private final DefenderType type;
    private final int[] stats = new int[Stat.values().length];
    private final Gear[] equipped = new Gear[Gear.Slot.values().length];
    private int level;
    private int exp;
    private int ap;
    private Job job;

    /**
     * Creates a sheet with every point earned so far still unspent.
     *
     * @param type the classmate
     * @param level starting level
     */
    public Hero(DefenderType type, int level) {
        this.type = type;
        this.level = Math.max(1, Math.min(MAX_LEVEL, level));
        this.ap = (this.level - 1) * AP_PER_LEVEL;
    }

    /**
     * Gets the EXP needed to go from a level to the next.
     *
     * @param level current level
     * @return EXP required
     */
    public static int expToNext(int level) {
        return 20 + 5 * level * level;
    }

    /**
     * Adds EXP and applies any level-ups.
     *
     * @param amount EXP gained
     * @return how many levels were gained
     */
    public int addExp(int amount) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        int gained = 0;
        exp += amount;
        while (level < MAX_LEVEL && exp >= expToNext(level)) {
            exp -= expToNext(level);
            level++;
            ap += AP_PER_LEVEL;
            gained++;
        }
        if (level >= MAX_LEVEL) {
            exp = 0;
        }
        return gained;
    }

    /**
     * Spends one stat point.
     *
     * @param s the stat to raise
     * @return true if a point was available
     */
    public boolean spend(Stat s) {
        if (ap <= 0) {
            return false;
        }
        ap--;
        stats[s.ordinal()]++;
        return true;
    }

    /**
     * Spends every free point using a split that suits this classmate's job.
     */
    public void autoAssign() {
        Stat[] pattern = autoPattern();
        int i = 0;
        while (ap > 0) {
            spend(pattern[i % pattern.length]);
            i++;
        }
    }

    private Stat[] autoPattern() {
        switch (type.role()) {
            case GENERATOR:
            case SUPPORT:
                return new Stat[] {Stat.DEX, Stat.DEX, Stat.STR, Stat.INT};
            case WALL:
                return new Stat[] {Stat.LUK, Stat.LUK, Stat.INT, Stat.STR};
            case BOMB:
            case MINE:
            case TRAP:
            case LANE_CLEAR:
                return new Stat[] {Stat.INT, Stat.INT, Stat.STR};
            default:
                return new Stat[] {Stat.STR, Stat.DEX, Stat.STR, Stat.DEX, Stat.LUK};
        }
    }

    /**
     * Takes back every spent stat point.
     */
    public void resetStats() {
        for (int i = 0; i < stats.length; i++) {
            ap += stats[i];
            stats[i] = 0;
        }
    }

    /**
     * Gets a stat including equipment.
     *
     * @param s the stat
     * @return points spent plus gear bonuses
     */
    public int total(Stat s) {
        int sum = stats[s.ordinal()];
        for (Gear g : equipped) {
            if (g != null && g.getSlot().getStat() == s) {
                sum += g.getBonus();
            }
        }
        return sum;
    }

    /**
     * Gets the multiplier on damage, healing and thorns.
     *
     * @return 1.0 for an untrained level 1 classmate
     */
    public double powerMultiplier() {
        return 1 + 0.01 * (level - 1) + 0.02 * total(Stat.STR) + setBonus() + (job == null ? 0 : job.getPower());
    }

    /**
     * Gets the multiplier on how often the classmate acts.
     *
     * @return 1.0 with no DEX
     */
    public double speedMultiplier() {
        return 1 + 0.015 * total(Stat.DEX) + (job == null ? 0 : job.getSpeed());
    }

    /**
     * Gets the multiplier on how fast the card recharges.
     *
     * @return 1.0 with no INT
     */
    public double rechargeMultiplier() {
        return 1 + 0.02 * total(Stat.INT) + (job == null ? 0 : job.getRecharge());
    }

    /**
     * Gets the multiplier on maximum health.
     *
     * @return 1.0 at level 1
     */
    public double healthMultiplier() {
        return 1 + 0.03 * (level - 1) + (job == null ? 0 : job.getHealth());
    }

    /**
     * Gets the chance that a hit does double damage.
     *
     * @return chance from 0 to 0.5
     */
    public double critChance() {
        return Math.min(MAX_CHANCE, 0.01 * total(Stat.LUK) + (job == null ? 0 : job.getCrit()));
    }

    /**
     * Gets the chance that a bite misses.
     *
     * @return chance from 0 to 0.5
     */
    public double dodgeChance() {
        return Math.min(MAX_CHANCE, 0.01 * total(Stat.LUK));
    }

    /**
     * Gets the school-year rank, which works like a job advancement title.
     *
     * @return Freshman, Sophomore, Junior or Senior
     */
    public String getRank() {
        if (level >= 30) {
            return "Senior";
        } else if (level >= 20) {
            return "Junior";
        } else if (level >= 10) {
            return "Sophomore";
        }
        return "Freshman";
    }

    /**
     * Gets the power bonus for wearing four items of the same tier.
     *
     * @return added multiplier, 0 without a full set
     */
    public double setBonus() {
        int tier = setTier();
        return tier < 0 ? 0 : Gear.SET_BONUS[tier];
    }

    /**
     * Gets the tier of the full set being worn.
     *
     * @return tier from 0 to 3, or -1 without a full set
     */
    public int setTier() {
        int tier = -1;
        for (Gear g : equipped) {
            if (g == null || (tier >= 0 && g.getTier() != tier)) {
                return -1;
            }
            tier = g.getTier();
        }
        return tier;
    }

    /**
     * Tells whether this classmate may choose a job now.
     *
     * @return true at level 10 or above with no job yet
     */
    public boolean canAdvance() {
        return job == null && level >= Job.ADVANCE_LEVEL;
    }

    /**
     * Advances into a job. It must be one of the two paths for this classmate.
     *
     * @param chosen the job
     * @return true if it was allowed
     */
    public boolean advance(Job chosen) {
        if (!canAdvance()) {
            return false;
        }
        for (Job option : Job.optionsFor(type)) {
            if (option == chosen) {
                job = chosen;
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the job.
     *
     * @return the job, or null before advancing
     */
    public Job getJob() {
        return job;
    }

    /**
     * Restores a saved job without checking the level.
     *
     * @param saved the job
     */
    void restoreJob(Job saved) {
        job = saved;
    }

    /**
     * Tells whether this classmate's shots splash.
     *
     * @return true for the Catapult job
     */
    public boolean hasSplash() {
        return job != null && job.hasSplash();
    }

    /**
     * Gets the title shown beside the level: the job if there is one, otherwise the rank.
     *
     * @return a short title
     */
    public String getTitle() {
        return job != null ? job.getDisplayName() : getRank();
    }

    /**
     * Gets the item in a slot.
     *
     * @param slot the slot
     * @return the item or null
     */
    public Gear getEquipped(Gear.Slot slot) {
        return equipped[slot.ordinal()];
    }

    /**
     * Puts an item in its slot. {@link Profile} moves items in and out of the inventory.
     *
     * @param slot the slot
     * @param gear the item, or null to empty the slot
     */
    void setEquipped(Gear.Slot slot, Gear gear) {
        equipped[slot.ordinal()] = gear;
    }

    /**
     * Restores saved values.
     *
     * @param savedExp EXP toward the next level
     * @param savedAp unspent points
     * @param savedStats spent points by stat
     */
    void restore(int savedExp, int savedAp, int[] savedStats) {
        exp = Math.max(0, savedExp);
        ap = Math.max(0, savedAp);
        for (int i = 0; i < stats.length; i++) {
            stats[i] = Math.max(0, savedStats[i]);
        }
    }

    /**
     * Gets the points spent on a stat, not counting gear.
     *
     * @param s the stat
     * @return points spent
     */
    public int getSpent(Stat s) {
        return stats[s.ordinal()];
    }

    /**
     * Gets the classmate.
     *
     * @return the defender type
     */
    public DefenderType getType() {
        return type;
    }

    /**
     * Gets the level.
     *
     * @return level from 1 to {@link #MAX_LEVEL}
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets the EXP earned toward the next level.
     *
     * @return EXP
     */
    public int getExp() {
        return exp;
    }

    /**
     * Gets how far the EXP bar is filled.
     *
     * @return fraction from 0 to 1, and 1 at the level cap
     */
    public double getExpFraction() {
        return level >= MAX_LEVEL ? 1.0 : (double) exp / expToNext(level);
    }

    /**
     * Gets the unspent stat points.
     *
     * @return points
     */
    public int getAp() {
        return ap;
    }
}
