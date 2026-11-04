package entities;

import game.Board;
import game.Hero;
import game.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * The character the player steers directly, like a MapleStory avatar. It runs
 * along a platform, jumps over enemies, climbs ladders up to the next platform,
 * drops down through a platform, swings at enemies in reach, picks up coffee
 * and drops by walking over them, and gets hurt by touching enemies.
 * It uses the chosen classmate's {@link Hero} sheet, so that classmate's level,
 * stats and gear all count, and the classmate's job decides how it attacks. A
 * knockout benches it for a few seconds and spills some of the level's Credits,
 * which it can run back and collect.
 */
public class Avatar {
    /** Running speed in cells per tick. */
    public static final double RUN_SPEED = 0.075;
    /** Damage per swing before stats. */
    public static final double BASE_DAMAGE = 18;
    /** Health before level bonuses. */
    public static final double BASE_HEALTH = 100;

    private static final int SWING_TICKS = 15;
    private static final int MAX_TARGETS = 3;
    private static final int ALL_TARGETS = 99;
    private static final double REACH = 1.3;
    private static final double WALL_HEALTH_BONUS = 1.5;
    private static final int SAGE_FOCUS = 3;
    private static final int STORM_STUN_TICKS = 10;
    private static final int TRAP_STUN_TICKS = 30;
    private static final double HEAL_POWER = 6;
    private static final double BEHIND = 0.25;
    private static final double TOUCH_RANGE = 0.4;
    private static final double PICKUP_RANGE = 0.6;
    private static final double JUMP_SPEED = 0.11;
    private static final double GRAVITY = 0.008;
    private static final double CLIMB_SPEED = 0.05;
    private static final double LADDER_REACH = 0.4;
    /** Height above its platform at which the avatar clears an enemy. */
    private static final double CLEAR_HEIGHT = 0.45;
    private static final int SAFE_TICKS = Board.seconds(1);
    private static final int KO_TICKS = Board.seconds(5);
    private static final double KNOCKBACK = 0.35;
    private static final double RIGHT_EDGE = Board.COLS - 0.5;
    private static final double LEFT_EDGE = 0.6;

    /**
     * The ladders drawn on every map, as {lower lane, column}. Each joins that
     * lane to the one above it. {@code tools/make_maps.py} draws the same spots.
     */
    public static final int[][] LADDERS = {{1, 2}, {2, 7}, {3, 4}, {4, 1}};

    private final Hero sheet;
    private int row;
    private double x = 0.6;
    private int facing = 1;
    private double health;
    private int swingTimer;
    private int swingAge = 999;
    private double height;
    private double riseSpeed;
    private boolean climbing;
    private boolean jumpRequested;
    private int safeTicks;
    private int koTicks;
    private int moveDir;
    private int vertical;
    private boolean attacking;

    /**
     * Creates the avatar.
     *
     * @param sheet the chosen classmate's sheet
     * @param row starting lane
     */
    public Avatar(Hero sheet, int row) {
        this.sheet = sheet;
        this.row = row;
        this.health = getMaxHealth();
    }

    /**
     * Sets what the player is pressing. Called every tick.
     *
     * @param move -1 for left, 1 for right, 0 to stand still
     * @param vertical -1 while up is held, 1 while down is held, 0 for neither
     * @param attack true while the attack key is held
     * @param jump true if the jump key was just pressed
     */
    public void command(int move, int vertical, boolean attack, boolean jump) {
        moveDir = move;
        this.vertical = vertical;
        attacking = attack;
        if (jump) {
            jumpRequested = true;
        }
    }

    /**
     * Advances the avatar one tick.
     *
     * @param board the board
     */
    public void update(Board board) {
        if (swingAge < 999) {
            swingAge++;
        }
        if (swingTimer > 0) {
            swingTimer--;
        }
        if (safeTicks > 0) {
            safeTicks--;
        }
        if (koTicks > 0) {
            koTicks--;
            if (koTicks == 0) {
                health = getMaxHealth();
                x = 0.6;
                height = 0;
                riseSpeed = 0;
                climbing = false;
                safeTicks = SAFE_TICKS;
            }
            return;
        }
        move(board);
        if (attacking && swingTimer == 0 && !climbing) {
            swing(board);
        }
        pickUp(board);
        touchEnemies(board);
    }

    private void move(Board board) {
        if (climbing) {
            climb();
        } else {
            run();
            startClimbOrJump(board);
            fall();
        }
        jumpRequested = false;
    }

    private void run() {
        if (moveDir != 0) {
            facing = moveDir;
            x = Math.max(LEFT_EDGE, Math.min(RIGHT_EDGE, x + moveDir * RUN_SPEED));
        }
    }

    private void climb() {
        if (jumpRequested && moveDir != 0) {
            climbing = false;
            riseSpeed = JUMP_SPEED * 0.6;
            return;
        }
        height -= vertical * CLIMB_SPEED;
        if (height >= 1) {
            row--;
            height = 0;
            climbing = false;
        } else if (height <= 0) {
            height = 0;
            climbing = false;
        }
    }

    private void startClimbOrJump(Board board) {
        if (vertical < 0 && hasLadder(board, row)) {
            climbing = true;
            riseSpeed = 0;
            x = ladderX(row);
            return;
        }
        if (height > 0) {
            return;
        }
        if (jumpRequested && vertical > 0) {
            dropThrough(board);
        } else if (jumpRequested) {
            riseSpeed = JUMP_SPEED;
            height = riseSpeed;
        } else if (vertical > 0 && hasLadder(board, row + 1)) {
            climbing = true;
            x = ladderX(row + 1);
            row++;
            height = 1 - CLIMB_SPEED;
        }
    }

    private void dropThrough(Board board) {
        int target = row + 1;
        while (target < Board.ROWS && !board.getLevel().hasLane(target)) {
            target++;
        }
        if (target < Board.ROWS) {
            height = target - row;
            riseSpeed = 0;
            row = target;
        }
    }

    private void fall() {
        if (height > 0) {
            height += riseSpeed;
            riseSpeed -= GRAVITY;
            if (height <= 0) {
                height = 0;
                riseSpeed = 0;
            }
        }
    }

    /** Tells whether a usable ladder within reach joins a lane to the one above it. */
    private boolean hasLadder(Board board, int lowerRow) {
        return ladderWorks(board.getLevel(), lowerRow) && Math.abs(x - ladderX(lowerRow)) <= LADDER_REACH;
    }

    /**
     * Tells whether a level has a working ladder from a lane up to the next.
     * The view draws only these, so every ladder on screen can be climbed.
     *
     * @param level the level
     * @param lowerRow the lower lane
     * @return true if a ladder stands there and both of its lanes are open
     */
    public static boolean ladderWorks(Level level, int lowerRow) {
        return lowerRow >= 1 && lowerRow < Board.ROWS && ladderX(lowerRow) >= 0
                && level.hasLane(lowerRow) && level.hasLane(lowerRow - 1);
    }

    /**
     * Gets where the ladder from a lane up to the next one stands.
     *
     * @param lowerRow the lower lane
     * @return position in cell units, or -1 if there is none
     */
    public static double ladderX(int lowerRow) {
        for (int[] ladder : LADDERS) {
            if (ladder[0] == lowerRow) {
                return ladder[1] + 0.5;
            }
        }
        return -1;
    }

    private void swing(Board board) {
        DefenderType type = sheet.getType();
        DefenderType.Kind kind = type.getKind();
        double damage = BASE_DAMAGE * sheet.powerMultiplier() * damageScale(kind);
        List<Enemy> targets = inReach(board, row, reach(type), kind == DefenderType.Kind.STOMPER);
        int limit = Math.min(targets.size(), maxTargets(type));
        for (int i = 0; i < limit; i++) {
            Enemy e = targets.get(i);
            if (kind == DefenderType.Kind.BOMB) {
                board.damageArea(e.getRow(), (int) Math.floor(e.getX()), 1, damage, sheet.critChance());
                board.addEffect(new Effect(Effect.Kind.BLAST, e.getRow(), (int) Math.floor(e.getX()), 1, 12));
            } else {
                board.strike(e, damage, sheet.critChance());
                applyHitEffect(board, type, e, damage);
            }
        }
        applySwingBonus(board, kind, limit);
        swingAge = 0;
        swingTimer = Math.max(1, (int) Math.round(swingTicks(kind) / sheet.speedMultiplier()));
    }

    private List<Enemy> inReach(Board board, int lane, double reach, boolean bothWays) {
        List<Enemy> found = new ArrayList<>();
        for (Enemy e : board.getEnemies()) {
            double ahead = bothWays ? Math.abs(e.getX() - x) : (e.getX() - x) * facing;
            if (e.getRow() == lane && !e.isDead() && e.getX() < Board.COLS && ahead >= -BEHIND && ahead <= reach) {
                found.add(e);
            }
        }
        found.sort((a, b) -> Double.compare(Math.abs(a.getX() - x), Math.abs(b.getX() - x)));
        return found;
    }

    private void applyHitEffect(Board board, DefenderType type, Enemy e, double damage) {
        int cell = (int) Math.floor(e.getX());
        switch (type.getKind()) {
            case STORM:
                e.stun(STORM_STUN_TICKS);
                board.addEffect(new Effect(Effect.Kind.BOLT, e.getRow(), cell, 0, 8));
                break;
            case TRAP:
                e.stun(TRAP_STUN_TICKS);
                board.addEffect(new Effect(Effect.Kind.STUN, e.getRow(), cell, 0, 12));
                break;
            case RICOCHET:
                Enemy bounce = board.nearestInAdjacentLanes(row, e.getX(), 2.0);
                if (bounce != null) {
                    board.strike(bounce, damage, sheet.critChance());
                    board.addEffect(new Effect(Effect.Kind.SNIPE, bounce.getRow(), (int) Math.floor(bounce.getX()), 0, 8));
                }
                break;
            case SHOOTER:
            case SNIPER:
            case LANE_CLEAR:
                board.addEffect(new Effect(Effect.Kind.SNIPE, e.getRow(), cell, 0, 8));
                if (type.getSlowSec() > 0) {
                    e.applySlow(Board.seconds(type.getSlowSec()));
                }
                break;
            default:
                break;
        }
    }

    private void applySwingBonus(Board board, DefenderType.Kind kind, int hits) {
        if (kind == DefenderType.Kind.SUPPORT) {
            double amount = HEAL_POWER * sheet.powerMultiplier();
            board.healArea(row, (int) Math.floor(x), 1, amount);
            health = Math.min(getMaxHealth(), health + amount / 2);
        } else if (kind == DefenderType.Kind.GENERATOR && hits > 0) {
            board.addFocusPoints(SAGE_FOCUS);
        } else if (kind == DefenderType.Kind.FAN) {
            for (int lane : new int[] {row - 1, row + 1}) {
                if (board.getLevel().hasLane(lane)) {
                    double side = BASE_DAMAGE * sheet.powerMultiplier() * 0.5;
                    List<Enemy> near = inReach(board, lane, reach(sheet.getType()), false);
                    for (int i = 0; i < near.size() && i < 2; i++) {
                        board.strike(near.get(i), side, sheet.critChance());
                    }
                }
            }
        }
    }

    private static double reach(DefenderType type) {
        switch (type.getKind()) {
            case SHOOTER:
                return type.isPiercing() ? 3.5 : 3.0;
            case RICOCHET:
                return 2.2;
            case FAN:
                return 1.8;
            case SNIPER:
            case LANE_CLEAR:
                return Board.COLS;
            case BOMB:
                return 1.6;
            case POINT_BLANK:
            case MINE:
                return 1.0;
            default:
                return REACH;
        }
    }

    private static int maxTargets(DefenderType type) {
        switch (type.getKind()) {
            case SHOOTER:
                return type.isPiercing() ? ALL_TARGETS : 1;
            case SNIPER:
            case BOMB:
            case MINE:
                return 1;
            case POINT_BLANK:
            case STOMPER:
            case LANE_CLEAR:
                return ALL_TARGETS;
            default:
                return MAX_TARGETS;
        }
    }

    private static double damageScale(DefenderType.Kind kind) {
        switch (kind) {
            case SNIPER:
                return 2.0;
            case LANE_CLEAR:
                return 2.5;
            case BOMB:
                return 2.0;
            case MINE:
                return 1.6;
            case POINT_BLANK:
                return 1.4;
            case TRAP:
                return 1.2;
            default:
                return 1.0;
        }
    }

    private static int swingTicks(DefenderType.Kind kind) {
        switch (kind) {
            case LANE_CLEAR:
                return 60;
            case TRAP:
                return 45;
            case SNIPER:
                return 36;
            case BOMB:
                return 30;
            default:
                return SWING_TICKS;
        }
    }

    private void pickUp(Board board) {
        for (Coffee c : new ArrayList<>(board.getCoffees())) {
            if ((int) Math.floor(c.getY()) == row && Math.abs(c.getX() - x) <= PICKUP_RANGE) {
                board.collectCoffee(c);
            }
        }
        for (Drop d : new ArrayList<>(board.getDrops())) {
            if (d.getRow() == row && Math.abs(d.getX() - x) <= PICKUP_RANGE) {
                board.collectDrop(d);
            }
        }
    }

    private void touchEnemies(Board board) {
        if (safeTicks > 0 || height > CLEAR_HEIGHT) {
            return;
        }
        for (Enemy e : board.getEnemies()) {
            if (e.getRow() == row && !e.isDead() && !e.isStunned() && Math.abs(e.getX() - x) <= TOUCH_RANGE) {
                safeTicks = SAFE_TICKS;
                if (board.roll(sheet.dodgeChance())) {
                    board.addPopup(new Popup(Popup.Kind.MISS, row, x, 0, null));
                    return;
                }
                health -= e.getTouchDamage();
                if (!climbing) {
                    x = Math.max(LEFT_EDGE, x - KNOCKBACK);
                }
                board.addPopup(new Popup(Popup.Kind.HURT, row, x, (int) Math.round(e.getTouchDamage()), null));
                if (health <= 0) {
                    health = 0;
                    koTicks = KO_TICKS;
                    board.avatarKnockedOut(row, x);
                }
                return;
            }
        }
    }

    /**
     * Refills health after a level-up, and wakes the avatar if it was knocked out.
     */
    public void onLevelUp() {
        health = getMaxHealth();
        koTicks = 0;
    }

    /**
     * Gets the character sheet.
     *
     * @return the sheet
     */
    public Hero getSheet() {
        return sheet;
    }

    /**
     * Gets the lane.
     *
     * @return lane index
     */
    public int getRow() {
        return row;
    }

    /**
     * Gets the position along the lane.
     *
     * @return position in cell units
     */
    public double getX() {
        return x;
    }

    /**
     * Gets how far above its platform the avatar is.
     *
     * @return height in cells, 0 when standing
     */
    public double getHeight() {
        return height;
    }

    /**
     * Tells whether the avatar is on a ladder.
     *
     * @return true while climbing
     */
    public boolean isClimbing() {
        return climbing;
    }

    /**
     * Gets the direction the avatar faces.
     *
     * @return 1 for right, -1 for left
     */
    public int getFacing() {
        return facing;
    }

    /**
     * Gets the current health.
     *
     * @return health points
     */
    public double getHealth() {
        return health;
    }

    /**
     * Gets the maximum health.
     *
     * @return health points after level bonuses
     */
    public final double getMaxHealth() {
        double job = sheet.getType().role() == DefenderType.Kind.WALL ? WALL_HEALTH_BONUS : 1;
        return BASE_HEALTH * job * sheet.healthMultiplier();
    }

    /**
     * Gets the ticks since the last swing, for the animation.
     *
     * @return ticks, 999 if long ago
     */
    public int getSwingAge() {
        return swingAge;
    }

    /**
     * Tells whether the avatar just got hurt and cannot be hurt again yet.
     *
     * @return true while it is safe
     */
    public boolean isSafe() {
        return safeTicks > 0;
    }

    /**
     * Tells whether the avatar is knocked out.
     *
     * @return true while waiting to return
     */
    public boolean isKnockedOut() {
        return koTicks > 0;
    }

    /**
     * Gets the seconds until a knocked-out avatar returns.
     *
     * @return whole seconds, rounded up
     */
    public int getKoSeconds() {
        return (int) Math.ceil(koTicks / (double) Board.TICKS_PER_SECOND);
    }
}
