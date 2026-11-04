package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Shashank, the Data Strategist: a sharp analyst in a dark-green suit with a gold tie and glasses, projecting a
 * holographic data tablet; a gold coin and three bar-chart bars orbit, and data rings pulse outward.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class ShashankArt extends TowerArt {
    private static final long serialVersionUID = 1L;
    private static final Color SUIT = new Color(0x1d3b2c);

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public ShashankArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        pulseWave(pri, 2.2);
        // hologram tablet
        Turret tu = turret();
        double d = r * 0.85;
        tu.poly(Art.rect(r * 0.2, -r * 0.05, d * 0.5, r * 0.1), Art.METAL);
        tu.plain(new double[] { d * 0.7, -r * 0.4, d + r * 0.45, -r * 0.55, d + r * 0.45, r * 0.55, d * 0.7, r * 0.4 }, Art.alpha(pri, 110));
        tu.poly(Art.rect(d * 0.6, -r * 0.42, r * 0.12, r * 0.84), Art.shade(pri, 0.8));
        for (int i = 0; i < 3; i++) tu.plain(Art.rect(d + r * 0.05 + i * r * 0.12, r * 0.3 - (i + 1) * r * 0.16, r * 0.08, (i + 1) * r * 0.16), Color.WHITE);
        tu.setFlash(tu.plain(Art.star(d + r * 0.7, 0, r * 0.3, r * 0.1, 4, 0), Art.alpha(pri, 230)));
        double hy = HeroFigure.draw(this, r, HeroFigure.SKIN_BROWN, new Color(0x1a1412), HeroFigure.Hair.NEAT, SUIT, Color.WHITE);
        // tie
        poly(new double[] { -r * 0.06, r * 0.0, r * 0.06, r * 0.0, r * 0.08, r * 0.35, 0, r * 0.45, -r * 0.08, r * 0.35 }, Art.GOLD);
        // glasses
        poly(Art.ring(-r * 0.15, hy + r * 0.04, r * 0.12, r * 0.08, 12), Art.INK);
        poly(Art.ring(r * 0.15, hy + r * 0.04, r * 0.12, r * 0.08, 12), Art.INK);
        plain(Art.rect(-r * 0.04, hy + r * 0.02, r * 0.08, r * 0.03), Art.INK);
        // orbiting coin and data bars
        acm.graphics.GCompound coin = orbitingOrb(0, -r * 0.1, r * 1.1, r * 0.16, 0, 1.1, Art.GOLD);
        Art.plain(coin, Art.rect(-r * 0.03, -r * 0.09, r * 0.06, r * 0.18), Art.GOLD_DARK);
        orbitingOrb(0, -r * 0.1, r * 1.1, r * 0.09, Math.PI * 2 / 3, 1.1, pri);
        orbitingOrb(0, -r * 0.1, r * 1.1, r * 0.09, Math.PI * 4 / 3, 1.1, Art.shade(pri, 1.3));
        sparkle(-r * 0.5, -r * 0.5, r * 0.08);
    }
}
