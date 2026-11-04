package lindefense.ui.sprites;

import acm.graphics.GCompound;
import acm.graphics.GLine;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import lindefense.enemy.Enemy;

/**
 * Lockheed Martin (DDT-class): the most kinetic boss short of Palantir. A
 * faceted black stealth delta (radar-absorbing panels, spine ridge, canards,
 * twin canted tails) with two glowing red sensor eyes at the nose, twin
 * afterburner flames that flicker and lengthen with speed, speed streaks, a
 * fading contrail and a heat-haze shimmer, blinking wingtip navigation lights
 * and a white anti-collision strobe. It is innately camo: the hull renders
 * translucent with a cloak shimmer sweeping along it, flickers briefly every
 * couple of seconds and decloaks with a flash when hit, when revealed, and on
 * its first frames (entrance sonic-boom rings). The LOCKHEED MARTIN logo panel
 * on the wing stays opaque. Panels crack and tear away, sparks fly and the
 * hull smokes as HP drops.
 */
@SuppressWarnings("serial")
public final class LockheedMartinSprite extends BossSprite {
    /** Airframe drawn at this multiple of the enemy radius. */
    private static final double VISUAL_SCALE = 1.4;
    private static final Color FLAME = new Color(255, 120, 40, 220);
    private static final Color FLAME_CORE = new Color(255, 240, 160, 230);

    private final GPolygon[] flames = new GPolygon[2];
    private final GPolygon[] cores = new GPolygon[2];
    private final double[] flameScale = { 1, 1 };
    private final GOval[] contrail = new GOval[6];
    private final GLine[] streaks = new GLine[3];
    private final GOval[] haze = new GOval[2];
    private final GOval[] sensors = new GOval[2];
    private final GOval[] sensorGlow = new GOval[2];
    private final GOval navL, navR, strobe;
    private final GPolygon shimmer, shimmer2;
    private final GPolygon wingPanel, facetTop;
    private final GPolygon[] cracks = new GPolygon[4];
    private final GOval[] sparks = new GOval[4];
    private final GOval[] boom = new GOval[2];
    private final GCompound logo;
    private double firstSync = Double.NaN;

    /** Builds the DDT-class stealth ship for the given boss. */
    public LockheedMartinSprite(Enemy e) {
        super(e);
        // The DDT has the smallest boss radius; draw its airframe larger than its
        // hit radius so it reads as a big, dramatic boss (visual only).
        final double r = this.r * VISUAL_SCALE;
        Color body = Art.LOCKHEED, facet = Art.tint(Art.LOCKHEED, 0.18), facetD = Art.shade(Art.LOCKHEED, 0.35);
        // trailing atmosphere: contrail, heat haze, streaks (all behind the hull)
        for (int i = 0; i < contrail.length; i++) {
            contrail[i] = Art.ovalAt(0, 0, r * 0.3, r * 0.3);
            contrail[i].setFilled(true);
            ((Thick.Oval) contrail[i]).setLineWidth(0);
            add(contrail[i]);
        }
        for (int i = 0; i < 2; i++) {
            haze[i] = Art.ovalAt(0, 0, r * 0.5, r * 0.3);
            haze[i].setFilled(true);
            ((Thick.Oval) haze[i]).setLineWidth(0);
            add(haze[i]);
        }
        for (int i = 0; i < 3; i++) {
            streaks[i] = Art.line(0, 0, 1, 0);
            ((Thick.Line) streaks[i]).setLineWidth(1.6);
            add(streaks[i]);
        }
        // afterburners: polygons rooted at the nozzle, pointing -x, scaled per frame
        for (int i = 0; i < 2; i++) {
            flames[i] = rotating(Art.poly(0, -0.16 * r, -0.5 * r, -0.22 * r, -1.1 * r, 0, -0.5 * r, 0.22 * r, 0, 0.16 * r));
            ((Thick.Poly) flames[i]).setLineWidth(1.4);
            flames[i].setFilled(true);
            flames[i].setFillColor(FLAME);
            flames[i].setColor(new Color(200, 60, 20, 200));
            add(flames[i]);
            cores[i] = rotating(Art.poly(0, -0.08 * r, -0.3 * r, -0.1 * r, -0.65 * r, 0, -0.3 * r, 0.1 * r, 0, 0.08 * r));
            ((Thick.Poly) cores[i]).setLineWidth(0);
            cores[i].setFilled(true);
            cores[i].setFillColor(FLAME_CORE);
            cores[i].setColor(FLAME_CORE);
            add(cores[i]);
        }
        // delta planform (nose +x), outlined heavily
        plate(body, 1.3 * r, 0, -0.55 * r, -0.9 * r, -0.9 * r, -0.8 * r, -0.65 * r, 0, -0.9 * r, 0.8 * r, -0.55 * r, 0.9 * r);
        // faceting: upper panel lit, lower panel dark, central spine ridge
        facetTop = lineWidth(plate(facet, 1.18 * r, -0.03 * r, -0.5 * r, -0.76 * r, -0.66 * r, -0.22 * r, -0.2 * r, -0.06 * r), 1.2);
        wingPanel = lineWidth(plate(facetD, 0.6 * r, 0.12 * r, -0.5 * r, 0.76 * r, -0.66 * r, 0.32 * r, -0.2 * r, 0.12 * r), 1.2);
        lineWidth(plate(Art.tint(Art.LOCKHEED, 0.3), 1.2 * r, 0, 0.2 * r, -0.05 * r, -0.6 * r, -0.03 * r, -0.6 * r, 0.03 * r, 0.2 * r, 0.05 * r), 0.8);
        // intakes, canards, twin tails
        lineWidth(plate(facetD, 0.3 * r, -0.14 * r, -0.1 * r, -0.3 * r, -0.4 * r, -0.3 * r, -0.2 * r, -0.14 * r), 1.2);
        lineWidth(plate(facetD, 0.3 * r, 0.14 * r, -0.1 * r, 0.3 * r, -0.4 * r, 0.3 * r, -0.2 * r, 0.14 * r), 1.2);
        lineWidth(plate(facetD, 0.6 * r, -0.1 * r, 0.35 * r, -0.42 * r, 0.18 * r, -0.42 * r, 0.28 * r, -0.1 * r), 1.4);
        lineWidth(plate(facetD, 0.6 * r, 0.1 * r, 0.35 * r, 0.42 * r, 0.18 * r, 0.42 * r, 0.28 * r, 0.1 * r), 1.4);
        lineWidth(plate(facet, -0.5 * r, -0.22 * r, -0.95 * r, -0.5 * r, -0.95 * r, -0.33 * r, -0.62 * r, -0.14 * r), 1.4);
        lineWidth(plate(facet, -0.5 * r, 0.22 * r, -0.95 * r, 0.5 * r, -0.95 * r, 0.33 * r, -0.62 * r, 0.14 * r), 1.4);
        // canopy and sensor eyes
        lineWidth(plate(new Color(140, 20, 35), 0.92 * r, -0.03 * r, 0.5 * r, -0.13 * r, 0.32 * r, 0, 0.5 * r, 0.13 * r), 1.2);
        for (int i = 0; i < 2; i++) {
            double sy = (i == 0 ? -1 : 1) * 0.12 * r;
            sensorGlow[i] = Art.ovalAt(0, 0, 0.36 * r, 0.36 * r);
            sensorGlow[i].setFilled(true);
            ((Thick.Oval) sensorGlow[i]).setLineWidth(0);
            add(sensorGlow[i]);
            orbit(sensorGlow[i], 1.02 * r, sy);
            sensors[i] = glow(Art.LOCKHEED_GLOW, 1.02 * r, sy, 0.14 * r);
            ((Thick.Oval) sensors[i]).setLineWidth(1.0);
            sensors[i].setColor(Art.OUTLINE);
        }
        // navigation lights and strobe
        navL = glow(new Color(255, 60, 60), -0.52 * r, -0.86 * r, 0.1 * r);
        navR = glow(new Color(60, 255, 90), -0.52 * r, 0.86 * r, 0.1 * r);
        strobe = glow(Color.WHITE, -0.1 * r, 0, 0.09 * r);
        // cloak shimmer
        shimmer = rotating(Art.poly(0, -0.12 * r, 0.14 * r, 0, 0, 0.12 * r, -0.14 * r, 0));
        ((Thick.Poly) shimmer).setLineWidth(0);
        shimmer.setFilled(true); shimmer.setFillColor(new Color(255, 255, 255, 120)); shimmer.setColor(new Color(255, 255, 255, 0));
        add(shimmer);
        shimmer2 = rotating(Art.poly(0, -0.5 * r, 0.06 * r, 0, 0, 0.5 * r, -0.06 * r, 0));
        ((Thick.Poly) shimmer2).setLineWidth(0);
        shimmer2.setFilled(true); shimmer2.setFillColor(new Color(255, 255, 255, 60)); shimmer2.setColor(new Color(255, 255, 255, 0));
        add(shimmer2);
        // damage dressing
        for (int i = 0; i < 4; i++) {
            double a = 150 + i * 60;
            cracks[i] = Art.crack(0.15 * r - i * 0.18 * r, (i % 2 == 0 ? -0.22 : 0.22) * r, 0.45 * r, a, i + 2);
            rotating(cracks[i]);
            fixed(cracks[i], new Color(255, 90, 80), new Color(255, 90, 80)).setVisible(false);
        }
        for (int i = 0; i < 4; i++) {
            sparks[i] = Art.ovalAt(0, 0, 0.08 * r, 0.08 * r);
            sparks[i].setFilled(true);
            sparks[i].setFillColor(new Color(255, 230, 120));
            sparks[i].setColor(new Color(255, 230, 120));
            ((Thick.Oval) sparks[i]).setLineWidth(0);
            sparks[i].setVisible(false);
            add(sparks[i]);
        }
        damageDressing(new double[][] { { -0.2 * r, -0.4 * r }, { -0.3 * r, 0.35 * r } }, 0.5, 0.8);
        // entrance sonic-boom rings
        for (int i = 0; i < 2; i++) {
            boom[i] = Art.ovalAt(0, 0, r, r);
            ((Thick.Oval) boom[i]).setLineWidth(2.5);
            boom[i].setVisible(false);
            add(boom[i]);
        }
        // opaque logo panel on the wing (stays upright)
        logo = Logo.lockheed(r);
        add(logo);
        orbit(logo, -0.35 * r, 0.5 * r);
        finishOverlays();
    }

    @Override protected void updateBody(Enemy e, double t) {
        final double r = this.r * VISUAL_SCALE;
        if (Double.isNaN(firstSync)) firstSync = t;
        double age = t - firstSync;
        setFacing(facingDeg);
        double nominal = e.getType().speed * Enemy.BASE_SPEED * e.getSpeedScale();
        double speed = nominal <= 0 ? 1 : Math.max(0, Math.min(1.6, e.currentSpeed(1.0) / nominal));
        double flick = 0.75 + 0.25 * Math.sin(t * 27) + 0.1 * Math.sin(t * 61);

        // afterburners
        for (int i = 0; i < 2; i++) {
            double[] root = local(-0.78 * r, (i == 0 ? -1 : 1) * 0.24 * r);
            flames[i].setLocation(root[0], root[1]);
            cores[i].setLocation(root[0], root[1]);
            double k = Math.max(0.15, (0.5 + 0.9 * speed) * flick);
            flames[i].scale(k / flameScale[i], 1);
            cores[i].scale(k / flameScale[i], 1);
            flameScale[i] = k;
            flames[i].setFillColor(Art.alpha(FLAME, (int) (160 + 80 * flick)));
        }
        // streaks, contrail and heat haze trail behind in screen space
        for (int i = 0; i < 3; i++) {
            double[] p = local(-0.9 * r, (i - 1) * 0.5 * r);
            double len = r * (0.9 + 1.4 * speed) * (0.7 + 0.3 * Math.sin(t * 19 + i * 2));
            streaks[i].setStartPoint(p[0], p[1]);
            streaks[i].setEndPoint(p[0] - dirX * len, p[1] - dirY * len);
            streaks[i].setColor(new Color(255, 120, 90, (int) (110 + 60 * speed)));
        }
        for (int i = 0; i < contrail.length; i++) {
            double ph = ((t * 1.6 + (double) i / contrail.length) % 1.0);
            double d = r * (1.3 + ph * 2.6 * (0.5 + speed));
            double side = (i % 2 == 0 ? -1 : 1) * 0.24 * r + Math.sin(t * 4 + i) * r * 0.08 * ph;
            double x = -dirX * d - dirY * side, y = -dirY * d + dirX * side;
            double s = r * (0.2 + 0.45 * ph);
            contrail[i].setBounds(x - s / 2, y - s / 2, s, s);
            Color c = new Color(230, 230, 240, (int) (90 * (1 - ph)));
            contrail[i].setFillColor(c); contrail[i].setColor(c);
        }
        for (int i = 0; i < 2; i++) {
            double d = r * (1.0 + 0.5 * i + 0.1 * Math.sin(t * 13 + i));
            double x = -dirX * d, y = -dirY * d;
            double s = r * (0.5 + 0.2 * i);
            haze[i].setBounds(x - s / 2, y - s * 0.3, s, s * 0.6);
            Color c = new Color(255, 255, 255, 25 + (int) (20 * Math.sin(t * 17 + i)));
            haze[i].setFillColor(c); haze[i].setColor(c);
        }
        // sensor eyes pulse, nav lights blink, strobe flashes
        double pulse = 0.5 + 0.5 * Math.sin(t * 7);
        for (int i = 0; i < 2; i++) {
            sensors[i].setFillColor(Art.mix(Art.LOCKHEED_GLOW, Color.WHITE, 0.4 * pulse));
            Color gc = new Color(255, 50, 50, (int) (40 + 70 * pulse));
            sensorGlow[i].setFillColor(gc); sensorGlow[i].setColor(gc);
        }
        boolean navOn = ((int) (t * 2)) % 2 == 0;
        navL.setFillColor(new Color(255, 60, 60, navOn ? 240 : 70));
        navR.setFillColor(new Color(60, 255, 90, navOn ? 240 : 70));
        boolean strobeOn = (t % 1.1) < 0.08 || ((t + 0.15) % 1.1) < 0.08;
        strobe.setFillColor(new Color(255, 255, 255, strobeOn ? 255 : 40));
        // cloak shimmer sweeps along the fuselage
        double ph = ((t * 0.8 + e.getId() * 0.1) % 1.0) * 2.0 - 1.0;
        double[] sp = local(ph * r, 0);
        shimmer.setLocation(sp[0], sp[1]);
        double ph2 = ((t * 0.55 + 0.4) % 1.0) * 2.0 - 1.0;
        double[] sp2 = local(ph2 * r, 0);
        shimmer2.setLocation(sp2[0], sp2[1]);
        // decloak: entrance, hit flash, periodic flicker
        double dc = 0;
        if (age < 0.8) dc = 1 - age / 0.8;
        if (t - e.getLastHitTime() < 0.35) dc = Math.max(dc, 1);
        double blink = (t + e.getId() * 0.7) % 2.3;
        if (blink < 0.12) dc = Math.max(dc, 0.85);
        if (dmg > 0.7 && ((int) (t * 9)) % 4 == 0) dc = Math.max(dc, 0.6);   // failing cloak
        decloak = dc;
        // entrance sonic-boom rings
        for (int i = 0; i < 2; i++) {
            double lt = age - i * 0.15;
            boolean on = lt >= 0 && lt < 0.7;
            boom[i].setVisible(on);
            if (on) {
                double f = lt / 0.7;
                double rad = r * (0.5 + 2.8 * f);
                boom[i].setBounds(-rad, -rad * 0.75, rad * 2, rad * 1.5);
                boom[i].setColor(new Color(255, 255, 255, (int) (220 * (1 - f))));
            }
        }
        // damage
        wingPanel.setVisible(dmg < 0.6);
        facetTop.setVisible(dmg < 0.85);
        showCracks(cracks, dmg);
        boolean sparking = dmg > 0.55;
        for (int i = 0; i < 4; i++) {
            boolean on = sparking && ((int) (t * 15 + i * 5)) % 3 == 0;
            sparks[i].setVisible(on);
            if (on) {
                double[] p = local(-0.3 * r + i * 0.22 * r, (i % 2 == 0 ? -1 : 1) * 0.3 * r);
                Art.center(sparks[i], p[0] + Math.sin(t * 31 + i) * r * 0.12, p[1] + Math.cos(t * 27 + i) * r * 0.12);
            }
        }
        updateDressing(t);
    }
}
