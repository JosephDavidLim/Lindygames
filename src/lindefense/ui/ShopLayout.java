package lindefense.ui;

import java.util.ArrayList;
import java.util.List;
import lindefense.tower.Faction;
import lindefense.tower.TowerCatalog;
import lindefense.tower.TowerType;

/**
 * Pure-logic layout of the tower shop (no ACM dependency) so it can be unit
 * tested headlessly. Produces cell rectangles in logical coordinates for the
 * single-faction view and for the all-towers view (every purchasable tower in
 * one compact grid grouped by faction in spec order: Heroes, Primary, Military,
 * Magic, Support, Titans).
 */
public final class ShopLayout {
    private ShopLayout() { }

    /** One shop cell. */
    public static final class Cell {
        public final TowerType type;
        public final Faction faction;
        public final double x, y, w, h;
        /** True for the first cell of a faction row group (where the header label goes). */
        public final boolean firstOfFaction;

        Cell(TowerType type, double x, double y, double w, double h, boolean firstOfFaction) {
            this.type = type; this.faction = type.faction; this.x = x; this.y = y; this.w = w; this.h = h; this.firstOfFaction = firstOfFaction;
        }

        public boolean contains(double px, double py) { return px >= x && px <= x + w && py >= y && py <= y + h; }
    }

    /** Area available for cards inside the sidebar. */
    public static final double CARDS_X = Ui.PANEL_X + 8;
    public static final double CARDS_Y = 72;
    public static final double CARDS_W = Ui.PANEL_W - 16;
    /** Bottom of the card area (info panel begins below). */
    public static final double CARDS_BOTTOM = 302;

    /** Width of the faction header column in the all-towers view. */
    public static final double HEADER_W = 56;
    public static final double ALL_ROW_H = 29;
    public static final double ALL_ROW_GAP = 1;
    public static final int ALL_MAX_COLS = 4;

    /** Cells for the single-faction view: 2 columns of 190 x 56 cards. */
    public static List<Cell> factionCells(Faction f) {
        List<Cell> cells = new ArrayList<>();
        int i = 0;
        for (TowerType t : TowerType.purchasable()) {
            if (t.faction != f) continue;
            double cx = CARDS_X + (i % 2) * 194, cy = CARDS_Y + (i / 2) * 60;
            cells.add(new Cell(t, cx, cy, 190, 56, i == 0));
            i++;
        }
        return cells;
    }

    /**
     * Cells for the all-towers view: one row group per faction (Titans wrap to
     * two rows), each row sharing the width after a header column. Fits in the
     * card area without scrolling.
     */
    public static List<Cell> allCells() {
        List<Cell> cells = new ArrayList<>();
        double y = CARDS_Y;
        double gridX = CARDS_X + HEADER_W;
        double gridW = CARDS_W - HEADER_W;
        for (Faction f : Faction.values()) {
            List<TowerType> types = new ArrayList<>();
            for (TowerType t : TowerType.purchasable()) if (t.faction == f) types.add(t);
            int n = types.size();
            int cols = Math.min(n, ALL_MAX_COLS);
            double cellW = gridW / cols;
            for (int i = 0; i < n; i++) {
                int row = i / cols, col = i % cols;
                double cx = gridX + col * cellW;
                double cy = y + row * (ALL_ROW_H + ALL_ROW_GAP);
                cells.add(new Cell(types.get(i), cx + 1, cy, cellW - 2, ALL_ROW_H, i == 0));
            }
            int rows = (n + cols - 1) / cols;
            y += rows * (ALL_ROW_H + ALL_ROW_GAP) + 2;
        }
        return cells;
    }

    /** Height used by the all-towers grid. */
    public static double allGridHeight() {
        double maxBottom = CARDS_Y;
        for (Cell c : allCells()) maxBottom = Math.max(maxBottom, c.y + c.h);
        return maxBottom - CARDS_Y;
    }

    public static boolean affordable(double cash, TowerType t) { return cash >= TowerCatalog.get(t).baseCost; }

    /** Cash still missing to buy the tower (0 if affordable). */
    public static int shortfall(double cash, TowerType t) {
        return (int) Math.max(0, Math.ceil(TowerCatalog.get(t).baseCost - cash));
    }

    /** Cell under a logical point, or null. */
    public static Cell cellAt(List<Cell> cells, double px, double py) {
        for (Cell c : cells) if (c.contains(px, py)) return c;
        return null;
    }
}
