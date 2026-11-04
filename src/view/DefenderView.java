package view;

import acm.graphics.GOval;
import acm.graphics.GRect;

import entities.Bomb;
import entities.Defender;
import entities.DefenderType;
import entities.Shooter;
import entities.Trigger;

import java.awt.Color;

/**
 * Draws one defender with motion: an idle bob, a lean-back wind-up and a
 * lunging release when it throws, a hop when it pays out or heals, a flinch
 * when hurt, and a shaking, pulsing bomb. It follows the defender every tick.
 */
public class DefenderView extends SpriteView {
    private static final long serialVersionUID = 1L;
    private static final int ACTION_TICKS = 14;
    private static final int FLINCH_TICKS = 4;
    private static final int FLINCH_EVERY = 18;

    private final transient Defender defender;
    private final transient GRect hpBack = Ui.rect(14, 2, 52, 6, new Color(40, 30, 30));
    private final transient GRect hpFill = Ui.rect(15, 3, 50, 4, new Color(90, 200, 90));
    private final transient GOval flash = new GOval(6, 6, 68, 68);
    private final transient GOval armedRing = new GOval(16, 62, 48, 12);
    private final transient acm.graphics.GLabel zzz = Ui.centered("Zzz", "SansSerif-bold-16", new Color(70, 110, 220), 40, 20);
    private double lastHealth;
    private int hurt;
    private int flinchCooldown;

    /**
     * Creates the view for a defender.
     *
     * @param defender the defender to draw
     */
    public DefenderView(Defender defender) {
        super(Assets.defenderSet(defender.getType(), Layout.CELL));
        this.defender = defender;
        this.lastHealth = defender.getHealth();
        flash.setFilled(true);
        flash.setFillColor(new Color(255, 60, 40, 120));
        flash.setColor(new Color(255, 60, 40, 0));
        flash.setVisible(false);
        add(flash);
        boolean mine = defender.getType().getKind() == DefenderType.Kind.MINE;
        Color ring = mine ? new Color(255, 80, 60, 190) : new Color(80, 170, 255, 190);
        armedRing.setFilled(true);
        armedRing.setFillColor(ring);
        armedRing.setColor(ring);
        armedRing.setVisible(false);
        add(armedRing);
        hpBack.setVisible(false);
        hpFill.setVisible(false);
        add(hpBack);
        add(hpFill);
        zzz.setVisible(false);
        add(zzz);
    }

    /**
     * Moves and refreshes the view.
     *
     * @param tick the current game tick, used for the idle bobbing
     */
    public void update(int tick) {
        setLocation(defender.getCol() * Layout.CELL, Layout.TOP + defender.getRow() * Layout.CELL);

        // One short flinch per bite, about in time with the enemy's chomp, instead of constant shaking.
        if (flinchCooldown > 0) {
            flinchCooldown--;
        }
        if (defender.getHealth() < lastHealth - 0.001 && flinchCooldown == 0) {
            hurt = FLINCH_TICKS;
            flinchCooldown = FLINCH_EVERY;
        }
        lastHealth = defender.getHealth();

        double bob = Math.sin((tick + defender.getCol() * 7) * 0.15) * 2;
        Pose pose = Pose.NEUTRAL;
        double dx = 0;
        double dy = bob;
        int age = defender.getActionAge();
        boolean windingUp = defender instanceof Shooter && ((Shooter) defender).isWindingUp();

        zzz.setVisible(defender.isDisabled());
        if (defender.isDisabled()) {
            pose = Pose.SQUASH;
            dy = 0;
        } else if (hurt > 0) {
            hurt--;
            pose = Pose.SQUASH;
            dx = -3;
            dy = 0;
        } else if (windingUp) {
            pose = Pose.TALL;
            dx = -3;
        } else if (age < ACTION_TICKS) {
            switch (defender.getType().role()) {
                case SHOOTER:
                case SNIPER:
                case POINT_BLANK:
                case WALL:
                    if (age < 3) {
                        pose = Pose.WIDE;
                        dx = 7;
                    } else if (age < 6) {
                        pose = Pose.SQUASH;
                        dx = 4;
                    } else if (age < 10) {
                        pose = Pose.TALL;
                        dx = 1;
                    }
                    break;
                case GENERATOR:
                case SUPPORT:
                    if (age < 4) {
                        pose = Pose.TALL;
                        dy = -9;
                    } else if (age < 7) {
                        pose = Pose.SQUASH;
                        dy = 0;
                    }
                    break;
                default:
                    break;
            }
        } else if (defender instanceof Bomb) {
            int fuse = ((Bomb) defender).getFuse();
            double amp = fuse < 20 ? 3 : 1.2;
            dx = Math.sin(tick * 1.9) * amp;
            if (fuse < 30) {
                pose = (fuse / 3) % 2 == 0 ? Pose.TALL : Pose.WIDE;
            }
        }
        setPose(pose);
        setBodyOffset(dx, dy);

        double frac = Math.max(0, defender.getHealth() / defender.getMaxHealth());
        boolean damaged = frac < 0.999 && defender.getMaxHealth() > 1;
        hpBack.setVisible(damaged);
        hpFill.setVisible(damaged);
        if (damaged) {
            hpFill.setSize(Math.max(1, 50 * frac), 4);
            hpFill.setFillColor(frac > 0.5 ? new Color(90, 200, 90)
                    : frac > 0.25 ? new Color(240, 190, 50) : new Color(220, 70, 60));
            hpFill.setColor(hpFill.getFillColor());
        }
        if (defender instanceof Trigger) {
            armedRing.setVisible(((Trigger) defender).isArmed() && (tick / 8) % 2 == 0);
        }
        if (defender instanceof Bomb) {
            flash.setVisible(((Bomb) defender).getFuse() % 10 < 5);
        }
    }

    @Override
    protected void onDeathStart() {
        hpBack.setVisible(false);
        hpFill.setVisible(false);
        flash.setVisible(false);
        armedRing.setVisible(false);
        zzz.setVisible(false);
    }
}
