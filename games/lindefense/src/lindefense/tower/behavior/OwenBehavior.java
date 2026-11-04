package lindefense.tower.behavior;

import lindefense.effects.Drone;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.AttackKind;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * Owen, Summoner and Multi-Target Specialist. Owen never fires himself: he keeps {@code drones} permanent drones in
 * orbit (respawned if missing, removed when Owen is sold). Each drone measures range from Owen, locks onto its own
 * target (preferring enemies no other Owen drone is locked on), keeps shooting it until it dies or leaves range, then
 * acquires a new one; drones inherit Owen's attack-speed buffs. Drone Swarm adds temporary drones with their own
 * damage and interval for {@code duration} seconds; the permanent drones are unaffected.
 */
public final class OwenBehavior extends TowerBehavior {
    public static final String CORE = "owen";
    public static final String SWARM = "swarm";

    @Override public boolean attack(Game game, Tower t) { return false; }   // the drones do the shooting

    @Override public void update(Game game, Tower t, double dt) {
        TowerStats s = t.getStats();
        int want = s.pi("drones");
        java.util.List<Drone> have = game.getEffects().dronesOf(t, CORE);
        for (Drone d : have) d.detection = t.canSeeCamo();
        for (int i = have.size(); i < want; i++) {
            TowerStats ds = Drone.statsFor(AttackKind.PROJECTILE, s.range, s.interval(), s.hit.damage, s.hit.type, 1, s.p("droneSpeed"));
            Drone d = Drone.orbit(t, ds, s.p("droneOrbit"), Math.PI * 2 * i / want);
            configure(d, t, CORE);
            d.orbitSpeed = 1.4;
            game.getEffects().addDrone(d);
        }
        for (Drone d : game.getEffects().dronesOf(t, SWARM)) d.detection = t.canSeeCamo();
    }

    private static void configure(Drone d, Tower t, String kind) {
        d.kind = kind; d.rangeFromOwner = true; d.inheritOwnerRate = true; d.stickyTarget = true;
        d.detection = t.canSeeCamo(); d.rgb = t.getType().primaryRgb;
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        TowerStats s = t.getStats();
        int n = (int) a.p("drones");
        for (int i = 0; i < n; i++) {
            TowerStats ds = Drone.statsFor(AttackKind.PROJECTILE, s.range, a.p("interval"), a.p("damage"), s.hit.type, 1, s.p("droneSpeed"));
            Drone d = Drone.orbit(t, ds, s.p("droneOrbit") * 1.8, Math.PI * 2 * i / n + 0.3);
            configure(d, t, SWARM);
            d.lifetime = a.p("duration");
            d.orbitSpeed = -2.2;
            d.rgb = 0x5fd6ff;
            game.getEffects().addDrone(d);
        }
        return true;
    }
}
