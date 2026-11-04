package lindefense.ui.sprites;

/**
 * Something that advances its own animation over time.
 *
 * <p>Enemy sprites return {@code true} for as long as the enemy they mirror is
 * alive; one-shot effects (death bursts, split animations) return {@code false}
 * once they have played out so the owning layer can remove them.</p>
 */
public interface Animated {
    /**
     * Advances the animation.
     *
     * @param dt elapsed seconds since the previous call
     * @return {@code false} when the animation is finished and the object can be removed
     */
    boolean step(double dt);
}
