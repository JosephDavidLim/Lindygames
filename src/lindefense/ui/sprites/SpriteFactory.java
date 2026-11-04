package lindefense.ui.sprites;

import acm.graphics.GCompound;
import acm.graphics.GOval;
import java.awt.Color;
import lindefense.enemy.Enemy;

/** PLACEHOLDER (to be replaced by the real sprite package). */
public final class SpriteFactory {
    private SpriteFactory() { }

    public static EnemySprite create(Enemy e) {
        return new EnemySprite() {
            private final GOval body = new GOval(-e.getRadius(), -e.getRadius(), e.getRadius() * 2, e.getRadius() * 2);
            { body.setFilled(true); body.setColor(e.isBoss() ? Color.RED : Color.ORANGE); add(body); }
            @Override public void sync(Enemy en, double t) {
                setLocation(en.getPosition().x, en.getPosition().y);
                body.setColor(new Color(255, (int) (160 * en.getHpFraction()), 0, en.isCamo() ? 120 : 255));
            }
            @Override public boolean step(double dt) { return true; }
        };
    }

    private static Animated burst(Enemy e, Color c) {
        return new Burst(e.getPosition().x, e.getPosition().y, e.getRadius(), c);
    }

    public static Animated createDeathEffect(Enemy e) { return burst(e, Color.WHITE); }
    public static Animated createSplitEffect(Enemy parent) { return burst(parent, Color.YELLOW); }

    private static final class Burst extends GCompound implements Animated {
        private final GOval ring; private double t; private final double r;
        Burst(double x, double y, double r, Color c) {
            this.r = r; ring = new GOval(-r, -r, 2 * r, 2 * r); ring.setColor(c); add(ring); setLocation(x, y);
        }
        @Override public boolean step(double dt) {
            t += dt; double s = r * (1 + 2 * t);
            ring.setBounds(-s, -s, 2 * s, 2 * s);
            return t < 0.4;
        }
    }
}
