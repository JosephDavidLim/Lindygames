package view;

import acm.graphics.GArc;
import acm.graphics.GCompound;
import acm.graphics.GLabel;

import entities.Avatar;

import java.awt.Color;

/**
 * Draws the player's character: a running bob, jumps and ladder climbs, a sword
 * slash, a blink after being hurt, a name tag, and a falling tombstone when
 * knocked out.
 */
public class AvatarView extends SpriteView {
    private static final long serialVersionUID = 1L;
    private static final int SIZE = 68;
    private static final int SLASH_TICKS = 7;
    private static final int TOMB_DROP_TICKS = 12;

    private final transient Avatar avatar;
    private final transient GArc slash = new GArc(92, 92, -60, 120);
    private final transient GCompound tomb = new GCompound();
    private final transient GLabel koTimer;
    private final transient GCompound hpBack = new GCompound();
    private final transient acm.graphics.GRect hpFill;
    private double lastX;
    private int koAge;
    private int lastSwing = 999;
    private boolean swungNow;
    private int frame;

    /**
     * Creates the view.
     *
     * @param avatar the character to draw
     */
    public AvatarView(Avatar avatar) {
        super(Assets.defenderSet(avatar.getSheet().getType(), SIZE));
        this.avatar = avatar;
        this.lastX = avatar.getX();

        slash.setFilled(true);
        slash.setVisible(false);
        add(slash);

        String name = avatar.getSheet().getType().getDisplayName();
        GLabel tagText = Ui.centered(name, "SansSerif-bold-11", Color.WHITE, SIZE / 2.0, SIZE + 9);
        add(Ui.round(SIZE / 2.0 - tagText.getWidth() / 2 - 5, SIZE - 2, tagText.getWidth() + 10, 15, 6,
                new Color(20, 24, 44, 215)));
        add(tagText);

        hpBack.add(Ui.rect(0, 0, 50, 6, new Color(6, 6, 18)));
        hpFill = Ui.rect(1, 1, 48, 4, Ui.HP_RED);
        hpBack.add(hpFill);
        add(hpBack, 9, -8);

        tomb.add(Ui.round(16, 8, 36, 46, 16, new Color(150, 154, 166)));
        tomb.add(Ui.centered("RIP", "SansSerif-bold-12", new Color(60, 60, 72), 34, 36));
        tomb.add(Ui.rect(10, 52, 48, 8, new Color(100, 104, 116)));
        tomb.setVisible(false);
        add(tomb);
        koTimer = Ui.centered("", "SansSerif-bold-14", Ui.GOLD, SIZE / 2.0, 4);
        koTimer.setVisible(false);
        add(koTimer);
    }

    private double rowY() {
        return Layout.TOP + avatar.getRow() * Layout.CELL + Layout.CELL - SIZE - 8;
    }

    /**
     * Moves and animates the view.
     *
     * @param tick the current game tick
     */
    public void update(int tick) {
        frame++;
        double px = avatar.getX() * Layout.CELL;
        boolean airborne = avatar.getHeight() > 0 && !avatar.isClimbing();
        setLocation(px - SIZE / 2.0, rowY() - avatar.getHeight() * Layout.CELL);

        int swing = avatar.getSwingAge();
        swungNow = swing < lastSwing;
        lastSwing = swing;

        hpFill.setSize(Math.max(1, 48 * avatar.getHealth() / avatar.getMaxHealth()), 4);
        if (avatar.isKnockedOut()) {
            showKnockedOut();
            lastX = avatar.getX();
            return;
        }
        koAge = 0;
        tomb.setVisible(false);
        koTimer.setVisible(false);
        hpBack.setVisible(true);
        setBodyVisible(!avatar.isSafe() || frame % 4 < 2);

        boolean running = Math.abs(avatar.getX() - lastX) > 0.001;
        lastX = avatar.getX();
        Pose pose = Pose.NEUTRAL;
        double dx = 0;
        double dy = Math.sin(tick * 0.15) * 1.5;
        if (running) {
            double phase = Math.abs(Math.sin(tick * 0.4));
            pose = phase < 0.25 ? Pose.SQUASH : phase > 0.8 ? Pose.TALL : Pose.NEUTRAL;
            dy = -phase * 5;
        }
        if (airborne) {
            pose = Pose.TALL;
            dy = 0;
        }
        if (avatar.isClimbing()) {
            pose = (int) (avatar.getHeight() * 8) % 2 == 0 ? Pose.TALL : Pose.NEUTRAL;
            dy = 0;
        }
        int facing = avatar.getFacing();
        if (swing < SLASH_TICKS) {
            pose = swing < 3 ? Pose.WIDE : Pose.NEUTRAL;
            dx = facing * (SLASH_TICKS - swing);
            dy = 0;
        }
        setPose(pose);
        setBodyOffset(dx, dy);
        drawSlash(swing, facing);
    }

    private void drawSlash(int swing, int facing) {
        slash.setVisible(swing < SLASH_TICKS);
        if (swing >= SLASH_TICKS) {
            return;
        }
        double fade = 1.0 - (double) swing / SLASH_TICKS;
        Color c = new Color(255, 250, 200, (int) (210 * fade));
        slash.setFillColor(c);
        slash.setColor(new Color(255, 255, 255, (int) (240 * fade)));
        double sweep = 40 + 110 * ((double) (swing + 1) / SLASH_TICKS);
        slash.setStartAngle(facing > 0 ? 80 - sweep : 100);
        slash.setSweepAngle(sweep);
        slash.setLocation(SIZE / 2.0 - 46 + facing * 16, SIZE / 2.0 - 46);
    }

    private void showKnockedOut() {
        setBodyVisible(false);
        slash.setVisible(false);
        hpBack.setVisible(false);
        koAge++;
        double drop = Math.min(1.0, koAge / (double) TOMB_DROP_TICKS);
        tomb.setLocation(0, 8 - 110 * (1 - drop * drop));
        tomb.setVisible(true);
        Ui.recenter(koTimer, "Back in " + avatar.getKoSeconds(), SIZE / 2.0);
        koTimer.setVisible(true);
    }

    /**
     * Tells whether a new swing started this frame, for its sound.
     *
     * @return true on the first frame of a swing
     */
    public boolean justSwung() {
        return swungNow;
    }
}
