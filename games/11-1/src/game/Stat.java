package game;

/**
 * The four stats a classmate can raise, named after MapleStory's.
 */
public enum Stat {
    /** Raises damage, healing and Focus Point payouts. */
    STR("STR", "+2% power"),
    /** Makes the classmate act faster. */
    DEX("DEX", "+1.5% action speed"),
    /** Makes the classmate's card recharge faster. */
    INT("INT", "+2% card recharge"),
    /** Adds critical hits and dodged bites. */
    LUK("LUK", "+1% crit and dodge");

    private final String label;
    private final String effect;

    Stat(String label, String effect) {
        this.label = label;
        this.effect = effect;
    }

    /**
     * Gets the short name.
     *
     * @return for example "STR"
     */
    public String getLabel() {
        return label;
    }

    /**
     * Describes what one point does.
     *
     * @return a short effect line
     */
    public String getEffect() {
        return effect;
    }
}
