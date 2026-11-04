package lindefense.ui.sprites;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import java.awt.Color;
import java.awt.Font;

/**
 * Simplified, primitive-built renditions of each boss organisation's mark.
 * Every logo is a {@link GCompound} centred on its own origin so the boss
 * sprite can {@code orbit} it on the hull while it stays upright and
 * unmirrored whichever way the ship faces. Logos are deliberately not
 * registered with the tint system, so they stay fully opaque on cloaked
 * hulls.
 *
 * <p>Sources checked: Bellarmine College Preparatory's colours are blue,
 * light blue and white and its mascot is "Bellarmine the Bell" (shield +
 * bell + "B" + wordmark banner here). Lockheed Martin uses a blue bold-italic
 * wordmark beside a fragmented star with two full points. 3Com's final mark
 * is the "3Com" wordmark with two swoosh arcs at the upper right. Palantir's
 * mark is a filled circle over two angled strokes (a seeing-stone on a
 * stand). No real logo exists for "SW Resource Consultant", so its SWRC badge
 * is invented.</p>
 */
@SuppressWarnings("serial")
public final class Logo {
    private Logo() { }

    /** Bellarmine blue. */
    public static final Color BELL_BLUE = new Color(20, 60, 160);
    /** Bellarmine light blue. */
    public static final Color BELL_LIGHT = new Color(120, 190, 250);
    /** Lockheed Martin blue. */
    public static final Color LM_BLUE = new Color(0, 91, 173);
    /** 3Com blue. */
    public static final Color COM_BLUE = new Color(0, 90, 175);

    private static GLabel text(GCompound g, String s, double size, boolean italic, Color c, double cx, double cy) {
        GLabel l = new GLabel(s);
        l.setFont(new Font("SansSerif", Font.BOLD | (italic ? Font.ITALIC : 0), Math.max(6, (int) Math.round(size))));
        l.setColor(c);
        l.setLocation(cx - l.getWidth() / 2, cy + l.getAscent() * 0.36);
        g.add(l);
        return l;
    }

    private static Thick.Rect panel(GCompound g, double cx, double cy, double w, double h, Color fill, Color line, double lw) {
        Thick.Rect p = Art.rect(cx - w / 2, cy - h / 2, w, h);
        p.setFilled(true);
        p.setFillColor(fill);
        p.setColor(line);
        p.setLineWidth(lw);
        g.add(p);
        return p;
    }

    /**
     * Bellarmine College Prep: white heater shield with a blue border, a
     * light-blue bell and a bold blue "B", above a white "BELLARMINE" banner.
     * Overall about 1.8r wide and 1.15r tall.
     */
    public static GCompound bellarmine(double r) {
        GCompound g = new GCompound();
        double sh = 0.42 * r;       // shield half-height
        Thick.Poly shield = Art.poly(-0.36 * r, -sh, 0.36 * r, -sh, 0.36 * r, 0.1 * r, 0, sh, -0.36 * r, 0.1 * r);
        shield.setFilled(true);
        shield.setFillColor(Color.WHITE);
        shield.setColor(BELL_BLUE);
        shield.setLineWidth(Math.max(2, r * 0.07));
        g.add(shield);
        // chief band
        Thick.Rect band = Art.rect(-0.33 * r, -sh + 0.02 * r, 0.66 * r, 0.14 * r);
        band.setFilled(true); band.setFillColor(BELL_BLUE); band.setColor(BELL_BLUE); band.setLineWidth(0);
        g.add(band);
        // bell: dome + lip + clapper
        Thick.Poly bell = Art.poly(-0.1 * r, 0.14 * r, -0.12 * r, 0.06 * r, -0.08 * r, -0.08 * r, -0.03 * r, -0.14 * r,
            0.03 * r, -0.14 * r, 0.08 * r, -0.08 * r, 0.12 * r, 0.06 * r, 0.1 * r, 0.14 * r);
        bell.setFilled(true); bell.setFillColor(BELL_LIGHT); bell.setColor(BELL_BLUE); bell.setLineWidth(Math.max(1.2, r * 0.035));
        bell.setLocation(0.13 * r, 0.03 * r);
        g.add(bell);
        Thick.Oval clapper = Art.ovalAt(0.13 * r, 0.2 * r, 0.07 * r, 0.07 * r);
        clapper.setFilled(true); clapper.setFillColor(BELL_BLUE); clapper.setColor(BELL_BLUE); clapper.setLineWidth(0);
        g.add(clapper);
        text(g, "B", r * 0.42, false, BELL_BLUE, -0.14 * r, 0.06 * r);
        // wordmark banner
        panel(g, 0, 0.62 * r, 1.8 * r, 0.3 * r, Color.WHITE, BELL_BLUE, Math.max(1.5, r * 0.05));
        text(g, "BELLARMINE", r * 0.25, false, BELL_BLUE, 0, 0.62 * r);
        return g;
    }

    /** SW Resource Consultant: invented SWRC monogram badge (orange disc, white ring and letters). */
    public static GCompound swrc(double r) {
        GCompound g = new GCompound();
        Thick.Oval disc = Art.ovalAt(0, 0, 0.95 * r, 0.95 * r);
        disc.setFilled(true); disc.setFillColor(Art.SW_ACCENT); disc.setColor(Art.OUTLINE); disc.setLineWidth(Math.max(2, r * 0.06));
        g.add(disc);
        Thick.Oval ring = Art.ovalAt(0, 0, 0.78 * r, 0.78 * r);
        ring.setColor(Color.WHITE); ring.setLineWidth(Math.max(1.5, r * 0.035));
        g.add(ring);
        text(g, "SWRC", r * 0.26, false, Color.WHITE, 0, -0.04 * r);
        text(g, "CONSULTING", r * 0.11, false, Color.WHITE, 0, 0.2 * r);
        return g;
    }

    /** 3Com: white rounded panel, blue "3Com" wordmark and the two swoosh arcs at the upper right. */
    public static GCompound threeCom(double r) {
        GCompound g = new GCompound();
        panel(g, 0, 0, 1.3 * r, 0.52 * r, Color.WHITE, Art.OUTLINE, Math.max(2, r * 0.045));
        text(g, "3Com", r * 0.36, false, COM_BLUE, -0.08 * r, 0.02 * r);
        for (int i = 0; i < 2; i++) {
            Thick.Arc sw = Art.arc(0.22 * r + i * 0.05 * r, -0.26 * r - i * 0.04 * r, 0.5 * r, 0.36 * r, 20, 110);
            sw.setColor(COM_BLUE);
            sw.setLineWidth(Math.max(1.5, r * 0.035));
            g.add(sw);
        }
        return g;
    }

    /**
     * Lockheed Martin: white panel with the blue fragmented star (two full
     * points, two fading rays) beside a two-line bold-italic "LOCKHEED /
     * MARTIN" wordmark.
     */
    public static GCompound lockheed(double r) {
        GCompound g = new GCompound();
        panel(g, 0, 0, 1.8 * r, 0.7 * r, Color.WHITE, Art.OUTLINE, Math.max(2, r * 0.05));
        double sx = -0.64 * r, sy = 0;
        Thick.Poly star = Art.poly(sx, sy - 0.28 * r, sx + 0.06 * r, sy - 0.06 * r, sx + 0.34 * r, sy,
            sx + 0.06 * r, sy + 0.06 * r, sx, sy + 0.12 * r, sx - 0.05 * r, sy + 0.06 * r, sx - 0.14 * r, sy, sx - 0.05 * r, sy - 0.06 * r);
        star.setFilled(true); star.setFillColor(LM_BLUE); star.setColor(LM_BLUE); star.setLineWidth(0);
        g.add(star);
        for (int i = 0; i < 2; i++) {
            Thick.Line ray = Art.line(sx - 0.06 * r, sy + 0.07 * r + i * 0.06 * r, sx - 0.22 * r, sy + 0.17 * r + i * 0.09 * r);
            ray.setColor(Art.alpha(LM_BLUE, 150 - i * 60));
            ray.setLineWidth(Math.max(1, r * 0.03));
            g.add(ray);
        }
        text(g, "LOCKHEED", r * 0.27, true, LM_BLUE, 0.2 * r, -0.13 * r);
        text(g, "MARTIN", r * 0.27, true, LM_BLUE, 0.2 * r, 0.17 * r);
        return g;
    }

    /** Palantir: the seeing-stone mark (filled circle over two angled strokes) above a "PALANTIR" wordmark, in white. */
    public static GCompound palantir(double r) {
        GCompound g = new GCompound();
        Thick.Oval stone = Art.ovalAt(0, -0.18 * r, 0.17 * r, 0.17 * r);
        stone.setFilled(true); stone.setFillColor(Color.WHITE); stone.setColor(Color.WHITE); stone.setLineWidth(0);
        g.add(stone);
        double lw = Math.max(2, r * 0.045);
        Thick.Line l1 = Art.line(-0.02 * r, -0.08 * r, -0.14 * r, 0.06 * r);
        Thick.Line l2 = Art.line(0.02 * r, -0.08 * r, 0.14 * r, 0.06 * r);
        l1.setColor(Color.WHITE); l2.setColor(Color.WHITE); l1.setLineWidth(lw); l2.setLineWidth(lw);
        g.add(l1); g.add(l2);
        text(g, "PALANTIR", r * 0.17, false, Color.WHITE, 0, 0.2 * r);
        return g;
    }
}
