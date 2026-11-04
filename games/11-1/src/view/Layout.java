package view;

/**
 * Pixel sizes shared by every screen. Game positions are in cell units and are
 * multiplied by {@link #CELL} to get pixels.
 */
public final class Layout {
    /** Width of one grid cell in pixels. */
    public static final int CELL = 80;
    /** Window width: nine columns. */
    public static final int W = 9 * CELL;
    /** Height of the shop bar above the lawn. */
    public static final int TOP = 90;
    /** Top edge of the status bar, just below the map. */
    public static final int BAR_Y = TOP + 5 * CELL;
    /** Height of the status bar. */
    public static final int BAR_H = 64;
    /** Window height: shop bar, five lanes and the status bar. */
    public static final int H = BAR_Y + BAR_H;

    private Layout() {
    }
}
