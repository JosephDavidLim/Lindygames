package lindefense.ui.sprites.towers;

import acm.graphics.GCompound;
import lindefense.tower.TowerType;

/**
 * Static tower icons for the shop and the info panel. An icon is the tower's
 * full body art (including its tier-4 aura) fitted into a {@code size x size}
 * box centred on (0,0), facing straight up.
 */
public final class TowerIcons {
    private TowerIcons() { }

    /** Widest extent of a body in footprint radii (the tier-4 aura ring). */
    private static final double EXTENT = 1.9;

    /**
     * Creates an icon.
     *
     * @param type tower type
     * @param tier game tier (0-3, clamped); drawn with art stages {0, 2, 3, 4}
     * @param size box side in logical units
     * @return a compound whose content fits in the box centred at (0,0)
     */
    public static GCompound create(TowerType type, int tier, double size) {
        int t = TowerArtFactory.artStage(tier);   // tier is a game tier (0-3)
        // tier 4 bodies carry an aura ring that needs room; lower tiers can be drawn larger
        double extent = t >= 4 ? EXTENT : 1.45;
        double r = size / (2 * extent);
        TowerArt art = TowerArtFactory.create(type, t, r);
        art.animate(0, 0);
        GCompound g = new GCompound();
        g.add(art, 0, 0);
        return g;
    }
}
