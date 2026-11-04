package view;

import acm.graphics.GCompound;
import acm.graphics.GImage;
import acm.graphics.GObject;

import java.awt.Image;

/**
 * Base class for an animated character on the lawn. The compound's own
 * location is the top-left of the character at normal size; the picture
 * inside it can be swapped to another {@link Pose} and nudged, so the
 * character can lean, lunge, hop and flatten without moving its health bar
 * or other attachments.
 */
public abstract class SpriteView extends GCompound {
    private static final long serialVersionUID = 1L;

    /** Normal width and height of the character in pixels. */
    protected final int size;
    private final transient Image[] frames;
    private final transient GImage bodyImage;
    private final transient GObject body;
    private final double baseX;
    private final double baseY;
    private Pose pose = Pose.NEUTRAL;

    /**
     * Creates the view's body from a sprite set.
     *
     * @param set the character's frames
     */
    protected SpriteView(Assets.SpriteSet set) {
        this.size = set.size;
        this.frames = set.frames;
        if (frames != null) {
            bodyImage = new GImage(frames[0]);
            body = bodyImage;
            baseX = -(set.canvas - size) / 2.0;
            baseY = size - set.canvas;
        } else {
            bodyImage = null;
            body = set.fallback();
            baseX = 0;
            baseY = 0;
        }
        add(body, baseX, baseY);
    }

    /**
     * Switches the body to another pose. Does nothing if the art is missing.
     *
     * @param p the new pose
     */
    protected void setPose(Pose p) {
        if (bodyImage != null && p != pose) {
            bodyImage.setImage(frames[p.ordinal()]);
            pose = p;
        }
    }

    /**
     * Shows or hides the picture, for blinking.
     *
     * @param on true to show it
     */
    protected void setBodyVisible(boolean on) {
        body.setVisible(on);
    }

    /**
     * Shifts the picture without moving the view.
     *
     * @param dx pixels right
     * @param dy pixels down
     */
    protected void setBodyOffset(double dx, double dy) {
        body.setLocation(baseX + dx, baseY + dy);
    }

    /**
     * Gets the horizontal center of the character on the canvas.
     *
     * @return x in pixels
     */
    public double centerX() {
        return getX() + size / 2.0;
    }

    /**
     * Gets the vertical center of the character on the canvas.
     *
     * @return y in pixels
     */
    public double centerY() {
        return getY() + size * 0.55;
    }

    /**
     * Called once when the defeat animation begins, so subclasses can hide
     * health bars and status rings.
     */
    protected void onDeathStart() {
    }

    /**
     * Plays one frame of the defeat animation: a hop up, a squash, then a flat
     * pancake.
     *
     * @param age ticks since the character was defeated
     * @return false once the animation is over and the view can be removed
     */
    public boolean playDeath(int age) {
        if (age == 0) {
            onDeathStart();
        }
        if (age < 4) {
            setPose(Pose.TALL);
            setBodyOffset(0, -age * 3);
        } else if (age < 9) {
            setPose(Pose.WIDE);
            setBodyOffset(0, -12 + (age - 4) * 2.4);
        } else if (age < 14) {
            setPose(Pose.FLAT);
            setBodyOffset(0, 0);
        } else {
            return false;
        }
        return true;
    }
}
