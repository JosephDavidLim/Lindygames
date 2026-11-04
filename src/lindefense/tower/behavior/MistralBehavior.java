package lindefense.tower.behavior;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lindefense.combat.DamageType;
import lindefense.combat.HitSpec;
import lindefense.combat.VisualEvent;
import lindefense.effects.Effects;
import lindefense.effects.TrackZone;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyGrid;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * Mistral (spec 3.6 #16). Spinning wind blades with knockback on fast enemies (HitSpec.knockbackVsFast) are the
 * data-driven base. U1 Gust, U2 Cyclone (tornado zone), U3 Hurricane (permanent vortex zone) and the player-targeted
 * Stratosphere Tempest are implemented here on top of {@link TrackZone}.
 *
 * <p>Interpretations: the Tempest's "pushes MOAB-class back 100 px" is applied once per airship per cast (a push
 * every 0.3 s tick would pin bosses at the entrance for the whole 15 s); the gust and the zones affect Camo enemies
 * too (they are area effects on the track, not targeted attacks).</p>
 */
public final class MistralBehavior extends TowerBehavior {
    private static final String VORTEX = "vortex";
    private static final String TEMPESTS = "tempests";

    /** A live Tempest zone and the airships it has already pushed. */
    private static final class Tempest {
        final TrackZone zone; final double push; final Set<Integer> pushed = new HashSet<>();
        Tempest(TrackZone zone, double push) { this.zone = zone; this.push = push; }
    }

    @Override public void update(Game game, Tower t, double dt) {
        TowerStats s = t.getStats();
        if (s.has("gustInterval") && t.periodic("gust", s.p("gustInterval"), dt)) gust(game, t, s);
        if (s.has("tornadoInterval")) cyclone(game, t, s, dt);
        if (s.has("vortexRadius")) vortex(game, t, s, dt);
        @SuppressWarnings("unchecked")
        List<Tempest> live = (List<Tempest>) t.state().get(TEMPESTS);
        if (live != null) {
            for (int i = live.size() - 1; i >= 0; i--) {
                Tempest tp = live.get(i);
                if (!tp.zone.isAlive()) { live.remove(i); continue; }
                pushBosses(game, tp);
            }
        }
    }

    /** U1 Gust: fast enemies are knocked back gustFast px, other non-boss enemies gustOther px. */
    private static void gust(Game game, Tower t, TowerStats s) {
        double range = t.effectiveRange(game);
        Vec tp = t.getPosition();
        double fast = s.p("gustFast"), other = s.p("gustOther");
        int n = 0;
        EnemyGrid.Scan scan = game.getEnemyManager().getGrid().scan(tp.x, tp.y, range);   // knockback moves enemies
        try {
            for (Enemy e = scan.next(); e != null; e = scan.next()) {
                if (!e.isAlive() || e.isStored() || e.isBoss()) continue;
                if (e.getPosition().dist(tp) > range + e.getRadius()) continue;
                if (e.applyKnockbackUncapped(e.isFastForKnockback() ? fast : other) > 0) n++;
            }
        } finally { scan.close(); }
        if (n > 0) game.getCombat().emit(VisualEvent.ability(tp, range, t.getType().primaryRgb));
    }

    /** U2 Cyclone: every tornadoInterval s a tornado zone sits on the track at the first enemy in range (no enemy, no tornado). */
    private static void cyclone(Game game, Tower t, TowerStats s, double dt) {
        double interval = s.p("tornadoInterval");
        if (t.addCounter("cyclone", dt) < interval) return;
        List<Enemy> first = game.getCombat().acquireTargets(t, 1);
        if (first.isEmpty()) { t.setCounter("cyclone", interval); return; }   // nothing to hit: stay ready, spawn nothing
        Vec at = first.get(0).getPosition();
        t.setCounter("cyclone", 0);
        TrackZone z = new TrackZone(at, s.p("tornadoRadius"), t);
        z.duration = s.p("tornadoDuration");
        z.tickInterval = s.p("tornadoTick");
        z.tickHit = new HitSpec(s.p("tornadoDamage"), DamageType.KINETIC);
        z.kind = "tornado";
        z.rgb = t.getType().primaryRgb;
        game.getEffects().addZone(z);
        game.getCombat().emit(VisualEvent.text(at, "Cyclone", t.getType().primaryRgb));
    }

    /** U3 Hurricane: one permanent vortex zone on the nearest track bend in range (re-created if it ever dies). */
    private static void vortex(Game game, Tower t, TowerStats s, double dt) {
        TrackZone z = (TrackZone) t.state().get(VORTEX);
        if (z != null && z.isAlive()) return;
        if (!t.periodic("vortexRetry", 0.5, dt)) return;
        List<Effects.TrackPoint> bends = game.getEffects().bendsInRange(t.getPosition(), t.effectiveRange(game));
        if (bends.isEmpty()) return;
        z = new TrackZone(bends.get(0).position, s.p("vortexRadius"), t);
        z.permanent = true;
        z.tickInterval = s.p("vortexTick");
        HitSpec h = new HitSpec(s.p("vortexDamage"), DamageType.KINETIC);
        h.multVsBoss = s.p("vortexBossMult");
        z.tickHit = h;
        z.slow = s.p("vortexSlow");
        z.kind = "vortex";
        z.rgb = t.getType().primaryRgb;
        game.getEffects().addZone(z);
        t.state().put(VORTEX, z);
    }

    /** Stratosphere Tempest: a super-hurricane on the bend nearest to the player's chosen point. */
    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        if (target == null) return false;
        AbilityDef a = t.getAbility();
        Effects fx = game.getEffects();
        List<Effects.TrackPoint> bends = fx.bendsInRange(target, a.p("snapRange"));
        Vec at;
        if (!bends.isEmpty()) {
            at = bends.get(0).position;
        } else {
            Effects.TrackPoint p = fx.nearestTrackPoint(target);
            if (p == null) return false;
            at = p.position;
        }
        TrackZone z = new TrackZone(at, a.p("radius"), t);
        z.duration = a.p("duration");
        z.bossesOnly = true;
        z.tickInterval = a.p("tick");
        z.tickHit = new HitSpec(a.p("damage"), DamageType.KINETIC);
        z.kind = "tempest";
        z.rgb = t.getType().primaryRgb;
        fx.addZone(z);
        @SuppressWarnings("unchecked")
        List<Tempest> live = (List<Tempest>) t.state().get(TEMPESTS);
        if (live == null) { live = new ArrayList<>(); t.state().put(TEMPESTS, live); }
        Tempest tp = new Tempest(z, a.p("bossKnockback"));
        live.add(tp);
        pushBosses(game, tp);
        game.getCombat().emit(VisualEvent.banner("STRATOSPHERE TEMPEST", t.getType().primaryRgb, 1.2));
        return true;
    }

    /** Pushes each airship inside the zone back once (Palantir's control-immune phase is retried until it drops). */
    private static void pushBosses(Game game, Tempest tp) {
        Vec c = tp.zone.center;
        EnemyGrid.Scan scan = game.getEnemyManager().getGrid().scan(c.x, c.y, tp.zone.radius);
        try {
            for (Enemy e = scan.next(); e != null; e = scan.next()) {
                if (!e.isBoss() || !e.isAlive() || e.isStored() || e.isControlImmune() || !tp.zone.contains(e)) continue;
                if (!tp.pushed.add(e.getId())) continue;
                e.applyKnockbackUncapped(tp.push);
            }
        } finally { scan.close(); }
    }
}
