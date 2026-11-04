package lindefense.ui.sprites.towers;

import acm.graphics.GOval;
import acm.graphics.GRect;
import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Suno, the Sonic Composer: a hot-pink boombox bot with two thumping speaker
 * cones, a carry handle and a music-note antenna, radiating sound rings.
 * v6-Mini adds a bouncing equaliser, v6 tweeters, v6-Wild a second speaker
 * row, and the Symphony superclass grows a golden gramophone horn with notes
 * dancing around it.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class SunoArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    // assigned in build(), which runs from the super constructor before field initialisers
    private java.util.List<GOval> cones;
    private java.util.List<GRect> eq;
    private double eqBase;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public SunoArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        cones = new java.util.ArrayList<>();
        eq = new java.util.ArrayList<>();
        double w = r * (1.45 + 0.05 * tier), h = r * (0.95 + (tier >= 3 ? 0.4 : 0)), top = -r * 0.55 - (tier >= 3 ? r * 0.3 : 0);
        pulseWave(pri, 0.8);
        // gramophone horn (tier 4)
        if (tier >= 4) {
            poly(new double[] { -r * 0.1, top, -r * 1.35, top - r * 0.95, -r * 1.45, top - r * 0.15, -r * 0.45, top + r * 0.15 }, Art.GOLD);
            plain(new double[] { -r * 0.3, top - r * 0.05, -r * 1.2, top - r * 0.75, -r * 1.25, top - r * 0.25 }, Art.GOLD_LIGHT);
            for (int i = 0; i < 3; i++) orbitingOrb(0, -r * 0.2, r * 1.45, r * 0.09, i * 2.1, 1.3, i == 1 ? Color.WHITE : sec);
        }
        // handle
        poly(Art.bar(-w * 0.3, top - r * 0.18, w * 0.3, top - r * 0.18, r * 0.09), priDark);
        // box body
        bodyBox(-w / 2, top, w, h, pri);
        // speaker cones
        int rows = tier >= 3 ? 2 : 1;
        double coneR = r * (0.26 + 0.03 * Math.min(tier, 2));
        for (int row = 0; row < rows; row++) {
            double cy = top + h * (rows == 1 ? 0.55 : 0.3 + 0.42 * row);
            for (int s = -1; s <= 1; s += 2) {
                double cx = s * w * 0.3;
                disc(cx, cy, coneR, Art.rgb(0x2a1f3a));
                GOval inner = new Flat.Oval(cx - coneR * 0.6, cy - coneR * 0.6, coneR * 1.2, coneR * 1.2);
                inner.setFilled(true); inner.setFillColor(sec); inner.setColor(Art.shade(sec, 0.7));
                add(inner);
                cones.add(inner);
                plain(Art.circle(cx, cy, coneR * 0.2, 8), Art.INK);
            }
        }
        // equaliser bars (tier 1+)
        if (tier >= 1) {
            eqBase = top + h * (rows == 1 ? 0.8 : 0.85);
            for (int i = 0; i < 5; i++) {
                GRect bar = new Flat.Rect(-r * 0.3 + i * r * 0.14, eqBase - r * 0.12, r * 0.09, r * 0.12);
                bar.setFilled(true); bar.setFillColor(i % 2 == 0 ? Art.rgb(0x7ee0a0) : Art.rgb(0xffd166)); bar.setColor(Art.INK);
                add(bar);
                eq.add(bar);
            }
        } else {
            visor(0, top + h * 0.85, w * 0.4, r * 0.1, sec);
        }
        // tweeters (tier 2+)
        if (tier >= 2) {
            disc(-w * 0.3, top + r * 0.1, r * 0.08, tier >= 4 ? Art.GOLD : Color.WHITE);
            disc(w * 0.3, top + r * 0.1, r * 0.08, tier >= 4 ? Art.GOLD : Color.WHITE);
        }
        // note antenna
        double nx = w * 0.42, ny = top - r * 0.55;
        poly(Art.bar(nx, top, nx, ny, r * 0.05), Art.METAL);
        plain(Art.bar(nx + r * 0.1, ny - r * 0.3, nx + r * 0.1, ny + r * 0.05, r * 0.035), Art.INK);
        plain(Art.circle(nx + r * 0.02, ny + r * 0.05, r * 0.09, 8), Art.INK);
        plain(new double[] { nx + r * 0.1, ny - r * 0.3, nx + r * 0.3, ny - r * 0.2, nx + r * 0.1, ny - r * 0.12 }, Art.INK);
        if (tier >= 4) crown(w * 0.1, top - r * 0.2, r * 0.22, 5);
        sparkle(-w * 0.42, top + r * 0.1, r * 0.09);
    }

    @Override protected void animateExtra(double t, double dt) {
        double beat = 1 + 0.12 * Math.max(0, Math.sin(t * 2 * Math.PI / 0.8 * 1.0)) * (tier >= 2 ? 1.6 : 1);
        for (GOval c : cones) {
            double cx = c.getX() + c.getWidth() / 2, cy = c.getY() + c.getHeight() / 2;
            double base = r * (0.26 + 0.03 * Math.min(tier, 2)) * 0.6;
            double rad = base * beat;
            c.setBounds(cx - rad, cy - rad, 2 * rad, 2 * rad);
        }
        for (int i = 0; i < eq.size(); i++) {
            double hgt = r * (0.06 + 0.1 * Math.abs(Math.sin(t * 7 + i * 1.3)));
            GRect b = eq.get(i);
            b.setBounds(b.getX(), eqBase - hgt, b.getWidth(), hgt);
        }
    }
}
