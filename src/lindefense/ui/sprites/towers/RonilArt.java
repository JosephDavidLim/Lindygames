package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Ronil, the Meteor Caller: a heavy artillerist in charcoal plate armour with magma-orange seams and a crested helmet,
 * shouldering a wide meteor mortar whose molten core glows; two rocks smoulder in orbit.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class RonilArt extends TowerArt {
    private static final long serialVersionUID = 1L;
    private static final Color PLATE = new Color(0x2c2522);
    private static final Color MAGMA = new Color(0xffb02e);

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public RonilArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        pulsingGlow(0, 0, r * 1.1, pri, 28, 20, 0.6);
        Turret tu = turret();
        double len = r * 1.1, hw = r * 0.3;
        tu.poly(Art.rect(-r * 0.4, -hw * 1.1, r * 0.55, hw * 2.2), PLATE);
        tu.poly(Art.rect(0, -hw, len, hw * 2), new Color(0x4a3a33));
        tu.poly(Art.rect(len - r * 0.1, -hw * 1.25, r * 0.3, hw * 2.5), pri);
        tu.plain(Art.circle(len + r * 0.06, 0, hw * 0.75, 12), MAGMA);
        tu.plain(Art.circle(len + r * 0.06, 0, hw * 0.4, 10), Color.WHITE);
        tu.plain(Art.rect(r * 0.2, -hw * 1.02, r * 0.08, hw * 2.04), pri);
        tu.plain(Art.rect(r * 0.5, -hw * 1.02, r * 0.08, hw * 2.04), pri);
        tu.setFlash(tu.plain(Art.star(len + r * 0.45, 0, r * 0.5, r * 0.2, 7, 0), Art.alpha(MAGMA, 240)));
        double hy = HeroFigure.draw(this, r, HeroFigure.SKIN_TAN, PLATE, HeroFigure.Hair.HELMET, PLATE, pri);
        // shoulder pauldrons with magma seams
        for (int s = -1; s <= 1; s += 2) {
            ellipse(s * r * 0.62, r * 0.08, r * 0.27, r * 0.22, Art.shade(PLATE, 1.3));
            plain(Art.rect(s * r * 0.62 - r * 0.16, r * 0.06, r * 0.32, r * 0.05), pri);
        }
        // helmet crest
        poly(new double[] { -r * 0.06, hy - r * 0.55, 0, hy - r * 0.9, r * 0.06, hy - r * 0.55 }, pri);
        // smouldering rocks
        orbitingOrb(0, 0, r * 1.1, r * 0.14, 0, 0.9, new Color(0x5a4038));
        orbitingOrb(0, 0, r * 1.1, r * 0.1, Math.PI, 0.9, pri);
        sparkle(r * 0.5, -r * 0.5, r * 0.08);
    }
}
