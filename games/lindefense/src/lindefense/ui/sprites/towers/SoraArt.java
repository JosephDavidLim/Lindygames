package lindefense.ui.sprites.towers;

import acm.graphics.GPolygon;
import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Sora, the Time Weaver: an ice-blue clockwork bot whose face is a ticking
 * clock that emits slow-time pulse rings. Turbo adds an hourglass, Sora 2
 * film-reel spools, 2 Pro a second rotating tick ring, and the Chronos
 * superclass spins a golden orrery of rings around the clock.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class SoraArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    private GPolygon hourHand, minuteHand;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public SoraArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double cr = r * (0.66 + 0.03 * tier), cy = -r * 0.1;
        pulseWave(pri, 1.25);
        // film reel spools (tier 2+)
        if (tier >= 2) {
            for (int s = -1; s <= 1; s += 2) {
                double sx = s * cr * 1.3;
                disc(sx, r * 0.2, r * 0.3, Art.rgb(0x3a4256));
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2;
                    plain(Art.circle(sx + Math.cos(a) * r * 0.15, r * 0.2 + Math.sin(a) * r * 0.15, r * 0.06, 8), sec);
                }
                plain(Art.circle(sx, r * 0.2, r * 0.07, 8), Art.METAL_LIGHT);
            }
        }
        // orrery rings (tier 4)
        if (tier >= 4) {
            spinningRing(0, cy, r * 1.25, r * 1.15, 14, Art.GOLD, 40);
            spinningRing(0, cy, r * 1.05, r * 0.97, 10, Art.GOLD_LIGHT, -70);
            orbitingOrb(0, cy, r * 1.3, r * 0.12, 0, 1.2, sec);
            orbitingOrb(0, cy, r * 1.3, r * 0.1, Math.PI * 0.8, 1.2, pri);
        }
        // tick ring (tier 3+)
        if (tier >= 3) spinningRing(0, cy, cr * 1.28, cr * 1.16, 12, sec, 20);
        // clock body: ice rim, white face
        disc(0, cy, cr, pri);
        plain(Art.circle(0, cy, cr * 0.8, 24), Color.WHITE);
        gloss(-cr * 0.35, cy - cr * 0.4, cr * 0.28, cr * 0.16);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            double len = (i % 3 == 0) ? cr * 0.16 : cr * 0.08;
            plain(Art.bar(Math.cos(a) * (cr * 0.78 - len), cy + Math.sin(a) * (cr * 0.78 - len), Math.cos(a) * cr * 0.76, cy + Math.sin(a) * cr * 0.76, r * 0.025), priDark);
        }
        hourHand = spinningPoly(Art.bar(0, 0, cr * 0.4, 0, r * 0.05), 0, cy, Art.INK, 30);
        minuteHand = spinningPoly(Art.bar(0, 0, cr * 0.62, 0, r * 0.035), 0, cy, tier >= 4 ? Art.GOLD_DARK : priDark, 360);
        plain(Art.circle(0, cy, r * 0.07, 8), tier >= 4 ? Art.GOLD : pri);
        // hourglass (tier 1+)
        if (tier >= 1) {
            double hx = -cr * 1.15, hy = -r * 0.55;
            poly(new double[] { hx - r * 0.2, hy - r * 0.3, hx + r * 0.2, hy - r * 0.3, hx - r * 0.2, hy + r * 0.3, hx + r * 0.2, hy + r * 0.3 }, secLight);
            plain(new double[] { hx - r * 0.14, hy - r * 0.26, hx + r * 0.14, hy - r * 0.26, hx, hy - r * 0.05 }, Art.rgb(0xffd166));
            plain(Art.rect(hx - r * 0.22, hy - r * 0.36, r * 0.44, r * 0.07), Art.rgb(0x8a5a2b));
            plain(Art.rect(hx - r * 0.22, hy + r * 0.29, r * 0.44, r * 0.07), Art.rgb(0x8a5a2b));
        }
        // crown + glow (tier 4)
        if (tier >= 4) {
            crown(0, cy - cr * 1.05, r * 0.26, 5);
            pulsingGlow(0, cy, cr * 1.1, sec, 40, 30, 1.0);
        }
        sparkle(cr * 0.55, cy - cr * 0.7, r * 0.1);
    }
}
