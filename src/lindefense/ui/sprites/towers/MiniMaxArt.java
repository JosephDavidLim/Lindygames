package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * MiniMax, the Clone Bouncer: a big glossy cherry-red ball-bot with a goofy
 * grin, a pinball launcher, and little clone balls that multiply with each
 * tier (sitting, then orbiting). The Hydra superclass sprouts five clone heads
 * on necks around a gold-collared core.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class MiniMaxArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public MiniMaxArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double br = r * (0.64 + 0.03 * tier), cy = -r * 0.1;
        if (tier >= 4) {
            // hydra heads on necks
            for (int i = 0; i < 5; i++) {
                double a = Math.PI + (i - 2) * 0.62 + Math.PI / 2;
                double hx = Math.cos(a) * r * 1.25, hy = cy + Math.sin(a) * r * 1.0;
                poly(Art.bar(Math.cos(a) * br * 0.6, cy + Math.sin(a) * br * 0.6, hx, hy, r * 0.1), priDark);
                body(hx, hy, r * 0.24, pri);
                eye(hx, hy - r * 0.04, r * 0.08, Art.INK);
            }
        } else {
            int clones = 1 + tier;
            for (int i = 0; i < clones; i++) {
                double a = Math.PI * 0.75 + i * (Math.PI * 1.5 / Math.max(1, clones - 1 + (clones == 1 ? 1 : 0)));
                if (tier >= 2) {
                    orbitingOrb(0, cy, r * 1.15, r * 0.17, a, 1.8, pri);
                } else {
                    double cx = Math.cos(a) * r * 1.05, cyy = cy + Math.sin(a) * r * 0.75;
                    body(cx, cyy, r * 0.18, pri);
                }
            }
        }
        // launcher
        Turret tu = turret();
        double len = r * (0.8 + 0.18 * tier), hw = r * 0.17;
        tu.poly(Art.rect(-r * 0.1, -hw, len + r * 0.1, 2 * hw), tier >= 4 ? Art.GOLD : sec);
        tu.poly(Art.rect(len - r * 0.1, -hw * 1.3, r * 0.2, hw * 2.6), priDark);
        for (int i = 0; i < 2 + Math.min(tier, 2); i++) tu.plain(Art.circle(r * 0.15 + i * r * 0.22, 0, hw * 0.45, 8), Art.alpha(pri, 200));
        tu.setFlash(tu.plain(Art.star(len + r * 0.3, 0, r * 0.3, r * 0.12, 5, 0), Art.alpha(Color.WHITE, 230)));
        // main ball
        body(0, cy, br, pri);
        if (tier >= 4) ellipse(0, cy + br * 0.75, br * 0.8, r * 0.16, Art.GOLD);
        eye(-br * 0.3, cy - br * 0.15, r * 0.17, Art.INK);
        eye(br * 0.3, cy - br * 0.15, r * 0.17, Art.INK);
        // grin
        plain(new double[] { -br * 0.4, cy + br * 0.3, br * 0.4, cy + br * 0.3, br * 0.2, cy + br * 0.55, -br * 0.2, cy + br * 0.55 }, Art.INK);
        plain(Art.rect(-br * 0.15, cy + br * 0.32, br * 0.3, br * 0.1), Color.WHITE);
        if (tier >= 3) {
            // launch tubes on the shoulders
            poly(Art.rect(-br * 1.05, cy - r * 0.4, r * 0.22, r * 0.5), sec);
            poly(Art.rect(br * 1.05 - r * 0.22, cy - r * 0.4, r * 0.22, r * 0.5), sec);
        }
        if (tier >= 4) crown(0, cy - br * 1.05, r * 0.24, 5);
        sparkle(-br * 0.6, cy - br * 0.6, r * 0.1);
    }
}
