package game;

/**
 * The screens the game can be on.
 */
public enum GameState {
    /** Title screen. */
    MENU,
    /** Level select. */
    SELECT,
    /** Choosing defenders before a level. */
    LOADOUT,
    /** A level is running. */
    PLAYING,
    /** A level is paused. */
    PAUSED,
    /** The level was cleared. */
    WON,
    /** The level was lost. */
    LOST
}
