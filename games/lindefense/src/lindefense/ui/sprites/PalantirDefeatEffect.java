package lindefense.ui.sprites;

import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import lindefense.enemy.Enemy;

/**
 * Palantir's defeat sequence, the most dramatic in the game (about 4.5 s):
 * <ol>
 *   <li>the eye flickers and its scanning beams spin out of control while a
 *       chain of explosions walks across the hull,</li>
 *   <li>the eye flares white and shatters into cyan shards,</li>
 *   <li>three huge shockwave rings (white, cyan, violet) roll out,</li>
 *   <li>the hull breaks into dark debris that rains down through smoke,</li>
 *   <li>sparkles linger as the smoke clears.</li>
 * </ol>
 */
@SuppressWarnings("serial")
public final class PalantirDefeatEffect extends Effect {
    private final GPolygon[] beams = new GPolygon[2];
    private final GOval eye;
    private final double r;

    /** Builds the sequence at the boss's current position. */
    public PalantirDefeatEffect(Enemy e) {
        super(e.getPosition().x, e.getPosition().y, 4.5);
        r = Math.max(10, e.getRadius());
        long seed = e.getId();
        for (int i = 0; i < 2; i++) {
            beams[i] = Art.poly(0, 0, 2.6 * r, -0.3 * r, 2.8 * r, 0, 2.6 * r, 0.3 * r);
            beams[i].setFilled(true);
            beams[i].setFillColor(Art.alpha(Art.PALANTIR_EYE0, 70));
            beams[i].setColor(Art.alpha(Art.PALANTIR_EYE0, 120));
            beams[i].rotate(i * 180);
            add(beams[i]);
        }
        eye = Art.ovalAt(0, 0, 0.62 * r, 0.62 * r);
        eye.setFilled(true);
        eye.setFillColor(Art.PALANTIR_EYE0);
        eye.setColor(Art.OUTLINE);
        ((Thick.Oval) eye).setLineWidth(Thick.BOSS);
        add(eye);

        // 1. walking explosions across the hull
        double[][] at = { { -0.7, -0.2 }, { 0.5, 0.3 }, { -0.3, 0.35 }, { 0.8, -0.25 }, { -0.9, 0.1 }, { 0.2, -0.4 } };
        for (int i = 0; i < at.length; i++) {
            Effect s = new Effect(at[i][0] * r, at[i][1] * r, 0.7);
            s.ring(0, 0, r * 0.08, r * 0.45, Art.alpha(Color.WHITE, 230), 0, 0.15, true);
            s.ring(0, 0, r * 0.1, r * 0.7, Art.alpha(new Color(255, 170, 60), 230), 0.02, 0.35, false);
            s.shards(6, 0, 0, r * 0.1, r * 2.2, r * 1.5, new Color(255, 190, 70), 0, 0, 0.4, seed + i * 31);
            s.shards(4, 0, 0, r * 0.18, r * 1.8, -r * 0.6, new Color(90, 90, 100, 170), 2, 0.1, 0.6, seed + i * 17);
            stage(s, 0.18 * i);
        }
        // 2. eye flare + shatter
        Effect shatter = new Effect(0, 0, 1.2);
        shatter.ring(0, 0, r * 0.3, r * 1.1, Art.alpha(Color.WHITE, 250), 0, 0.3, true);
        shatter.shards(14, 0, 0, r * 0.22, r * 3.5, r * 3, Art.PALANTIR_EYE0, 0, 0.05, 0.9, seed + 3);
        shatter.shards(8, 0, 0, r * 0.16, r * 2.5, r * 3, Color.WHITE, 0, 0.08, 0.6, seed + 4);
        stage(shatter, 1.35);
        // 3. shockwaves
        Effect waves = new Effect(0, 0, 1.4);
        waves.ring(0, 0, r * 0.2, r * 1.8, Art.alpha(Color.WHITE, 240), 0, 0.3, true);
        waves.ring(0, 0, r * 0.4, r * 4.2, Art.alpha(Color.WHITE, 230), 0, 0.8, false);
        waves.ring(0, 0, r * 0.4, r * 3.6, Art.alpha(Art.PALANTIR_EYE0, 230), 0.12, 0.8, false);
        waves.ring(0, 0, r * 0.4, r * 3.0, Art.alpha(Art.PALANTIR_EYE1, 230), 0.24, 0.8, false);
        stage(waves, 1.5);
        // 4. hull debris + smoke
        Effect debris = new Effect(0, 0, 2.0);
        debris.shards(22, 0, 0, r * 0.3, r * 3.0, r * 3.5, Art.shade(Art.PALANTIR, 0.2), 1, 0, 1.3, seed + 5);
        debris.shards(12, 0, 0, r * 0.2, r * 2.6, r * 3.5, new Color(70, 74, 92), 0, 0.03, 1.1, seed + 6);
        debris.shards(8, 0, 0, r * 0.25, r * 2.2, r * 2.5, Art.PALANTIR_EYE2, 0, 0.05, 0.9, seed + 7);
        debris.shards(10, 0, 0, r * 0.55, r * 1.0, -r * 1.0, new Color(80, 80, 92, 150), 2, 0.1, 1.6, seed + 8);
        stage(debris, 1.6);
        // 5. lingering sparkles
        Effect sparkle = new Effect(0, 0, 2.0);
        for (int i = 0; i < 10; i++) {
            Thick.Oval sp = Art.ovalAt(0, 0, r * 0.08, r * 0.08);
            sp.setLineWidth(0);
            double a = rnd(seed + i * 3) * Math.PI * 2, d = r * (0.4 + 1.4 * rnd(seed + i * 5));
            sparkle.piece(sp, Math.cos(a) * d, Math.sin(a) * d, 0, -r * 0.3, 0, Art.alpha(Color.WHITE, 240), 0.15 * i, 0.5, 0, true);
        }
        stage(sparkle, 2.4);
    }

    @Override protected void update(double t) {
        if (t < 1.4) {
            double spin = 140 + 500 * t;
            for (GPolygon b : beams) b.rotate(spin / 60.0);
            int a = (int) (70 * (1 - t / 1.4));
            for (GPolygon b : beams) {
                b.setFillColor(Art.alpha(Art.PALANTIR_EYE0, a));
                b.setColor(Art.alpha(Art.PALANTIR_EYE0, a + 40));
            }
            boolean on = ((int) (t * (8 + t * 20))) % 3 != 0;
            Color c = Art.mix(Art.PALANTIR_EYE0, Color.WHITE, Math.min(1, t / 1.4));
            eye.setFillColor(on ? c : Art.shade(c, 0.6));
            double s = r * (0.62 + 0.3 * t / 1.4);
            eye.setBounds(-s / 2, -s / 2, s, s);
        } else {
            for (GPolygon b : beams) if (b.isVisible()) b.setVisible(false);
            if (eye.isVisible()) eye.setVisible(false);
        }
    }
}
