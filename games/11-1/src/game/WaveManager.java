package game;

import entities.EnemyType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builds a level's enemy schedule with a point budget, like Plants vs.
 * Zombies. Each wave gets a budget that grows every wave; the manager spends
 * it on random allowed enemies and spreads them over random lanes. The final
 * wave is a "huge wave" with a much bigger budget, and bosses arrive in it.
 */
public class WaveManager {
    private static final double FIRST_DELAY_SEC = 20;
    private static final double WAVE_GAP_SEC = 30;
    private static final double SPREAD_SEC = 15;
    private static final double HUGE_SPREAD_SEC = 10;
    private static final double HUGE_MULTIPLIER = 1.35;
    private static final int GROUP_SPACING_TICKS = 8;

    private final ArrayList<SpawnEvent> events = new ArrayList<>();
    private final int hugeWaveTick;
    private int next;

    /**
     * Builds the schedule for a level.
     *
     * @param level the level
     * @param rng random source (pass a seeded Random for repeatable waves)
     */
    public WaveManager(Level level, Random rng) {
        int[] lanes = level.getLanes();
        List<EnemyType> allowed = level.getAllowed();
        int lastWave = level.getWaves() - 1;

        for (int w = 0; w <= lastWave; w++) {
            boolean huge = w == lastWave;
            int start = Board.seconds(FIRST_DELAY_SEC + w * WAVE_GAP_SEC);
            int spread = Board.seconds(huge ? HUGE_SPREAD_SEC : SPREAD_SEC);
            int budget = level.getBaseBudget() + level.getGrowth() * w;
            if (huge) {
                budget = (int) Math.round(budget * HUGE_MULTIPLIER);
            }
            spendBudget(budget, start, spread, lanes, allowed, rng);
        }

        hugeWaveTick = Board.seconds(FIRST_DELAY_SEC + lastWave * WAVE_GAP_SEC);
        if (level.getBoss() != null) {
            int row = lanes[rng.nextInt(lanes.length)];
            events.add(new SpawnEvent(hugeWaveTick + Board.seconds(5), row, level.getBoss()));
        }
        Collections.sort(events);
    }

    private void spendBudget(int budget, int start, int spread, int[] lanes,
                             List<EnemyType> allowed, Random rng) {
        int remaining = budget;
        while (remaining > 0) {
            List<EnemyType> affordable = new ArrayList<>();
            for (EnemyType t : allowed) {
                if (t.getPoints() <= remaining) {
                    affordable.add(t);
                }
            }
            if (affordable.isEmpty()) {
                break;
            }
            EnemyType pick = affordable.get(rng.nextInt(affordable.size()));
            int row = lanes[rng.nextInt(lanes.length)];
            int tick = start + rng.nextInt(Math.max(1, spread));
            for (int k = 0; k < pick.getGroupSize(); k++) {
                events.add(new SpawnEvent(tick + k * GROUP_SPACING_TICKS, row, pick));
            }
            remaining -= pick.getPoints();
        }
    }

    /**
     * Spawns every enemy whose time has come.
     *
     * @param board the board to spawn onto
     */
    public void spawnDue(Board board) {
        while (next < events.size() && events.get(next).getTick() <= board.getTick()) {
            SpawnEvent e = events.get(next);
            board.queueSpawn(e.getType(), e.getRow(), Board.COLS + 0.5);
            next++;
        }
    }

    /**
     * Tells whether every enemy has been spawned.
     *
     * @return true when the schedule is used up
     */
    public boolean isFinished() {
        return next >= events.size();
    }

    /**
     * Gets the tick the final "huge wave" starts, for showing a banner.
     *
     * @return tick number
     */
    public int getHugeWaveTick() {
        return hugeWaveTick;
    }

    /**
     * Gets the full schedule, for debugging and tests.
     *
     * @return the sorted spawn events
     */
    public List<SpawnEvent> getEvents() {
        return events;
    }

    /**
     * Gets how much of the schedule has already spawned.
     *
     * @return 0.0 at the start, 1.0 when every enemy has appeared
     */
    public double getProgress() {
        return events.isEmpty() ? 1.0 : (double) next / events.size();
    }
}
