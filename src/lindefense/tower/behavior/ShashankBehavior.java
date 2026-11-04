package lindefense.tower.behavior;

import lindefense.combat.AttackContext;
import lindefense.combat.VisualEvent;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.BuffStat;
import lindefense.tower.Tower;

/**
 * Shashank, Economic Support and Tactical Debuffer. Data Drain bolts pierce 2 enemies in a straight line; the +10%
 * damage-taken debuff is the hit's percent MARK (strongest mark wins, a reapplication refreshes the duration and never
 * stacks), so it boosts every tower and hero. Resource Optimization pays its cash at once and gives every placed tower
 * and hero within {@code radius} (Shashank included) a timed +20% attack-speed buff; buffs from the same source type
 * never stack, so a second activation only refreshes it.
 */
public final class ShashankBehavior extends TowerBehavior {
    @Override public void modifyAttack(Game game, Tower t, AttackContext c) {
        c.onImpact = HeroShots.STRAIGHT_PIERCE;
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        int cash = (int) a.p("cash");
        game.addCash(cash, t.getPosition());
        game.getCombat().emit(VisualEvent.text(t.getPosition().add(new Vec(0, -28)), "+$" + cash, 0xffd166));
        double r = a.p("radius");
        for (Tower o : game.getTowerManager().getTowers()) {
            if (o.isTemporary() || !o.isAlive()) continue;
            if (o.getPosition().dist(t.getPosition()) > r) continue;
            o.addTimedBuff(BuffStat.RATE, t.getType(), a.p("rateBonus"), a.p("duration"));
        }
        return true;
    }
}
