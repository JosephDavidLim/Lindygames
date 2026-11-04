package entities;

import game.Board;

import java.util.List;

/**
 * A boss that walks, bites hard and uses its own special on a timer. Each boss
 * attacks the squad differently, so each area ends with a different fight.
 */
public class Boss extends Enemy {
    private static final int DETENTION_TICKS = Board.seconds(3);
    private static final int SHUSH_TICKS = Board.seconds(2);
    private static final int HASTE_TICKS = Board.seconds(3);
    private static final double FOOD_DAMAGE = 40;
    private static final double BALL_DAMAGE = 60;
    private static final int FOOD_TARGETS = 3;

    private final int intervalTicks;
    private int timer;

    /**
     * Creates a boss.
     *
     * @param type the enemy type
     * @param row lane index
     * @param x starting position in cell units
     */
    public Boss(EnemyType type, int row, double x) {
        super(type, row, x);
        this.intervalTicks = Board.seconds(type.getSpecialSec());
        this.timer = intervalTicks;
    }

    @Override
    protected void act(Board board) {
        timer--;
        if (timer <= 0) {
            special(board);
            boolean desperate = type == EnemyType.THE_DEADLINE && health < maxHealth / 2;
            timer = desperate ? intervalTicks / 2 : intervalTicks;
        }
        walkOrBite(board);
    }

    private void special(Board board) {
        switch (type) {
            case THE_SUBSTITUTE:
                shout(board, "Pop quiz!");
                summon(board, EnemyType.HOMEWORK_SHEET);
                break;
            case VICE_PRINCIPAL:
                shout(board, "Detention!");
                for (int lane = row - 1; lane <= row + 1; lane++) {
                    Defender d = board.frontDefender(lane, x);
                    if (d != null) {
                        d.disable(DETENTION_TICKS);
                        board.addEffect(new Effect(Effect.Kind.STUN, lane, d.getCol(), 0, 20));
                    }
                }
                break;
            case LUNCH_LADY:
                shout(board, "Food fight!");
                for (Defender d : board.randomDefenders(FOOD_TARGETS)) {
                    d.takeDamage(FOOD_DAMAGE * biteMultiplier());
                    board.addEffect(new Effect(Effect.Kind.BLAST, d.getRow(), d.getCol(), 0, 12));
                }
                break;
            case LIBRARIAN:
                shout(board, "SHHH!");
                for (int lane = row - 1; lane <= row + 1; lane++) {
                    if (board.getLevel().hasLane(lane)) {
                        board.disableLane(lane, SHUSH_TICKS);
                        board.addEffect(new Effect(Effect.Kind.LANE, lane, 0, 0, 14));
                    }
                }
                break;
            case COACH:
                shout(board, "Run laps!");
                hasteEveryone(board);
                Defender front = board.frontDefender(row, x);
                if (front != null) {
                    front.takeDamage(BALL_DAMAGE * biteMultiplier());
                    board.addEffect(new Effect(Effect.Kind.BLAST, row, front.getCol(), 0, 12));
                }
                break;
            case THE_DEADLINE:
                shout(board, "Time's up!");
                summon(board, EnemyType.SHARP_PENCIL);
                if (health < maxHealth / 2) {
                    hasteEveryone(board);
                }
                break;
            default:
                summon(board, EnemyType.SHARP_PENCIL);
                break;
        }
    }

    private void summon(Board board, EnemyType minion) {
        board.queueSpawn(minion, row, x + 0.3);
        board.queueSpawn(minion, row, x + 0.6);
    }

    private void hasteEveryone(Board board) {
        List<Enemy> all = board.getEnemies();
        for (Enemy e : all) {
            e.haste(HASTE_TICKS);
        }
    }

    private void shout(Board board, String text) {
        board.addPopup(new Popup(row, x, text));
    }
}
