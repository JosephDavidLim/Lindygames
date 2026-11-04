package lindefense.tower;

/** Target priority options available on every offensive tower. */
public enum Targeting {
    FIRST("First"), LAST("Last"), STRONGEST("Strong"), WEAKEST("Weak"), CLOSEST("Close"),
    /** Bellarmine Student + AI: camo > bosses > strongest. */
    SMART("Auto");

    public final String label;
    Targeting(String label) { this.label = label; }

    public Targeting next() {
        Targeting[] v = values();
        Targeting n = v[(ordinal() + 1) % v.length];
        return n == SMART ? FIRST : n;
    }

    public Targeting prev() {
        Targeting[] v = values();
        Targeting n = v[(ordinal() + v.length - 1) % v.length];
        return n == SMART ? CLOSEST : n;
    }
}
