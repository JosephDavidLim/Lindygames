package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * The DALL·E Sprite summon: a tiny buttercup paint-blob familiar with sky-blue
 * wings, one big eye and a paintbrush tail. It has no base plate and bobs in
 * the air; tier is ignored.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class SpriteArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public SpriteArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected boolean drawsPlate() { return false; }

    @Override protected void build() {
        double br = r * 0.7, cy = -r * 0.2;
        // wings
        poly(new double[] { -br * 0.6, cy, -br * 1.6, cy - br * 0.9, -br * 1.3, cy + br * 0.3 }, sec);
        poly(new double[] { br * 0.6, cy, br * 1.6, cy - br * 0.9, br * 1.3, cy + br * 0.3 }, sec);
        // brush tail
        poly(Art.bar(0, cy + br * 0.6, 0, cy + br * 1.5, r * 0.08), Art.rgb(0x8a5a2b));
        poly(new double[] { -r * 0.14, cy + br * 1.45, r * 0.14, cy + br * 1.45, 0, cy + br * 1.95 }, Art.rgb(0xff5fa2));
        body(0, cy, br, pri);
        eye(0, cy - br * 0.05, br * 0.42, sec);
        // paint drips
        plain(Art.circle(-br * 0.5, cy + br * 0.5, r * 0.1, 8), Art.rgb(0x7ee0a0));
        plain(Art.circle(br * 0.55, cy + br * 0.4, r * 0.08, 8), Art.rgb(0x9b5cff));
        Turret tu = turret();
        tu.poly(Art.wedge(r * 0.2, r * 0.9, r * 0.1, 0), Art.rgb(0xff5fa2));
        tu.setFlash(tu.plain(Art.star(r * 1.05, 0, r * 0.2, r * 0.08, 4, 0), Art.alpha(Color.WHITE, 220)));
        sparkle(-br * 0.5, cy - br * 0.55, r * 0.1);
    }

    @Override protected void animateExtra(double t, double dt) {
        // gentle hover bob of the whole figure
        setLocation(getX(), Math.sin(t * 4) * r * 0.12);
    }
}
