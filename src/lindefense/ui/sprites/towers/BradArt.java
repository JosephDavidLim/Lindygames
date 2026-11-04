package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Brad Lindemann, the Commander: a black-and-gold command mech with fringed
 * epaulettes, rank stars and a howitzer. Captain adds a muzzle brake and
 * second star, Colonel twin cannons and a radar, General triple cannons and a
 * banner, and the Supreme Commander superclass is a towering four-cannon
 * siege platform under a crown and standard, built to read as the strongest
 * unit on the field.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class BradArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    private static final Color BLACK = new Color(0x1c1c22);
    private static final Color BLACK_LIGHT = new Color(0x34343e);

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public BradArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double scale = 1 + 0.07 * tier;
        double w = r * 1.35 * scale, h = r * 1.05 * scale, top = -r * 0.62 * scale;
        // banner (tier 3+)
        if (tier >= 3) {
            double bx = -w * 0.62, by = top - r * 0.9;
            poly(Art.bar(bx, by, bx, top + r * 0.3, r * 0.05), Art.GOLD_DARK);
            poly(new double[] { bx, by, bx + r * 0.7, by + r * 0.15, bx + r * 0.55, by + r * 0.3, bx + r * 0.7, by + r * 0.45, bx, by + r * 0.6 }, tier >= 4 ? Art.GOLD : BLACK);
            plain(Art.star(bx + r * 0.3, by + r * 0.3, r * 0.1, r * 0.04, 5, 0), tier >= 4 ? BLACK : Art.GOLD);
        }
        // radar (tier 2+)
        if (tier >= 2) {
            antenna(w * 0.45, top, top - r * 0.35, Art.METAL);
            spinningPoly(new double[] { 0, 0, -r * 0.3, -r * 0.18, -r * 0.34, 0, -r * 0.3, r * 0.18 }, w * 0.45, top - r * 0.35, Art.GOLD_LIGHT, 150);
        }
        // tracks / treads
        box(-w * 0.62, top + h * 0.3, r * 0.26, h * 0.85, BLACK_LIGHT);
        box(w * 0.62 - r * 0.26, top + h * 0.3, r * 0.26, h * 0.85, BLACK_LIGHT);
        for (int i = 0; i < 3; i++) {
            plain(Art.rect(-w * 0.6, top + h * 0.38 + i * h * 0.25, r * 0.22, r * 0.05), Art.GOLD_DARK);
            plain(Art.rect(w * 0.62 - r * 0.24, top + h * 0.38 + i * h * 0.25, r * 0.22, r * 0.05), Art.GOLD_DARK);
        }
        // hull with gold trim
        poly(Art.rect(-w / 2 - r * 0.05, top - r * 0.05, w + r * 0.1, h + r * 0.1), Art.GOLD);
        bodyBox(-w / 2, top, w, h, BLACK);
        // epaulettes with fringes
        for (int s = -1; s <= 1; s += 2) {
            ellipse(s * w * 0.42, top + r * 0.12, r * 0.26, r * 0.14, Art.GOLD);
            for (int i = 0; i < 3; i++) plain(Art.rect(s * w * 0.42 - r * 0.2 + i * r * 0.17, top + r * 0.2, r * 0.07, r * 0.18), Art.GOLD_LIGHT);
        }
        // rank stars
        int stars = Math.min(5, 1 + tier);
        for (int i = 0; i < stars; i++) {
            double sx = -(stars - 1) * r * 0.16 / 1 / 2 + i * r * 0.16;
            plain(Art.star(sx, top + h * 0.68, r * 0.09, r * 0.04, 5, 0), Art.GOLD);
        }
        // visor
        visor(0, top + h * 0.35, w * 0.55, r * 0.16, Art.GOLD);
        // artillery
        Turret tu = turret();
        double len = r * (1.2 + 0.3 * tier), hw = r * (0.16 + 0.015 * tier);
        int cannons = tier >= 4 ? 4 : tier >= 3 ? 3 : tier >= 2 ? 2 : 1;
        for (int c = 0; c < cannons; c++) {
            double off = (c - (cannons - 1) / 2.0) * hw * 2.3;
            double bl = len * (cannons >= 3 && Math.abs(c - (cannons - 1) / 2.0) > 0.6 ? 0.85 : 1);
            tu.poly(Art.rect(-r * 0.25, off - hw, bl + r * 0.25, 2 * hw), Art.METAL);
            tu.poly(Art.rect(bl * 0.45, off - hw * 1.3, r * 0.14, hw * 2.6), Art.GOLD);
            if (tier >= 1) tu.poly(Art.rect(bl - r * 0.12, off - hw * 1.4, r * 0.3, hw * 2.8), Art.GOLD);
            else tu.poly(Art.rect(bl - r * 0.1, off - hw * 1.15, r * 0.2, hw * 2.3), Art.GOLD_DARK);
        }
        // mantlet
        tu.poly(Art.rect(-r * 0.35, -hw * (cannons * 1.2 + 0.6), r * 0.7, hw * (cannons * 2.4 + 1.2)), BLACK_LIGHT);
        tu.plain(Art.rect(-r * 0.3, -hw * (cannons * 1.2 + 0.3), r * 0.12, hw * (cannons * 2.4 + 0.6)), Art.GOLD);
        if (tier >= 4) {
            tu.spinner(Art.star(0, 0, hw * 2.2, hw * 1.4, 6, 0), len * 0.75, 0, Art.GOLD_LIGHT);
            tu.setSpinSpeed(120);
        }
        tu.setFlash(tu.plain(Art.star(len + r * 0.5, 0, r * 0.45, r * 0.18, 8, 0), Art.alpha(Art.rgb(0xffe08a), 240)));
        poly(Art.regular(0, 0, r * 0.26, 8, 22.5), Art.GOLD);
        plain(Art.star(0, 0, r * 0.14, r * 0.06, 5, 0), BLACK);
        if (tier >= 4) {
            crown(0, top - r * 0.2, r * 0.32, 7);
            pulsingGlow(0, 0, r * 1.2, Art.GOLD, 40, 30, 1.2);
        }
        sparkle(-w * 0.3, top + r * 0.1, r * 0.1);
    }
}
