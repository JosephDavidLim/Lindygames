package game;

/**
 * Equipment a classmate can wear. Each slot boosts one stat, and higher tiers
 * boost it more. Items drop from enemies or are bought at the Black Market.
 */
public enum Gear {
    /** Tier 0 weapon. */
    WOODEN_RULER("Wooden Ruler", Slot.WEAPON, 0),
    /** Tier 1 weapon. */
    STEEL_RULER("Steel Ruler", Slot.WEAPON, 1),
    /** Tier 2 weapon. */
    GRAPHING_CALCULATOR("Graphing Calculator", Slot.WEAPON, 2),
    /** Tier 3 weapon. */
    GOLDEN_PROTRACTOR("Golden Protractor", Slot.WEAPON, 3),
    /** Tier 0 hat. */
    BEANIE("Beanie", Slot.HAT, 0),
    /** Tier 1 hat. */
    THINKING_CAP("Thinking Cap", Slot.HAT, 1),
    /** Tier 2 hat. */
    GRADUATION_CAP("Graduation Cap", Slot.HAT, 2),
    /** Tier 3 hat. */
    LAUREL_CROWN("Laurel Crown", Slot.HAT, 3),
    /** Tier 0 shoes. */
    WORN_SNEAKERS("Worn Sneakers", Slot.SHOES, 0),
    /** Tier 1 shoes. */
    TRACK_SPIKES("Track Spikes", Slot.SHOES, 1),
    /** Tier 2 shoes. */
    HALL_PASS_RUNNERS("Hall-Pass Runners", Slot.SHOES, 2),
    /** Tier 3 shoes. */
    WINGED_LOAFERS("Winged Loafers", Slot.SHOES, 3),
    /** Tier 0 charm. */
    LUCKY_PENCIL("Lucky Pencil", Slot.CHARM, 0),
    /** Tier 1 charm. */
    FOUR_LEAF_BOOKMARK("Four-Leaf Bookmark", Slot.CHARM, 1),
    /** Tier 2 charm. */
    ATTENDANCE_PIN("Attendance Pin", Slot.CHARM, 2),
    /** Tier 3 charm. */
    VALEDICTORIAN_MEDAL("Valedictorian Medal", Slot.CHARM, 3);

    /** Where an item is worn, and which stat that slot boosts. */
    public enum Slot {
        /** Boosts STR. */
        WEAPON("Weapon", Stat.STR),
        /** Boosts INT. */
        HAT("Hat", Stat.INT),
        /** Boosts DEX. */
        SHOES("Shoes", Stat.DEX),
        /** Boosts LUK. */
        CHARM("Charm", Stat.LUK);

        private final String label;
        private final Stat stat;

        Slot(String label, Stat stat) {
            this.label = label;
            this.stat = stat;
        }

        /**
         * Gets the slot's name.
         *
         * @return display name
         */
        public String getLabel() {
            return label;
        }

        /**
         * Gets the stat this slot boosts.
         *
         * @return the stat
         */
        public Stat getStat() {
            return stat;
        }
    }

    /** Power bonus for wearing all four slots of one tier, by tier. */
    public static final double[] SET_BONUS = {0.05, 0.10, 0.15, 0.25};

    private static final String[] SET_NAMES = {"Starter Set", "Honors Set", "AP Set", "Valedictorian Set"};
    private static final int[] BONUS = {3, 6, 10, 16};
    private static final int[] PRICE = {150, 500, 1400, 3500};
    private static final String[] RARITY = {"Common", "Rare", "Epic", "Legendary"};

    private final String displayName;
    private final Slot slot;
    private final int tier;

    Gear(String displayName, Slot slot, int tier) {
        this.displayName = displayName;
        this.slot = slot;
        this.tier = tier;
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
     * Gets the slot this item fits.
     *
     * @return the slot
     */
    public Slot getSlot() {
        return slot;
    }

    /**
     * Gets the quality tier.
     *
     * @return 0 (Common) to 3 (Legendary)
     */
    public int getTier() {
        return tier;
    }

    /**
     * Gets how many points this item adds to its slot's stat.
     *
     * @return stat bonus
     */
    public int getBonus() {
        return BONUS[tier];
    }

    /**
     * Gets the Black Market price.
     *
     * @return price in Credits
     */
    public int getPrice() {
        return PRICE[tier];
    }

    /**
     * Gets the rarity name.
     *
     * @return for example "Rare"
     */
    public String getRarity() {
        return RARITY[tier];
    }

    /**
     * Describes the item's bonus.
     *
     * @return for example "+6 STR"
     */
    public String describe() {
        return "+" + getBonus() + " " + slot.getStat().getLabel();
    }

    /**
     * Gets the name of a full set.
     *
     * @param tier tier from 0 to 3
     * @return the set's name
     */
    public static String setName(int tier) {
        return SET_NAMES[tier];
    }

    /**
     * Gets the highest tier available once a level has been unlocked.
     *
     * @param maxUnlocked the highest unlocked level
     * @return the best tier the store sells and enemies drop
     */
    public static int bestTierAt(int maxUnlocked) {
        if (maxUnlocked >= 16) {
            return 3;
        } else if (maxUnlocked >= 10) {
            return 2;
        } else if (maxUnlocked >= 4) {
            return 1;
        }
        return 0;
    }
}
