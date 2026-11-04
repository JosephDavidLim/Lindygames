package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Copilot, the Pair Programmer: a friendly monitor-headed bot wearing big blue
 * headphones whose screen scrolls code, firing snippets from a keyboard
 * launcher. Pro adds a second monitor, Microsoft 365 hex firewall shields,
 * GitHub Enterprise a triple snippet launcher and antenna headset, and the
 * Autopilot superclass sprouts aviator wings and a spinning gold propeller.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class CopilotArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public CopilotArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double w = r * (1.25 + 0.05 * tier), h = r * 1.0, top = -r * 0.65;
        if (tier >= 4) {
            // aviator wings
            poly(new double[] { -w * 0.5, -r * 0.1, -w * 0.5 - r * 0.9, -r * 0.45, -w * 0.5 - r * 0.95, r * 0.05, -w * 0.5, r * 0.3 }, Art.GOLD);
            poly(new double[] { w * 0.5, -r * 0.1, w * 0.5 + r * 0.9, -r * 0.45, w * 0.5 + r * 0.95, r * 0.05, w * 0.5, r * 0.3 }, Art.GOLD);
            plain(Art.rect(-w * 0.5 - r * 0.8, -r * 0.25, r * 0.7, r * 0.08), Art.GOLD_LIGHT);
            plain(Art.rect(w * 0.5 + r * 0.1, -r * 0.25, r * 0.7, r * 0.08), Art.GOLD_LIGHT);
        }
        // headphones band and cups
        poly(Art.bar(-w * 0.5, top + r * 0.1, w * 0.5, top + r * 0.1, r * 0.1), priDark);
        ellipse(-w * 0.52, -r * 0.15, r * 0.17, r * 0.3, pri);
        ellipse(w * 0.52, -r * 0.15, r * 0.17, r * 0.3, pri);
        // monitor head
        bodyBox(-w / 2, top, w, h, sec);
        box(-w * 0.4, top + r * 0.12, w * 0.8, h * 0.6, Art.rgb(0x16213a));
        // code lines on screen
        Color[] codeColors = { pri, Art.rgb(0x7ee0a0), Art.rgb(0xffd166), Color.WHITE };
        for (int i = 0; i < 3; i++) {
            plain(Art.rect(-w * 0.33 + (i % 2) * r * 0.12, top + r * 0.22 + i * r * 0.17, w * (0.25 + 0.15 * ((i + 1) % 3)), r * 0.07), codeColors[i]);
        }
        // smile LED
        plain(Art.rect(-w * 0.12, top + h * 0.8, w * 0.24, r * 0.06), pri);
        // second monitor (tier 1+)
        if (tier >= 1) {
            box(-w * 0.5 - r * 0.45, top - r * 0.05, r * 0.45, r * 0.4, sec);
            plain(Art.rect(-w * 0.5 - r * 0.38, top + r * 0.03, r * 0.3, r * 0.22), Art.rgb(0x16213a));
            plain(Art.rect(-w * 0.5 - r * 0.34, top + r * 0.08, r * 0.2, r * 0.05), Art.rgb(0x7ee0a0));
        }
        // firewall hex shields (tier 2+)
        if (tier >= 2) {
            poly(Art.regular(-w * 0.62, r * 0.5, r * 0.22, 6, 0), tier >= 4 ? Art.GOLD : pri);
            poly(Art.regular(w * 0.62, r * 0.5, r * 0.22, 6, 0), tier >= 4 ? Art.GOLD : pri);
            plain(Art.regular(-w * 0.62, r * 0.5, r * 0.1, 6, 0), Color.WHITE);
            plain(Art.regular(w * 0.62, r * 0.5, r * 0.1, 6, 0), Color.WHITE);
        }
        // headset antennae (tier 3+)
        if (tier >= 3) {
            antenna(w * 0.5, top + r * 0.1, top - r * 0.45, Art.rgb(0x7ee0a0));
        }
        // propeller (tier 4)
        if (tier >= 4) {
            antenna(0, top, top - r * 0.3, Art.GOLD);
            spinningPoly(Art.star(0, 0, r * 0.55, r * 0.08, 3, 0), 0, top - r * 0.3, Art.GOLD_LIGHT, 540);
        }
        // keyboard snippet launcher
        Turret tu = turret();
        double len = r * (0.75 + 0.15 * tier), hw = r * 0.16;
        int tubes = tier >= 3 ? 3 : 1;
        for (int i = 0; i < tubes; i++) {
            double off = (i - (tubes - 1) / 2.0) * hw * 1.9;
            tu.poly(Art.rect(-r * 0.1, off - hw * 0.8, len + r * 0.1, hw * 1.6), tier >= 4 ? Art.GOLD : pri);
            tu.poly(Art.rect(len - r * 0.1, off - hw, r * 0.2, hw * 2), sec);
        }
        // keycaps on the launcher
        for (int i = 0; i < 3; i++) tu.plain(Art.rect(r * 0.1 + i * r * 0.2, -hw * 0.45, r * 0.12, hw * 0.9), Art.alpha(Color.WHITE, 160));
        tu.setFlash(tu.plain(Art.star(len + r * 0.3, 0, r * 0.28, r * 0.11, 4, 45), Art.alpha(Color.WHITE, 230)));
        disc(0, 0, r * 0.18, Art.METAL_LIGHT);
        sparkle(-w * 0.3, top + r * 0.08, r * 0.09);
    }
}
