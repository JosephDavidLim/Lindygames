package view;

import acm.graphics.GLabel;
import acm.graphics.GOval;
import acm.graphics.GRect;

import entities.Enemy;
import entities.EnemyType;
import entities.ReportCard;

import java.awt.Color;

/**
 * Draws one enemy with motion: a squash-and-stretch waddle while walking, a
 * wind-up and lunging chomp while biting, knockback when hit, a dizzy lean
 * when stunned, and a hop-and-flatten defeat. It also shows a health bar once
 * hurt, a blue ring while slowed, and a live letter grade on report cards.
 */
public class EnemyView extends SpriteView {
    private static final long serialVersionUID = 1L;

    private final transient Enemy enemy;
    private final transient GRect hpBack;
    private final transient GRect hpFill;
    private final transient GOval slowRing;
    private final transient GOval flashOval;
    private final transient GOval stunRing;
    private transient GLabel grade;
    private double lastHealth;
    private int flash;
    private int frame;
    private int biteFrame;

    /**
     * Creates the view for an enemy.
     *
     * @param enemy the enemy to draw
     */
    public EnemyView(Enemy enemy) {
        super(Assets.enemySet(enemy.getType(), enemy.getType().getKind() == EnemyType.Kind.BOSS ? 124 : 84));
        this.enemy = enemy;
        this.lastHealth = enemy.getHealth();

        slowRing = new GOval(size * 0.12, size * 0.86, size * 0.76, size * 0.12);
        slowRing.setFilled(true);
        slowRing.setFillColor(new Color(110, 190, 255, 150));
        slowRing.setColor(new Color(110, 190, 255, 220));
        slowRing.setVisible(false);
        add(slowRing);

        flashOval = new GOval(size * 0.2, size * 0.2, size * 0.6, size * 0.6);
        flashOval.setFilled(true);
        flashOval.setFillColor(new Color(255, 255, 255, 140));
        flashOval.setColor(new Color(255, 255, 255, 0));
        flashOval.setVisible(false);
        add(flashOval);

        stunRing = new GOval(size * 0.22, 2, size * 0.56, size * 0.12);
        stunRing.setFilled(true);
        stunRing.setFillColor(new Color(255, 225, 60, 210));
        stunRing.setColor(new Color(200, 150, 20));
        stunRing.setVisible(false);
        add(stunRing);

        double barW = size * 0.6;
        hpBack = Ui.rect(size * 0.2, 0, barW, 7, new Color(40, 30, 30));
        hpFill = Ui.rect(size * 0.2 + 1, 1, barW - 2, 5, new Color(220, 70, 60));
        hpBack.setVisible(false);
        hpFill.setVisible(false);
        add(hpBack);
        add(hpFill);


        if (enemy instanceof ReportCard) {
            grade = Ui.centered("A", "SansSerif-bold-" + (int) (size * 0.3), new Color(40, 150, 60),
                    size * 0.5, size * 0.62);
            add(grade);
        }
    }

    /**
     * Moves and refreshes the view.
     */
    public void update() {
        double px = enemy.getX() * Layout.CELL;
        setLocation(px - size / 2.0,
                Layout.TOP + enemy.getRow() * Layout.CELL + Layout.CELL - size + 4);
        frame++;

        if (enemy.getHealth() < lastHealth - 0.01) {
            flash = 3;
        }
        lastHealth = enemy.getHealth();

        // walking waddle: stretch up in the air, squash on landing
        double phase = Math.abs(Math.sin(px * 0.09));
        Pose pose = phase < 0.2 ? Pose.SQUASH : phase > 0.85 ? Pose.TALL : Pose.NEUTRAL;
        double dx = 0;
        double dy = -phase * 3;

        if (enemy.isBiting()) {
            biteFrame++;
            int c = biteFrame % 18;
            dy = 0;
            if (c < 9) {
                pose = Pose.TALL;
                dx = 3;
            } else if (c < 13) {
                pose = Pose.WIDE;
                dx = -8;
            } else {
                pose = Pose.SQUASH;
                dx = -2;
            }
        } else {
            biteFrame = 0;
        }
        if (enemy.isStunned()) {
            pose = Pose.TALL;
            dx = Math.sin(frame * 1.3) * 3;
            dy = 0;
        }
        if (flash > 0) {
            pose = Pose.SQUASH;
            dx = 3;
            dy = 0;
        }
        setPose(pose);
        setBodyOffset(dx, dy);

        flashOval.setVisible(flash > 0);
        if (flash > 0) {
            flash--;
        }
        stunRing.setVisible(enemy.isStunned());

        double frac = Math.max(0, enemy.getHealth() / enemy.getMaxHealth());
        boolean hurtNow = frac < 0.999;
        hpBack.setVisible(hurtNow);
        hpFill.setVisible(hurtNow);
        if (hurtNow) {
            hpFill.setSize(Math.max(1, (size * 0.6 - 2) * frac), 5);
        }
        slowRing.setVisible(enemy.isSlowed());

        if (grade != null) {
            char g = ((ReportCard) enemy).getGrade();
            Color c = g == 'A' || g == 'B' ? new Color(40, 150, 60)
                    : g == 'C' || g == 'D' ? new Color(220, 140, 30) : new Color(210, 40, 40);
            grade.setColor(c);
            Ui.recenter(grade, String.valueOf(g), size * 0.5);
        }
    }

    @Override
    protected void onDeathStart() {
        hpBack.setVisible(false);
        hpFill.setVisible(false);
        slowRing.setVisible(false);
        stunRing.setVisible(false);
        flashOval.setVisible(false);
        if (grade != null) {
            grade.setVisible(false);
        }
    }
}
