package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLine;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * The Lindefense emblem: a hexagonal circuit shield around an angular "L"
 * monogram with a glowing AI core. Built once from ACM primitives; call
 * {@link #animate(double)} to pulse the core and glow. Origin = centre.
 */
public final class LogoEmblem extends GCompound {
    private static final long serialVersionUID = 1L;

    public static final Color CYAN = new Color(0x00f0ff);
    public static final Color MAGENTA = new Color(0xff2bd6);
    public static final Color YELLOW = new Color(0xf5ff3b);
    public static final Color NAVY = new Color(0x070a16);

    private final double size;
    private final List<GPolygon> glowRings = new ArrayList<>();
    private final GOval core, coreGlow, iris;
    private final List<GOval> pads = new ArrayList<>();
    private final GPolygon monogram;

    public LogoEmblem(double size) {
        this.size = size;
        double r = size / 2;
        // outer glow rings (largest first, lowest alpha)
        for (int i = 3; i >= 1; i--) {
            GPolygon ring = hex(r * (1 + 0.06 * i));
            ring.setColor(Ui.withAlpha(CYAN, 30 + 25 * (3 - i)));
            add(ring);
            glowRings.add(ring);
        }
        GPolygon shell = hex(r);
        shell.setFilled(true);
        shell.setFillColor(NAVY);
        shell.setColor(CYAN);
        add(shell);
        GPolygon inner = hex(r * 0.78);
        inner.setColor(Ui.withAlpha(MAGENTA, 160));
        add(inner);
        // circuit traces: spokes from the inner hex to the shell with pads
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(60 * k + 30);
            double x1 = Math.cos(a) * r * 0.78, y1 = Math.sin(a) * r * 0.78;
            double x2 = Math.cos(a) * r * 0.97, y2 = Math.sin(a) * r * 0.97;
            GLine l = new GLine(x1, y1, x2, y2);
            l.setColor(Ui.withAlpha(CYAN, 190));
            add(l);
            GOval pad = new GOval(x1 - r * 0.045, y1 - r * 0.045, r * 0.09, r * 0.09);
            pad.setFilled(true); pad.setFillColor(k % 2 == 0 ? CYAN : MAGENTA); pad.setColor(NAVY);
            add(pad);
            pads.add(pad);
        }
        // a few trace elbows inside the shield
        double[][] traces = { { -0.55, -0.1, -0.55, -0.45, -0.3, -0.45 }, { 0.55, 0.1, 0.55, 0.45, 0.3, 0.45 }, { 0.2, -0.55, 0.45, -0.55, 0.45, -0.3 } };
        for (double[] t : traces) {
            GLine a = new GLine(t[0] * r, t[1] * r, t[2] * r, t[3] * r);
            GLine b = new GLine(t[2] * r, t[3] * r, t[4] * r, t[5] * r);
            a.setColor(Ui.withAlpha(CYAN, 120)); b.setColor(Ui.withAlpha(CYAN, 120));
            add(a); add(b);
            GOval dot = new GOval(t[4] * r - r * 0.03, t[5] * r - r * 0.03, r * 0.06, r * 0.06);
            dot.setFilled(true); dot.setFillColor(Ui.withAlpha(CYAN, 170)); dot.setColor(Ui.withAlpha(CYAN, 170));
            add(dot);
        }
        // "L" monogram (cut-corner, italic)
        monogram = new GPolygon();
        double u = r * 0.085; // grid unit
        double[][] pts = { { -4, -6 }, { -1.4, -6 }, { -1.4, 3.2 }, { 4.5, 3.2 }, { 4.5, 5.8 }, { 3.6, 6.6 }, { -4, 6.6 } };
        for (double[] p : pts) monogram.addVertex((p[0] + (6.6 - p[1]) * 0.14) * u, p[1] * u);
        monogram.setFilled(true);
        monogram.setFillColor(MAGENTA);
        monogram.setColor(Ui.withAlpha(Color.WHITE, 200));
        add(monogram);
        // AI core / eye in the crook of the L
        coreGlow = new GOval(r * 0.08, -r * 0.42, r * 0.44, r * 0.44);
        coreGlow.setFilled(true); coreGlow.setFillColor(Ui.withAlpha(YELLOW, 60)); coreGlow.setColor(Ui.withAlpha(YELLOW, 0));
        add(coreGlow);
        core = new GOval(r * 0.16, -r * 0.34, r * 0.28, r * 0.28);
        core.setFilled(true); core.setFillColor(YELLOW); core.setColor(Color.WHITE);
        add(core);
        iris = new GOval(r * 0.25, -r * 0.25, r * 0.10, r * 0.10);
        iris.setFilled(true); iris.setFillColor(NAVY); iris.setColor(NAVY);
        add(iris);
    }

    private static GPolygon hex(double r) {
        GPolygon p = new GPolygon();
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(60 * k);
            p.addVertex(Math.cos(a) * r, Math.sin(a) * r);
        }
        return p;
    }

    public double getEmblemSize() { return size; }

    /** Pulses the core and glow; t in seconds. */
    public void animate(double t) {
        double pulse = 0.5 + 0.5 * Math.sin(t * 2.2);
        coreGlow.setFillColor(Ui.withAlpha(YELLOW, (int) (40 + 90 * pulse)));
        core.setFillColor(pulse > 0.5 ? YELLOW : new Color(0xfff6a0));
        for (int i = 0; i < glowRings.size(); i++) {
            glowRings.get(i).setColor(Ui.withAlpha(CYAN, (int) ((30 + 25 * i) * (0.6 + 0.4 * pulse))));
        }
        int lit = (int) (t * 3) % pads.size();
        for (int i = 0; i < pads.size(); i++) {
            pads.get(i).setFillColor(i == lit ? YELLOW : (i % 2 == 0 ? CYAN : MAGENTA));
        }
    }
}
