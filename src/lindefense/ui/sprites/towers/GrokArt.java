package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Grok, the Disruptor: an edgy jet-black hex-headed bot with slanted orange
 * eyes and a lightning-bolt grin, firing from a tesla-coil disruptor. 4.6 adds
 * a triple meme launcher, 4.20 Reasoning a crackling chaos ring, 4.7 horns,
 * and the Heavy superclass is a hulking twin-coil mech wreathed in orange flame.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class GrokArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public GrokArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double hr = r * (0.72 + 0.04 * tier), cy = -r * 0.1;
        Color shell = Art.rgb(0x262630);
        // flame aura (tier 4)
        if (tier >= 4) {
            pulsingGlow(0, cy, r * 1.3, sec, 60, 40, 2.5);
            spinningPoly(Art.star(0, 0, r * 1.3, r * 0.95, 9, 0), 0, cy, Art.alpha(sec, 150), 90);
        }
        // chaos ring (tier 2+)
        if (tier >= 2) spinningPoly(Art.gearRing(0, 0, hr * 1.35, hr * 1.15, hr * 1.05, 6), 0, cy, Art.alpha(sec, 200), -160);
        // shoulder pods
        poly(Art.regular(-hr * 0.95, cy + r * 0.2, r * 0.26, 6, 30), shell);
        poly(Art.regular(hr * 0.95, cy + r * 0.2, r * 0.26, 6, 30), shell);
        plain(Art.circle(-hr * 0.95, cy + r * 0.2, r * 0.09, 8), sec);
        plain(Art.circle(hr * 0.95, cy + r * 0.2, r * 0.09, 8), sec);
        // horns (tier 3+)
        if (tier >= 3) {
            poly(new double[] { -hr * 0.55, cy - hr * 0.75, -hr * 0.95, cy - hr * 1.45, -hr * 0.3, cy - hr * 0.95 }, tier >= 4 ? Art.GOLD : sec);
            poly(new double[] { hr * 0.55, cy - hr * 0.75, hr * 0.95, cy - hr * 1.45, hr * 0.3, cy - hr * 0.95 }, tier >= 4 ? Art.GOLD : sec);
        }
        // hex head
        poly(Art.regular(0, cy, hr, 6, 30), shell);
        plain(Art.shift(Art.regular(0, cy, hr * 0.8, 6, 30), -hr * 0.06, -hr * 0.06), Art.rgb(0x383846));
        gloss(-hr * 0.35, cy - hr * 0.42, hr * 0.3, hr * 0.16);
        // slanted eyes
        poly(new double[] { -hr * 0.55, cy - hr * 0.35, -hr * 0.15, cy - hr * 0.2, -hr * 0.15, cy - hr * 0.02, -hr * 0.55, cy - hr * 0.12 }, sec);
        poly(new double[] { hr * 0.55, cy - hr * 0.35, hr * 0.15, cy - hr * 0.2, hr * 0.15, cy - hr * 0.02, hr * 0.55, cy - hr * 0.12 }, sec);
        plain(Art.circle(-hr * 0.3, cy - hr * 0.26, r * 0.04, 6), Color.WHITE);
        plain(Art.circle(hr * 0.3, cy - hr * 0.26, r * 0.04, 6), Color.WHITE);
        // lightning grin
        plain(Art.shift(Art.bolt(-hr * 0.45, hr * 0.45, hr * 0.12, hr * 0.06), 0, cy + hr * 0.4), sec);
        // disruptor coil(s)
        Turret tu = turret();
        double len = r * (0.85 + 0.2 * tier), hw = r * 0.13;
        int coils = tier >= 4 ? 2 : 1;
        for (int c = 0; c < coils; c++) {
            double off = coils == 2 ? (c == 0 ? -hw * 2.2 : hw * 2.2) : 0;
            tu.poly(Art.rect(-r * 0.15, off - hw, len + r * 0.15, 2 * hw), shell);
            for (int i = 0; i < 3; i++) tu.poly(Art.rect(len * 0.25 + i * len * 0.2, off - hw * 1.6, r * 0.1, hw * 3.2), sec);
            tu.poly(Art.circle(len + r * 0.12, off, hw * 1.7, 12), tier >= 4 ? Art.GOLD : sec);
            tu.gloss(len + r * 0.05, off - hw * 0.5, hw * 0.6, hw * 0.4);
        }
        if (tier >= 1 && tier < 4) {
            // triple meme launcher tubes
            tu.poly(Art.rect(r * 0.05, -hw * 2.6, len * 0.6, hw), sec);
            tu.poly(Art.rect(r * 0.05, hw * 1.6, len * 0.6, hw), sec);
        }
        tu.setFlash(tu.plain(Art.star(len + r * 0.45, 0, r * 0.35, r * 0.13, 7, 0), Art.alpha(Art.rgb(0xffb060), 240)));
        poly(Art.regular(0, 0, r * 0.22, 6, 30), tier >= 4 ? Art.GOLD : Art.METAL);
        if (tier >= 4) crown(0, cy - hr * 1.15, r * 0.25, 5);
        sparkle(-hr * 0.6, cy - hr * 0.6, r * 0.09);
    }
}
