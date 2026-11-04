package lindefense.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lindefense.enemy.Boss;

/**
 * Pure logic for the boss health dock: which bosses get a row, which one is
 * primary, and how many are hidden behind "+N more". Kept free of ACM so it
 * can be unit tested.
 */
public final class BossDockModel {
    private BossDockModel() { }

    /** Maximum rows shown (1 primary + compact rows). */
    public static final int MAX_ROWS = 3;

    /** Result of laying out the dock. */
    public static final class Layout {
        public final Boss primary;
        public final List<Boss> compact;
        public final int hidden;
        Layout(Boss primary, List<Boss> compact, int hidden) { this.primary = primary; this.compact = compact; this.hidden = hidden; }
        public int rows() { return (primary == null ? 0 : 1) + compact.size(); }
    }

    /**
     * Orders bosses by importance (Palantir first, then biggest max HP, then
     * closest to the exit) and caps the dock at {@link #MAX_ROWS} rows.
     */
    public static Layout layout(List<Boss> bosses) {
        List<Boss> sorted = new ArrayList<>(bosses);
        sorted.sort(Comparator.comparingInt((Boss b) -> b.isPalantir() ? 0 : 1)
            .thenComparingDouble((Boss b) -> -b.getMaxHp())
            .thenComparingDouble(Boss::getRemainingDistance));
        if (sorted.isEmpty()) return new Layout(null, new ArrayList<>(), 0);
        Boss primary = sorted.get(0);
        List<Boss> compact = new ArrayList<>();
        for (int i = 1; i < sorted.size() && compact.size() < MAX_ROWS - 1; i++) compact.add(sorted.get(i));
        int hidden = sorted.size() - 1 - compact.size();
        return new Layout(primary, compact, hidden);
    }
}
