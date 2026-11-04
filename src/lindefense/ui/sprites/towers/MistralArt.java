package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Mistral, the Gale Caster: a sea-green wind-turbine bot with a whirring
 * front fan that flings wind blades. 14B spins a bigger four-blade fan,
 * Small 4 adds twin side fans, Large 4 a dashed wind-wall ring, and the
 * Tempest superclass whips a gold-edged cyclone swirl around itself.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class MistralArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public MistralArt(TowerType type, int tier, double r) { super(type, tier, r); }

    private static double[] blades(int n, double outer, double inner, double width) {
        double[] v = new double[n * 6];
        int k = 0;
        for (int i = 0; i < n; i++) {
            double a = i * 2 * Math.PI / n;
            v[k++] = Math.cos(a - width) * inner; v[k++] = Math.sin(a - width) * inner;
            v[k++] = Math.cos(a + width * 0.4) * outer; v[k++] = Math.sin(a + width * 0.4) * outer;
            v[k++] = Math.cos(a + width) * inner * 0.9; v[k++] = Math.sin(a + width) * inner * 0.9;
        }
        return v;
    }

    @Override protected void build() {
        double br = r * (0.62 + 0.03 * tier), cy = -r * 0.1;
        // cyclone swirl (tier 4)
        if (tier >= 4) {
            spinningPoly(blades(5, r * 1.45, r * 0.7, 0.55), 0, cy, Art.alpha(secLight, 150), 200);
            spinningPoly(blades(5, r * 1.25, r * 0.6, 0.55), 0, cy, Art.alpha(Art.GOLD, 150), -140);
        }
        // wind wall ring (tier 3+)
        if (tier >= 3) spinningRing(0, cy, r * 1.18, r * 1.08, 16, Art.alpha(sec, 190), 120);
        // side fans (tier 2+)
        if (tier >= 2) {
            for (int s = -1; s <= 1; s += 2) {
                disc(s * br * 1.25, cy + r * 0.15, r * 0.3, priDark);
                spinningPoly(blades(3, r * 0.26, r * 0.07, 0.6), s * br * 1.25, cy + r * 0.15, secLight, s * 700);
                plain(Art.circle(s * br * 1.25, cy + r * 0.15, r * 0.06, 8), Art.INK);
            }
        }
        body(0, cy, br, pri);
        eye(-br * 0.6, cy - br * 0.55, r * 0.13, pri);
        eye(br * 0.6, cy - br * 0.55, r * 0.13, pri);
        // main fan housing + blades
        double fr = br * (0.72 + 0.05 * Math.min(tier, 2));
        disc(0, cy + br * 0.1, fr, priDark);
        plain(Art.circle(0, cy + br * 0.1, fr * 0.88, 20), Art.shade(pri, 0.55));
        int n = tier >= 1 ? 4 : 3;
        spinningPoly(blades(n, fr * 0.82, fr * 0.2, 0.55), 0, cy + br * 0.1, tier >= 4 ? Art.GOLD_LIGHT : secLight, tier >= 3 ? 900 : 600);
        disc(0, cy + br * 0.1, fr * 0.2, tier >= 4 ? Art.GOLD : sec);
        // wind nozzle
        Turret tu = turret();
        double len = r * (0.75 + 0.18 * tier), hw = r * 0.1;
        tu.poly(Art.rect(-r * 0.1, -hw, len + r * 0.1, 2 * hw), priDark);
        tu.poly(new double[] { len - r * 0.05, -hw * 2.2, len + r * 0.25, -hw * 1.2, len + r * 0.25, hw * 1.2, len - r * 0.05, hw * 2.2 }, tier >= 4 ? Art.GOLD : secLight);
        tu.setFlash(tu.plain(Art.star(len + r * 0.45, 0, r * 0.28, r * 0.11, 3, 90), Art.alpha(Color.WHITE, 220)));
        if (tier >= 4) crown(0, cy - br * 1.05, r * 0.24, 5);
        sparkle(-br * 0.4, cy - br * 0.75, r * 0.09);
    }
}
