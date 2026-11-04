package lindefense.ui.sprites.towers;

import java.awt.Color;

/**
 * Shared top-down cartoon person used by the Hero Store heroes: shoulders, torso, head, a hairstyle and a face.
 * Each hero adds its own outfit details, gear and weapon around it, so no two heroes share a silhouette.
 */
final class HeroFigure {
    private HeroFigure() { }

    /** Hairstyles. */
    enum Hair { SHORT, SPIKY, HOOD, HELMET, NEAT, CAP }

    static final Color SKIN_LIGHT = new Color(0xf1c7a0);
    static final Color SKIN_TAN = new Color(0xd9a074);
    static final Color SKIN_BROWN = new Color(0xa86b45);

    /** Draws the figure centred slightly above the plate centre; returns the head centre y. */
    static double draw(TowerArt a, double r, Color skin, Color hair, Hair style, Color outfit, Color outfitTrim) {
        double cy = -r * 0.05;
        a.ellipse(0, cy + r * 0.22, r * 0.8, r * 0.5, outfit);
        a.plain(Art.rect(-r * 0.06, cy + r * 0.0, r * 0.12, r * 0.6), outfitTrim);
        a.ellipse(-r * 0.62, cy + r * 0.2, r * 0.2, r * 0.3, Art.shade(outfit, 0.85));
        a.ellipse(r * 0.62, cy + r * 0.2, r * 0.2, r * 0.3, Art.shade(outfit, 0.85));
        double hy = cy - r * 0.2;
        if (style == Hair.HOOD) a.ellipse(0, hy + r * 0.02, r * 0.52, r * 0.5, hair);
        a.body(0, hy, r * 0.4, skin);
        switch (style) {
            case SPIKY:
                a.plain(Art.star(0, hy - r * 0.2, r * 0.42, r * 0.24, 7, -90), hair);
                break;
            case HOOD:
                a.plain(new double[] { -r * 0.46, hy - r * 0.05, -r * 0.3, hy - r * 0.48, r * 0.3, hy - r * 0.48, r * 0.46, hy - r * 0.05, r * 0.3, hy - r * 0.3, -r * 0.3, hy - r * 0.3 }, hair);
                break;
            case HELMET:
                a.poly(new double[] { -r * 0.46, hy, -r * 0.4, hy - r * 0.42, 0, hy - r * 0.58, r * 0.4, hy - r * 0.42, r * 0.46, hy, r * 0.3, hy - r * 0.18, -r * 0.3, hy - r * 0.18 }, hair);
                break;
            case NEAT:
                a.plain(new double[] { -r * 0.41, hy - r * 0.05, -r * 0.38, hy - r * 0.38, -r * 0.05, hy - r * 0.5, r * 0.3, hy - r * 0.46, r * 0.42, hy - r * 0.2, r * 0.36, hy - r * 0.08, r * 0.1, hy - r * 0.3, -r * 0.28, hy - r * 0.24 }, hair);
                break;
            case CAP:
                a.plain(new double[] { -r * 0.42, hy - r * 0.1, -r * 0.34, hy - r * 0.42, r * 0.34, hy - r * 0.42, r * 0.42, hy - r * 0.1 }, hair);
                a.plain(Art.rect(-r * 0.1, hy - r * 0.62, r * 0.6, r * 0.2), Art.shade(hair, 1.2));
                break;
            case SHORT:
            default:
                a.plain(new double[] { -r * 0.42, hy - r * 0.05, -r * 0.36, hy - r * 0.4, 0, hy - r * 0.48, r * 0.36, hy - r * 0.4, r * 0.42, hy - r * 0.05, r * 0.2, hy - r * 0.22, -r * 0.2, hy - r * 0.22 }, hair);
                break;
        }
        a.plain(Art.circle(-r * 0.14, hy + r * 0.04, r * 0.05, 6), Art.INK);
        a.plain(Art.circle(r * 0.14, hy + r * 0.04, r * 0.05, 6), Art.INK);
        a.plain(Art.rect(-r * 0.09, hy + r * 0.2, r * 0.18, r * 0.035), Art.INK);
        return hy;
    }
}
