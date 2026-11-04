package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Bellarmine Student + AI, the Autonomous Scholar: the same student in a
 * sky-blue hoodie, laptop open, with a floating AI drone orb that paints a
 * detection beam. Each tier adds another drone (Agentic, Autonomous, AGI) and
 * holo gear; the AGI superclass wears a glowing holo-visor under a halo of
 * drones with gold trim.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class StudentAiArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public StudentAiArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double cy = -r * 0.05;
        Color hoodie = Art.shade(pri, 0.85);
        // detection halo (tier 4)
        if (tier >= 4) spinningRing(0, cy, r * 1.3, r * 1.18, 18, Art.alpha(pri, 180), 25);
        // laptop in front
        StudentArt.figure(this, r, tier, hoodie, tier >= 2 ? Color.WHITE : null);
        poly(Art.rect(-r * 0.5, cy + r * 0.35, r * 1.0, r * 0.3), Art.rgb(0x8d94a3));
        plain(Art.rect(-r * 0.44, cy + r * 0.39, r * 0.88, r * 0.22), Art.rgb(0x16213a));
        plain(Art.rect(-r * 0.38, cy + r * 0.43, r * 0.5, r * 0.04), pri);
        plain(Art.rect(-r * 0.38, cy + r * 0.5, r * 0.3, r * 0.04), Art.rgb(0x7ee0a0));
        // holo visor
        if (tier >= 4) {
            visor(0, cy - r * 0.2, r * 0.8, r * 0.14, Art.GOLD);
        } else if (tier >= 2) {
            visor(0, cy - r * 0.2, r * 0.72, r * 0.12, pri);
        }
        // earpiece (tier 1+)
        if (tier >= 1) {
            disc(r * 0.42, cy - r * 0.2, r * 0.09, pri);
            plain(Art.circle(r * 0.42, cy - r * 0.2, r * 0.04, 6), Color.WHITE);
        }
        // drones
        int drones = 1 + tier;
        for (int i = 0; i < drones; i++) {
            double ph = i * 2 * Math.PI / drones;
            Color c = tier >= 4 && i == 0 ? Art.GOLD : (i % 2 == 0 ? pri : Color.WHITE);
            orbitingOrb(0, cy - r * 0.2, r * (0.95 + 0.08 * tier), r * 0.14, ph, 1.3 + 0.1 * tier, c);
        }
        // detection emitter from the laptop
        Turret tu = turret();
        double len = r * (0.6 + 0.12 * tier);
        tu.poly(Art.wedge(r * 0.1, len, r * 0.1, 0), tier >= 4 ? Art.GOLD : pri);
        tu.poly(Art.circle(len, 0, r * 0.11, 10), Color.WHITE);
        tu.setFlash(tu.plain(Art.star(len + r * 0.25, 0, r * 0.22, r * 0.09, 4, 0), Art.alpha(Color.WHITE, 220)));
        if (tier >= 4) crown(0, cy - r * 0.78, r * 0.2, 5);
        sparkle(-r * 0.3, cy - r * 0.5, r * 0.08);
    }
}
