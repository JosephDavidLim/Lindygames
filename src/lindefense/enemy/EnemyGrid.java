package lindefense.enemy;

import java.util.ArrayList;
import java.util.List;
import lindefense.map.GameMap;

/**
 * Uniform spatial hash grid over the live enemies, used for every "enemies near a point" query in the simulation
 * (tower targeting, projectile collisions, splash / aura / field / zone / walker checks, chain and bounce hops, the
 * densest-cluster search) instead of scanning the whole list.
 *
 * <h3>Structure</h3>
 * The 1200x900 world is cut into {@value #CELL}-px square cells, stored as a two-dimensional array
 * {@code cells[column][row]} of {@link ArrayList} buckets (25 x 19). Each bucket holds the enemies whose centre lies in
 * that cell; positions outside the world are clamped into the border cells. A query for a box (for a circle: its
 * bounding box) widens it by the largest enemy radius, visits only the overlapped buckets and returns the enemies in
 * them: a <b>superset</b> of the real answer, on which callers then run their original exact test (distance,
 * camo, line of sight...). Results are returned in {@link EnemyManager#getEnemies() list order}, so every loop that
 * switched from a full scan to the grid visits the same enemies in the same order and the simulation stays
 * bit-for-bit identical (and therefore deterministic for a given seed).
 *
 * <h3>Keeping it current</h3>
 * <ul>
 *   <li><b>Membership</b>: the grid remembers the {@link EnemyList} modification count it was built for and rebuilds
 *       lazily (O(n)) on the first query after enemies were added or removed, which also re-numbers each enemy's list
 *       index.</li>
 *   <li><b>Movement</b>: {@link Enemy} reports every position change (movement, knockback, rewind, teleport, swaps);
 *       if the enemy crossed into another cell it is moved between buckets immediately (O(bucket)). So the grid is
 *       always exact, even for queries in the middle of a tick, after knockback, or in tests that never call
 *       {@code Game.step}.</li>
 *   <li><b>Loops that move enemies while iterating</b> (a hit can knock an enemy into the area being scanned) use a
 *       {@link Scan}: when any enemy changed cell since the candidates were gathered, the scan re-gathers the
 *       remaining (higher-index) candidates, exactly matching what an index-ordered full scan would see.</li>
 * </ul>
 * All of this is single-threaded simulation state; nothing here is shared with the renderer.
 */
public final class EnemyGrid {
    /** Cell edge in world pixels. */
    public static final int CELL = 48;
    /** Number of columns (x) and rows (y) covering the world. */
    public static final int COLS = (GameMap.WIDTH + CELL - 1) / CELL, ROWS = (GameMap.HEIGHT + CELL - 1) / CELL;
    /** A query touching at least this fraction of all cells simply returns the whole list (cheaper than visiting buckets). */
    private static final double FULL_SCAN_FRACTION = 0.5;

    private final EnemyList list;
    /** cells[column][row]: the enemies whose centre lies in that cell. */
    private final ArrayList<Enemy>[][] cells;
    private int builtVersion = Integer.MIN_VALUE;
    private int epoch;
    /** Bumped whenever an enemy changes cell or the grid is rebuilt; lets a {@link Scan} detect stale candidates. */
    private int cellStamp;
    private double maxRadius;
    /** Scratch bitmap of list indices used to emit candidates in list order without sorting. */
    private long[] marks = new long[16];
    private final ArrayList<Scan> freeScans = new ArrayList<>();

    @SuppressWarnings("unchecked")
    EnemyGrid(EnemyList list) {
        this.list = list;
        cells = (ArrayList<Enemy>[][]) new ArrayList<?>[COLS][ROWS];
        for (int c = 0; c < COLS; c++) for (int r = 0; r < ROWS; r++) cells[c][r] = new ArrayList<>(8);
        list.setGrid(this);
    }

    private static int col(double x) {
        int c = (int) Math.floor(x / CELL);
        return c < 0 ? 0 : c >= COLS ? COLS - 1 : c;
    }

    private static int row(double y) {
        int r = (int) Math.floor(y / CELL);
        return r < 0 ? 0 : r >= ROWS ? ROWS - 1 : r;
    }

    /** Rebuilds the buckets if enemies were added or removed since the last build. */
    private void ensure() {
        if (builtVersion != list.version()) rebuild();
    }

    private void rebuild() {
        for (int c = 0; c < COLS; c++) for (int r = 0; r < ROWS; r++) cells[c][r].clear();
        epoch++;
        cellStamp++;
        maxRadius = 0;
        for (int i = 0, n = list.size(); i < n; i++) {
            Enemy e = list.get(i);
            int c = col(e.getPosition().x), r = row(e.getPosition().y);
            e.grid = this;
            e.gridEpoch = epoch;
            e.gridIndex = i;
            e.gridCol = c;
            e.gridRow = r;
            cells[c][r].add(e);
            maxRadius = Math.max(maxRadius, e.getRadius());
        }
        builtVersion = list.version();
        if (marks.length * 64 < list.size()) marks = new long[(list.size() >> 6) + 16];
    }

    /** Called by {@link Enemy} after its position changed: moves it to its new bucket if it crossed a cell border. */
    void moved(Enemy e) {
        if (e.gridEpoch != epoch) return;
        int c = col(e.getPosition().x), r = row(e.getPosition().y);
        if (c == e.gridCol && r == e.gridRow) return;
        ArrayList<Enemy> from = cells[e.gridCol][e.gridRow];
        for (int i = from.size() - 1; i >= 0; i--) {
            if (from.get(i) == e) {
                int last = from.size() - 1;
                from.set(i, from.get(last));
                from.remove(last);
                break;
            }
        }
        cells[c][r].add(e);
        e.gridCol = c;
        e.gridRow = r;
        cellStamp++;
    }

    /** Largest radius of any enemy in the grid (radii only shrink after spawning, so this is a safe bound). */
    public double maxRadius() {
        ensure();
        return maxRadius;
    }

    /**
     * Candidates for "enemy whose centre is within {@code reach} + its own radius of (cx, cy)": every enemy in the
     * cells overlapping that circle's bounding box widened by the largest enemy radius. A superset, in list order.
     *
     * @param out cleared and filled; returned for convenience
     */
    public List<Enemy> near(double cx, double cy, double reach, List<Enemy> out) {
        return box(cx - reach, cy - reach, cx + reach, cy + reach, out);
    }

    /**
     * Candidates for anything touching the axis-aligned box (for example a beam's bounding box): every enemy whose
     * centre lies in the cells overlapping the box widened by the largest enemy radius. A superset, in list order.
     *
     * @param out cleared and filled; returned for convenience
     */
    public List<Enemy> box(double x0, double y0, double x1, double y1, List<Enemy> out) {
        out.clear();
        ensure();
        collect(x0, y0, x1, y1, 0, out);
        return out;
    }

    /** Appends the candidates with list index &ge; {@code fromIndex}, in list order. */
    private void collect(double x0, double y0, double x1, double y1, int fromIndex, List<Enemy> out) {
        int n = list.size();
        if (fromIndex >= n) return;
        double m = maxRadius;
        int c0 = col(x0 - m), c1 = col(x1 + m), r0 = row(y0 - m), r1 = row(y1 + m);
        if ((c1 - c0 + 1) * (r1 - r0 + 1) >= FULL_SCAN_FRACTION * COLS * ROWS) {
            for (int i = fromIndex; i < n; i++) out.add(list.get(i));
            return;
        }
        // mark the candidates' list indices in a bitmap, then emit them in increasing index order
        long[] mk = marks;
        int lo = Integer.MAX_VALUE, hi = -1;
        for (int c = c0; c <= c1; c++) {
            ArrayList<Enemy>[] column = cells[c];
            for (int r = r0; r <= r1; r++) {
                ArrayList<Enemy> b = column[r];
                for (int k = 0, bn = b.size(); k < bn; k++) {
                    int idx = b.get(k).gridIndex;
                    if (idx < fromIndex) continue;
                    mk[idx >>> 6] |= 1L << idx;
                    if (idx < lo) lo = idx;
                    if (idx > hi) hi = idx;
                }
            }
        }
        if (hi < 0) return;
        for (int w = lo >>> 6, wEnd = hi >>> 6; w <= wEnd; w++) {
            long bits = mk[w];
            if (bits == 0) continue;
            mk[w] = 0;
            while (bits != 0) {
                int idx = (w << 6) + Long.numberOfTrailingZeros(bits);
                out.add(list.get(idx));
                bits &= bits - 1;
            }
        }
    }

    /**
     * Opens an iteration over the candidates near (cx, cy) (see {@link #near}) for loops whose body may move enemies
     * (hits with knockback, teleports). Always {@link Scan#close() close} it (try / finally); scans are pooled.
     */
    public Scan scan(double cx, double cy, double reach) {
        return scanBox(cx - reach, cy - reach, cx + reach, cy + reach);
    }

    /** As {@link #scan} for an axis-aligned box. */
    public Scan scanBox(double x0, double y0, double x1, double y1) {
        Scan s = freeScans.isEmpty() ? new Scan() : freeScans.remove(freeScans.size() - 1);
        s.open(x0, y0, x1, y1);
        return s;
    }

    /**
     * An index-ordered walk over a box's candidates that stays exact while the loop body moves enemies: if any enemy
     * changed cell (or the list changed) since the candidates were gathered, the remaining candidates (those after the
     * last one returned) are gathered again from the current buckets. Equivalent to {@code for (i = 0; i < n; i++)}
     * over the whole list followed by the caller's exact test.
     */
    public final class Scan {
        private final ArrayList<Enemy> buf = new ArrayList<>(32);
        private double x0, y0, x1, y1;
        private int pos, stamp, version, nextIndex;
        private boolean open;

        private void open(double ax0, double ay0, double ax1, double ay1) {
            x0 = ax0; y0 = ay0; x1 = ax1; y1 = ay1;
            open = true;
            nextIndex = 0;
            refill();
        }

        private void refill() {
            buf.clear();
            pos = 0;
            ensure();
            collect(x0, y0, x1, y1, nextIndex, buf);
            stamp = cellStamp;
            version = list.version();
        }

        /** The next candidate in list order, or null when done. */
        public Enemy next() {
            if (stamp != cellStamp || version != list.version()) refill();
            if (pos >= buf.size()) return null;
            Enemy e = buf.get(pos++);
            nextIndex = e.gridIndex + 1;
            return e;
        }

        /** Returns the scan to the pool. */
        public void close() {
            if (!open) return;
            open = false;
            buf.clear();
            freeScans.add(this);
        }
    }
}
