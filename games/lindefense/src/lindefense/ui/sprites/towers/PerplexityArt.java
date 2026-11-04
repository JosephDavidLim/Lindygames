package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Perplexity, the Research Engine: a teal scholar-bot peering through a big
 * magnifying lens with a sweeping radar dish on its head. Pro stacks source
 * books, Reasoning Pro adds a second lens and antenna array, Deep Research
 * unfolds satellite panels, and the Oracle superclass becomes a great
 * all-seeing lens radiating golden rays with lenses orbiting it.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class PerplexityArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public PerplexityArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double br = r * (0.62 + 0.04 * tier), cy = -r * 0.05;
        // golden rays (tier 4)
        if (tier >= 4) spinningPoly(Art.star(0, 0, r * 1.4, r * 0.9, 12, 0), 0, cy, Art.alpha(Art.GOLD, 170), 20);
        // satellite panels (tier 3+)
        if (tier >= 3) {
            for (int s = -1; s <= 1; s += 2) {
                poly(Art.bar(s * br * 0.9, cy, s * br * 1.5, cy, r * 0.05), Art.METAL);
                poly(Art.rect(s * br * 1.5 - r * 0.2, cy - r * 0.45, r * 0.4, r * 0.9), Art.rgb(0x1f3b8f));
                for (int i = 0; i < 3; i++) plain(Art.rect(s * br * 1.5 - r * 0.15, cy - r * 0.38 + i * r * 0.28, r * 0.3, r * 0.18), Art.rgb(0x4d7cff));
            }
        }
        // source books (tier 1+)
        if (tier >= 1) {
            Color[] bk = { Art.rgb(0xff7f6e), Art.rgb(0xffd166), Art.rgb(0x7ee0a0) };
            for (int i = 0; i < Math.min(3, tier); i++) box(br * 0.75, r * 0.55 - i * r * 0.16, r * 0.5, r * 0.14, bk[i]);
        }
        body(0, cy, br, pri);
        // magnifying lens
        double lx = tier >= 4 ? 0 : -br * 0.1, ly = tier >= 4 ? cy : cy + br * 0.15, lr = tier >= 4 ? br * 0.75 : br * 0.5;
        poly(Art.bar(lx + lr * 0.7, ly + lr * 0.7, lx + lr * 1.4, ly + lr * 1.4, r * 0.08), tier >= 4 ? Art.GOLD : Art.rgb(0x8a5a2b));
        disc(lx, ly, lr, tier >= 4 ? Art.GOLD : Art.METAL_LIGHT);
        plain(Art.circle(lx, ly, lr * 0.75, 20), Art.alpha(secLight, 220));
        plain(Art.circle(lx + lr * 0.05, ly + lr * 0.05, lr * 0.4, 14), tier >= 4 ? Art.GOLD_DARK : priDark);
        plain(Art.circle(lx + lr * 0.08, ly + lr * 0.08, lr * 0.18, 8), Art.INK);
        gloss(lx - lr * 0.3, ly - lr * 0.35, lr * 0.3, lr * 0.16);
        // second lens (tier 2+)
        if (tier >= 2 && tier < 4) {
            disc(br * 0.55, cy - br * 0.45, br * 0.28, Art.METAL_LIGHT);
            plain(Art.circle(br * 0.55, cy - br * 0.45, br * 0.2, 12), Art.alpha(secLight, 220));
            plain(Art.circle(br * 0.58, cy - br * 0.42, br * 0.08, 8), priDark);
        }
        // antenna array (tier 2+)
        if (tier >= 2) {
            antenna(-br * 0.85, cy - br * 0.5, cy - br * 1.2, Art.rgb(0xffd166));
            if (tier >= 3) antenna(-br * 0.6, cy - br * 0.7, cy - br * 1.45, Art.rgb(0xff7f6e));
        }
        // radar dish on the head
        antenna(br * 0.1, cy - br * 0.85, cy - br * 1.15, Art.METAL);
        spinningPoly(new double[] { 0, 0, -r * 0.38, -r * 0.22, -r * 0.42, 0, -r * 0.38, r * 0.22 }, br * 0.1, cy - br * 1.15, secLight, tier >= 4 ? 240 : 120);
        // orbiting lenses (tier 4)
        if (tier >= 4) {
            for (int i = 0; i < 3; i++) orbitingOrb(0, cy, r * 1.3, r * 0.14, i * 2 * Math.PI / 3, 1.0, secLight);
        }
        // search-pin emitter
        Turret tu = turret();
        double len = r * (0.8 + 0.18 * tier), hw = r * 0.1;
        tu.poly(Art.rect(-r * 0.1, -hw, len + r * 0.1, 2 * hw), tier >= 4 ? Art.GOLD : priDark);
        tu.poly(Art.circle(len, 0, hw * 1.8, 12), secLight);
        tu.poly(Art.wedge(len + hw, len + hw * 3.5, hw * 1.2, 0), secLight);
        tu.setFlash(tu.plain(Art.star(len + r * 0.4, 0, r * 0.28, r * 0.1, 4, 0), Art.alpha(Color.WHITE, 230)));
        disc(0, 0, r * 0.15, priDark);
        if (tier >= 4) crown(0, cy - br * 1.45, r * 0.24, 5);
        sparkle(-br * 0.55, cy - br * 0.55, r * 0.09);
    }
}
