package lindefense.ui.sprites.towers;

import acm.graphics.GCompound;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import lindefense.combat.VisualEvent;
import lindefense.sim.Vec;
import lindefense.ui.sprites.Animated;

/**
 * Cartoon attack effects built from {@link VisualEvent}s: a thick glowing
 * beam, a crackling chain arc, a hit spark ring and a chunky comic explosion.
 * Each is a {@link GCompound} placed in world coordinates whose
 * {@link Animated#step} fades and grows it, returning false when done.
 */
public final class AttackEffects {
    private AttackEffects() { }

    private static double life(VisualEvent ev, double min) { return Math.max(min, ev.duration); }

    /** A thick beam from a to b with a muzzle flash and an impact star. */
    @SuppressWarnings({ "serial", "this-escape" })
    public static final class Beam extends GCompound implements Animated {
        private static final long serialVersionUID = 1L;
        private final double total;
        private double t;
        private final GPolygon glow, core;
        private final GPolygon flash, burst;
        private final Color c;
        private double burstScale = 1;

        /** @param ev a BEAM event */
        public Beam(VisualEvent ev) {
            total = life(ev, 0.22);
            c = Art.rgb(ev.rgb);
            Vec a = ev.a, b = ev.b;
            double w = Math.max(2.5, ev.radius) * 1.6;
            glow = Art.rawPoly(Art.bar(a.x, a.y, b.x, b.y, w * 1.8), Art.alpha(c, 110), Art.alpha(c, 0));
            add(glow);
            core = Art.rawPoly(Art.bar(a.x, a.y, b.x, b.y, w * 0.7), Art.alpha(Color.WHITE, 230), Art.alpha(c, 200));
            add(core);
            flash = Art.rawPoly(Art.star(0, 0, w * 3.5, w * 1.4, 6, 0), Art.alpha(Color.WHITE, 220), Art.alpha(c, 220));
            flash.setLocation(a.x, a.y);
            add(flash);
            burst = Art.rawPoly(Art.star(0, 0, w * 3, w * 1.5, 8, 15), Art.alpha(Art.shade(c, 1.3), 230), Art.INK);
            burst.setLocation(b.x, b.y);
            add(burst);
        }

        @Override public boolean step(double dt) {
            t += dt;
            double f = Math.min(1, t / total);
            int a = (int) (255 * (1 - f));
            glow.setFillColor(Art.alpha(c, (int) (110 * (1 - f))));
            core.setFillColor(Art.alpha(Color.WHITE, a));
            core.setColor(Art.alpha(c, a));
            flash.setFillColor(Art.alpha(Color.WHITE, a));
            flash.setColor(Art.alpha(c, a));
            burst.setFillColor(Art.alpha(Art.shade(c, 1.3), a));
            burst.setColor(Art.alpha(Art.INK, a));
            double want = 1 + f * 0.8;
            burst.scale(want / burstScale);
            burstScale = want;
            return t < total;
        }
    }

    /** A jagged lightning arc that re-crackles every few frames. */
    @SuppressWarnings({ "serial", "this-escape" })
    public static final class Chain extends GCompound implements Animated {
        private static final long serialVersionUID = 1L;
        private final double total;
        private double t, sinceCrackle = 1;
        private final Vec a, b;
        private final Color c;
        private final List<GPolygon> segs = new ArrayList<>(10);
        private final GOval spark;
        private long seed = 12345;

        /** @param ev a CHAIN event */
        public Chain(VisualEvent ev) {
            total = life(ev, 0.22);
            a = ev.a; b = ev.b; c = Art.rgb(ev.rgb);
            spark = Art.glow(this, b.x, b.y, 6, Art.shade(c, 1.3), 220);
            crackle(1);
        }

        private double rnd() {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            return ((seed >>> 11) & 0xffffff) / (double) 0x1000000 - 0.5;
        }

        private void crackle(double alpha) {
            for (GPolygon p : segs) remove(p);
            segs.clear();
            int n = 5;
            double dx = b.x - a.x, dy = b.y - a.y, len = Math.hypot(dx, dy);
            double nx = len < 1e-6 ? 0 : -dy / len, ny = len < 1e-6 ? 0 : dx / len;
            double amp = Math.min(14, len * 0.18);
            double px = a.x, py = a.y;
            for (int i = 1; i <= n; i++) {
                double f = i / (double) n;
                double off = i == n ? 0 : rnd() * 2 * amp;
                double qx = a.x + dx * f + nx * off, qy = a.y + dy * f + ny * off;
                GPolygon outer = Art.rawPoly(Art.bar(px, py, qx, qy, 2.6), Art.alpha(c, (int) (170 * alpha)), Art.alpha(c, 0));
                GPolygon inner = Art.rawPoly(Art.bar(px, py, qx, qy, 1.0), Art.alpha(Color.WHITE, (int) (240 * alpha)), Art.alpha(Color.WHITE, 0));
                add(outer); add(inner);
                segs.add(outer); segs.add(inner);
                px = qx; py = qy;
            }
            spark.sendToFront();
        }

        @Override public boolean step(double dt) {
            t += dt;
            sinceCrackle += dt;
            double f = Math.min(1, t / total);
            if (sinceCrackle > 0.045) { sinceCrackle = 0; crackle(1 - f); }
            double rad = 6 * (1 + f);
            spark.setBounds(b.x - rad, b.y - rad, 2 * rad, 2 * rad);
            spark.setFillColor(Art.alpha(Art.shade(c, 1.3), (int) (220 * (1 - f))));
            return t < total;
        }
    }

    /** A small expanding ring with four radiating sparks. */
    @SuppressWarnings({ "serial", "this-escape" })
    public static final class Hit extends GCompound implements Animated {
        private static final long serialVersionUID = 1L;
        private final double total, r0;
        private double t, sparkScale = 1;
        private final Vec a;
        private final Color c;
        private final GOval ring;
        private final GPolygon sparks;

        /** @param ev a HIT event */
        public Hit(VisualEvent ev) {
            total = life(ev, 0.18);
            a = ev.a; c = Art.rgb(ev.rgb);
            r0 = Math.max(4, ev.radius);
            ring = new GOval(a.x - r0, a.y - r0, 2 * r0, 2 * r0);
            ring.setColor(Art.alpha(Color.WHITE, 230));
            add(ring);
            sparks = Art.rawPoly(Art.star(0, 0, r0 * 2.2, r0 * 0.5, 4, 0), Art.alpha(Art.shade(c, 1.2), 230), Art.alpha(Art.INK, 200));
            sparks.setLocation(a.x, a.y);
            add(sparks);
        }

        @Override public boolean step(double dt) {
            t += dt;
            double f = Math.min(1, t / total);
            double rad = r0 * (1 + 1.6 * f);
            ring.setBounds(a.x - rad, a.y - rad, 2 * rad, 2 * rad);
            ring.setColor(Art.alpha(Color.WHITE, (int) (230 * (1 - f))));
            double want = 1 + f * 0.9;
            sparks.scale(want / sparkScale);
            sparkScale = want;
            sparks.setFillColor(Art.alpha(Art.shade(c, 1.2), (int) (230 * (1 - f))));
            sparks.setColor(Art.alpha(Art.INK, (int) (200 * (1 - f))));
            return t < total;
        }
    }

    /** A chunky comic explosion: dark rim, coloured fill, hot white core, spiky burst and flying debris. */
    @SuppressWarnings({ "serial", "this-escape" })
    public static final class Explosion extends GCompound implements Animated {
        private static final long serialVersionUID = 1L;
        private final double total, radius;
        private double t, burstScale = 1;
        private final Vec a;
        private final Color c;
        private final GOval rim, fill, core;
        private final GPolygon burst;
        private final GOval[] debris = new GOval[4];

        /** @param ev an EXPLOSION event */
        public Explosion(VisualEvent ev) {
            total = life(ev, 0.35);
            a = ev.a; c = Art.rgb(ev.rgb);
            radius = Math.max(10, ev.radius);
            rim = Art.glow(this, a.x, a.y, 1, Art.INK, 200);
            fill = Art.glow(this, a.x, a.y, 1, c, 220);
            burst = Art.rawPoly(Art.star(0, 0, radius * 0.9, radius * 0.45, 9, 10), Art.alpha(Art.shade(c, 1.25), 240), Art.alpha(Art.INK, 230));
            burst.setLocation(a.x, a.y);
            add(burst);
            core = Art.glow(this, a.x, a.y, 1, Art.rgb(0xfff4c2), 250);
            for (int i = 0; i < debris.length; i++) {
                debris[i] = Art.glow(this, a.x, a.y, radius * 0.1, i % 2 == 0 ? Art.INK : Art.shade(c, 0.8), 230);
            }
        }

        @Override public boolean step(double dt) {
            t += dt;
            double f = Math.min(1, t / total);
            double grow = Math.sin(Math.min(1, f * 1.4) * Math.PI / 2); // fast out
            double rr = radius * (0.35 + 0.65 * grow);
            int a255 = (int) (255 * (1 - f * f));
            rim.setBounds(a.x - rr * 1.08, a.y - rr * 1.08, rr * 2.16, rr * 2.16);
            rim.setFillColor(Art.alpha(Art.INK, (int) (200 * (1 - f * f))));
            fill.setBounds(a.x - rr, a.y - rr, rr * 2, rr * 2);
            fill.setFillColor(Art.alpha(c, (int) (220 * (1 - f * f))));
            double cr = rr * (0.55 - 0.3 * f);
            core.setBounds(a.x - cr, a.y - cr, cr * 2, cr * 2);
            core.setFillColor(Art.alpha(Art.rgb(0xfff4c2), (int) (250 * (1 - f))));
            double want = 0.4 + 1.0 * grow;
            burst.scale(want / burstScale);
            burstScale = want;
            burst.setFillColor(Art.alpha(Art.shade(c, 1.25), a255));
            burst.setColor(Art.alpha(Art.INK, (int) (230 * (1 - f))));
            for (int i = 0; i < debris.length; i++) {
                double ang = i * Math.PI / 2 + 0.6, d = radius * (0.3 + 1.2 * f);
                double dr = radius * 0.1 * (1 - f * 0.5);
                debris[i].setBounds(a.x + Math.cos(ang) * d - dr, a.y + Math.sin(ang) * d * 0.8 - dr + radius * 0.6 * f * f, dr * 2, dr * 2);
                debris[i].setFillColor(Art.alpha(i % 2 == 0 ? Art.INK : Art.shade(c, 0.8), (int) (230 * (1 - f))));
            }
            return t < total;
        }
    }
}
