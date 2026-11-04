package game;

import entities.DefenderType;
import entities.EnemyType;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings for one level: which lanes are open, how hard the waves are, and
 * what the level unlocks.
 */
public class Level {
    private final int number;
    private final String name;
    private final int[] lanes;
    private final int waves;
    private final int baseBudget;
    private final int growth;
    private final List<EnemyType> allowed;
    private final EnemyType boss;
    private final List<DefenderType> unlocks;
    private final GameMap map;
    private final int enemyLevel;
    private final Terrain[][] terrain = new Terrain[Board.ROWS][Board.COLS];

    /**
     * Creates a level.
     *
     * @param number level number starting at 1
     * @param name title shown to the player
     * @param lanes row indexes that are in play
     * @param waves number of waves
     * @param baseBudget point budget of the first wave
     * @param growth budget added each later wave
     * @param allowed enemy types the wave builder may pick
     * @param boss boss placed in the final wave, or null
     * @param unlocks defenders unlocked by clearing this level
     * @param map the map this level is on
     * @param enemyLevel the level of the enemies here
     * @param layout one string per lane, one character per tile (see {@link Terrain}), or null for plain floor
     */
    public Level(int number, String name, int[] lanes, int waves, int baseBudget, int growth,
                 List<EnemyType> allowed, EnemyType boss, List<DefenderType> unlocks,
                 GameMap map, int enemyLevel, String[] layout) {
        this.number = number;
        this.name = name;
        this.lanes = lanes;
        this.waves = waves;
        this.baseBudget = baseBudget;
        this.growth = growth;
        this.allowed = new ArrayList<>(allowed);
        this.boss = boss;
        this.unlocks = new ArrayList<>(unlocks);
        this.map = map;
        this.enemyLevel = enemyLevel;
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                terrain[r][c] = layout == null ? Terrain.FLOOR : Terrain.fromSymbol(layout[r].charAt(c));
            }
        }
    }

    /**
     * Gets the tile at a position. Anywhere off the grid counts as plain floor.
     *
     * @param row lane index
     * @param col column index
     * @return the tile
     */
    public Terrain terrainAt(int row, int col) {
        if (row < 0 || row >= Board.ROWS || col < 0 || col >= Board.COLS) {
            return Terrain.FLOOR;
        }
        return terrain[row][col];
    }

    /**
     * Gets the map this level is on.
     *
     * @return the map
     */
    public GameMap getMap() {
        return map;
    }

    /**
     * Gets the level of the enemies here.
     *
     * @return enemy level
     */
    public int getEnemyLevel() {
        return enemyLevel;
    }

    /**
     * Tells whether a lane is in play on this level.
     *
     * @param row lane index
     * @return true if enemies and defenders may use it
     */
    public boolean hasLane(int row) {
        for (int lane : lanes) {
            if (lane == row) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the level number.
     *
     * @return number starting at 1
     */
    public int getNumber() {
        return number;
    }

    /**
     * Gets the level title.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the open lanes.
     *
     * @return row indexes
     */
    public int[] getLanes() {
        return lanes;
    }

    /**
     * Gets the number of waves.
     *
     * @return wave count
     */
    public int getWaves() {
        return waves;
    }

    /**
     * Gets the first wave's budget.
     *
     * @return starting points
     */
    public int getBaseBudget() {
        return baseBudget;
    }

    /**
     * Gets the budget increase per wave.
     *
     * @return points added each wave
     */
    public int getGrowth() {
        return growth;
    }

    /**
     * Gets the enemies the wave builder may use.
     *
     * @return allowed enemy types
     */
    public List<EnemyType> getAllowed() {
        return allowed;
    }

    /**
     * Gets the boss for this level.
     *
     * @return the boss type or null
     */
    public EnemyType getBoss() {
        return boss;
    }

    /**
     * Gets the defenders unlocked by finishing this level.
     *
     * @return unlocked defender types
     */
    public List<DefenderType> getUnlocks() {
        return unlocks;
    }
}
