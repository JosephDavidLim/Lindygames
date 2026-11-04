package lindefense.ui.sprites;

import java.awt.Color;
import lindefense.enemy.Enemy;

/**
 * Multi-stage explosion for the MOAB/BFB/ZOMG/DDT-class bosses: a chain of
 * four staggered bursts walking across the hull, hull plating and steel debris
 * raining down under gravity, rising smoke, and a final white flash with a
 * large shockwave ring in the boss's accent colour, followed by a rising smoke
 * column. Lockheed Martin opens with a decloak flash and sonic shockwave and
 * throws afterburner-coloured debris.
 */
@SuppressWarnings("serial")
public final class BossDeathEffect extends Effect {
    /** Builds the explosion at the boss's current position. */
    public BossDeathEffect(Enemy e) {
        super(e.getPosition().x, e.getPosition().y, 2.6);
        double r = Math.max(10, e.getRadius());
        Color hull = Art.colorOf(e.getType());
        Color dark = Art.shade(hull, 0.45);
        long seed = e.getId();
        boolean ddt = e.getType() == lindefense.enemy.EnemyType.LOCKHEED_MARTIN;
        if (ddt) {
            // decloak flash and sonic shockwave first
            Effect pre = new Effect(0, 0, 0.6);
            pre.ring(0, 0, r * 0.3, r * 1.6, Art.alpha(Color.WHITE, 230), 0, 0.2, true);
            pre.ring(0, 0, r * 0.5, r * 3.6, Art.alpha(Color.WHITE, 220), 0.05, 0.5, false);
            pre.ring(0, 0, r * 0.5, r * 3.0, Art.alpha(Art.LOCKHEED_GLOW, 220), 0.12, 0.5, false);
            stage(pre, 0);
            dark = Art.shade(Art.LOCKHEED, 0.2);
        }
        double[][] at = { { -0.5, -0.2 }, { 0.3, 0.25 }, { -0.1, -0.35 }, { 0.55, -0.1 } };
        for (int i = 0; i < at.length; i++) {
            Effect s = new Effect(at[i][0] * r, at[i][1] * r, 0.7);
            s.ring(0, 0, r * 0.1, r * 0.55, Art.alpha(Color.WHITE, 230), 0, 0.15, true);
            s.ring(0, 0, r * 0.15, r * 0.8, Art.alpha(new Color(255, 170, 60), 230), 0.02, 0.35, false);
            s.shards(7, 0, 0, r * 0.14, r * 2.5, r * 1.5, new Color(255, 190, 70), 0, 0, 0.4, seed + i * 31);
            s.shards(5, 0, 0, r * 0.22, r * 2.2, -r * 0.8, new Color(90, 90, 100, 170), 2, 0.1, 0.6, seed + i * 17);
            stage(s, 0.22 * i);
        }
        // final blast
        Effect fin = new Effect(0, 0, 1.2);
        fin.ring(0, 0, r * 0.2, r * 1.3, Art.alpha(Color.WHITE, 240), 0, 0.22, true);
        fin.ring(0, 0, r * 0.3, r * 2.6, Art.alpha(hull, 230), 0.02, 0.6, false);
        fin.ring(0, 0, r * 0.3, r * 2.1, Art.alpha(new Color(255, 200, 80), 200), 0.08, 0.5, false);
        fin.shards(14, 0, 0, r * 0.26, r * 3.2, r * 4, dark, 1, 0, 0.9, seed + 5);
        fin.shards(10, 0, 0, r * 0.2, r * 3.0, r * 4, Art.STEEL, 1, 0.02, 0.8, seed + 9);
        fin.shards(8, 0, 0, r * 0.18, r * 2.6, r * 2, new Color(255, 180, 60), 0, 0, 0.5, seed + 13);
        fin.shards(8, 0, 0, r * 0.4, r * 1.2, -r * 1.2, new Color(80, 80, 90, 160), 2, 0.15, 1.0, seed + 21);
        stage(fin, 0.9);
        if (ddt) {
            fin.shards(10, 0, 0, r * 0.22, r * 3.4, r * 2.5, new Color(255, 120, 40), 0, 0.03, 0.7, seed + 41);
            fin.shards(6, 0, 0, r * 0.3, r * 1.6, r * 3, new Color(255, 60, 60), 1, 0.05, 0.9, seed + 43);
        }
        // smoke column rising from the wreck
        Effect column = new Effect(0, 0, 1.9);
        for (int i = 0; i < 9; i++) {
            Thick.Oval puff = Art.ovalAt(0, 0, r * (0.35 + 0.05 * i), r * (0.35 + 0.05 * i));
            puff.setLineWidth(0);
            double dx = (rnd(seed + i * 3) - 0.5) * r * 0.5;
            column.piece(puff, dx, r * 0.1, dx * 0.4, -r * (1.2 + 0.15 * (i % 3)), -r * 0.2,
                new Color(70, 70, 80, 170), 0.1 * i, 1.1, 0, false);
        }
        stage(column, 1.0);
    }
}
