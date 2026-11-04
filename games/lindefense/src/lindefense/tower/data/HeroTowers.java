package lindefense.tower.data;

import static lindefense.tower.TowerStats.u;

import java.util.Arrays;
import java.util.List;
import lindefense.combat.DamageType;
import lindefense.tower.AttackKind;
import lindefense.tower.Targeting;
import lindefense.tower.TowerDef;
import lindefense.tower.TowerType;
import lindefense.tower.data.TowerBuilder.AbilityBuilder;

/**
 * HERO STORE: Brad Lindemann, Brandon, Rohan, Ronil, Shashank, Owen. Heroes are bought once in the Hero Store
 * ({@code unlockCost}, a permanent unlock) and then deployed for free, once per match. They have no upgrade path.
 *
 * <p>Distances are written in the specification's map units with {@link lindefense.tower.TowerStats#u} (1 unit =
 * {@value lindefense.tower.TowerStats#MAP_UNIT} px); intervals are seconds between attacks. Mechanic parameters
 * (read with {@code stats.p("name")} / {@code ability.p("name")}):</p>
 * <ul>
 *   <li>Brad: {@code chainDamage}; chain count and reach are the typed {@code chainHops} / {@code chainRange}. Supreme
 *       Command: {@code duration, damageMult, splashMult, chainTargets, chainDamage}.</li>
 *   <li>Brandon: Overdrive {@code duration, rateMult, damageMult, pierce}.</li>
 *   <li>Rohan: {@code slowPct, slowDuration, freezeChance, freezeDuration, bossSlowPct}; Absolute Zero
 *       {@code freezeDuration, damage, bossSlowPct, bossSlowDuration}.</li>
 *   <li>Ronil: Meteor Shower {@code duration, meteors, damage, splash, fallTime}.</li>
 *   <li>Shashank: the vulnerability debuff is the hit's {@code markBonus/markDuration}; Resource Optimization
 *       {@code cash, duration, rateBonus, radius}.</li>
 *   <li>Owen: {@code drones, droneSpeed, droneOrbit}; drone damage / interval / range are the typed attack stats.
 *       Drone Swarm {@code drones, duration, damage, interval}.</li>
 * </ul>
 * Numbers the specification leaves open (kept here so they are easy to retune): Rohan's projectile speed 50 units/s,
 * Shashank's 55 units/s, the meteor fall time (0.5 s) and the swarm drones' range and projectile speed (Owen's).
 */
public final class HeroTowers {
    private HeroTowers() { }

    public static List<TowerDef> all() { return Arrays.asList(brad(), brandon(), rohan(), ronil(), shashank(), owen()); }

    static TowerDef brad() {
        return TowerBuilder.of(TowerType.BRAD_LINDEMANN, "Legendary Area-Damage Commander", 0)
            .unlockCost(1800).targeting(Targeting.STRONGEST)
            .description("Hero. Massive explosions and chaining energy: a command-energy shell at the strongest enemy explodes for full damage in 18 units, then lightning chains through up to 5 more enemies. Slow attack interval. No camo detection.")
            .tiers("Battlefield Commander")
            .attack(AttackKind.PROJECTILE, DamageType.EXPLOSIVE, u(55), 2.0, 35, 1)
            .base(s -> { s.splashRadius = u(18); s.projectileSpeed = u(45); s.chainHops = 5; s.chainRange = u(15);
                s.set("chainDamage", 25); })
            .ability(AbilityBuilder.of("Supreme Command", 45, "For 12 s: double primary damage, splash radius 18 -> 27 units, up to 10 chain targets at 35 damage each.")
                .p("duration", 12).p("damageMult", 2.0).p("splashMult", 1.5).p("chainTargets", 10).p("chainDamage", 35))
            .build();
    }

    static TowerDef brandon() {
        return TowerBuilder.of(TowerType.BRANDON, "Rapid-Fire Single-Target Specialist", 0)
            .unlockCost(1100).targeting(Targeting.STRONGEST)
            .description("Hero. Extremely fast precision bolts that fly in a straight line through up to 3 enemies. Great against fast enemies and medium groups; low damage per bolt. No camo detection.")
            .tiers("Precision Gunner")
            .attack(AttackKind.PROJECTILE, DamageType.ENERGY, u(42), 0.30, 12, 3)
            .base(s -> { s.projectileSpeed = u(65); })
            .ability(AbilityBuilder.of("Overdrive", 35, "For 10 s: fires twice as fast (interval halved), +50% damage, pierce 5.")
                .p("duration", 10).p("rateMult", 2.0).p("damageMult", 1.5).p("pierce", 5))
            .build();
    }

    static TowerDef rohan() {
        return TowerBuilder.of(TowerType.ROHAN, "Defensive Crowd-Control Specialist", 0)
            .unlockCost(1300)
            .description("Hero. Icy blasts damage everything within 12 units, slow by 40% for 3 s and may freeze each enemy for 1 s (20%). Bosses cannot be frozen and take a 15% slow instead. Low direct damage. No camo detection.")
            .tiers("Cryo Warden")
            .attack(AttackKind.PROJECTILE, DamageType.ENERGY, u(40), 1.5, 18, 1)
            .base(s -> { s.splashRadius = u(12); s.projectileSpeed = u(50);
                s.set("slowPct", 0.40).set("slowDuration", 3).set("freezeChance", 0.20).set("freezeDuration", 1).set("bossSlowPct", 0.15); })
            .ability(AbilityBuilder.of("Absolute Zero", 50, "Deals 50 damage to every enemy on the map and freezes all non-boss enemies for 4 s. Bosses are slowed 50% for 4 s instead.")
                .p("freezeDuration", 4).p("damage", 50).p("bossSlowPct", 0.50).p("bossSlowDuration", 4))
            .build();
    }

    static TowerDef ronil() {
        return TowerBuilder.of(TowerType.RONIL, "Explosive Artillery Specialist", 0)
            .unlockCost(1500)
            .description("Hero. Long-range meteor strikes deal full damage to every enemy within 16 units. Slow firing speed; excels against armoured groups and large enemies. No camo detection.")
            .tiers("Meteor Caller")
            .attack(AttackKind.PROJECTILE, DamageType.EXPLOSIVE, u(75), 3.0, 75, 1)
            .base(s -> { s.splashRadius = u(16); s.projectileSpeed = u(35); })
            .ability(AbilityBuilder.of("Meteor Shower", 55, "Calls 8 meteors over 6 s, each at a different spot with enemies anywhere on the map: 100 damage within 14 units.")
                .p("duration", 6).p("meteors", 8).p("damage", 100).p("splash", u(14)).p("fallTime", 0.5))
            .build();
    }

    static TowerDef shashank() {
        return TowerBuilder.of(TowerType.SHASHANK, "Economic Support and Tactical Debuffer", 0)
            .unlockCost(1000)
            .description("Hero. Data-draining shots hit up to 2 enemies and make them take +10% damage from every tower and hero for 4 s (refreshes, never stacks). Low direct damage. No camo detection.")
            .tiers("Data Strategist")
            .attack(AttackKind.PROJECTILE, DamageType.ENERGY, u(48), 1.2, 15, 2)
            .base(s -> { s.projectileSpeed = u(55); s.hit.markBonus = 0.10; s.hit.markDuration = 4; })
            .ability(AbilityBuilder.of("Resource Optimization", 60, "Immediately generates $500 and gives every tower and hero within 35 units +20% attack speed for 12 s (does not stack).")
                .p("cash", 500).p("duration", 12).p("rateBonus", 0.20).p("radius", u(35)))
            .build();
    }

    static TowerDef owen() {
        return TowerBuilder.of(TowerType.OWEN, "Summoner and Multi-Target Specialist", 0)
            .unlockCost(1400)
            .description("Hero. Deploys 3 autonomous drones that each pick, shoot and switch targets on their own (20 damage every 1.5 s). Relies on drone uptime. No camo detection.")
            .tiers("Drone Operator")
            .attack(AttackKind.CUSTOM, DamageType.ENERGY, u(50), 1.5, 20, 1)
            .base(s -> { s.projectileSpeed = u(55); s.set("drones", 3).set("droneSpeed", u(55)).set("droneOrbit", 30); })
            .ability(AbilityBuilder.of("Drone Swarm", 50, "Summons 5 extra drones for 15 s, each dealing 25 damage once per second. The original 3 drones stay.")
                .p("drones", 5).p("duration", 15).p("damage", 25).p("interval", 1.0))
            .build();
    }
}
