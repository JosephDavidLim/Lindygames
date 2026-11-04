package lindefense.wave;

import java.util.EnumSet;
import lindefense.enemy.EnemyType;
import lindefense.enemy.Modifier;

/**
 * One group within a wave: {@code count} enemies of one type spawned every
 * {@code interval} seconds starting {@code startDelay} seconds into the wave.
 * Groups in a wave run concurrently, so overlapping groups form mixed rushes.
 */
public class SpawnGroup {
    public final EnemyType type;
    public final int count;
    public final EnumSet<Modifier> modifiers;
    public final double interval;
    public final double startDelay;
    /** Route index, or -1 for a random route per enemy. */
    public final int route;

    public SpawnGroup(EnemyType type, int count, EnumSet<Modifier> modifiers, double interval, double startDelay, int route) {
        this.type = type; this.count = count;
        this.modifiers = modifiers == null ? EnumSet.noneOf(Modifier.class) : modifiers;
        this.interval = interval; this.startDelay = startDelay; this.route = route;
    }

    public String modifierLabel() {
        if (modifiers.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Modifier m : modifiers) { if (sb.length() > 0) sb.append('+'); sb.append(m.label); }
        return sb.toString();
    }

    /** Seconds from wave start until the last enemy of the group spawns. */
    public double endTime() { return startDelay + interval * Math.max(0, count - 1); }
}
