package game;

import entities.EnemyType;

/**
 * One scheduled enemy arrival.
 */
public class SpawnEvent implements Comparable<SpawnEvent> {
    private final int tick;
    private final int row;
    private final EnemyType type;

    /**
     * Creates a spawn event.
     *
     * @param tick tick the enemy appears on
     * @param row lane index
     * @param type enemy type
     */
    public SpawnEvent(int tick, int row, EnemyType type) {
        this.tick = tick;
        this.row = row;
        this.type = type;
    }

    /**
     * Gets the spawn tick.
     *
     * @return tick number
     */
    public int getTick() {
        return tick;
    }

    /**
     * Gets the lane.
     *
     * @return row index
     */
    public int getRow() {
        return row;
    }

    /**
     * Gets the enemy type.
     *
     * @return the type
     */
    public EnemyType getType() {
        return type;
    }

    @Override
    public int compareTo(SpawnEvent other) {
        return Integer.compare(tick, other.tick);
    }
}
