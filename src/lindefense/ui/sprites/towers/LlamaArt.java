package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Llama, the Open Herd: a golden llama-faced mech with tall ears and a brown
 * muzzle that spits kinetic wads. Maverick straps on a saddle and collar,
 * Muse Glimmer grows ram horns, Muse Wonder a triple spit nozzle, and the
 * Behemoth superclass is a massive gold-armoured horned beast.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class LlamaArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public LlamaArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        double hr = r * (0.66 + 0.05 * tier), cy = -r * 0.05;
        Color fur = tier >= 4 ? Art.shade(pri, 0.9) : pri;
        // ears
        poly(new double[] { -hr * 0.55, cy - hr * 0.6, -hr * 0.75, cy - hr * 1.45, -hr * 0.2, cy - hr * 0.85 }, fur);
        poly(new double[] { hr * 0.55, cy - hr * 0.6, hr * 0.75, cy - hr * 1.45, hr * 0.2, cy - hr * 0.85 }, fur);
        plain(new double[] { -hr * 0.52, cy - hr * 0.72, -hr * 0.66, cy - hr * 1.25, -hr * 0.32, cy - hr * 0.88 }, Art.rgb(0xf3b4c4));
        plain(new double[] { hr * 0.52, cy - hr * 0.72, hr * 0.66, cy - hr * 1.25, hr * 0.32, cy - hr * 0.88 }, Art.rgb(0xf3b4c4));
        // ram horns (tier 2+)
        if (tier >= 2) {
            Color horn = tier >= 4 ? Art.GOLD : Art.rgb(0xd8c3a5);
            for (int s = -1; s <= 1; s += 2) {
                poly(new double[] { s * hr * 0.8, cy - hr * 0.5, s * hr * 1.35, cy - hr * 0.95, s * hr * 1.45, cy - hr * 0.3, s * hr * 1.15, cy + hr * 0.05, s * hr * 0.95, cy - hr * 0.15 }, horn);
            }
        }
        // saddle / armour band (tier 1+)
        if (tier >= 1) ellipse(0, cy + hr * 0.95, hr * 1.1, r * 0.28, tier >= 4 ? Art.GOLD : sec);
        // head
        body(0, cy, hr, fur);
        if (tier >= 4) {
            // gold armour plate
            poly(new double[] { -hr * 0.5, cy - hr * 0.95, hr * 0.5, cy - hr * 0.95, hr * 0.7, cy - hr * 0.3, -hr * 0.7, cy - hr * 0.3 }, Art.GOLD);
            plain(Art.star(0, cy - hr * 0.6, r * 0.12, r * 0.05, 4, 0), Color.WHITE);
        }
        // eyes
        eye(-hr * 0.35, cy - hr * 0.2, r * 0.17, Art.rgb(0x5a3b2a));
        eye(hr * 0.35, cy - hr * 0.2, r * 0.17, Art.rgb(0x5a3b2a));
        // muzzle
        ellipse(0, cy + hr * 0.45, hr * 0.55, hr * 0.38, sec);
        plain(Art.circle(-hr * 0.2, cy + hr * 0.4, r * 0.05, 6), Art.INK);
        plain(Art.circle(hr * 0.2, cy + hr * 0.4, r * 0.05, 6), Art.INK);
        // spit nozzle(s)
        Turret tu = turret();
        double len = r * (0.7 + 0.15 * tier), hw = r * 0.12;
        int nozzles = tier >= 3 ? 3 : 1;
        for (int i = 0; i < nozzles; i++) {
            double ang = (i - (nozzles - 1) / 2.0) * 18;
            tu.poly(Art.spin(Art.rect(-r * 0.1, -hw, len + r * 0.1, 2 * hw), ang), tier >= 4 ? Art.GOLD_DARK : Art.rgb(0x5a3b2a));
            tu.poly(Art.spin(Art.circle(len + r * 0.05, 0, hw * 1.3, 10), ang), sec);
        }
        tu.setFlash(tu.plain(Art.star(len + r * 0.4, 0, r * 0.28, r * 0.11, 5, 0), Art.alpha(Color.WHITE, 230)));
        disc(0, 0, r * 0.14, Art.rgb(0x5a3b2a));
        if (tier >= 4) crown(0, cy - hr * 1.35, r * 0.26, 5);
        sparkle(-hr * 0.55, cy - hr * 0.55, r * 0.09);
    }
}
