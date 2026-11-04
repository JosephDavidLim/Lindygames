package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Rohan, the Cryo Warden: a frost guardian in a white fur-trimmed parka with a deep-blue hood, holding a crystal
 * cryo-staff whose prism head glows; three ice shards orbit him and frost mist pulses around the plate.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class RohanArt extends TowerArt {
    private static final long serialVersionUID = 1L;
    private static final Color PARKA = new Color(0xe8f6ff);
    private static final Color ICE = new Color(0x8fe6ff);

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public RohanArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        pulsingGlow(0, 0, r * 1.15, ICE, 30, 22, 0.8);
        // staff
        Turret tu = turret();
        double len = r * 1.2;
        tu.poly(Art.rect(-r * 0.1, -r * 0.05, len, r * 0.1), new Color(0x6d8fb3));
        tu.poly(new double[] { len - r * 0.05, 0, len + r * 0.12, -r * 0.2, len + r * 0.42, 0, len + r * 0.12, r * 0.2 }, ICE);
        tu.plain(new double[] { len + r * 0.08, 0, len + r * 0.14, -r * 0.08, len + r * 0.3, 0, len + r * 0.14, r * 0.08 }, Color.WHITE);
        tu.poly(Art.rect(len * 0.55, -r * 0.13, r * 0.12, r * 0.26), sec);
        tu.setFlash(tu.plain(Art.star(len + r * 0.55, 0, r * 0.38, r * 0.12, 6, 0), Art.alpha(Color.WHITE, 230)));
        HeroFigure.draw(this, r, HeroFigure.SKIN_BROWN, sec, HeroFigure.Hair.HOOD, PARKA, sec);
        // fur trim collar
        for (int i = 0; i < 7; i++) disc(-r * 0.48 + i * r * 0.16, r * 0.0, r * 0.08, Color.WHITE);
        // snowflake emblem
        plain(Art.star(r * 0.32, r * 0.3, r * 0.12, r * 0.04, 6, 0), ICE);
        // orbiting ice shards
        for (int i = 0; i < 3; i++) {
            acm.graphics.GCompound g = orbitingOrb(0, 0, r * 1.05, r * 0.08, i * Math.PI * 2 / 3, 1.3, ICE);
            Art.plain(g, Art.star(0, 0, r * 0.16, r * 0.06, 4, 45), Art.alpha(Color.WHITE, 200));
        }
        sparkle(-r * 0.45, -r * 0.55, r * 0.09);
    }
}
