package lindefense.wave;

import java.util.ArrayList;
import java.util.List;
import lindefense.enemy.EnemyType;

/** A complete wave: number, spawn groups, optional note describing special mechanics. */
public class WaveDef {
    public final int number;
    public final List<SpawnGroup> groups = new ArrayList<>();
    public final String note;

    public WaveDef(int number, String note, SpawnGroup... gs) {
        this.number = number;
        this.note = note;
        for (SpawnGroup g : gs) groups.add(g);
    }

    /** Total initially spawned enemies (children from splitting are extra). */
    public int totalSpawns() {
        int n = 0;
        for (SpawnGroup g : groups) n += g.count;
        return n;
    }

    public boolean containsBoss() {
        for (SpawnGroup g : groups) if (g.type.isBoss()) return true;
        return false;
    }

    public boolean contains(EnemyType t) {
        for (SpawnGroup g : groups) if (g.type == t) return true;
        return false;
    }

    public int countOf(EnemyType t) {
        int n = 0;
        for (SpawnGroup g : groups) if (g.type == t) n += g.count;
        return n;
    }

    /** Seconds until the final spawn of the wave. */
    public double duration() {
        double d = 0;
        for (SpawnGroup g : groups) d = Math.max(d, g.endTime());
        return d;
    }
}
