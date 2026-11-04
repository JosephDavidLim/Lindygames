package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Qwen, the Layer Stripper: a stacked pagoda of violet and lilac discs with a
 * peeler-blade arm that shears layers off enemies. Each tier adds a storey to
 * the stack and sharper tooling (spinning sawblade, twin blades); the Omni
 * superclass is a tall rotating-ring pagoda capped in gold.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class QwenArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public QwenArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        int layers = 2 + tier;
        double base = r * 0.3;
        // stacked layers from the bottom up
        for (int i = 0; i < layers; i++) {
            double ly = base - i * r * 0.27;
            double rx = r * (0.78 - i * 0.09), ry = rx * 0.45;
            Color c = (i % 2 == 0) ? pri : sec;
            if (tier >= 4 && i == layers - 1) c = Art.GOLD;
            ellipse(0, ly, rx, ry, c);
            plain(Art.rect(-rx * 0.9, ly - ry * 0.3, rx * 1.8, ry * 0.3), Art.alpha(Color.WHITE, 60));
            if (tier >= 4 && i % 2 == 1) {
                spinningRing(0, ly, rx * 1.2, rx * 1.08, 12, Art.alpha(Art.GOLD, 200), (i % 4 == 1 ? 60 : -60));
            }
        }
        double topY = base - (layers - 1) * r * 0.27;
        // eye on the top layer
        eye(0, topY - r * 0.02, r * 0.14, pri);
        // blade arm(s)
        Turret tu = turret();
        double len = r * (0.9 + 0.2 * tier), hw = r * 0.09;
        int arms = tier >= 3 ? 2 : 1;
        for (int a = 0; a < arms; a++) {
            double off = arms == 2 ? (a == 0 ? -hw * 3 : hw * 3) : 0;
            tu.poly(Art.rect(-r * 0.15, off - hw, len + r * 0.15, 2 * hw), Art.METAL);
            if (tier >= 2) {
                tu.spinner(Art.star(0, 0, r * 0.3, r * 0.2, 8, 0), len, off, tier >= 4 ? Art.GOLD : secLight);
            } else {
                tu.poly(new double[] { len - r * 0.1, off - hw * 2.2, len + r * 0.3, off, len - r * 0.1, off + hw * 2.2 }, secLight);
            }
        }
        tu.setSpinSpeed(tier >= 4 ? 900 : 540);
        tu.setFlash(tu.plain(Art.star(len + r * 0.4, 0, r * 0.28, r * 0.11, 4, 0), Art.alpha(Color.WHITE, 230)));
        if (tier >= 4) crown(0, topY - r * 0.42, r * 0.24, 5);
        sparkle(-r * 0.45, topY + r * 0.05, r * 0.09);
    }
}
