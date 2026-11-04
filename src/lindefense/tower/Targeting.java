package lindefense.tower;

/**
 * Target priority options. Every attacking tower supports the first five; {@link #DENSEST_CLUSTER} is only
 * selectable by towers whose behaviour allows it (Grok U2+).
 */
public enum Targeting {
    FIRST("First"), LAST("Last"), STRONGEST("Strong"), WEAKEST("Weak"), CLOSEST("Close"),
    /** Enemy with the most other enemies within {@link lindefense.combat.Clusters#RADIUS} px; ties go to First. */
    DENSEST_CLUSTER("Dense");

    public final String label;
    Targeting(String label) { this.label = label; }

    /** Next in the cycle, skipping DENSEST_CLUSTER (towers that allow it use {@link Tower#cycleTargeting}). */
    public Targeting next() {
        Targeting[] v = values();
        Targeting n = v[(ordinal() + 1) % v.length];
        return n == DENSEST_CLUSTER ? FIRST : n;
    }

    /** Previous in the cycle, skipping DENSEST_CLUSTER. */
    public Targeting prev() {
        Targeting[] v = values();
        Targeting n = v[(ordinal() + v.length - 1) % v.length];
        return n == DENSEST_CLUSTER ? CLOSEST : n;
    }
}
