package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Cohere, the Command Breaker: a squat coral siege-bot with a target reticle
 * emblem and one enormous rerank beam cannon. R+ adds a focusing lens ring,
 * Command A stabiliser struts, A+ heat fins, and the Apex superclass carries a
 * colossal spiked cannon with a pulsing gold charge at the muzzle.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class CohereArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public CohereArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double w = r * (1.3 + 0.06 * tier), h = r * 1.05, top = -r * 0.6;
        // stabiliser struts (tier 2+)
        if (tier >= 2) {
            for (int s = -1; s <= 1; s += 2) {
                poly(Art.bar(s * w * 0.45, r * 0.2, s * w * 0.75, r * 0.65, r * 0.08), Art.METAL);
                poly(Art.rect(s * w * 0.75 - r * 0.18, r * 0.6, r * 0.36, r * 0.14), tier >= 4 ? Art.GOLD : secDark);
            }
        }
        bodyBox(-w / 2, top, w, h, pri);
        // reticle emblem
        disc(0, top + h * 0.6, r * 0.26, sec);
        plain(Art.circle(0, top + h * 0.6, r * 0.14, 12), Color.WHITE);
        plain(Art.rect(-r * 0.3, top + h * 0.6 - r * 0.02, r * 0.6, r * 0.04), priDark);
        plain(Art.rect(-r * 0.02, top + h * 0.6 - r * 0.3, r * 0.04, r * 0.6), priDark);
        plain(Art.circle(0, top + h * 0.6, r * 0.05, 8), priDark);
        // eyes slit
        visor(0, top + h * 0.22, w * 0.6, r * 0.14, sec);
        // heat fins (tier 3+)
        if (tier >= 3) {
            for (int i = 0; i < 3; i++) {
                box(-w * 0.5 - r * 0.22, top + r * 0.12 + i * r * 0.28, r * 0.22, r * 0.14, tier >= 4 ? Art.GOLD : Art.METAL_LIGHT);
                box(w * 0.5, top + r * 0.12 + i * r * 0.28, r * 0.22, r * 0.14, tier >= 4 ? Art.GOLD : Art.METAL_LIGHT);
            }
        }
        // beam cannon
        Turret tu = turret();
        double len = r * (1.05 + 0.28 * tier), hw = r * (0.2 + 0.03 * tier);
        Color barrel = tier >= 4 ? Art.shade(pri, 0.8) : priDark;
        tu.poly(Art.rect(-r * 0.3, -hw * 1.3, r * 0.6, hw * 2.6), Art.METAL);
        tu.poly(Art.rect(-r * 0.1, -hw, len + r * 0.1, 2 * hw), barrel);
        tu.poly(Art.rect(len * 0.35, -hw * 1.25, r * 0.16, hw * 2.5), tier >= 4 ? Art.GOLD : sec);
        if (tier >= 1) {
            tu.poly(Art.rect(len - r * 0.15, -hw * 1.6, r * 0.22, hw * 3.2), tier >= 4 ? Art.GOLD : sec);
            tu.poly(Art.circle(len + r * 0.1, 0, hw * 0.9, 12), tier >= 4 ? Art.GOLD_LIGHT : secLight);
        } else {
            tu.poly(Art.rect(len - r * 0.1, -hw * 1.3, r * 0.2, hw * 2.6), sec);
        }
        if (tier >= 4) {
            tu.poly(Art.star(len * 0.7, 0, hw * 2.4, hw * 1.3, 6, 0), Art.GOLD);
            tu.spinner(Art.star(0, 0, hw * 1.6, hw * 1.0, 5, 0), len + r * 0.1, 0, Art.GOLD_LIGHT);
            tu.setSpinSpeed(240);
        }
        tu.gloss(len * 0.25, -hw * 0.4, len * 0.2, hw * 0.3);
        tu.setFlash(tu.plain(Art.star(len + r * 0.45, 0, r * 0.4, r * 0.16, 6, 0), Art.alpha(Color.WHITE, 240)));
        disc(0, 0, r * 0.24, tier >= 4 ? Art.GOLD : Art.METAL_LIGHT);
        if (tier >= 4) crown(0, top - r * 0.15, r * 0.26, 5);
        sparkle(-w * 0.35, top + r * 0.1, r * 0.09);
    }
}
