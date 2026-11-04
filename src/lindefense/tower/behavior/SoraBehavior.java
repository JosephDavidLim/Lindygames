package lindefense.tower.behavior;

import java.util.ArrayList;
import java.util.List;
import lindefense.combat.HitSpec;
import lindefense.combat.VisualEvent;
import lindefense.effects.TrackZone;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * Sora (spec 3.4 #10). The film-frame projectile and the slow field (35% / 55%) are data-driven
 * (TowerStats.fieldRadius / auraEnemySlow). Cinematic Rewind is the ability. U2 Scene Rewind knocks back every non-boss
 * enemy in range by {@code rewindKnockback} px every {@code rewindInterval} s (uncapped, bosses immune). U3 Time Loop
 * places {@code loopZones} permanent "loop" TrackZones on the track in range, spread out by farthest-point selection
 * starting from the track point nearest the tower.
 */
public final class SoraBehavior extends TowerBehavior {
    @Override public void update(Game game, Tower t, double dt) {
        TowerStats s = t.getStats();
        if (s.has("rewindInterval") && t.periodic("rewind", s.p("rewindInterval"), dt)) sceneRewind(game, t, s);
        if (s.has("loopZones")) ensureLoopZones(game, t, s);
    }

    private void sceneRewind(Game game, Tower t, TowerStats s) {
        double range = t.effectiveRange(game);
        boolean any = false;
        for (Enemy e : enemiesWithin(game, t.getPosition(), range)) {
            if (e.isBoss()) continue;
            e.applyKnockbackUncapped(s.p("rewindKnockback"));
            any = true;
        }
        if (any) game.getCombat().emit(VisualEvent.ability(t.getPosition(), range, t.getType().secondaryRgb));
    }

    private void ensureLoopZones(Game game, Tower t, TowerStats s) {
        int want = s.pi("loopZones");
        List<TrackZone> have = game.getEffects().zonesOf(t, "loop");
        if (have.size() >= want) return;
        List<DalleBehavior.Spot> spots = DalleBehavior.trackSpotsInRange(game, t, t.effectiveRange(game));
        if (spots.isEmpty()) return;
        List<Vec> chosen = new ArrayList<>();
        for (TrackZone z : have) chosen.add(z.center);
        while (chosen.size() < want) {
            DalleBehavior.Spot best = null;
            double bestScore = -1;
            for (DalleBehavior.Spot sp : spots) {
                // first zone: nearest the tower; others: farthest from the zones already placed
                double score;
                if (chosen.isEmpty()) score = -sp.pos.dist(t.getPosition()) + 1e6;
                else {
                    score = Double.MAX_VALUE;
                    for (Vec c : chosen) score = Math.min(score, c.dist(sp.pos));
                }
                if (score > bestScore) { bestScore = score; best = sp; }
            }
            chosen.add(best.pos);
            TrackZone z = new TrackZone(best.pos, s.p("loopRadius"), t);
            z.permanent = true;
            z.tickInterval = s.p("loopTick");
            z.tickHit = new HitSpec(s.p("loopTickDamage"), s.hit.type);
            z.slow = s.p("loopSlow");
            z.kind = "loop";
            z.rgb = t.getType().secondaryRgb;
            game.getEffects().addZone(z);
            game.getCombat().emit(VisualEvent.ability(best.pos, z.radius, z.rgb));
        }
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        for (Enemy e : game.getEnemyManager().getEnemies()) {
            if (!e.isAlive() || e.isStored()) continue;
            e.rewind(e.isBoss() ? a.p("bossRewindSeconds") : a.p("rewindSeconds"));
        }
        return true;
    }
}
