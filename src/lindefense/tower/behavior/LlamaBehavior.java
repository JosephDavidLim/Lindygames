package lindefense.tower.behavior;

import java.util.List;
import lindefense.combat.DamageType;
import lindefense.combat.HitSpec;
import lindefense.combat.VisualEvent;
import lindefense.effects.TrackWalker;
import lindefense.enemy.Enemy;
import lindefense.map.Path;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * Llama (spec 3.6 #15). The base attack is a CUSTOM attack: every interval, if an enemy is in range,
 * {@code chargeCount} llamas (TrackWalkers) charge against the flow along a {@code chargeSegment} px stretch of
 * track centred on the first enemy. Pack Mentality (3 per cycle), spectral camo detection and pierce 12 are
 * data-driven. The U3 shockwave and the herd ability are implemented below.
 */
public final class LlamaBehavior extends TowerBehavior {
    @Override public boolean attack(Game game, Tower t) {
        TowerStats s = t.getStats();
        List<Enemy> targets = game.getCombat().acquireTargets(t, 1);
        if (targets.isEmpty()) return false;
        Enemy e = targets.get(0);
        double half = s.p("chargeSegment") / 2;
        double centre = e.getDistance();
        int n = s.pi("chargeCount");
        for (int i = 0; i < n; i++) {
            double start = Math.min(e.getRoute().getTotalLength(), centre + half + i * 24);
            double end = Math.max(0, centre - half);
            HitSpec h = s.hit;
            TrackWalker w = new TrackWalker(e.getRoute(), start, end, s.p("chargeSpeed"), t, h);
            w.pierce = t.effectivePierce();
            w.detection = t.canSeeCamo();
            w.respectCamo = true; // B3: no detection before Illusion Pack
            w.kind = "llama";
            w.rgb = t.getType().primaryRgb;
            game.getEffects().addWalker(w);
        }
        t.setLastTarget(e);
        t.onAttacked(game);
        return true;
    }

    /** U3 Mythical Stampede: a kinetic shockwave across the full range every {@code shockInterval} s. */
    @Override public void update(Game game, Tower t, double dt) {
        TowerStats s = t.getStats();
        if (!s.has("shockInterval") || !t.periodic("shock", s.p("shockInterval"), dt)) return;
        HitSpec h = new HitSpec(s.p("shockDamage"), DamageType.KINETIC);
        h.knockback = s.p("shockKnockback");
        h.uncappedKnockback = true;
        h.multVsBoss = s.p("shockBossMult");
        double range = t.effectiveRange(game);
        int n = game.getCombat().areaHit(t.getPosition(), range, h, t, t.damageMultiplier(game), 9999, null);
        if (n > 0) game.getCombat().emit(VisualEvent.ability(t.getPosition(), range, t.getType().primaryRgb));
    }

    /** Open Titan Stampede: one herd per route runs exit to entrance, hitting every enemy once (bosses 60 + 50 px knockback). */
    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        HitSpec h = new HitSpec(a.p("damage"), DamageType.KINETIC);
        HitSpec bossHit = new HitSpec(a.p("bossDamage"), DamageType.KINETIC);
        bossHit.bossKnockback = a.p("bossKnockback");
        bossHit.uncappedKnockback = true;
        for (Path r : game.getMap().getRoutes()) {
            TrackWalker w = new TrackWalker(r, r.getTotalLength(), 0, a.p("herdSpeed"), t, h);
            w.bossHit = bossHit;
            w.pierce = Integer.MAX_VALUE;
            w.detection = true;
            w.kind = "herd";
            w.rgb = t.getType().primaryRgb;
            game.getEffects().addWalker(w);
        }
        game.getCombat().emit(VisualEvent.banner("OPEN TITAN STAMPEDE", t.getType().primaryRgb, 1.2));
        return true;
    }
}
