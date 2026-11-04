package lindefense.tower.behavior;

import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;

/**
 * Bellarmine Student (spec 3.1 #1). Base attack, splash (U1) and the Calculator Shield / All-Nighter auras come
 * from the data table via the generic attack and aura code. Caffeine Rush is implemented here.
 */
public final class BellarmineStudentBehavior extends TowerBehavior {
    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        t.addSelfBuff("caffeineRush", a.p("rateMult"), a.p("projectileSpeedMult"), a.p("duration"));
        return true;
    }
}
