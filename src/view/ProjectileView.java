package view;

import acm.graphics.GCompound;
import acm.graphics.GLine;
import acm.graphics.GOval;
import acm.graphics.GRect;

import entities.DefenderType;
import entities.Projectile;

import java.awt.Color;

/**
 * Draws a thrown shot. Each thrower has its own kind of ammo (a crumpled
 * paper ball, an eraser chunk, a pencil, a snowball, a spear). Round shots are lobbed in a small arc; sharp ones fly flat. A short
 * fading trail follows each one.
 */
public class ProjectileView extends GCompound {
    private static final long serialVersionUID = 1L;
    private static final int ARC_TICKS = 20;

    private final transient Projectile shot;
    private final boolean flat;
    private int age;

    /**
     * Creates the view for a projectile.
     *
     * @param shot the shot to draw
     */
    public ProjectileView(Projectile shot) {
        this.shot = shot;
        DefenderType src = shot.getSource();
        boolean sharp = src == DefenderType.MATT || src == DefenderType.HANBO;
        this.flat = sharp;
        Color trail = trailColor(src);
        for (int i = 1; i <= 2; i++) {
            GOval t = new GOval(-9 * i - 4, 4 - i, 8 - i * 2, 8 - i * 2);
            t.setFilled(true);
            Color c = new Color(trail.getRed(), trail.getGreen(), trail.getBlue(), 120 - i * 40);
            t.setFillColor(c);
            t.setColor(c);
            add(t);
        }
        buildAmmo(src);
    }

    private void buildAmmo(DefenderType src) {
        Color ink = Ui.INK;
        if (src == DefenderType.ABHINAV) {
            GRect chunk = Ui.rect(0, 1, 15, 10, new Color(244, 150, 164));
            chunk.setColor(ink);
            add(chunk);
            add(Ui.rect(9, 1, 6, 10, new Color(70, 110, 210)));
        } else if (src == DefenderType.MATT) {
            add(Ui.rect(0, 2, 5, 6, new Color(244, 150, 164)));
            GRect body = Ui.rect(5, 2, 14, 6, new Color(250, 204, 50));
            body.setColor(ink);
            add(body);
            add(Ui.rect(19, 3, 5, 4, new Color(60, 60, 70)));
        } else if (src == DefenderType.AUSTIN) {
            GOval ball = new GOval(0, 0, 15, 15);
            ball.setFilled(true);
            ball.setFillColor(new Color(150, 210, 255));
            ball.setColor(new Color(70, 140, 210));
            add(ball);
            GOval shine = new GOval(3, 3, 5, 5);
            shine.setFilled(true);
            shine.setFillColor(Color.WHITE);
            shine.setColor(Color.WHITE);
            add(shine);
        } else if (src == DefenderType.HANBO) {
            GRect shaft = Ui.rect(0, 3, 26, 4, new Color(255, 160, 60));
            shaft.setColor(ink);
            add(shaft);
            add(Ui.rect(26, 1, 7, 8, new Color(70, 70, 80)));
        } else {
            GOval ball = new GOval(0, 0, 14, 14);
            ball.setFilled(true);
            ball.setFillColor(Color.WHITE);
            ball.setColor(ink);
            add(ball);
            GLine crease1 = new GLine(3, 4, 10, 7);
            crease1.setColor(new Color(170, 170, 180));
            add(crease1);
            GLine crease2 = new GLine(5, 10, 11, 4);
            crease2.setColor(new Color(170, 170, 180));
            add(crease2);
        }
    }

    private static Color trailColor(DefenderType src) {
        if (src == DefenderType.AUSTIN) {
            return new Color(150, 210, 255);
        } else if (src == DefenderType.MATT) {
            return new Color(250, 204, 50);
        } else if (src == DefenderType.HANBO) {
            return new Color(255, 160, 60);
        }
        return new Color(240, 240, 240);
    }

    /**
     * Moves the shot to where the game says it is, plus the lob arc.
     */
    public void update() {
        age++;
        double arc = flat ? 0 : -9 * Math.sin(Math.PI * Math.min(1.0, age / (double) ARC_TICKS));
        setLocation(shot.getX() * Layout.CELL + 10,
                Layout.TOP + shot.getRow() * Layout.CELL + Layout.CELL * 0.38 + arc);
    }

    /**
     * Gets the shot's color for impact sparks.
     *
     * @return the main color of this ammo
     */
    public Color sparkColor() {
        return trailColor(shot.getSource());
    }

    /**
     * Gets where the shot is on the canvas, for impact sparks.
     *
     * @return x in pixels
     */
    public double impactX() {
        return getX() + 8;
    }

    /**
     * Gets where the shot is on the canvas, for impact sparks.
     *
     * @return y in pixels
     */
    public double impactY() {
        return getY() + 6;
    }
}
