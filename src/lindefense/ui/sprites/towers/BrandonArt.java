package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Brandon, the Rapid-Fire Specialist: a lean gunner in a lime-and-black tactical suit with spiky hair, wraparound
 * targeting goggles and a bandolier, aiming a spinning six-barrel volley blaster.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class BrandonArt extends TowerArt {
    private static final long serialVersionUID = 1L;
    private static final Color SUIT = new Color(0x1e2a22);

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public BrandonArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        // ammo canister on the back
        box(-r * 0.32, r * 0.3, r * 0.64, r * 0.32, Art.METAL);
        plain(Art.rect(-r * 0.28, r * 0.36, r * 0.56, r * 0.08), pri);
        // blaster
        Turret tu = turret();
        double len = r * 1.25;
        tu.poly(Art.rect(-r * 0.15, -r * 0.2, r * 0.55, r * 0.4), SUIT);
        tu.poly(Art.rect(r * 0.32, -r * 0.24, r * 0.2, r * 0.48), pri);
        for (int i = -1; i <= 1; i++) tu.poly(Art.rect(r * 0.5, i * r * 0.12 - r * 0.045, len - r * 0.5, r * 0.09), Art.METAL_LIGHT);
        tu.spinner(Art.regular(0, 0, r * 0.2, 6, 0), len * 0.62, 0, Art.shade(pri, 0.8));
        tu.setSpinSpeed(900);
        tu.plain(Art.rect(len - r * 0.08, -r * 0.24, r * 0.1, r * 0.48), pri);
        tu.setFlash(tu.plain(Art.star(len + r * 0.25, 0, r * 0.32, r * 0.1, 5, 0), Art.alpha(pri, 235)));
        double hy = HeroFigure.draw(this, r, HeroFigure.SKIN_TAN, new Color(0x2a1a10), HeroFigure.Hair.SPIKY, SUIT, pri);
        // bandolier
        poly(Art.bar(-r * 0.55, -r * 0.05, r * 0.45, r * 0.45, r * 0.07), Art.shade(pri, 0.7));
        for (int i = 0; i < 4; i++) plain(Art.circle(-r * 0.4 + i * r * 0.26, r * 0.03 + i * r * 0.13, r * 0.05, 6), Art.GOLD);
        // goggles
        box(-r * 0.36, hy - r * 0.06, r * 0.72, r * 0.16, Art.INK);
        plain(Art.rect(-r * 0.3, hy - r * 0.03, r * 0.24, r * 0.1), pri);
        plain(Art.rect(r * 0.06, hy - r * 0.03, r * 0.24, r * 0.1), pri);
        pulsingGlow(0, hy, r * 0.42, pri, 30, 25, 3);
        sparkle(r * 0.5, -r * 0.55, r * 0.08);
    }
}
