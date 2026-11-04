package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/**
 * Bellarmine Student, the Textbook Thrower: a cartoon student in a navy hoodie
 * with a grey backpack, winding up a textbook. Each year adds gear: a coffee
 * cup and headphones, a varsity letter jacket and bigger throwing arm, a
 * book-stack bandolier, and the graduate superclass wears a gold-tasselled
 * mortarboard and hurls books from a two-book catapult arm.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class StudentArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    static final Color SKIN = new Color(0xf1c7a0);
    static final Color HAIR = new Color(0x5a3b2a);
    static final Color NAVY = new Color(0x1b3a6b);
    static final Color BOOK_RED = new Color(0xd9534f);
    static final Color BOOK_GREEN = new Color(0x3fa66b);

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public StudentArt(TowerType type, int tier, double r) { super(type, tier, r); }

    /** Draws the shared student figure (shoulders, backpack, head). */
    static void figure(TowerArt a, double r, int tier, Color jacket, Color sleeves) {
        double cy = -r * 0.05;
        // backpack behind shoulders
        a.box(-r * 0.4, cy - r * 0.05, r * 0.8, r * 0.55, Art.rgb(0x8d94a3));
        // shoulders / torso
        a.ellipse(0, cy + r * 0.2, r * 0.78, r * 0.5, jacket);
        if (sleeves != null) {
            a.ellipse(-r * 0.6, cy + r * 0.22, r * 0.2, r * 0.32, sleeves);
            a.ellipse(r * 0.6, cy + r * 0.22, r * 0.2, r * 0.32, sleeves);
        }
        // hood collar
        a.ellipse(0, cy - r * 0.02, r * 0.45, r * 0.22, Art.shade(jacket, 1.25));
        // head
        a.body(0, cy - r * 0.2, r * 0.42, SKIN);
        // hair cap
        a.plain(new double[] { -r * 0.42, cy - r * 0.25, -r * 0.38, cy - r * 0.52, -r * 0.1, cy - r * 0.63, r * 0.2, cy - r * 0.62, r * 0.42, cy - r * 0.4, r * 0.42, cy - r * 0.25, r * 0.2, cy - r * 0.4, -r * 0.2, cy - r * 0.38 }, HAIR);
        // eyes + smile
        a.plain(Art.circle(-r * 0.14, cy - r * 0.18, r * 0.05, 6), Art.INK);
        a.plain(Art.circle(r * 0.14, cy - r * 0.18, r * 0.05, 6), Art.INK);
        a.plain(Art.rect(-r * 0.1, cy - r * 0.02, r * 0.2, r * 0.035), Art.INK);
    }

    @Override protected void build() {
        double cy = -r * 0.05;
        Color jacket = tier >= 2 ? NAVY : Art.shade(NAVY, 1.1);
        Color sleeves = tier >= 2 ? Color.WHITE : null;
        // book-stack bandolier behind (tier 3+)
        if (tier >= 3) {
            Color[] bk = { BOOK_RED, BOOK_GREEN, Art.rgb(0xffd166), Art.rgb(0x5bc0eb) };
            for (int i = 0; i < 4; i++) box(-r * 1.05, r * 0.35 - i * r * 0.17, r * 0.5, r * 0.15, bk[i]);
        }
        // throwing arm with textbook(s)
        Turret tu = turret();
        double len = r * (0.75 + 0.12 * tier);
        tu.poly(Art.bar(0, 0, len * 0.7, 0, r * (0.1 + 0.02 * Math.min(tier, 2))), tier >= 2 ? Color.WHITE : jacket);
        tu.poly(Art.circle(len * 0.7, 0, r * 0.14, 10), SKIN);
        double bw = r * 0.45, bh = r * 0.32;
        tu.poly(Art.rect(len * 0.6, -bh / 2, bw, bh), BOOK_RED);
        tu.poly(Art.rect(len * 0.6, -bh / 2, bw * 0.18, bh), Art.shade(BOOK_RED, 0.7));
        tu.plain(Art.rect(len * 0.6 + bw * 0.3, -bh * 0.3, bw * 0.55, bh * 0.1), Color.WHITE);
        if (tier >= 4) {
            tu.poly(Art.rect(len * 0.6 + bw * 0.2, -bh * 0.95, bw, bh * 0.45), BOOK_GREEN);
            tu.poly(Art.rect(len * 0.6 + bw * 0.2, bh * 0.5, bw, bh * 0.45), Art.rgb(0x5bc0eb));
        }
        tu.setFlash(tu.plain(Art.star(len + bw * 0.9, 0, r * 0.25, r * 0.1, 4, 0), Art.alpha(Color.WHITE, 220)));
        figure(this, r, tier, jacket, sleeves);
        // varsity letter (tier 2+)
        if (tier >= 2) plain(Art.rect(-r * 0.3, cy + r * 0.15, r * 0.14, r * 0.26), tier >= 4 ? Art.GOLD : Art.rgb(0xffd166));
        // coffee + headphones (tier 1+)
        if (tier >= 1) {
            box(-r * 0.95, cy + r * 0.05, r * 0.26, r * 0.3, Color.WHITE);
            plain(Art.rect(-r * 0.95, cy + r * 0.12, r * 0.26, r * 0.08), BOOK_GREEN);
            poly(Art.bar(-r * 0.42, cy - r * 0.35, r * 0.42, cy - r * 0.35, r * 0.05), Art.rgb(0x2a3140));
            disc(-r * 0.44, cy - r * 0.2, r * 0.1, Art.rgb(0x2a3140));
            disc(r * 0.44, cy - r * 0.2, r * 0.1, Art.rgb(0x2a3140));
        }
        // mortarboard (tier 4)
        if (tier >= 4) {
            poly(Art.regular(0, cy - r * 0.55, r * 0.6, 4, 45), Art.INK);
            plain(Art.shift(Art.regular(0, cy - r * 0.55, r * 0.45, 4, 45), -r * 0.05, -r * 0.05), Art.rgb(0x2a3140));
            plain(Art.circle(0, cy - r * 0.55, r * 0.06, 8), Art.GOLD);
            plain(Art.bar(0, cy - r * 0.55, r * 0.45, cy - r * 0.15, r * 0.03), Art.GOLD);
            plain(Art.circle(r * 0.45, cy - r * 0.15, r * 0.07, 8), Art.GOLD);
        }
        sparkle(-r * 0.3, cy - r * 0.5, r * 0.08);
    }
}
