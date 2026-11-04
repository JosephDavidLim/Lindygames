package entities;

import game.Board;

/**
 * Fires projectiles down its lane whenever an enemy is ahead of it.
 */
public class Shooter extends Defender {
    private int cooldown;
    private boolean targetAhead;

    /**
     * Creates a shooter.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    public Shooter(DefenderType type, int row, int col) {
        super(type, row, col);
        this.cooldown = 0;
    }

    @Override
    public void update(Board board) {
        if (cooldown > 0) {
            cooldown--;
            targetAhead = hasTarget(board);
            return;
        }
        if (hasTarget(board)) {
            int slowTicks = Board.seconds(type.getSlowSec());
            for (int r : targetLanes(board)) {
                for (int i = 0; i < type.getBurst(); i++) {
                    board.addProjectile(new Projectile(r, x - i * 0.3, power(),
                            type.isPiercing(), slowTicks, type).withCrit(critChance()).withSplash(hero != null && hero.hasSplash()));
                }
            }
            cooldown = actionTicks(board);
            actionDone();
        }
    }

    private int[] targetLanes(Board board) {
        if (type.getKind() != DefenderType.Kind.FAN) {
            return new int[] {row};
        }
        return java.util.stream.IntStream.of(row - 1, row, row + 1)
                .filter(r -> board.getLevel().hasLane(r)).toArray();
    }

    private boolean hasTarget(Board board) {
        for (int r : targetLanes(board)) {
            if (board.hasEnemyAhead(r, x)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether the shooter is about to throw, so the view can show a wind-up.
     *
     * @return true in the last few ticks before a shot when a target is in the lane
     */
    public boolean isWindingUp() {
        return cooldown > 0 && cooldown <= 5 && targetAhead;
    }
}
