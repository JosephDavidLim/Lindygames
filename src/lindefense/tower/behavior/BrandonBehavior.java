package lindefense.tower.behavior;

import lindefense.combat.AttackContext;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;

/**
 * Brandon, Rapid-Fire Single-Target Specialist. Rapid Volley: one energy bolt every 0.30 s at the strongest enemy in
 * range; after the first hit the bolt flies straight on through up to 3 enemies in total. Overdrive (self buff)
 * halves the attack interval, adds 50% damage and raises pierce to 5.
 */
public final class BrandonBehavior extends TowerBehavior {
    /** Self-buff key while Overdrive is active. */
    public static final String OVERDRIVE = "overdrive";

    @Override public void modifyAttack(Game game, Tower t, AttackContext c) {
        if (t.hasSelfBuff(OVERDRIVE)) {
            AbilityDef a = t.getAbility();
            c.damageMult *= a.p("damageMult");
            c.pierce += (int) a.p("pierce") - t.getStats().pierce;   // base pierce 3 -> 5, buffs kept on top
            c.tag = "overdrive";
        }
        c.onImpact = HeroShots.STRAIGHT_PIERCE;
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        t.addSelfBuff(OVERDRIVE, a.p("rateMult"), 1, a.p("duration"));
        return true;
    }
}
