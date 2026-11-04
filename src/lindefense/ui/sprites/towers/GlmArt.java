package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * GLM, the Gravity Grid: a dark gravity-well generator ringed in cyan, with a
 * glowing grid etched over the void and a spinning accretion ring. 5.2 adds a
 * second ring, 5.3 Flash three gravity pylons, 5.3 a grid plate, and the
 * Singularity superclass is a yawning black hole behind a gold event-horizon
 * ring with debris circling it.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class GlmArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public GlmArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double wr = r * (0.62 + 0.06 * tier), cy = -r * 0.08;
        pulseWave(pri, 1.1);
        // grid plate (tier 3+)
        if (tier >= 3) {
            poly(Art.rect(-r * 1.05, cy - r * 0.95, r * 2.1, r * 1.9), Art.rgb(0x10384a));
            for (int i = 1; i < 4; i++) {
                plain(Art.rect(-r * 1.05 + i * r * 0.525 - r * 0.02, cy - r * 0.95, r * 0.04, r * 1.9), Art.alpha(pri, 150));
                plain(Art.rect(-r * 1.05, cy - r * 0.95 + i * r * 0.475 - r * 0.02, r * 2.1, r * 0.04), Art.alpha(pri, 150));
            }
        }
        // gravity pylons (tier 2+)
        if (tier >= 2) {
            for (int i = 0; i < 3; i++) {
                double a = -Math.PI / 2 + i * 2 * Math.PI / 3 + Math.PI;
                double px = Math.cos(a) * r * 1.0, py = cy + Math.sin(a) * r * 0.9;
                poly(Art.regular(px, py, r * 0.2, 3, Math.toDegrees(a) + 90), tier >= 4 ? Art.GOLD : sec);
                plain(Art.circle(px, py, r * 0.07, 8), pri);
            }
        }
        // accretion rings
        spinningRing(0, cy, wr * 1.3, wr * 1.12, 8, Art.alpha(pri, 190), 90);
        if (tier >= 1) spinningRing(0, cy, wr * 1.5, wr * 1.38, 12, Art.alpha(secLight, 160), -60);
        if (tier >= 4) spinningRing(0, cy, wr * 1.75, wr * 1.62, 16, Art.alpha(Art.GOLD, 230), 45);
        // the well
        disc(0, cy, wr, tier >= 4 ? Art.GOLD : pri);
        plain(Art.circle(0, cy, wr * 0.82, 24), sec);
        plain(Art.circle(0, cy, wr * 0.5, 20), Art.rgb(0x02070c));
        // grid over the void
        for (int i = -1; i <= 1; i++) {
            plain(Art.rect(i * wr * 0.3 - r * 0.015, cy - wr * 0.78, r * 0.03, wr * 1.56), Art.alpha(pri, 120));
            plain(Art.rect(-wr * 0.78, cy + i * wr * 0.3 - r * 0.015, wr * 1.56, r * 0.03), Art.alpha(pri, 120));
        }
        gloss(-wr * 0.35, cy - wr * 0.45, wr * 0.25, wr * 0.14);
        pulsingGlow(0, cy, wr * 0.55, pri, 70, 60, 1.6);
        // debris orbs (tier 4)
        if (tier >= 4) {
            for (int i = 0; i < 4; i++) orbitingOrb(0, cy, r * 1.35, r * 0.1, i * Math.PI / 2, -1.5, i % 2 == 0 ? Art.METAL_LIGHT : pri);
            crown(0, cy - wr * 1.15, r * 0.24, 5);
        }
        sparkle(wr * 0.5, cy - wr * 0.65, r * 0.09);
    }
}
