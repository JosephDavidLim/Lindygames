package lindefense;

/**
 * Single source of truth for the game's release version, shown on the title screen.
 * Bump {@link #NUMBER} together with each "alpha vX.Y" release commit.
 */
public final class Version {
    private Version() { }

    /** Release number, matching the "alpha vX.Y" commit names. */
    public static final String NUMBER = "0.5";

    /** Stage of development. */
    public static final String STAGE = "ALPHA";

    /** Label shown on the title screen, e.g. "v0.5 ALPHA". */
    public static final String LABEL = "v" + NUMBER + " " + STAGE;
}
