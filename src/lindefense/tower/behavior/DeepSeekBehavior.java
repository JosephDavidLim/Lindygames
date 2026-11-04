package lindefense.tower.behavior;

import lindefense.combat.AttackContext;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Targeting;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * DeepSeek (spec 3.3 #7). Map-wide instant sniper (unlimited range, ignores line of sight), 3-shot volleys with
 * pierce 3 (U1) and the V4 Pro multipliers (U3) are data-driven. Recursive Lock (consecutive-hit bonus, default
 * Strongest targeting) and the Deep Reasoning Strike are implemented here.
 */
public final class DeepSeekBehavior extends TowerBehavior {
    @Override public void onUpgraded(Game game, Tower t) {
        if (t.getStats().has("defaultStrongest") && t.counter("strongestApplied") == 0) {
            t.setCounter("strongestApplied", 1);
            t.setTargeting(Targeting.STRONGEST);
        }
    }

    @Override public void modifyAttack(Game game, Tower t, AttackContext c) {
        TowerStats s = t.getStats();
        if (!s.has("streakMax") || c.primary == null) return;
        int streak = t.registerStreak(c.primary.getId());
        c.extraDamage += Math.min(s.p("streakMax"), streak * s.p("streakStep"));
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        Enemy best = null;
        for (Enemy e : game.getEnemyManager().getEnemies()) {
            if (!e.isAlive() || e.isStored() || e.isBoss()) continue;
            if (best == null || e.getHp() > best.getHp()) best = e;
        }
        if (best == null) return false;
        game.getEnemyManager().destroyWithoutChildren(best, t);
        return true;
    }
}
