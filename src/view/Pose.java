package view;

/**
 * A body shape for a character, made by squashing or stretching its picture.
 * Swapping between poses every few ticks gives throws, bites and bounces
 * without drawing new art.
 */
public enum Pose {
    /** Normal. */
    NEUTRAL(1.00, 1.00),
    /** Stretched tall: winding up or jumping. */
    TALL(0.93, 1.08),
    /** Stretched wide and low: lunging or releasing a throw. */
    WIDE(1.12, 0.92),
    /** Squashed: landing or taking a hit. */
    SQUASH(1.16, 0.84),
    /** Flattened: defeated. */
    FLAT(1.40, 0.35);

    /** Horizontal scale. */
    final double sx;
    /** Vertical scale. */
    final double sy;

    Pose(double sx, double sy) {
        this.sx = sx;
        this.sy = sy;
    }
}
