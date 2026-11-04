package lindefense.tower.behavior;

import java.util.List;
import lindefense.combat.AttackContext;
import lindefense.combat.Projectile;
import lindefense.combat.VisualEvent;
import lindefense.effects.Drone;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * MiniMax (spec 3.6 #20). Bouncing data packets (+10% speed per bounce), the 6-bounce upgrade and packet
 * splitting (U2, TowerStats.clones) are data-driven in the projectile engine. This class adds the orbiting clones
 * (U1: 1, U3: 3), the 20-packet barrage every 6 s (U3) and Scale Factor Overclock.
 *
 * <p>Clones are non-mirroring {@link Drone}s whose stats are a copy of the tower's current stats, refreshed when
 * the tier changes or the Overclock starts/ends (so they also fire x3 faster with x3 bounces while it lasts).</p>
 */
public final class MiniMaxBehavior extends TowerBehavior {
    private static final String CLONES = "clones";
    private static final String SIG = "cloneSig";
    private static final String OVERCLOCK = "overclock";

    @Override public void update(Game game, Tower t, double dt) {
        TowerStats s = t.getStats();
        if (s.has("cloneCount")) maintainClones(game, t, s);
        if (s.has("barrageInterval") && t.addCounter("barrage", dt) >= s.p("barrageInterval")) barrage(game, t, s);
    }

    private static void maintainClones(Game game, Tower t, TowerStats s) {
        int n = s.pi("cloneCount");
        Drone[] ds = (Drone[]) t.state().get(CLONES);
        if (ds == null || ds.length != n) {
            Drone[] bigger = new Drone[n];
            if (ds != null) System.arraycopy(ds, 0, bigger, 0, Math.min(ds.length, n));
            ds = bigger;
            t.state().put(CLONES, ds);
        }
        boolean oc = t.hasSelfBuff(OVERCLOCK);
        double sig = t.getTier() * 2 + (oc ? 1 : 0) + 1;
        boolean refresh = t.counter(SIG) != sig;
        TowerStats cs = null;
        for (int i = 0; i < n; i++) {
            Drone d = ds[i];
            if (d == null || !d.isAlive()) {
                d = Drone.orbit(t, cloneStats(t, oc), s.p("cloneOrbit"), Math.PI * 2 * i / n);
                d.kind = "clone";
                d.rgb = t.getType().secondaryRgb;
                game.getEffects().addDrone(d);
                ds[i] = d;
            } else if (refresh) {
                if (cs == null) cs = cloneStats(t, oc);
                d.stats = cs;
            }
        }
        if (refresh) t.setCounter(SIG, sig);
    }

    /** The base attack as the clones fire it: the tower's current stats, x3 rate and bounces during Overclock. */
    private static TowerStats cloneStats(Tower t, boolean overclock) {
        TowerStats cs = t.getStats().copy();
        if (overclock) {
            AbilityDef a = t.getAbility();
            cs.rate *= a.p("rateMult");
            cs.bounces = (int) Math.round(cs.bounces * a.p("bounceMult"));
        }
        return cs;
    }

    /** U3: a synchronized barrage of barrageCount packets, spread round-robin over the targets in range. */
    private static void barrage(Game game, Tower t, TowerStats s) {
        int count = s.pi("barrageCount");
        List<Enemy> targets = game.getCombat().acquireTargets(t, count);
        if (targets.isEmpty()) { t.setCounter("barrage", s.p("barrageInterval")); return; }   // wait for a target
        t.setCounter("barrage", 0);
        boolean oc = t.hasSelfBuff(OVERCLOCK);
        double bounceMult = oc ? t.getAbility().p("bounceMult") : 1;
        for (int i = 0; i < count; i++) {
            Projectile.Shot sh = Projectile.Shot.of(game, t, t.getPosition(), targets.get(i % targets.size()));
            sh.bounces = (int) Math.round(sh.bounces * bounceMult);
            game.getCombat().fire(sh);
        }
        game.getCombat().emit(VisualEvent.ability(t.getPosition(), 60, t.getType().primaryRgb));
    }

    /** Overclock: bounce count x3 on every packet fired while the self buff lasts. */
    @Override public void modifyAttack(Game game, Tower t, AttackContext c) {
        if (t.hasSelfBuff(OVERCLOCK)) c.bounces = (int) Math.round(c.bounces * t.getAbility().p("bounceMult"));
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        t.addSelfBuff(OVERCLOCK, a.p("rateMult"), 1, a.p("duration"));
        game.getCombat().emit(VisualEvent.banner("SCALE FACTOR OVERCLOCK", t.getType().primaryRgb, 1.2));
        return true;
    }
}
