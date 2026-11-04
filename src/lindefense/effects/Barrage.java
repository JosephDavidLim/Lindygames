package lindefense.effects;

import java.util.List;
import lindefense.combat.Clusters;
import lindefense.combat.HitSpec;
import lindefense.combat.VisualEvent;
import lindefense.enemy.Boss;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Tower;

/**
 * Describes N explosions (shells, missiles, drones) landing at a point or at the densest cluster, spread over a
 * short time. Fire it with {@link Effects#fire(Barrage)}; the aim point is chosen when fired.
 *
 * <ul>
 *   <li>Each shell is an area hit of {@link #splash} radius with {@link #hit} (so {@code multVsBoss} etc. work),
 *       credited to {@link #source}, with {@link #scatter} px of random offset around the aim point.</li>
 *   <li>Shells land {@link #stagger} seconds apart.</li>
 *   <li>Detection for the EXPLOSIVE-vs-camo rule is {@link #detection} (default: the source tower's).</li>
 * </ul>
 */
public final class Barrage {
    public enum Aim {
        /** At {@link #point}. */
        POINT,
        /** At the densest cluster of all enemies on the map (see {@link Clusters}). */
        DENSEST,
        /** At a boss if one is alive (the one furthest along the track), otherwise the densest cluster. */
        BOSS_ELSE_DENSEST
    }

    public final Tower source;
    public Aim aim = Aim.DENSEST;
    public Vec point;
    public int count = 1;
    public HitSpec hit;
    public double splash = 50;
    public double scatter = 0;
    public double stagger = 0.1;
    public boolean detection;
    public int rgb = 0xff8844;

    public Barrage(Tower source, HitSpec hit, int count, double splash) {
        this.source = source; this.hit = hit; this.count = count; this.splash = splash;
        this.detection = source != null && source.canSeeCamo();
    }

    /** Resolves the aim point against the current enemies, or null if there is nothing to aim at. */
    public Vec resolveAim(Game game) {
        List<Enemy> all = game.getEnemyManager().getEnemies();
        switch (aim) {
            case POINT: return point;
            case BOSS_ELSE_DENSEST: {
                Enemy boss = null;
                for (Enemy e : all) {
                    if (e instanceof Boss && e.isAlive() && !e.isStored()
                        && (boss == null || e.getRemainingDistance() < boss.getRemainingDistance())) boss = e;
                }
                if (boss != null) return boss.getPosition();
                return Clusters.densestPoint(all);
            }
            case DENSEST:
            default: return Clusters.densestPoint(all);
        }
    }

    /** Lands one shell at {@code p} now. */
    void land(Game game, Vec p) {
        Vec at = p;
        if (scatter > 0) {
            double ang = game.getRng().range(0, Math.PI * 2);
            double d = Math.sqrt(game.getRng().next()) * scatter;
            at = p.add(new Vec(Math.cos(ang) * d, Math.sin(ang) * d));
        }
        game.getCombat().areaHit(at, splash, hit, source, source == null ? 1 : source.damageMultiplier(game), 0, 999, null,
            detection, false, null);
        game.getCombat().emit(VisualEvent.explosion(at, splash, rgb));
    }
}
