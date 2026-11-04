package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * DeepSeek, the Deep Strike Railgun: a navy diving-bell bot with three glowing
 * portholes and a periscope, mounting a long electric-blue railgun. Flash
 * tiers add charge coils and a sonar dish, R1 twin rails, V4 Pro a triple
 * expert battery, and the Abyss superclass is a dark abyssal hulk with an
 * anglerfish lure and gold-ringed triple railguns.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class DeepSeekArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public DeepSeekArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double br = r * (0.7 + 0.04 * tier);
        Color hull = tier >= 4 ? Art.shade(pri, 0.6) : pri;
        // side ballast tanks
        ellipse(-br * 0.95, r * 0.05, r * 0.22, r * 0.38, Art.shade(hull, 0.85));
        ellipse(br * 0.95, r * 0.05, r * 0.22, r * 0.38, Art.shade(hull, 0.85));
        body(0, -r * 0.1, br, hull);
        // rivet band
        plain(Art.rect(-br * 0.85, r * 0.12, br * 1.7, r * 0.1), Art.shade(hull, 0.65));
        // portholes
        for (int i = -1; i <= 1; i++) {
            disc(i * br * 0.42, r * 0.38, r * 0.12, Art.METAL_LIGHT);
            plain(Art.circle(i * br * 0.42, r * 0.38, r * 0.07, 8), tier >= 4 ? Art.GOLD_LIGHT : secLight);
        }
        // periscope / lure
        if (tier >= 4) {
            antenna(br * 0.5, -br * 0.8, -br * 1.55, Art.GOLD_LIGHT);
            pulsingGlow(br * 0.5, -br * 1.55, r * 0.3, Art.GOLD_LIGHT, 90, 70, 2.0);
        } else {
            poly(Art.bar(br * 0.5, -br * 0.8, br * 0.5, -br * 1.25, r * 0.06), Art.METAL);
            poly(Art.rect(br * 0.5 - r * 0.05, -br * 1.35, r * 0.28, r * 0.14), Art.METAL_LIGHT);
        }
        // sonar dish (tier 2+)
        if (tier >= 2) {
            poly(new double[] { -br * 0.9, -br * 0.9, -br * 0.4, -br * 0.9, -br * 0.65, -br * 0.55 }, secLight);
            poly(Art.bar(-br * 0.65, -br * 0.55, -br * 0.65, -br * 0.3, r * 0.05), Art.METAL);
        }
        // railgun battery
        Turret tu = turret();
        double len = r * (1.15 + 0.25 * tier), hw = r * 0.12;
        int barrels = tier >= 3 ? 3 : 1;
        Color rail = tier >= 4 ? Art.GOLD : sec;
        for (int b = 0; b < barrels; b++) {
            double ang = barrels == 3 ? (b - 1) * 14 : 0;
            double bl = barrels == 3 && b != 1 ? len * 0.8 : len;
            tu.poly(Art.spin(Art.rect(-r * 0.2, -hw, bl + r * 0.2, 2 * hw), ang), priDark);
            tu.poly(Art.spin(Art.rect(r * 0.05, -hw * 1.5, bl * 0.75, hw * 0.55), ang), rail);
            tu.poly(Art.spin(Art.rect(r * 0.05, hw * 0.95, bl * 0.75, hw * 0.55), ang), rail);
            tu.poly(Art.spin(Art.rect(bl - r * 0.05, -hw * 1.3, r * 0.25, hw * 2.6), ang), Art.METAL_LIGHT);
            if (tier >= 1) {
                for (int c = 0; c < 2 + (tier >= 4 ? 1 : 0); c++) {
                    double cx = bl * (0.3 + c * 0.2);
                    tu.poly(Art.spin(Art.rect(cx, -hw * 1.9, r * 0.14, hw * 3.8), ang), tier >= 4 ? Art.GOLD_LIGHT : secLight);
                }
            }
        }
        tu.poly(Art.circle(0, 0, r * 0.3, 14), tier >= 4 ? Art.GOLD : Art.METAL);
        tu.gloss(-r * 0.08, -r * 0.1, r * 0.12, r * 0.08);
        tu.setFlash(tu.plain(Art.star(len + r * 0.4, 0, r * 0.35, r * 0.14, 4, 0), Art.alpha(secLight, 240)));
        if (tier >= 4) {
            pulsingGlow(0, -r * 0.1, r * 0.95, sec, 30, 25, 1.5);
            crown(0, -br * 1.1, r * 0.25, 5);
        }
        sparkle(-br * 0.6, -br * 0.6, r * 0.1);
    }
}
