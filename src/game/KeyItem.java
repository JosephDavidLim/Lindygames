package game;

/**
 * What each floor's boss leaves behind. Key items stay with the student for good.
 */
public enum KeyItem {
    /** From the Substitute. */
    HALL_PASS(3, "Hall Pass", "+50 FP at the start of every level"),
    /** From the Vice Principal. */
    LOCKER_KEY(6, "Master Locker Key", "A 7th card slot"),
    /** From the Lunch Lady. */
    COFFEE_REFILLS(9, "Free Coffee Refills", "Every coffee gives 10 more FP"),
    /** From the Librarian. */
    STUDY_GUIDE(12, "Forbidden Study Guide", "An 8th card slot"),
    /** From the Coach. */
    TEAM_SPIRIT(15, "Team Spirit", "10% more EXP"),
    /** From the Deadline. */
    THE_WAY_OUT(18, "The Way Out", "Please report to your assignment");

    private final int level;
    private final String displayName;
    private final String description;

    KeyItem(int level, String displayName, String description) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Finds the item a level's boss leaves.
     *
     * @param level level number
     * @return the item, or null
     */
    public static KeyItem fromLevel(int level) {
        for (KeyItem k : values()) {
            if (k.level == level) {
                return k;
            }
        }
        return null;
    }

    /**
     * Tells whether the player holds this item.
     *
     * @param maxUnlocked the highest unlocked level
     * @return true once its level has been cleared
     */
    public boolean isOwned(int maxUnlocked) {
        return maxUnlocked > level;
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
     * Describes what the item does.
     *
     * @return a short line
     */
    public String getDescription() {
        return description;
    }
}
