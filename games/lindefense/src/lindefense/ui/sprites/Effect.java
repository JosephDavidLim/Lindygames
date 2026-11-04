package lindefense.ui.sprites;

import acm.graphics.GCompound;
import acm.graphics.GFillable;
import acm.graphics.GObject;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A self-contained, one-shot visual effect. Everything is built in the
 * constructor; {@link #step} advances an internal clock and moves, resizes or
 * fades the pieces until {@link #duration} has passed.
 *
 * <p>Two building blocks cover most needs: {@link #shards} (flying particles
 * with gravity and fade) and {@link #ring} (an expanding, fading ring).
 * Effects can also be nested via {@link #stage} so bosses can chain several
 * bursts with delays.</p>
 */
@SuppressWarnings({ "serial", "this-escape" })
public class Effect extends GCompound implements Animated {
    /** Total length of the effect in seconds. */
    protected final double duration;
    /** Elapsed seconds. */
    protected double t;
    private boolean done;

    private final List<Particle> particles = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();
    private final List<Effect> stages = new ArrayList<>();
    private final List<Double> stageDelay = new ArrayList<>();
    private final List<Boolean> stageDone = new ArrayList<>();

    private static final class Particle {
        GObject g; double x0, y0, vx, vy, gravity, delay, life, spin, w, h; Color c; boolean shrink;
    }

    private static final class Ring {
        GOval g; double r0, r1, delay, life; Color c; boolean filled;
    }

    /** Creates an effect lasting {@code duration} seconds positioned at (x,y). */
    public Effect(double x, double y, double duration) {
        this.duration = duration;
        setLocation(x, y);
    }

    /** Hashes a seed into a pseudo-random double in [0,1). */
    protected static double rnd(long seed) {
        seed = (seed ^ 0x5DEECE66DL) * 0x2545F4914F6CDD1DL;
        seed ^= seed >>> 29;
        return ((seed >>> 11) & 0xFFFFFFFFL) / 4294967296.0;
    }

    /**
     * Adds {@code n} shards flying out from (cx,cy).
     *
     * @param shape 0 = triangles, 1 = quads, 2 = circles, 3 = thin slivers
     */
    protected void shards(int n, double cx, double cy, double size, double speed, double gravity,
                          Color c, int shape, double delay, double life, long seed) {
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * (i + rnd(seed + i * 7)) / n;
            double sp = speed * (0.55 + 0.9 * rnd(seed + i * 13 + 1));
            double s = size * (0.6 + 0.8 * rnd(seed + i * 17 + 2));
            Particle p = new Particle();
            p.x0 = cx; p.y0 = cy; p.vx = Math.cos(a) * sp; p.vy = Math.sin(a) * sp - speed * 0.2;
            p.gravity = gravity; p.delay = delay; p.life = life * (0.75 + 0.5 * rnd(seed + i * 19 + 3));
            p.c = c; p.w = s; p.h = s; p.spin = (rnd(seed + i) - 0.5) * 720; p.shrink = shape != 2;
            GObject g;
            switch (shape) {
                case 0: g = Art.poly(-s / 2, s / 2, s / 2, s / 2, 0, -s / 2); break;
                case 1: g = Art.poly(-s / 2, -s / 2, s / 2, -s / 2, s / 2, s / 2, -s / 2, s / 2); break;
                case 3: g = Art.poly(-s, -s * 0.15, s, -s * 0.15, s, s * 0.15, -s, s * 0.15); break;
                default: g = Art.ovalAt(0, 0, s, s); break;
            }
            ((GFillable) g).setFilled(true);
            ((GFillable) g).setFillColor(c);
            g.setColor(Art.outline(c));
            g.setVisible(false);
            p.g = g;
            add(g);
            particles.add(p);
        }
    }

    /** Adds a single drifting particle (used for halves and other bespoke pieces). */
    protected GObject piece(GObject g, double cx, double cy, double vx, double vy, double gravity,
                            Color c, double delay, double life, double spinDegPerSec, boolean shrink) {
        Particle p = new Particle();
        p.g = g; p.x0 = cx; p.y0 = cy; p.vx = vx; p.vy = vy; p.gravity = gravity; p.delay = delay; p.life = life;
        p.c = c; p.spin = spinDegPerSec; p.shrink = shrink; p.w = g.getWidth(); p.h = g.getHeight();
        if (g instanceof GFillable) { ((GFillable) g).setFilled(true); ((GFillable) g).setFillColor(c); }
        g.setColor(Art.outline(c));
        g.setVisible(false);
        add(g);
        particles.add(p);
        return g;
    }

    /** Adds an expanding ring from radius r0 to r1 (filled = a flash disc). */
    protected void ring(double cx, double cy, double r0, double r1, Color c, double delay, double life, boolean filled) {
        Ring rg = new Ring();
        rg.g = Art.ovalAt(cx, cy, r0 * 2, r0 * 2);
        rg.g.setFilled(filled);
        rg.g.setFillColor(c);
        rg.g.setColor(c);
        rg.g.setVisible(false);
        rg.r0 = r0; rg.r1 = r1; rg.c = c; rg.delay = delay; rg.life = life; rg.filled = filled;
        add(rg.g);
        rings.add(rg);
    }

    /** Nests another effect (positioned relative to this one) that starts after {@code delay}. */
    protected void stage(Effect e, double delay) {
        e.setVisible(false);
        add(e);
        stages.add(e);
        stageDelay.add(delay);
        stageDone.add(false);
    }

    @Override public boolean step(double dt) {
        if (done) return false;
        t += dt;
        update(t);
        for (Particle p : particles) {
            double lt = t - p.delay;
            if (lt < 0 || lt > p.life) { if (p.g.isVisible()) p.g.setVisible(false); continue; }
            double x = p.x0 + p.vx * lt, y = p.y0 + p.vy * lt + 0.5 * p.gravity * lt * lt;
            double f = 1 - lt / p.life;
            double k = p.shrink ? 0.35 + 0.65 * f : 1;
            if (p.g instanceof GPolygon) {
                p.g.setLocation(x, y);
                if (p.spin != 0) ((GPolygon) p.g).rotate(p.spin * dt);
            } else if (p.g instanceof GOval) {
                ((GOval) p.g).setBounds(x - p.w * k / 2, y - p.h * k / 2, p.w * k, p.h * k);
            } else {
                p.g.setLocation(x - p.w / 2, y - p.h / 2);
            }
            int a = (int) (p.c.getAlpha() * Math.min(1, f * 1.6));
            if (p.g instanceof GFillable) ((GFillable) p.g).setFillColor(Art.alpha(p.c, a));
            p.g.setColor(Art.alpha(Art.outline(p.c), a));
            p.g.setVisible(true);
        }
        for (Ring rg : rings) {
            double lt = t - rg.delay;
            if (lt < 0 || lt > rg.life) { if (rg.g.isVisible()) rg.g.setVisible(false); continue; }
            double f = lt / rg.life;
            double e = 1 - (1 - f) * (1 - f);
            double rad = rg.r0 + (rg.r1 - rg.r0) * e;
            double cx = rg.g.getX() + rg.g.getWidth() / 2, cy = rg.g.getY() + rg.g.getHeight() / 2;
            rg.g.setBounds(cx - rad, cy - rad, rad * 2, rad * 2);
            Color c = Art.alpha(rg.c, (int) (rg.c.getAlpha() * (1 - f)));
            rg.g.setColor(c);
            if (rg.filled) rg.g.setFillColor(c);
            rg.g.setVisible(true);
        }
        boolean stagesAlive = false;
        for (int i = 0; i < stages.size(); i++) {
            if (stageDone.get(i)) continue;
            if (t < stageDelay.get(i)) { stagesAlive = true; continue; }
            Effect s = stages.get(i);
            s.setVisible(true);
            if (!s.step(dt)) { stageDone.set(i, true); s.setVisible(false); } else stagesAlive = true;
        }
        if (t >= duration && !stagesAlive) {
            done = true;
            setVisible(false);
            return false;
        }
        return true;
    }

    /** Hook for bespoke per-frame animation; default does nothing. */
    protected void update(double t) { }
}
