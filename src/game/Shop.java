package game;

import entities.DefenderType;

import java.util.ArrayList;
import java.util.List;

/**
 * The shop bar: up to six cards chosen in the loadout, one of which can be selected.
 */
public class Shop {
    /** Cards a loadout may hold at the start of the game. */
    public static final int MAX_CARDS = Levels.BASE_SLOTS;

    private final ArrayList<Card> cards = new ArrayList<>();
    private final int slots;
    private int selected = -1;

    /**
     * Creates a shop with the starting number of slots.
     *
     * @param loadout the chosen defenders; extras past {@link #MAX_CARDS} are ignored
     */
    public Shop(List<DefenderType> loadout) {
        this(loadout, MAX_CARDS);
    }

    /**
     * Creates a shop from a loadout.
     *
     * @param loadout the chosen defenders; extras past the slot count are ignored
     * @param slots how many cards the player may bring
     */
    public Shop(List<DefenderType> loadout, int slots) {
        this.slots = slots;
        for (int i = 0; i < loadout.size() && i < slots; i++) {
            cards.add(new Card(loadout.get(i)));
        }
    }

    /**
     * Creates the shop bar for a party, so each card uses its classmate's INT.
     *
     * @param loadout the defenders the player chose
     * @param slots how many cards the player may bring
     * @param party the party in this level
     */
    public Shop(List<DefenderType> loadout, int slots, Party party) {
        this.slots = slots;
        for (int i = 0; i < loadout.size() && i < slots; i++) {
            DefenderType t = loadout.get(i);
            cards.add(new Card(t, party.hero(t).rechargeMultiplier()));
        }
    }

    /**
     * Counts every card's recharge down by one tick.
     */
    public void update() {
        for (Card c : cards) {
            c.update();
        }
    }

    /**
     * Selects a card, or deselects it if it was already selected.
     *
     * @param index card index
     */
    public void select(int index) {
        if (index < 0 || index >= cards.size()) {
            selected = -1;
        } else {
            selected = (selected == index) ? -1 : index;
        }
    }

    /**
     * Places the selected defender if the card is ready and the board accepts it.
     *
     * @param board the board
     * @param row lane index
     * @param col column index
     * @return true if a defender was placed
     */
    public boolean tryPlace(Board board, int row, int col) {
        if (selected < 0) {
            return false;
        }
        Card card = cards.get(selected);
        if (!card.isReady() || !board.place(card.getType(), row, col)) {
            return false;
        }
        card.startRecharge();
        selected = -1;
        return true;
    }

    /**
     * Tells whether the card at an index can be used right now.
     *
     * @param index card index
     * @param focusPoints the player's Focus Points
     * @return true if it is recharged and affordable
     */
    public boolean isUsable(int index, int focusPoints) {
        Card c = cards.get(index);
        return c.isReady() && focusPoints >= c.getType().getCost();
    }

    /**
     * Gets how many card slots this shop has, filled or not.
     *
     * @return the slot count
     */
    public int getSlots() {
        return slots;
    }

    /**
     * Gets the cards.
     *
     * @return the card list
     */
    public List<Card> getCards() {
        return cards;
    }

    /**
     * Gets the selected card index.
     *
     * @return index, or -1 if none is selected
     */
    public int getSelected() {
        return selected;
    }
}
