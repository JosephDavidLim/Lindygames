package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Zethos, the Quantum Architect: a hovering hooded architect over a violet quantum core, aiming a prism emitter.
 * Every game tier is its own stage (0-4):
 * <ol start="0">
 *   <li>Quantum Pulse: small cyan core and a single-prism emitter.</li>
 *   <li>Quantum Acceleration: a spinning accelerator ring and a longer, brighter twin-rail emitter.</li>
 *   <li>Dimensional Fracture: orbiting fracture shards and split emitter fins.</li>
 *   <li>Quantum Singularity: a dark singularity core with an accretion ring and a third rail.</li>
 *   <li>Reality Engine: a large, blazing quantum energy core, double counter-rotating rings, a halo crown and the
 *       superclass aura.</li>
 * </ol>
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class ZethosArt extends TowerArt {
    private static final long serialVersionUID = 1L;
    private static final Color ROBE = new Color(0x2a1d52);
    private static final Color VOID = new Color(0x0b0716);

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public ZethosArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double core = r * (0.28 + 0.07 * tier);
        // core below the figure
        pulsingGlow(0, r * 0.15, core * 2.2, sec, 40 + 8 * tier, 30, 1.4);
        if (tier >= 3) {
            spinningRing(0, r * 0.15, core * 1.7, core * 1.35, 18, Art.alpha(pri, 220), 90);
            disc(0, r * 0.15, core, VOID);
            plain(Art.ring(0, r * 0.15, core * 0.95, core * 0.75, 16), sec);
        } else {
            disc(0, r * 0.15, core, sec);
            plain(Art.circle(0, r * 0.15, core * 0.5, 12), Color.WHITE);
        }
        if (tier >= 1) spinningRing(0, r * 0.15, core * (tier >= 3 ? 2.2 : 1.8), core * (tier >= 3 ? 2.0 : 1.6), 20, Art.alpha(sec, 200), tier >= 4 ? -160 : -110);
        if (tier >= 4) {
            spinningPoly(Art.star(0, 0, core * 1.25, core * 0.6, 8, 0), 0, r * 0.15, Art.alpha(Color.WHITE, 230), 200);
            spinningRing(0, r * 0.15, core * 2.7, core * 2.5, 24, Art.alpha(Art.GOLD_LIGHT, 200), 60);
        }
        if (tier >= 2) {
            int shards = tier >= 4 ? 5 : tier >= 3 ? 4 : 3;
            for (int i = 0; i < shards; i++) {
                acm.graphics.GCompound g = orbitingOrb(0, 0, r * (1.0 + 0.05 * tier), r * 0.05, i * Math.PI * 2 / shards, 1.6, sec);
                Art.poly(g, Art.regular(0, 0, r * 0.13, 3, i * 40), i % 2 == 0 ? pri : sec);
            }
        }
        // emitter
        Turret tu = turret();
        double len = r * (0.9 + 0.16 * tier);
        int rails = tier >= 3 ? 3 : tier >= 1 ? 2 : 1;
        for (int i = 0; i < rails; i++) {
            double off = (i - (rails - 1) / 2.0) * r * 0.2;
            tu.poly(Art.rect(r * 0.1, off - r * 0.05, len * (rails == 3 && i != 1 ? 0.8 : 1), r * 0.1), tier >= 4 ? Art.GOLD : Art.METAL_LIGHT);
        }
        tu.poly(new double[] { len - r * 0.05, -r * 0.18, len + r * 0.3, 0, len - r * 0.05, r * 0.18 }, sec);
        tu.plain(new double[] { len, -r * 0.08, len + r * 0.18, 0, len, r * 0.08 }, Color.WHITE);
        if (tier >= 2) {
            tu.poly(new double[] { len * 0.55, -r * 0.18, len * 0.85, -r * 0.42, len * 0.8, -r * 0.18 }, pri);
            tu.poly(new double[] { len * 0.55, r * 0.18, len * 0.85, r * 0.42, len * 0.8, r * 0.18 }, pri);
        }
        if (tier >= 1) tu.plain(Art.rect(r * 0.1, -r * 0.02, len * 0.95, r * 0.04), Art.alpha(sec, 230));
        tu.setFlash(tu.plain(Art.star(len + r * 0.5, 0, r * (0.32 + 0.05 * tier), r * 0.12, 6, 0), Art.alpha(sec, 240)));
        // hooded architect
        ellipse(0, -r * 0.02, r * 0.62, r * 0.46, ROBE);
        plain(new double[] { -r * 0.5, r * 0.1, 0, -r * 0.1, r * 0.5, r * 0.1, r * 0.35, r * 0.32, -r * 0.35, r * 0.32 }, Art.shade(ROBE, 1.25));
        poly(Art.regular(0, -r * 0.32, r * 0.36, 6, 0), ROBE);
        disc(0, -r * 0.3, r * 0.22, VOID);
        disc(-r * 0.08, -r * 0.3, r * 0.05, sec);
        disc(r * 0.08, -r * 0.3, r * 0.05, sec);
        plain(Art.rect(-r * 0.4, -r * 0.05, r * 0.8, r * 0.05), pri);
        if (tier >= 4) crown(0, -r * 0.78, r * 0.3, 6);
        sparkle(-r * 0.55, -r * 0.6, r * 0.09);
        if (tier >= 2) sparkle(r * 0.6, r * 0.5, r * 0.07);
    }
}
