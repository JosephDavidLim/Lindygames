package lindefense.ui.sprites;

import acm.graphics.GArc;
import acm.graphics.GLine;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import lindefense.enemy.Boss;
import lindefense.enemy.Enemy;

/**
 * Palantir (BAD-class final boss): an enormous slate-black surveillance
 * airship built around a single all-seeing eye (the seeing-stone). Two
 * translucent scanning beams sweep ahead of it and three sensor drones orbit
 * the hull. The Palantir seeing-stone mark and "PALANTIR" wordmark sit in
 * white on the rear hull.
 *
 * <p>Phases (from {@link Boss#getPhase()}):</p>
 * <ol start="0">
 *   <li><b>Surveillance Shell</b>: eight dark hex shell plates ring the eye,
 *       which glows cyan; beams sweep slowly.</li>
 *   <li><b>Predictive Core</b>: the shell is gone and a spinning ring of violet
 *       data segments is exposed around a violet eye; beams sweep faster.</li>
 *   <li><b>Exposed Core</b>: the ring and eye burn red, the hull is cracked and
 *       sparking, the pupil narrows to a slit and the beams whip about.</li>
 * </ol>
 *
 * <p>While {@link Boss#isTelegraphing()} (Predictive Surveillance charging) the
 * iris charges white-hot under a pulsing glow, a red countdown ring around the
 * eye drains with {@link Boss#getTelegraphRemaining()}, the beams snap forward
 * and two lock-on lines reach ahead of the ship.</p>
 */
@SuppressWarnings("serial")
public final class PalantirSprite extends BossSprite {
    private static final Color[] EYE = { Art.PALANTIR_EYE0, Art.PALANTIR_EYE1, Art.PALANTIR_EYE2 };
    private static final double[] BEAM_SPEED = { 0.6, 1.1, 1.8 };

    private final GPolygon[] beams = new GPolygon[2];
    private final double[] beamDeg = new double[2];
    private final GOval[] engines = new GOval[4];
    private final GPolygon[] shell = new GPolygon[8];
    private final GPolygon[] ring = new GPolygon[12];
    private double ringSpin;
    private final GOval iris, pupil, glint, charge;
    private final GArc countdown;
    private final GLine[] lock = new GLine[2];
    private final GOval[] drones = new GOval[3];
    private final GOval[] pips = new GOval[3];
    private final GPolygon[] cracks = new GPolygon[6];
    private final GOval[] sparks = new GOval[4];
    private int shownPhase = -1;

    /** Builds the BAD-class surveillance airship for the given boss. */
    public PalantirSprite(Enemy e) {
        super(e);
        double rx = 1.0 * r, ry = 0.6 * r, hx = -0.1 * r;   // hull centre sits behind the eye
        Color hullC = Art.PALANTIR, dark = Art.shade(Art.PALANTIR, 0.4), steel = new Color(70, 74, 92);

        // scanning beams (own rotation), lock lines
        for (int i = 0; i < 2; i++) {
            beams[i] = Art.poly(0.2 * r, 0, 2.7 * r, -0.38 * r, 2.9 * r, 0, 2.7 * r, 0.38 * r);
            beams[i].setFilled(true);
            beams[i].setFillColor(Art.alpha(EYE[0], 45));
            beams[i].setColor(Art.alpha(EYE[0], 90));
            add(beams[i]);
            lock[i] = Art.line(0, 0, 1, 0);
            lock[i].setColor(Art.alpha(Art.MARK, 200));
            lock[i].setVisible(false);
            add(lock[i]);
        }
        // fins + tail
        fin(dark, hx - 0.5 * r, -1, 0.42 * r, 0.4 * r, ry);
        fin(dark, hx - 0.5 * r, 1, 0.42 * r, 0.4 * r, ry);
        fin(steel, hx - 0.2 * r, -1, 0.18 * r, 0.25 * r, ry);
        fin(steel, hx - 0.2 * r, 1, 0.18 * r, 0.25 * r, ry);
        plate(dark, hx - 0.95 * r, -0.16 * r, hx - 1.3 * r, -0.08 * r, hx - 1.3 * r, 0.08 * r, hx - 0.95 * r, 0.16 * r);
        for (int i = 0; i < 4; i++) {
            double side = (i % 2 == 0 ? -1 : 1) * (i < 2 ? 0.2 : 0.42) * r;
            engines[i] = glow(Art.alpha(EYE[0], 220), hx - 1.3 * r, side, 0.16 * r);
        }
        // gondola + antenna
        hull(dark, hx + 0.1 * r, 0.6 * r, 0.42 * r, 0.12 * r);
        plate(steel, hx + 0.1 * r, 0.7 * r, hx + 0.13 * r, 0.7 * r, hx + 0.13 * r, 0.95 * r, hx + 0.1 * r, 0.95 * r);
        glow(new Color(255, 80, 80, 230), hx + 0.115 * r, 0.97 * r, 0.07 * r);
        // hull
        hull(hullC, hx, 0, rx, ry);
        hullShine(hx, 0, rx, ry);
        band(Art.tint(hullC, 0.08), hx - 0.55 * r, 0.1 * r, rx, ry * 1.02);
        band(Art.tint(hullC, 0.08), hx + 0.62 * r, 0.1 * r, rx, ry * 1.02);
        // sensor fairing around the eye
        hull(Art.shade(hullC, 0.25), 0, 0, 0.72 * r, 0.56 * r);
        // predictive core ring (phase >= 1)
        for (int i = 0; i < 12; i++) {
            double a0 = Math.toRadians(i * 30 - 10), a1 = Math.toRadians(i * 30 + 10);
            double ri = 0.52 * r, ro = 0.64 * r;
            ring[i] = Art.poly(Math.cos(a0) * ri, Math.sin(a0) * ri, Math.cos(a0) * ro, Math.sin(a0) * ro,
                Math.cos(a1) * ro, Math.sin(a1) * ro, Math.cos(a1) * ri, Math.sin(a1) * ri);
            fixed(ring[i], EYE[1], Art.shade(EYE[1], 0.4)).setVisible(false);
        }
        // eye
        fixed(Art.ovalAt(0, 0, 0.98 * r, 0.98 * r), new Color(18, 18, 26), Art.OUTLINE);
        iris = fixed(Art.ovalAt(0, 0, 0.62 * r, 0.62 * r), EYE[0], Art.shade(EYE[0], 0.4));
        pupil = fixed(Art.ovalAt(0, 0, 0.28 * r, 0.28 * r), Color.BLACK, Color.BLACK);
        glint = fixed(Art.ovalAt(-0.14 * r, -0.16 * r, 0.16 * r, 0.11 * r), new Color(255, 255, 255, 200), new Color(255, 255, 255, 0));
        charge = Art.ovalAt(0, 0, 0.7 * r, 0.7 * r);
        charge.setFilled(true);
        charge.setVisible(false);
        add(charge);
        countdown = Art.arc(-0.78 * r, -0.78 * r, 1.56 * r, 1.56 * r, 90, 360);
        countdown.setColor(Art.MARK);
        countdown.setVisible(false);
        add(countdown);
        // shell plates (phase 0)
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45 + 22.5);
            double cx = Math.cos(a) * 0.66 * r, cy = Math.sin(a) * 0.5 * r;
            GPolygon p = new Thick.Poly();
            for (int k = 0; k < 6; k++) {
                double b = k * Math.PI / 3 + a;
                p.addVertex(cx + Math.cos(b) * 0.17 * r, cy + Math.sin(b) * 0.17 * r);
            }
            rotating(p);
            shell[i] = part(p, i % 2 == 0 ? steel : Art.shade(steel, 0.2), Art.OUTLINE);
        }
        // phase pips, drones, cracks, sparks
        for (int i = 0; i < 3; i++) {
            pips[i] = Art.ovalAt((-0.16 + i * 0.16) * r, 0.84 * r, 0.09 * r, 0.09 * r);
            pips[i].setFilled(true);
            add(pips[i]);
            drones[i] = Art.ovalAt(0, 0, 0.14 * r, 0.14 * r);
            drones[i].setFilled(true);
            drones[i].setFillColor(steel);
            drones[i].setColor(Art.OUTLINE);
            add(drones[i]);
        }
        for (int i = 0; i < 6; i++) {
            double a = 30 + i * 60;
            cracks[i] = Art.crack(hx + Math.cos(Math.toRadians(a)) * 0.5 * r, Math.sin(Math.toRadians(a)) * 0.3 * r, 0.6 * r, a + 25, i + 13);
            rotating(cracks[i]);
            fixed(cracks[i], new Color(255, 90, 70), new Color(255, 90, 70)).setVisible(false);
        }
        for (int i = 0; i < 4; i++) {
            sparks[i] = Art.ovalAt(0, 0, 0.07 * r, 0.07 * r);
            sparks[i].setFilled(true);
            sparks[i].setFillColor(new Color(255, 230, 140));
            sparks[i].setColor(new Color(255, 230, 140));
            sparks[i].setVisible(false);
            add(sparks[i]);
        }
        damageDressing(new double[][] { { hx - 0.5 * r, -0.3 * r }, { hx + 0.4 * r, 0.3 * r }, { hx - 0.2 * r, 0.45 * r }, { hx + 0.2 * r, -0.45 * r } }, 0.72, 0.88);
        // the seeing-stone mark and wordmark on the rear hull, always upright
        acm.graphics.GCompound logo = Logo.palantir(r);
        add(logo);
        orbit(logo, hx - 0.74 * r, 0.0);
        finishOverlays();
        applyPhase(0);
    }

    private void applyPhase(int ph) {
        shownPhase = ph;
        Color c = EYE[ph];
        recolor(iris, c, Art.shade(c, 0.4));
        for (GPolygon p : shell) p.setVisible(ph == 0);
        for (GPolygon p : ring) { p.setVisible(ph >= 1); recolor(p, c, Art.shade(c, 0.4)); }
        for (GOval g : engines) recolor(g, Art.alpha(c, 220), Art.alpha(c, 0));
        for (int i = 0; i < 2; i++) {
            beams[i].setFillColor(Art.alpha(c, 45));
            beams[i].setColor(Art.alpha(c, 90));
        }
        for (int i = 0; i < 3; i++) {
            Color pc = i <= ph ? EYE[i] : new Color(40, 40, 50);
            pips[i].setFillColor(pc);
            pips[i].setColor(Art.OUTLINE);
        }
        double pw = ph == 2 ? 0.12 * r : 0.28 * r;
        pupil.setSize(pw, 0.28 * r);
    }

    @Override protected void updateBody(Enemy e, double t) {
        setFacing(facingDeg);
        Boss b = boss(e);
        int ph = b == null ? 0 : Math.max(0, Math.min(2, b.getPhase()));
        if (ph != shownPhase) applyPhase(ph);
        boolean tele = b != null && b.isTelegraphing();
        double remaining = tele ? b.getTelegraphRemaining() : 0;
        double frac = tele ? Math.max(0, Math.min(1, remaining / Boss.TELEGRAPH_TIME)) : 0;

        // beams: sweep, or converge forward while telegraphing
        for (int i = 0; i < 2; i++) {
            double swing = tele ? 6 * (i == 0 ? -1 : 1) : Math.sin(t * BEAM_SPEED[ph] + i * Math.PI) * 38;
            double target = facingDeg + swing;
            double d = target - beamDeg[i];
            if (Math.abs(d) > 0.2) { beams[i].rotate(d); beamDeg[i] = target; }
            if (tele) {
                Color c = Art.mix(EYE[ph], Color.WHITE, 1 - frac);
                beams[i].setFillColor(Art.alpha(c, 60 + (int) (60 * (1 - frac))));
            } else if (((int) (t * 10)) % 10 == 0) {
                beams[i].setFillColor(Art.alpha(EYE[ph], 45));
            }
        }
        // pupil looks where it is going; core ring spins
        Art.center(pupil, dirX * 0.1 * r, dirY * 0.1 * r);
        if (ph >= 1) {
            double spin = (ph == 1 ? 40 : 110) * (t - ringSpin);
            if (ringSpin == 0) spin = 0;
            ringSpin = t;
            if (spin != 0) for (GPolygon p : ring) p.rotate(spin);
        }
        // engines pulse
        for (int i = 0; i < 4; i++) {
            engines[i].setFillColor(Art.alpha(EYE[ph], 150 + (int) (90 * Math.sin(t * (6 + 2 * ph) + i))));
        }
        // drones orbit in screen space
        for (int i = 0; i < 3; i++) {
            double a = t * (0.9 + 0.3 * ph) + i * 2.094;
            Art.center(drones[i], Math.cos(a) * 1.35 * r, Math.sin(a) * 0.85 * r);
        }
        // telegraph visuals
        charge.setVisible(tele);
        countdown.setVisible(tele);
        for (GLine l : lock) l.setVisible(tele);
        if (tele) {
            double pulse = 0.5 + 0.5 * Math.sin(t * (10 + 20 * (1 - frac)));
            double s = r * (0.7 + 0.45 * (1 - frac) + 0.08 * pulse);
            charge.setBounds(-s / 2, -s / 2, s, s);
            Color c = Art.mix(EYE[ph], Color.WHITE, 0.6 * (1 - frac) + 0.3 * pulse);
            charge.setFillColor(Art.alpha(c, 70 + (int) (100 * pulse)));
            charge.setColor(Art.alpha(Color.WHITE, 120 + (int) (100 * pulse)));
            countdown.setSweepAngle(-360 * frac);
            for (int i = 0; i < 2; i++) {
                double px = -dirY * (i == 0 ? -1 : 1) * 0.3 * r, py = dirX * (i == 0 ? -1 : 1) * 0.3 * r;
                lock[i].setStartPoint(dirX * 0.5 * r + px, dirY * 0.5 * r + py);
                double len = r * (1.5 + 2.0 * (1 - frac));
                lock[i].setEndPoint(dirX * len + px, dirY * len + py);
            }
        }
        // damage: cracks in the last phase (or heavy damage), sparks
        showCracks(cracks, ph == 2 ? Math.min(1, 0.5 + dmg) : Math.max(0, dmg - 0.5));
        boolean sparking = ph == 2;
        for (int i = 0; i < 4; i++) {
            boolean on = sparking && ((int) (t * 14 + i * 4)) % 4 == 0;
            sparks[i].setVisible(on);
            if (on) {
                double[] p = local(-0.6 * r + i * 0.35 * r, (i % 2 == 0 ? -1 : 1) * 0.35 * r);
                Art.center(sparks[i], p[0] + Math.sin(t * 29 + i) * r * 0.12, p[1] + Math.cos(t * 23 + i) * r * 0.12);
            }
        }
        updateDressing(t);
    }
}
