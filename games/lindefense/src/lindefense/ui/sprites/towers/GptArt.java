package lindefense.ui.sprites.towers;

import acm.graphics.GRect;
import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * GPT, the Adaptive Generalist: a tidy white box-bot with a green visor band
 * and a hex-nut emblem that fires token bolts from a stubby green cannon.
 * Terra adds a context dish, Luna adaptive module chips, Sol twin cannons, and
 * the Cosmos superclass becomes a deep-green starfield sphere wearing a white
 * orbital halo and gold-tipped twin cannons.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class GptArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public GptArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double w = r * (1.3 + 0.05 * tier), h = r * 1.15;
        if (tier >= 4) {
            // Cosmos: starfield sphere
            body(0, -r * 0.1, r * 0.8, Art.shade(pri, 0.45));
            for (int i = 0; i < 7; i++) {
                double a = i * 0.9 + 0.3, d = r * (0.25 + 0.07 * (i % 3));
                plain(Art.star(Math.cos(a) * d, -r * 0.1 + Math.sin(a) * d, r * 0.07, r * 0.025, 4, 0), Color.WHITE);
            }
            visor(0, -r * 0.3, r * 1.0, r * 0.22, pri);
            spinningRing(0, -r * 0.1, r * 1.08, r * 0.96, 16, Color.WHITE, 30);
        } else {
            GRect b = bodyBox(-w / 2, -h / 2 - r * 0.1, w, h, Color.WHITE);
            b.setFillColor(Art.shade(sec, 0.98));
            visor(0, -r * 0.3, w * 0.8, r * 0.22, pri);
            poly(Art.regular(0, r * 0.22, r * 0.2, 6, 30), pri);
            plain(Art.regular(0, r * 0.22, r * 0.09, 6, 30), Color.WHITE);
        }
        // tier 1+: context dish
        if (tier >= 1) {
            antenna(-w * 0.42, -h * 0.5 - r * 0.1, -h * 0.5 - r * 0.55, sec);
            poly(new double[] { -w * 0.42 - r * 0.22, -h * 0.5 - r * 0.55, -w * 0.42 + r * 0.22, -h * 0.5 - r * 0.55, -w * 0.42, -h * 0.5 - r * 0.3 }, priLight);
        }
        // tier 2+: adaptive module chips
        if (tier >= 2) {
            Color[] chips = { pri, Art.rgb(0x2fd4b3), Art.rgb(0xffcf5a) };
            for (int i = 0; i < 3; i++) box(w * 0.5 - r * 0.02, -r * 0.45 + i * r * 0.3, r * 0.22, r * 0.2, chips[i]);
        }
        // cannons
        Turret tu = turret();
        double len = r * (0.85 + 0.15 * tier), hw = r * 0.14;
        Color barrel = tier >= 4 ? priDark : pri;
        if (tier >= 3) {
            tu.poly(Art.rect(-r * 0.2, -hw * 2.4, len + r * 0.2, hw * 1.6), barrel);
            tu.poly(Art.rect(-r * 0.2, hw * 0.8, len + r * 0.2, hw * 1.6), barrel);
            tu.poly(Art.rect(len - r * 0.12, -hw * 2.7, r * 0.25, hw * 2.2), tier >= 4 ? Art.GOLD : Color.WHITE);
            tu.poly(Art.rect(len - r * 0.12, hw * 0.5, r * 0.25, hw * 2.2), tier >= 4 ? Art.GOLD : Color.WHITE);
            tu.poly(Art.rect(-r * 0.1, -hw * 1.2, r * 0.6, hw * 2.4), Art.METAL);
        } else {
            tu.poly(Art.rect(-r * 0.2, -hw, len + r * 0.2, 2 * hw), barrel);
            tu.poly(Art.rect(len - r * 0.12, -hw * 1.5, r * 0.25, hw * 3), Color.WHITE);
            tu.gloss(len * 0.3, -hw * 0.3, len * 0.25, hw * 0.3);
        }
        tu.setFlash(tu.plain(Art.star(len + r * 0.35, 0, r * 0.3, r * 0.12, 5, 0), Art.alpha(Color.WHITE, 230)));
        disc(0, 0, r * 0.22, tier >= 4 ? Art.GOLD : Art.METAL_LIGHT);
        if (tier >= 4) crown(0, -r * 1.0, r * 0.26, 5);
        sparkle(-w * 0.3, -h * 0.45 - r * 0.1, r * 0.1);
    }
}
