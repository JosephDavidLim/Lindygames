package view;

import entities.DefenderType;

import java.util.List;

/**
 * Lets a screen ask the program to switch screens. {@code game.GameApp}
 * implements it, and tests can use a stub.
 */
public interface Navigator {
    /** Shows the title screen. */
    void showMenu();

    /** Shows the level select screen. */
    void showLevelSelect();

    /**
     * Shows the loadout screen for a level.
     *
     * @param level level number
     */
    void showLoadout(int level);

    /**
     * Starts playing a level.
     *
     * @param level level number
     * @param loadout the defenders the player chose
     */
    void startLevel(int level, List<DefenderType> loadout);

    /**
     * Shows the win or lose screen.
     *
     * @param level level that was played
     * @param won true if it was cleared
     * @param loadout the defenders used, so Retry can reuse them
     * @param party what the party earned in the level
     */
    void showResult(int level, boolean won, List<DefenderType> loadout, game.Party party);

    /**
     * Shows the character stats window.
     *
     * @param returnLevel level whose loadout to return to, or 0 for the world map
     */
    void showSquad(int returnLevel);

    /**
     * Shows the Black Market.
     *
     * @param returnLevel level whose loadout to return to, or 0 for the world map
     */
    void showStore(int returnLevel);

    /**
     * Shows a story sequence.
     *
     * @param ending true for the ending, false for the opening
     */
    void showStory(boolean ending);

    /**
     * Shows the corridor before a level, which the player walks through.
     *
     * @param level the level at its end
     */
    void showExplore(int level);

    /**
     * Shows the quest board.
     *
     * @param returnLevel level whose loadout to return to, or 0 for the map
     */
    void showQuests(int returnLevel);

    /**
     * Gets what the player keeps between levels.
     *
     * @return the profile
     */
    game.Profile getProfile();

    /**
     * Writes the profile to disk.
     */
    void saveProfile();

    /**
     * Gets the highest level the player may start.
     *
     * @return level number
     */
    int getMaxUnlocked();

    /**
     * Gets how many cards the player may bring into a level.
     *
     * @return number of card slots, 6 to 8
     */
    int getSlots();

    /**
     * Tells whether clearing the level just played earned a new card slot.
     * Only true while the result screen is being shown.
     *
     * @return true if a slot was just unlocked
     */
    boolean slotJustUnlocked();

    /**
     * Tells whether the level just played was cleared for the first time.
     * Only true while the result screen is being shown.
     *
     * @return true on a first clear
     */
    boolean firstClearJustHappened();

    /**
     * Records that a level was cleared so the next one unlocks.
     *
     * @param level the cleared level
     */
    void levelCleared(int level);
}
