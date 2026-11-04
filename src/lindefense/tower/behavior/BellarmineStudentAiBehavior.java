package lindefense.tower.behavior;

import java.util.Comparator;
import lindefense.combat.DamageType;
import lindefense.effects.Drone;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.AttackKind;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * Bellarmine Student + AI (spec 3.1 #2). Homing notes, 3-target volleys (U1) and armour crack (U2) come from the
 * data table. Notes prioritise Camo and Regrow enemies ({@link #priorityComparator}). The Autonomous Scholar
 * companion (U3) and the Prompt Overclock drones are built on the {@link Drone} primitive.
 */
public final class BellarmineStudentAiBehavior extends TowerBehavior {
    private static final String COMPANION = "companion";

    @Override public Comparator<Enemy> priorityComparator(Tower t) {
        return Comparator.comparingInt((Enemy e) -> e.isCamo() || e.isRegrow() ? 0 : 1);
    }

    @Override public void update(Game game, Tower t, double dt) {
        TowerStats s = t.getStats();
        if (!s.has("companionRange")) return;
        if (game.getEffects().dronesOf(t, COMPANION).isEmpty()) {
            TowerStats ds = Drone.statsFor(AttackKind.PROJECTILE, s.p("companionRange"), s.p("companionInterval"),
                s.p("companionDamage"), DamageType.ENERGY, s.pi("companionPierce"), s.projectileSpeed);
            ds.hit.markBonus = s.p("markPct"); ds.hit.markDuration = s.p("markDuration");
            Drone d = Drone.orbit(t, ds, s.p("companionOrbit"), 0);
            d.detection = true; d.kind = COMPANION; d.rgb = t.getType().secondaryRgb;
            game.getEffects().addDrone(d);
        }
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        int n = (int) a.p("drones");
        for (int i = 0; i < n; i++) {
            TowerStats ds = Drone.statsFor(AttackKind.INSTANT, a.p("range"), a.p("interval"), a.p("damage"), DamageType.ENERGY, 1, 0);
            ds.hit.multVsLead = a.p("armoredMult"); ds.hit.multVsCeramic = a.p("armoredMult");
            Drone d = Drone.orbit(t, ds, a.p("orbit"), Math.PI * 2 * i / n + 1);
            d.lifetime = a.p("duration"); d.kind = "buddy"; d.detection = t.canSeeCamo(); d.orbitSpeed = -3; d.rgb = t.getType().primaryRgb;
            game.getEffects().addDrone(d);
        }
        return true;
    }
}
