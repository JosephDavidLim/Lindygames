package game;

import entities.Avatar;
import entities.Coffee;
import entities.Defender;
import entities.DefenderType;
import entities.Drop;
import entities.Effect;
import entities.Enemy;
import entities.EnemyType;
import entities.Popup;
import entities.Projectile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * The game world for one level. It owns the 5x9 defender grid and the lists of
 * live enemies, projectiles and coffees, and advances them one tick at a time.
 * It knows nothing about drawing, so it can be tested without ACM.
 */
public class Board {
    /** Number of lanes. */
    public static final int ROWS = 5;
    /** Number of columns. */
    public static final int COLS = 9;
    /** Game ticks per second. */
    public static final int TICKS_PER_SECOND = 30;

    private static final int START_FOCUS = 100;
    private static final int MAX_POPUPS = 60;
    private static final int HALL_PASS_FOCUS = 50;
    private static final int REFILL_FOCUS = 10;

    private final Level level;
    private final Random rng;
    private final WaveManager waves;
    private final Defender[][] grid = new Defender[ROWS][COLS];
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<Coffee> coffees = new ArrayList<>();
    private final ArrayList<Effect> effects = new ArrayList<>();
    private final ArrayList<Enemy> pendingSpawns = new ArrayList<>();
    private final boolean[] highlighterUsed = new boolean[ROWS];
    private final ArrayList<Popup> popups = new ArrayList<>();
    private final ArrayList<Hero> levelUpQueue = new ArrayList<>();
    private final ArrayList<Drop> drops = new ArrayList<>();
    private final Party party;
    private final Avatar avatar;
    private final int coffeeIntervalTicks;

    private int focusPoints = START_FOCUS;
    private int tick;
    private int coffeeTimer;
    private boolean won;
    private boolean lost;

    /**
     * Creates a board for a level.
     *
     * @param level the level to play
     * @param rng random source (seed it for repeatable games)
     */
    public Board(Level level, Random rng) {
        this(level, rng, null);
    }

    /**
     * Creates a board where defenders use character sheets and kills pay out.
     *
     * @param level the level to play
     * @param rng random source (seed it for repeatable games)
     * @param party the classmates in this level, or null for base stats and no rewards
     */
    public Board(Level level, Random rng, Party party) {
        this.level = level;
        this.rng = rng;
        this.party = party;
        this.waves = new WaveManager(level, rng);
        this.coffeeIntervalTicks = seconds(level.getMap().getCoffeeSec());
        this.coffeeTimer = coffeeIntervalTicks;
        if (party != null && party.has(KeyItem.HALL_PASS)) {
            focusPoints += HALL_PASS_FOCUS;
        }
        int[] lanes = level.getLanes();
        this.avatar = party == null ? null : new Avatar(party.mainHero(), lanes[lanes.length / 2]);
    }

    /**
     * Converts seconds to ticks.
     *
     * @param sec time in seconds
     * @return time in ticks
     */
    public static int seconds(double sec) {
        return (int) Math.round(sec * TICKS_PER_SECOND);
    }

    /**
     * Places a defender if the cell is open, the lane is in play and the
     * player can afford it. Recharge is checked by {@link Shop}.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     * @return true if it was placed
     */
    public boolean place(DefenderType type, int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS || !level.hasLane(row)) {
            return false;
        }
        if (!level.terrainAt(row, col).isBuildable() || grid[row][col] != null || focusPoints < type.getCost()) {
            return false;
        }
        focusPoints -= type.getCost();
        Defender d = type.create(row, col);
        d.assignHero(party == null ? null : party.hero(type));
        d.onPlaced(this);
        grid[row][col] = d;
        return true;
    }

    /**
     * Advances the whole world by one tick.
     */
    public void update() {
        if (won || lost) {
            return;
        }
        tick++;
        waves.spawnDue(this);
        updateCoffees();
        updateDrops();
        if (avatar != null) {
            avatar.update(this);
        }

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null) {
                    grid[r][c].ageAction();
                    if (grid[r][c].tickDisable()) {
                        grid[r][c].update(this);
                    }
                }
            }
        }

        Iterator<Projectile> pi = projectiles.iterator();
        while (pi.hasNext()) {
            Projectile p = pi.next();
            p.update(this);
            if (p.isSpent()) {
                pi.remove();
            }
        }

        for (Enemy e : enemies) {
            e.update(this);
        }
        checkBreakthroughs();

        removeDead();
        Iterator<Effect> fx = effects.iterator();
        while (fx.hasNext()) {
            Effect e = fx.next();
            e.update();
            if (e.isDone()) {
                fx.remove();
            }
        }
        enemies.addAll(pendingSpawns);
        pendingSpawns.clear();

        if (waves.isFinished() && enemies.isEmpty()) {
            won = true;
        }
        if (won || lost) {
            for (Drop d : new ArrayList<>(drops)) {
                collectDrop(d);
            }
        }
    }

    private void updateDrops() {
        Iterator<Drop> it = drops.iterator();
        while (it.hasNext()) {
            Drop d = it.next();
            d.update();
            if (d.isExpired()) {
                it.remove();
            }
        }
    }

    /**
     * Picks up a drop and banks what is inside.
     *
     * @param d the drop
     */
    public void collectDrop(Drop d) {
        if (party != null && drops.remove(d)) {
            party.collect(d.getCredits(), d.getGear());
            addPopup(new Popup(d.getGear() != null ? Popup.Kind.GEAR : Popup.Kind.COIN, d.getRow(), d.getX(),
                    d.getCredits(), d.getGear()));
        }
    }

    /**
     * Spills part of the level's Credits where the avatar fell. It has a few
     * seconds after returning to run back for them.
     *
     * @param row lane where it fell
     * @param x position where it fell
     */
    public void avatarKnockedOut(int row, double x) {
        int spilled = party.spillCredits();
        if (spilled > 0) {
            drops.add(new Drop(row, x, spilled, null));
        }
    }

    /**
     * Finds the defender closest to an enemy, ahead of it in a lane.
     *
     * @param row lane index
     * @param x the enemy's position
     * @return the defender, or null if the way is clear
     */
    public Defender frontDefender(int row, double x) {
        int start = Math.min(COLS - 1, (int) Math.floor(x));
        for (int c = start; c >= 0; c--) {
            Defender d = defenderAt(row, c);
            if (d != null && !d.isDead()) {
                return d;
            }
        }
        return null;
    }

    /**
     * Picks random living defenders from anywhere on the board.
     *
     * @param count how many at most
     * @return the defenders
     */
    public List<Defender> randomDefenders(int count) {
        List<Defender> all = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null && !grid[r][c].isDead() && grid[r][c].isBlocking()) {
                    all.add(grid[r][c]);
                }
            }
        }
        java.util.Collections.shuffle(all, rng);
        return new ArrayList<>(all.subList(0, Math.min(count, all.size())));
    }

    /**
     * Stops every defender in a lane acting for a while.
     *
     * @param row lane index
     * @param ticks how long
     */
    public void disableLane(int row, int ticks) {
        for (int c = 0; c < COLS; c++) {
            Defender d = defenderAt(row, c);
            if (d != null) {
                d.disable(ticks);
            }
        }
    }

    /**
     * Finds the boss on the board.
     *
     * @return the on-screen boss, or null
     */
    public Enemy findBoss() {
        for (Enemy e : enemies) {
            if (e.getType().getKind() == EnemyType.Kind.BOSS && e.getX() < COLS && !e.isDead()) {
                return e;
            }
        }
        return null;
    }

    /**
     * Gets the drops lying on the floor.
     *
     * @return the drop list
     */
    public List<Drop> getDrops() {
        return drops;
    }

    /**
     * Gets the character the player steers.
     *
     * @return the avatar, or null on a board with no party
     */
    public Avatar getAvatar() {
        return avatar;
    }

    private void updateCoffees() {
        coffeeTimer--;
        if (coffeeTimer <= 0) {
            coffeeTimer = coffeeIntervalTicks;
            int[] lanes = level.getLanes();
            double y = lanes[rng.nextInt(lanes.length)] + 0.5;
            coffees.add(new Coffee(0.5 + rng.nextDouble() * (COLS - 1), y));
        }
        Iterator<Coffee> it = coffees.iterator();
        while (it.hasNext()) {
            Coffee c = it.next();
            c.update();
            if (c.isExpired()) {
                it.remove();
            }
        }
    }

    private void checkBreakthroughs() {
        for (Enemy e : enemies) {
            if (e.isDead() || e.getX() >= 0) {
                continue;
            }
            int row = e.getRow();
            if (highlighterUsed[row]) {
                lost = true;
                return;
            }
            highlighterUsed[row] = true;
            for (Enemy other : enemies) {
                if (other.getRow() == row) {
                    other.removeWithoutReward();
                }
            }
        }
    }

    private void removeDead() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null && grid[r][c].isDead()) {
                    grid[r][c] = null;
                }
            }
        }
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (e.isDead()) {
                it.remove();
                if (party != null && e.givesReward()) {
                    payOut(e);
                }
            }
        }
    }

    private void payOut(Enemy e) {
        for (Hero h : party.award(e.getType(), e.getEnemyLevel())) {
            levelUp(h);
        }
        drops.add(new Drop(e.getRow(), e.getX(), party.creditsFor(e.getType(), e.getEnemyLevel()), null));
        Gear found = party.rollDrop(e.getType());
        if (found != null) {
            drops.add(new Drop(e.getRow(), e.getX() + 0.3, 0, found));
        }
    }

    private void levelUp(Hero h) {
        levelUpQueue.add(h);
        if (avatar != null && avatar.getSheet() == h) {
            avatar.onLevelUp();
        }
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Defender d = grid[r][c];
                if (d != null && d.getHero() == h && !d.isDead()) {
                    d.onLevelUp();
                    addEffect(new Effect(Effect.Kind.LEVEL_UP, r, c, 0, 30));
                }
            }
        }
    }

    /**
     * Hits an enemy and shows the damage. The hit may be critical, doubling it.
     *
     * @param e the enemy
     * @param damage damage before any crit
     * @param critChance chance from 0 to 1
     */
    public void strike(Enemy e, double damage, double critChance) {
        boolean crit = roll(critChance);
        double dealt = crit ? damage * 2 : damage;
        e.takeDamage(dealt);
        addPopup(new Popup(crit ? Popup.Kind.CRIT : Popup.Kind.DAMAGE, e.getRow(), e.getX(),
                (int) Math.round(dealt), null));
    }

    /**
     * Rolls a chance. A chance of zero uses no random number, so games with
     * no stats replay identically for a given seed.
     *
     * @param chance chance from 0 to 1
     * @return true if it succeeded
     */
    public boolean roll(double chance) {
        return chance > 0 && rng.nextDouble() < chance;
    }

    /**
     * Queues a popup for the view. The queue stops growing if nothing is reading it.
     *
     * @param p the popup
     */
    public void addPopup(Popup p) {
        if (popups.size() < MAX_POPUPS) {
            popups.add(p);
        }
    }

    /**
     * Takes every waiting popup.
     *
     * @return the popups, oldest first
     */
    public List<Popup> drainPopups() {
        List<Popup> out = new ArrayList<>(popups);
        popups.clear();
        return out;
    }

    /**
     * Takes every level-up that happened since the last call.
     *
     * @return the classmates who levelled up
     */
    public List<Hero> drainLevelUps() {
        List<Hero> out = new ArrayList<>(levelUpQueue);
        levelUpQueue.clear();
        return out;
    }

    /**
     * Gets the tile at a position.
     *
     * @param row lane index
     * @param col column index
     * @return the tile, plain floor if off the grid
     */
    public Terrain terrainAt(int row, int col) {
        return level.terrainAt(row, col);
    }

    /**
     * Gets the party in this level.
     *
     * @return the party, or null
     */
    public Party getParty() {
        return party;
    }

    /**
     * Tells whether any living enemy is ahead of a position in a lane and on screen.
     *
     * @param row lane index
     * @param x position in cell units
     * @return true if a shot fired now could have a target
     */
    public boolean hasEnemyAhead(int row, double x) {
        for (Enemy e : enemies) {
            if (e.getRow() == row && !e.isDead() && e.getX() >= x && e.getX() < COLS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Damages every enemy in a square of cells around a center cell.
     *
     * @param row center lane
     * @param col center column
     * @param radius how many cells out the blast reaches
     * @param damage damage dealt
     * @param critChance chance from 0 to 1 that each hit is critical
     */
    public void damageArea(int row, int col, int radius, double damage, double critChance) {
        for (Enemy e : enemies) {
            int enemyCol = (int) Math.floor(e.getX());
            if (Math.abs(e.getRow() - row) <= radius && Math.abs(enemyCol - col) <= radius) {
                strike(e, damage, critChance);
            }
        }
    }

    /**
     * Damages every enemy in one lane.
     *
     * @param row lane index
     * @param damage damage dealt
     * @param critChance chance from 0 to 1 that each hit is critical
     */
    public void damageRow(int row, double damage, double critChance) {
        for (Enemy e : enemies) {
            if (e.getRow() == row) {
                strike(e, damage, critChance);
            }
        }
    }

    /**
     * Heals every defender in a square of cells around a center cell, not
     * including the center itself.
     *
     * @param row center lane
     * @param col center column
     * @param radius how many cells out the heal reaches
     * @param amount health restored
     */
    public void healArea(int row, int col, int radius, double amount) {
        for (int r = row - radius; r <= row + radius; r++) {
            for (int c = col - radius; c <= col + radius; c++) {
                Defender d = defenderAt(r, c);
                if (d != null && !(r == row && c == col)) {
                    d.heal(amount);
                }
            }
        }
    }

    /**
     * Finds the living enemies standing in one cell.
     *
     * @param row lane index
     * @param col column index
     * @return the enemies in that cell, possibly empty
     */
    public List<Enemy> enemiesInCell(int row, int col) {
        List<Enemy> result = new ArrayList<>();
        for (Enemy e : enemies) {
            if (e.getRow() == row && !e.isDead() && (int) Math.floor(e.getX()) == col) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Finds the living enemies within a distance ahead of a position in a lane.
     *
     * @param row lane index
     * @param x position in cell units
     * @param range distance in cells
     * @return the enemies found, possibly none
     */
    public List<Enemy> enemiesAhead(int row, double x, double range) {
        List<Enemy> result = new ArrayList<>();
        for (Enemy e : enemies) {
            if (e.getRow() == row && !e.isDead() && e.getX() >= x && e.getX() <= x + range) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Finds the on-screen enemy with the most health, in any lane.
     *
     * @return the enemy, or null if none is on screen
     */
    public Enemy strongestOnScreen() {
        Enemy best = null;
        for (Enemy e : enemies) {
            if (!e.isDead() && e.getX() < COLS && (best == null || e.getHealth() > best.getHealth())) {
                best = e;
            }
        }
        return best;
    }

    /**
     * Finds the on-screen enemies closest to the left edge.
     *
     * @param count how many to find at most
     * @return the enemies, closest first
     */
    public List<Enemy> frontmost(int count) {
        List<Enemy> onScreen = new ArrayList<>();
        for (Enemy e : enemies) {
            if (!e.isDead() && e.getX() < COLS) {
                onScreen.add(e);
            }
        }
        onScreen.sort((a, b) -> Double.compare(a.getX(), b.getX()));
        return new ArrayList<>(onScreen.subList(0, Math.min(count, onScreen.size())));
    }

    /**
     * Finds the closest living enemy in the lanes next to a lane.
     *
     * @param row lane index
     * @param x position in cell units
     * @param range farthest distance allowed, in cells
     * @return the enemy, or null if none is in range
     */
    public Enemy nearestInAdjacentLanes(int row, double x, double range) {
        Enemy best = null;
        double bestDist = range;
        for (Enemy e : enemies) {
            double dist = Math.abs(e.getX() - x);
            if (!e.isDead() && Math.abs(e.getRow() - row) == 1 && e.getX() < COLS && dist <= bestDist) {
                best = e;
                bestDist = dist;
            }
        }
        return best;
    }

    /**
     * Adds a visual effect.
     *
     * @param e the effect
     */
    public void addEffect(Effect e) {
        effects.add(e);
    }

    /**
     * Gets the visual effects currently playing.
     *
     * @return the effect list
     */
    public List<Effect> getEffects() {
        return effects;
    }

    /**
     * Collects a coffee and gives its Focus Points.
     *
     * @param coffee the coffee that was clicked
     */
    public void collectCoffee(Coffee coffee) {
        if (coffees.remove(coffee)) {
            focusPoints += Coffee.VALUE;
            if (party != null && party.has(KeyItem.COFFEE_REFILLS)) {
                focusPoints += REFILL_FOCUS;
            }
        }
    }

    /**
     * Adds a projectile to the board.
     *
     * @param p the projectile
     */
    public void addProjectile(Projectile p) {
        projectiles.add(p);
    }

    /**
     * Schedules an enemy to join the board at the end of this tick. Safe to
     * call while enemies are being updated.
     *
     * @param type enemy type
     * @param row lane index
     * @param x starting position in cell units
     */
    public void queueSpawn(EnemyType type, int row, double x) {
        Enemy e = type.create(row, x);
        e.applyLevel(level.getEnemyLevel());
        pendingSpawns.add(e);
    }

    /**
     * Adds or removes Focus Points. The total never drops below zero.
     *
     * @param amount points to add, negative to remove
     */
    public void addFocusPoints(int amount) {
        focusPoints = Math.max(0, focusPoints + amount);
    }

    /**
     * Gets the defender in a cell.
     *
     * @param row lane index
     * @param col column index
     * @return the defender or null if the cell is empty or off the board
     */
    public Defender defenderAt(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
            return null;
        }
        return grid[row][col];
    }

    /**
     * Gets the live enemies.
     *
     * @return the enemy list
     */
    public List<Enemy> getEnemies() {
        return enemies;
    }

    /**
     * Gets the live projectiles.
     *
     * @return the projectile list
     */
    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    /**
     * Gets the coffees waiting to be collected.
     *
     * @return the coffee list
     */
    public List<Coffee> getCoffees() {
        return coffees;
    }

    /**
     * Gets the player's Focus Points.
     *
     * @return current Focus Points
     */
    public int getFocusPoints() {
        return focusPoints;
    }

    /**
     * Gets the number of ticks since the level began.
     *
     * @return tick count
     */
    public int getTick() {
        return tick;
    }

    /**
     * Gets the level being played.
     *
     * @return the level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Gets the wave schedule.
     *
     * @return the wave manager
     */
    public WaveManager getWaves() {
        return waves;
    }

    /**
     * Tells whether a lane's Highlighter has already been used.
     *
     * @param row lane index
     * @return true if it has swept the lane
     */
    public boolean isHighlighterUsed(int row) {
        return highlighterUsed[row];
    }

    /**
     * Tells whether the level was cleared.
     *
     * @return true if every wave is dead
     */
    public boolean isWon() {
        return won;
    }

    /**
     * Tells whether an enemy broke through.
     *
     * @return true if the level was lost
     */
    public boolean isLost() {
        return lost;
    }
}
