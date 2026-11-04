package lindefense.ui.sprites.towers;

import acm.graphics.GCompound;
import acm.graphics.GObject;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import lindefense.tower.Faction;
import lindefense.tower.TowerType;
import lindefense.ui.sprites.Thick;

/**
 * Base class for every tower's cartoon body. A {@code TowerArt} is built once
 * for a given (type, tier, radius) and afterwards only mutated: turrets aim,
 * spinners spin, orbiters orbit, glows pulse. Subclasses implement
 * {@link #build()} and add parts in z-order; the base class has already added
 * the ground shadow, the superclass aura (tier 4) and the faction base plate.
 *
 * <p>Coordinates are logical world units centred on the tower; {@link #r} is
 * the tower footprint radius (20 for normal towers). Tiers are clamped to 0-4.</p>
 */
@SuppressWarnings({ "serial", "this-escape" })
public abstract class TowerArt extends GCompound {
    private static final long serialVersionUID = 1L;

    /** Idle heading (straight up on screen). */
    public static final double IDLE_ANGLE = -Math.PI / 2;

    protected final TowerType type;
    protected final int tier;
    protected final double r;
    protected final Color pri, sec, priDark, priLight, secDark, secLight, trim;

    private final List<Turret> turrets = new ArrayList<>();
    private final List<GPolygon> spinners = new ArrayList<>();
    private final List<Double> spinSpeeds = new ArrayList<>();
    private final List<Orbiter> orbiters = new ArrayList<>();
    private final List<Pulser> pulsers = new ArrayList<>();
    private GPolygon auraRing;
    private GOval auraGlow;
    private double time;

    private static final class Orbiter {
        final GObject o; final double cx, cy, rad, phase, speed, w, h;
        Orbiter(GObject o, double cx, double cy, double rad, double phase, double speed) {
            this.o = o; this.cx = cx; this.cy = cy; this.rad = rad; this.phase = phase; this.speed = speed;
            this.w = o.getWidth(); this.h = o.getHeight();
        }
    }

    private static final class Pulser {
        final GOval o; final Color c; final int base, amp; final double hz, phase;
        Pulser(GOval o, Color c, int base, int amp, double hz, double phase) {
            this.o = o; this.c = c; this.base = base; this.amp = amp; this.hz = hz; this.phase = phase;
        }
    }

    /**
     * Builds the art. Subclass constructors must not add parts themselves; the
     * base constructor calls {@link #build()}.
     */
    protected TowerArt(TowerType type, int tier, double r) {
        this.type = type;
        this.tier = Math.max(0, Math.min(4, tier));
        this.r = r;
        pri = Art.rgb(type.primaryRgb);
        sec = Art.rgb(type.secondaryRgb);
        priDark = Art.shade(pri, 0.7);
        priLight = Art.shade(pri, 1.35);
        secDark = Art.shade(sec, 0.7);
        secLight = Art.shade(sec, 1.3);
        trim = this.tier >= 4 ? Art.GOLD : (this.tier >= 2 ? sec : Art.METAL_LIGHT);
        Art.shadow(this, r * 0.12, r * 0.22, r * 1.15, r * 0.85);
        if (this.tier >= 4) buildAura();
        if (drawsPlate()) buildPlate();
        build();
        for (Turret t : turrets) t.setAngle(IDLE_ANGLE);
    }

    /** Whether the faction base plate is drawn under the body (sprites skip it). */
    protected boolean drawsPlate() { return true; }

    /** Adds the body parts. Called once from the constructor. */
    protected abstract void build();

    /** Per-frame hook for subclass-specific motion; default does nothing. */
    protected void animateExtra(double t, double dt) { }

    // ------------------------------------------------------------ construction helpers

    private void buildAura() {
        auraGlow = Flat.detail(Art.glow(this, 0, 0, r * 1.85, Art.GOLD, 40), Thick.DETAIL_DECOR);
        Flat.detail(Art.glow(this, 0, 0, r * 1.5, pri, 50), Thick.DETAIL_DECOR);
        GPolygon ring = Flat.detail(Art.rawPoly(Art.gearRing(0, 0, r * 1.8, r * 1.55, r * 1.4, 12), Art.alpha(Art.GOLD, 150), Art.alpha(Art.GOLD_LIGHT, 200)), Thick.DETAIL_DECOR);
        add(ring);
        auraRing = ring;
        spinner(ring, 25);
        GPolygon ring2 = Flat.detail(Art.rawPoly(Art.ring(0, 0, r * 1.35, r * 1.25, 24), Art.alpha(Color.WHITE, 90), Art.alpha(Color.WHITE, 0)), Thick.DETAIL_DECOR);
        add(ring2);
        spinner(ring2, -40);
    }

    private void buildPlate() {
        double pr = r * (0.98 + 0.03 * tier);
        Color plate = tier >= 4 ? Art.shade(Art.SLATE, 1.1) : Art.SLATE;
        Color rim = trim;
        double[] shape;
        switch (type.faction) {
            case HERO: shape = Art.regular(0, 0, pr * 1.08, 5, 0); break;
            case MILITARY: shape = Art.regular(0, 0, pr * 1.1, 4, 45); break;
            case MAGIC: shape = Art.regular(0, 0, pr * 1.1, 4, 0); break;
            case SUPPORT: shape = Art.regular(0, 0, pr * 1.05, 6, 0); break;
            case TITAN: shape = Art.regular(0, 0, pr * 1.05, 8, 22.5); break;
            case PRIMARY: default: shape = Art.circle(0, 0, pr, 24); break;
        }
        // rim then inset plate, leaving a coloured trim band
        Art.poly(this, shape, rim);
        Art.plain(this, Art.scaled(shape, 0.8), plate);
        Art.plain(this, Art.shift(Art.scaled(shape, 0.55), -pr * 0.08, -pr * 0.08), Art.shade(plate, 1.15));
        // tier studs on the rim: one per tier, at the corners
        if (type.faction != Faction.PRIMARY) {
            int n = shape.length / 2;
            for (int i = 0; i < Math.min(tier, n); i++) {
                int k = (i * (n / Math.max(1, Math.min(tier, n)))) % n;
                double sx = shape[2 * k] * 0.9, sy = shape[2 * k + 1] * 0.9;
                Art.plain(this, Art.circle(sx, sy, r * 0.07, 8), Art.INK);
            }
        }
    }

    /** Creates, registers and adds a new aimable turret. */
    protected Turret turret() {
        Turret t = new Turret();
        turrets.add(t);
        add(t);
        return t;
    }

    /** Registers an already-added polygon (centred at its own origin) to spin continuously. */
    protected void spinner(GPolygon p, double degPerSec) {
        spinners.add(p);
        spinSpeeds.add(degPerSec);
    }

    /** Adds a polygon centred on (cx,cy) that spins in place. Returns the fill polygon. */
    protected GPolygon spinningPoly(double[] xyAtOrigin, double cx, double cy, Color fill, double degPerSec) {
        GPolygon ink = Art.rawPoly(Art.expand(xyAtOrigin, Art.OUT), Art.INK, Art.INK);
        ink.setLocation(cx, cy);
        add(ink);
        spinner(ink, degPerSec);
        GPolygon p = Art.rawPoly(xyAtOrigin, fill, Art.shade(fill, 0.75));
        p.setLocation(cx, cy);
        add(p);
        spinner(p, degPerSec);
        return p;
    }

    /**
     * Adds an outlined ring (real hole) centred on (cx,cy) that spins in place;
     * {@code n} sides, outer/inner radii. Returns the fill polygon.
     */
    protected GPolygon spinningRing(double cx, double cy, double ro, double ri, int n, Color fill, double degPerSec) {
        GPolygon ink = Art.rawPoly(Art.ring(0, 0, ro + Art.OUT * 0.7, Math.max(1, ri - Art.OUT * 0.7), n), Art.INK, Art.INK);
        ink.setLocation(cx, cy);
        add(ink);
        spinner(ink, degPerSec);
        GPolygon p = Art.rawPoly(Art.ring(0, 0, ro, ri, n), fill, Art.shade(fill, 0.75));
        p.setLocation(cx, cy);
        add(p);
        spinner(p, degPerSec);
        return p;
    }

    /** Registers an already-added object to orbit (cx,cy). */
    protected void orbiter(GObject o, double cx, double cy, double rad, double phase, double speed) {
        orbiters.add(new Orbiter(o, cx, cy, rad, phase, speed));
    }

    /** Adds an outlined orb that orbits (cx,cy); returns a compound containing it. */
    protected GCompound orbitingOrb(double cx, double cy, double rad, double orbSize, double phase, double speed, Color fill) {
        GCompound g = new GCompound();
        Art.disc(g, 0, 0, orbSize, fill);
        Art.gloss(g, -orbSize * 0.3, -orbSize * 0.35, orbSize * 0.35, orbSize * 0.25);
        add(g);
        orbiters.add(new Orbiter(g, cx, cy, rad, phase, speed));
        g.setLocation(cx + Math.cos(phase) * rad, cy + Math.sin(phase) * rad);
        return g;
    }

    /** Registers a glow oval whose alpha pulses. */
    protected void pulser(GOval o, Color c, int base, int amp, double hz) {
        pulsers.add(new Pulser(o, c, base, amp, hz, pulsers.size() * 0.7));
    }

    /** Adds a pulsing glow. */
    protected GOval pulsingGlow(double cx, double cy, double rad, Color c, int base, int amp, double hz) {
        GOval o = Flat.detail(Art.glow(this, cx, cy, rad, c, base), Thick.DETAIL_DECOR);
        pulser(o, c, base, amp, hz);
        return o;
    }

    private GOval wave;
    private Color waveColor;
    private double wavePeriod;

    /** Adds an expanding, fading ring that repeats every {@code period} seconds (pulse / aura attackers). */
    protected void pulseWave(Color c, double period) {
        wave = new Flat.Oval(-r, -r, 2 * r, 2 * r);
        wave.setFilled(false);
        wave.setColor(Art.alpha(c, 150));
        add(wave);
        waveColor = c;
        wavePeriod = period;
    }

    // thin wrappers so subclasses read cleanly
    protected GOval disc(double cx, double cy, double rad, Color fill) { return Art.disc(this, cx, cy, rad, fill); }
    protected GOval ellipse(double cx, double cy, double rx, double ry, Color fill) { return Art.ellipse(this, cx, cy, rx, ry, fill); }
    protected GRect box(double x, double y, double w, double h, Color fill) { return Art.box(this, x, y, w, h, fill); }
    protected GPolygon poly(double[] xy, Color fill) { return Art.poly(this, xy, fill); }
    protected GPolygon plain(double[] xy, Color fill) { return Art.plain(this, xy, fill); }
    protected GOval body(double cx, double cy, double rad, Color fill) { return Art.body(this, cx, cy, rad, fill); }
    protected GRect bodyBox(double x, double y, double w, double h, Color fill) { return Art.bodyBox(this, x, y, w, h, fill); }
    protected GOval gloss(double cx, double cy, double rx, double ry) { return Art.gloss(this, cx, cy, rx, ry); }
    protected GPolygon sparkle(double cx, double cy, double s) { return Art.sparkle(this, cx, cy, s); }
    protected GOval glow(double cx, double cy, double rad, Color c, int a) { return Flat.detail(Art.glow(this, cx, cy, rad, c, a), Thick.DETAIL_DECOR); }

    /** A cartoon robot eye: white sclera, coloured iris, pupil and catch-light. */
    protected void eye(double cx, double cy, double rad, Color iris) {
        disc(cx, cy, rad, Color.WHITE);
        Art.plain(this, Art.circle(cx + rad * 0.1, cy + rad * 0.1, rad * 0.6, 12), iris);
        Art.plain(this, Art.circle(cx + rad * 0.15, cy + rad * 0.15, rad * 0.3, 8), Art.INK);
        Flat.detail(Art.plain(this, Art.circle(cx - rad * 0.25, cy - rad * 0.3, rad * 0.22, 8), Color.WHITE), Thick.DETAIL_DECOR);
    }

    /** A glowing visor slit. */
    protected void visor(double cx, double cy, double w, double h, Color c) {
        box(cx - w / 2, cy - h / 2, w, h, Art.shade(c, 0.6));
        Art.plain(this, Art.rect(cx - w / 2 + 1, cy - h / 2 + 1, w - 2, h * 0.45), Art.shade(c, 1.4));
    }

    /** Antenna with a ball tip. */
    protected GOval antenna(double x, double y0, double y1, Color ball) {
        poly(Art.bar(x, y0, x, y1, r * 0.05), Art.METAL);
        return disc(x, y1, r * 0.12, ball);
    }

    /** Gold superclass crown spikes around a point. */
    protected void crown(double cx, double cy, double rad, int n) {
        poly(Art.star(cx, cy, rad, rad * 0.55, n, 0), Art.GOLD);
        Art.plain(this, Art.circle(cx, cy, rad * 0.4, 10), Art.GOLD_LIGHT);
    }

    // ------------------------------------------------------------ runtime

    /** Points every turret at the given heading (radians, screen space). */
    public void aim(double angle) {
        for (Turret t : turrets) t.setAngle(angle);
    }

    /** Current heading of the first turret, or the idle angle. */
    public double getAim() { return turrets.isEmpty() ? IDLE_ANGLE : turrets.get(0).getAngle(); }

    /** Whether this body has anything that aims. */
    public boolean hasTurret() { return !turrets.isEmpty(); }

    /**
     * Applies firing feedback. {@code sinceFire} is seconds since the last
     * shot; a short recoil and muzzle flash play for the first ~150 ms.
     */
    public void setFiring(double sinceFire) {
        double k = sinceFire < 0 ? 0 : Math.max(0, 1 - sinceFire / 0.18);
        double rec = k * k * r * 0.18;
        boolean flash = sinceFire >= 0 && sinceFire < 0.08;
        for (Turret t : turrets) {
            t.setRecoil(rec);
            t.showFlash(flash);
        }
    }

    /** Advances all built-in motion (aura, spinners, orbiters, pulsers) plus {@link #animateExtra}. */
    public void animate(double t, double dt) {
        time = t;
        for (int i = 0; i < spinners.size(); i++) spinners.get(i).rotate(spinSpeeds.get(i) * dt);
        for (Turret tu : turrets) tu.spin(dt);
        for (Orbiter ob : orbiters) {
            double a = ob.phase + t * ob.speed;
            ob.o.setLocation(ob.cx + Math.cos(a) * ob.rad - (ob.o instanceof GCompound ? 0 : ob.w / 2),
                ob.cy + Math.sin(a) * ob.rad * 0.6 - (ob.o instanceof GCompound ? 0 : ob.h / 2));
        }
        for (Pulser p : pulsers) {
            int a = (int) (p.base + p.amp * Math.sin(t * p.hz * 2 * Math.PI + p.phase));
            p.o.setFillColor(Art.alpha(p.c, a));
        }
        if (auraGlow != null) auraGlow.setFillColor(Art.alpha(Art.GOLD, (int) (35 + 25 * Math.sin(t * 3))));
        if (wave != null) {
            double ph = (t % wavePeriod) / wavePeriod;
            double rad = r * (0.7 + 1.1 * ph);
            wave.setBounds(-rad, -rad * 0.85, 2 * rad, 2 * rad * 0.85);
            wave.setColor(Art.alpha(waveColor, (int) (170 * (1 - ph))));
        }
        animateExtra(t, dt);
    }

    /** Time of the last {@link #animate} call. */
    public double getTime() { return time; }

    /** Progression tier (0-4). */
    public int getTier() { return tier; }

    /** Whether this body is the tier-4 superclass form. */
    public boolean isSuperclass() { return tier >= 4; }
}
