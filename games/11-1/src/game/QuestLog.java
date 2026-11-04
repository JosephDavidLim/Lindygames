package game;

import entities.EnemyType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks each quest's state and progress. Part of the player's {@link Profile}.
 */
public class QuestLog {
    /** Where a quest stands. */
    public enum State {
        /** Not yet accepted. */
        NEW,
        /** Accepted and in progress. */
        ACTIVE,
        /** Goal met, reward waiting. */
        READY,
        /** Reward collected. */
        CLAIMED
    }

    private final Map<Quest, State> states = new EnumMap<>(Quest.class);
    private final Map<Quest, Integer> progress = new EnumMap<>(Quest.class);

    /**
     * Gets a quest's state.
     *
     * @param q the quest
     * @return its state
     */
    public State stateOf(Quest q) {
        return states.getOrDefault(q, State.NEW);
    }

    /**
     * Gets a quest's progress.
     *
     * @param q the quest
     * @return how much of the goal is done
     */
    public int progressOf(Quest q) {
        return progress.getOrDefault(q, 0);
    }

    /**
     * Accepts a quest.
     *
     * @param q the quest
     */
    public void accept(Quest q) {
        if (stateOf(q) == State.NEW) {
            states.put(q, State.ACTIVE);
        }
    }

    /**
     * Counts a defeated enemy toward active quests.
     *
     * @param type the enemy
     * @return quests this finished
     */
    public List<Quest> recordKill(EnemyType type) {
        List<Quest> done = new ArrayList<>();
        for (Quest q : Quest.values()) {
            if (q.getTarget() == type) {
                advance(q, done);
            }
        }
        return done;
    }

    private void advance(Quest q, List<Quest> done) {
        if (stateOf(q) != State.ACTIVE) {
            return;
        }
        int now = progressOf(q) + 1;
        progress.put(q, now);
        if (now >= q.getCount()) {
            states.put(q, State.READY);
            done.add(q);
        }
    }

    /**
     * Marks a quest's reward as collected.
     *
     * @param q the quest
     * @return true if it was waiting
     */
    boolean markClaimed(Quest q) {
        if (stateOf(q) != State.READY) {
            return false;
        }
        states.put(q, State.CLAIMED);
        return true;
    }

    /**
     * Restores a saved quest.
     *
     * @param q the quest
     * @param state its state
     * @param amount its progress
     */
    void restore(Quest q, State state, int amount) {
        states.put(q, state);
        progress.put(q, Math.max(0, amount));
    }
}
