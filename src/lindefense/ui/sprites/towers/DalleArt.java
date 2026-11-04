package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * DALL·E, the Image Generator: a round tangerine painter-bot with a beret,
 * a paint palette and a brush it flicks paint from. Each tier adds a bigger
 * brush, a sprite summoning portal ring, a canvas easel, and the Renaissance
 * superclass stands inside an ornate gold frame halo with rainbow orbs.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class DalleArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    private static final int[] PAINTS = { 0xff5fa2, 0xffd166, 0x5bc0eb, 0x7ee0a0, 0x9b5cff };

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public DalleArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double br = r * (0.66 + 0.03 * tier);
        // Renaissance frame halo
        if (tier >= 4) {
            poly(Art.rect(-r * 1.25, -r * 1.3, r * 2.5, r * 2.2), Art.GOLD);
            plain(Art.rect(-r * 1.1, -r * 1.15, r * 2.2, r * 1.9), Art.rgb(0x6c4a1e));
            plain(Art.rect(-r * 1.02, -r * 1.07, r * 2.04, r * 1.74), Art.rgb(0xfbe9c2));
            for (int i = 0; i < 4; i++) {
                double sx = (i % 2 == 0 ? -1 : 1) * r * 1.25, sy = (i < 2 ? -1 : 1) * r * (i < 2 ? 1.3 : 0.9);
                plain(Art.star(sx, sy, r * 0.22, r * 0.1, 4, 0), Art.GOLD_LIGHT);
            }
        }
        // canvas easel (tier 3+)
        if (tier >= 3 && tier < 4) {
            poly(Art.rect(-r * 1.05, -r * 1.05, r * 0.7, r * 0.85), Art.rgb(0x8a5a2b));
            plain(Art.rect(-r * 0.98, -r * 0.98, r * 0.56, r * 0.7), Color.WHITE);
            plain(Art.circle(-r * 0.7, -r * 0.65, r * 0.12, 8), Art.rgb(PAINTS[0]));
            plain(Art.rect(-r * 0.9, -r * 0.5, r * 0.2, r * 0.1), Art.rgb(PAINTS[2]));
        }
        // portal ring(s) (tier 2+)
        if (tier >= 2) {
            spinningRing(br * 1.25, r * 0.15, r * 0.3, r * 0.2, 10, sec, 200);
            pulsingGlow(br * 1.25, r * 0.15, r * 0.22, sec, 90, 60, 1.5);
            if (tier >= 3) {
                spinningRing(-br * 1.25, r * 0.15, r * 0.3, r * 0.2, 10, sec, -200);
                pulsingGlow(-br * 1.25, r * 0.15, r * 0.22, sec, 90, 60, 1.5);
            }
        }
        body(0, -r * 0.05, br, pri);
        // palette
        ellipse(-br * 0.55, br * 0.45, r * 0.38, r * 0.26, Art.rgb(0xe8c89a));
        int dots = 3 + Math.min(2, tier);
        for (int i = 0; i < dots; i++) {
            plain(Art.circle(-br * 0.55 - r * 0.24 + i * r * 0.12, br * 0.45 - r * 0.06 + (i % 2) * r * 0.1, r * 0.055, 8), Art.rgb(PAINTS[i]));
        }
        // beret
        ellipse(br * 0.25, -br * 0.78, r * 0.42, r * 0.17, tier >= 4 ? Art.GOLD : sec);
        plain(Art.circle(br * 0.42, -br * 0.95, r * 0.06, 8), Art.INK);
        eye(-br * 0.22, -br * 0.3, r * 0.19, sec);
        eye(br * 0.22, -br * 0.3, r * 0.19, sec);
        // rainbow orbs (tier 4)
        if (tier >= 4) {
            for (int i = 0; i < 5; i++) orbitingOrb(0, -r * 0.05, r * 1.35, r * 0.12, i * 2 * Math.PI / 5, 1.1, Art.rgb(PAINTS[i]));
        }
        // paintbrush
        Turret tu = turret();
        double len = r * (0.9 + 0.2 * tier), hw = r * 0.08;
        tu.poly(Art.rect(-r * 0.3, -hw, len * 0.7 + r * 0.3, 2 * hw), tier >= 4 ? Art.GOLD : Art.rgb(0x8a5a2b));
        tu.poly(Art.rect(len * 0.68, -hw * 1.5, len * 0.14, hw * 3), Art.METAL_LIGHT);
        tu.poly(new double[] { len * 0.8, -hw * 1.6, len + r * 0.25, 0, len * 0.8, hw * 1.6 }, Art.rgb(PAINTS[tier % PAINTS.length]));
        if (tier >= 1) {
            tu.poly(new double[] { len * 0.6, -hw * 1.4, len * 0.6, -hw * 4.0, len * 0.9, -hw * 2.2 }, Art.rgb(PAINTS[(tier + 1) % PAINTS.length]));
            tu.poly(new double[] { len * 0.6, hw * 1.4, len * 0.6, hw * 4.0, len * 0.9, hw * 2.2 }, Art.rgb(PAINTS[(tier + 2) % PAINTS.length]));
        }
        tu.setFlash(tu.plain(Art.star(len + r * 0.35, 0, r * 0.3, r * 0.12, 5, 0), Art.alpha(Color.WHITE, 230)));
        disc(0, 0, r * 0.15, priDark);
        sparkle(-br * 0.6, -br * 0.55, r * 0.1);
    }
}
