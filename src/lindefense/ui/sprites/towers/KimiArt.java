package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Kimi, the Long-Context Scaler: an indigo moon-marked sniper bot with a very
 * long scoped barrel. K2.6 adds a bipod and longer barrel, K3 a glowing scope
 * lens, K3.1 Pro a double-stacked barrel, and the Moonshot superclass mounts a
 * finned rocket-barrel under a lavender moon halo with stars circling.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class KimiArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public KimiArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double br = r * (0.62 + 0.03 * tier), cy = -r * 0.05;
        // moon halo (tier 4)
        if (tier >= 4) {
            spinningRing(0, cy, r * 1.3, r * 1.18, 20, Art.alpha(sec, 200), 15);
            for (int i = 0; i < 4; i++) {
                orbitingOrb(0, cy, r * 1.35, r * 0.08, i * Math.PI / 2 + 0.4, 0.9, i % 2 == 0 ? Art.GOLD_LIGHT : Color.WHITE);
            }
        }
        // bipod (tier 1+)
        if (tier >= 1) {
            poly(Art.bar(-br * 0.4, cy - br * 0.3, -br * 1.1, cy - br * 1.0, r * 0.06), Art.METAL);
            poly(Art.bar(br * 0.4, cy - br * 0.3, br * 1.1, cy - br * 1.0, r * 0.06), Art.METAL);
        }
        body(0, cy, br, pri);
        // crescent moon emblem
        plain(Art.circle(-br * 0.15, cy + br * 0.3, br * 0.36, 16), sec);
        plain(Art.circle(-br * 0.02, cy + br * 0.22, br * 0.3, 16), pri);
        plain(Art.star(br * 0.4, cy + br * 0.4, r * 0.08, r * 0.03, 4, 0), sec);
        // sniper visor
        visor(0, cy - br * 0.35, br * 1.1, r * 0.2, tier >= 4 ? Art.GOLD : sec);
        // barrel
        Turret tu = turret();
        double len = r * (1.3 + 0.3 * tier), hw = r * 0.075;
        Color metal = tier >= 4 ? Art.GOLD_DARK : priDark;
        int stack = tier >= 3 ? 2 : 1;
        for (int i = 0; i < stack; i++) {
            double off = stack == 2 ? (i == 0 ? -hw * 1.6 : hw * 1.6) : 0;
            tu.poly(Art.rect(-r * 0.3, off - hw, len + r * 0.3, 2 * hw), metal);
            tu.poly(Art.rect(len - r * 0.1, off - hw * 1.6, r * 0.22, hw * 3.2), sec);
        }
        // scope
        tu.poly(Art.rect(r * 0.05, -hw * (stack == 2 ? 4.2 : 3.2), r * 0.55, hw * 1.8), Art.METAL);
        tu.poly(Art.circle(r * 0.62, -hw * (stack == 2 ? 3.3 : 2.3), hw * 1.4, 10), tier >= 2 ? Art.rgb(0x7ee0a0) : Art.METAL_LIGHT);
        // stock
        tu.poly(Art.rect(-r * 0.45, -hw * 1.8, r * 0.3, hw * 3.6), sec);
        if (tier >= 4) {
            // rocket fins
            tu.poly(new double[] { len * 0.25, -hw, len * 0.45, -hw, len * 0.2, -hw * 4 }, Art.GOLD);
            tu.poly(new double[] { len * 0.25, hw, len * 0.45, hw, len * 0.2, hw * 4 }, Art.GOLD);
            tu.poly(Art.wedge(len + r * 0.1, len + r * 0.45, hw * 1.8, 0), Art.GOLD_LIGHT);
        }
        tu.setFlash(tu.plain(Art.star(len + r * 0.5, 0, r * 0.3, r * 0.1, 4, 0), Art.alpha(Color.WHITE, 240)));
        disc(0, 0, r * 0.16, tier >= 4 ? Art.GOLD : Art.METAL_LIGHT);
        if (tier >= 4) crown(0, cy - br * 1.05, r * 0.22, 5);
        sparkle(-br * 0.5, cy - br * 0.7, r * 0.09);
    }
}
