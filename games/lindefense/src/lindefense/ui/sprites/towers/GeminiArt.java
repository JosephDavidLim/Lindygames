package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Gemini, the Prism Chain: twin dome-headed bots, one blue and one violet,
 * flanking a tall crystal prism that aims at the chain's first link. Flash
 * tiers add arc antennae and orbiting shards; Argon crowns both twins; the
 * Ultra superclass raises a huge iridescent star crystal inside a gold ring.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class GeminiArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public GeminiArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double hx = r * 0.5, hy = r * 0.05, hr = r * (0.36 + 0.02 * tier);
        // bridge between twins
        box(-hx, hy - r * 0.15, 2 * hx, r * 0.3, Art.METAL);
        // twin heads
        body(-hx, hy, hr, pri);
        body(hx, hy, hr, sec);
        eye(-hx + hr * 0.1, hy - hr * 0.05, hr * 0.42, sec);
        eye(hx - hr * 0.1, hy - hr * 0.05, hr * 0.42, pri);
        if (tier >= 1) {
            // arc antennae
            antenna(-hx - hr * 0.5, hy - hr * 0.6, hy - hr * 1.5, sec);
            antenna(hx + hr * 0.5, hy - hr * 0.6, hy - hr * 1.5, pri);
        }
        if (tier >= 3) {
            crown(-hx, hy - hr * 1.05, r * 0.2, 3);
            crown(hx, hy - hr * 1.05, r * 0.2, 3);
        }
        // orbiting shards
        int shards = tier >= 4 ? 6 : tier >= 3 ? 4 : tier >= 2 ? 2 : 0;
        for (int i = 0; i < shards; i++) {
            orbitingOrb(0, -r * 0.1, r * (1.05 + 0.15 * (tier >= 4 ? 1 : 0)), r * 0.13, i * 2 * Math.PI / shards, 1.4,
                i % 2 == 0 ? pri : sec);
        }
        // central prism (aims)
        Turret tu = turret();
        double len = r * (0.9 + 0.2 * tier), hw = r * (0.22 + 0.03 * tier);
        if (tier >= 4) {
            tu.ringSpinner(r * 0.95, r * 0.78, 10, r * 0.1, 0, Art.GOLD);
            tu.setSpinSpeed(60);
            tu.poly(Art.star(0, 0, len * 0.75, hw * 1.1, 6, 90), Art.mix(pri, sec, 0.5));
            tu.poly(new double[] { -r * 0.35, 0, 0, -hw, len, 0, 0, hw }, pri);
            tu.poly(new double[] { 0, -hw * 0.5, len * 0.8, 0, 0, hw * 0.5 }, Art.shade(sec, 1.25));
            tu.gloss(len * 0.25, -hw * 0.3, len * 0.2, hw * 0.25);
        } else {
            tu.poly(new double[] { -r * 0.35, 0, 0, -hw, len, 0, 0, hw }, pri);
            tu.poly(new double[] { -r * 0.35, 0, 0, hw, len, 0 }, sec);
            tu.gloss(len * 0.2, -hw * 0.35, len * 0.2, hw * 0.25);
        }
        tu.setFlash(tu.plain(Art.star(len + r * 0.2, 0, r * 0.3, r * 0.12, 4, 0), Art.alpha(Color.WHITE, 230)));
        disc(0, 0, r * 0.18, tier >= 4 ? Art.GOLD : Art.METAL_LIGHT);
        sparkle(-hx - hr * 0.3, hy - hr * 0.6, r * 0.09);
    }
}
