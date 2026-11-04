package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Claude, the Constitutional Lance: a calm cream-and-terracotta robot with a
 * single big amber eye that carries a long piercing thought-lance. Each tier
 * lengthens the lance (Haiku, Sonnet, Fable, Opus); the Mythos superclass
 * wields a golden double-railed lance ringed by halos with cream orbs circling.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class ClaudeArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public ClaudeArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double br = r * (0.68 + 0.03 * tier);
        // shoulder nubs
        disc(-br * 0.95, -r * 0.05, r * 0.2, pri);
        disc(br * 0.95, -r * 0.05, r * 0.2, pri);
        // cream body with terracotta chest plate
        body(0, -r * 0.08, br, sec);
        poly(new double[] { -br * 0.7, r * 0.15, br * 0.7, r * 0.15, br * 0.55, br * 0.9 - r * 0.08, -br * 0.55, br * 0.9 - r * 0.08 }, pri);
        // thinking antenna (tier 2+)
        if (tier >= 2) {
            antenna(-br * 0.55, -br * 0.8, -br * 1.35, priLight);
            pulsingGlow(-br * 0.55, -br * 1.35, r * 0.25, priLight, 80, 60, 1.2);
        }
        if (tier >= 4) {
            orbitingOrb(0, -r * 0.1, r * 1.2, r * 0.16, 0, 1.6, sec);
            orbitingOrb(0, -r * 0.1, r * 1.2, r * 0.16, Math.PI, 1.6, Art.GOLD_LIGHT);
        }
        // lance
        Turret tu = turret();
        double len = r * (1.0 + 0.24 * tier);
        double hw = r * (0.11 + 0.02 * tier);
        Color shaft = tier >= 4 ? Art.GOLD : pri;
        if (tier >= 3) {
            tu.poly(Art.rect(0, -hw * 2.3, len * 0.72, hw * 0.7), sec);
            tu.poly(Art.rect(0, hw * 1.6, len * 0.72, hw * 0.7), sec);
        }
        tu.poly(Art.rect(-r * 0.25, -hw, len + r * 0.25, 2 * hw), shaft);
        if (tier >= 1) {
            tu.poly(new double[] { -r * 0.05, -hw, r * 0.3, -hw, 0, -hw * 2.8 }, secDark);
            tu.poly(new double[] { -r * 0.05, hw, r * 0.3, hw, 0, hw * 2.8 }, secDark);
        }
        double tipLen = r * (0.3 + 0.06 * tier);
        tu.poly(tier >= 3
            ? new double[] { len - r * 0.1, -hw * 2.1, len + tipLen, 0, len - r * 0.1, hw * 2.1, len - r * 0.3, 0 }
            : Art.wedge(len - r * 0.05, len + tipLen, hw * 1.7, 0), tier >= 4 ? Color.WHITE : sec);
        tu.gloss(len * 0.35, -hw * 0.35, len * 0.3, hw * 0.3);
        if (tier >= 4) {
            tu.ringSpinner(hw * 3.4, hw * 2.4, 8, len * 0.45, 0, Art.GOLD);
            tu.ringSpinner(hw * 3.0, hw * 2.0, 8, len * 0.8, 0, Art.GOLD_LIGHT);
            tu.setSpinSpeed(140);
        }
        tu.setFlash(tu.plain(Art.star(len + tipLen + r * 0.15, 0, r * 0.32, r * 0.12, 6, 0), Art.alpha(Color.WHITE, 230)));
        // pivot cap and face
        disc(0, 0, r * 0.2, priDark);
        eye(0, -br * 0.38, r * 0.26, pri);
        if (tier >= 4) crown(0, -br * 1.08, r * 0.28, 5);
        sparkle(-br * 0.75, -br * 0.55, r * 0.1);
    }
}
