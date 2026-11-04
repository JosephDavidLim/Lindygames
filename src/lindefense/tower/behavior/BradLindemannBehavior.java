package lindefense.tower.behavior;

import lindefense.combat.DamageType;
import lindefense.combat.HitSpec;
import lindefense.effects.Barrage;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.BuffStat;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * Brad Lindemann (spec 3.1 #3). The megaphone aura (+attack speed, +pierce, +damage), shockwave shells with
 * +2 vs MOAB-class, and the U1 Discipline Field (enemy slow + flat mark in range, non-bosses only) are
 * data-driven. The U3 barrage and Inspiration Wave are implemented here.
 */
public final class BradLindemannBehavior extends TowerBehavior {
    @Override public void update(Game game, Tower t, double dt) {
        TowerStats s = t.getStats();
        if (!s.has("barrageInterval")) return;
        if (!t.periodic("barrage", s.p("barrageInterval"), dt)) return;
        HitSpec h = new HitSpec(s.p("barrageDamage"), DamageType.EXPLOSIVE);
        h.multVsBoss = s.p("barrageMoabMult");
        Barrage b = new Barrage(t, h, s.pi("barrageShells"), s.p("barrageSplash"));
        b.aim = Barrage.Aim.BOSS_ELSE_DENSEST;
        b.scatter = 20;
        game.getEffects().fire(b);
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        double range = t.effectiveRange(game);
        for (Tower o : game.getTowerManager().getTowers()) {
            if (o == t || o.getPosition().dist(t.getPosition()) > range) continue;
            o.resetAbilityCooldown();
            o.addTimedBuff(BuffStat.PIERCE_PCT, t.getType(), a.p("pierceBonusPct"), a.p("duration"));
        }
        return true;
    }
}
