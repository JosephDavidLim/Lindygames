package lindefense.tower.data;

import java.util.Arrays;
import java.util.List;
import lindefense.combat.DamageType;
import lindefense.tower.AttackKind;
import lindefense.tower.TowerDef;
import lindefense.tower.TowerType;
import lindefense.tower.data.TowerBuilder.AbilityBuilder;

/**
 * Spec 3.2 PRIMARY: Gemini, Claude, GPT. Owner: agent B1.
 * Parameters: GPT {@code moonCount, moonRadius, moonDamage, moonInterval} plus the unspecified {@code moonOrbitSpeed} (rad/s) and
 * {@code moonSize} (contact radius px); Astra Beam {@code duration, dps, bossMult, tick}, GPT U3 knockback is the hit's
 * {@code knockbackVsLead}; Gemini U2 strip flag is the hit's {@code stripsCamoRegrow}.
 */
public final class PrimaryTowers {
    private PrimaryTowers() { }

    public static List<TowerDef> all() { return Arrays.asList(gemini(), claude(), gpt()); }

    static TowerDef gemini() {
        return TowerBuilder.of(TowerType.GEMINI, "Prism Blaster", 400)
            .description("Instant-hit laser bolts that ricochet and later chain through a crowd. Gains camo detection at U2.")
            .tiers("Gemini 3.7 Flash", "Gemini 3.8 Flash", "Gemini 3.1 Pro", "Gemini 4 Argon")
            .attack(AttackKind.INSTANT, DamageType.ENERGY, 180, 0.8, 1, 1)
            .upgrade(650, null, "Interval 0.5, pierce 3, beams ricochet to up to 3 more enemies within 80 px.",
                s -> { s.interval(0.5); s.pierce = 3; s.chainHops = 3; s.chainRange = 80; })
            .upgrade(700, "Spectrum Analysis", "Gains camo detection. Hits permanently remove Camo and Regrow from non-boss enemies.",
                s -> { s.seesCamo = true; s.hit.stripsCamoRegrow = true; })
            .upgrade(1200, "Prismatic Beam", "Chains to up to 8 enemies; damage 2; x3 vs MOAB-class.",
                s -> { s.chainHops = 7; s.hit.damage = 2; s.hit.multVsBoss = 3; })
            .ability(AbilityBuilder.of("Argon Singularity", 50, "Pulls all non-boss enemies within 150 px into a vortex for 2 s (they stop moving), then deals 10 damage to each. Bosses take 30.")
                .p("radius", 150).p("holdTime", 2).p("damage", 10).p("bossDamage", 30).p("pullSpeed", 150))
            .build();
    }

    static TowerDef claude() {
        return TowerBuilder.of(TowerType.CLAUDE, "Ink Weaver", 450)
            .description("Needle prose spikes. Sharp damage cannot hurt Lead-type enemies until the final upgrade. No camo detection.")
            .tiers("Claude Haiku 5.5", "Claude Sonnet 5.5", "Claude Opus 5.5", "Claude Fable 5.1")
            .attack(AttackKind.PROJECTILE, DamageType.SHARP, 190, 0.7, 1, 2)
            .base(s -> { s.projectileSpeed = 700; })
            .upgrade(550, null, "Pierce 8, interval 0.55.",
                s -> { s.pierce = 8; s.interval(0.55); })
            .upgrade(650, "Context Snare", "Hit enemies are MARKED (+1 damage taken) and SLOWED 15% for 4 s.",
                s -> { s.hit.markFlat = 1; s.hit.markDuration = 4; s.hit.slowFraction = 0.15; s.hit.slowDuration = 4; })
            .upgrade(1100, null, "Pierce 20, damage 2, +3 damage vs Fortified / Lead-type / Ceramic-type, and can damage Lead.",
                s -> { s.pierce = 20; s.hit.damage = 2; s.hit.flatVsFortified = 3; s.hit.flatVsLead = 3; s.hit.flatVsCeramic = 3; s.hit.ignoresResistance = true; })
            .ability(AbilityBuilder.of("Opus Time-Lock", 45, "FREEZES all non-boss enemies in range for 4 s (bosses SLOWED 50%). Frozen enemies take x2 damage.")
                .p("freezeTime", 4).p("bossSlow", 0.5).p("bossSlowTime", 4))
            .build();
    }

    static TowerDef gpt() {
        return TowerBuilder.of(TowerType.GPT, "Cosmic Strategist", 500)
            .description("Plasma orbs that gain splash, two orbiting moons, burning hits and an adaptive final form. No camo detection.")
            .tiers("GPT-5.6 Terra", "GPT-6 Luna", "GPT-6 Astra", "GPT-6.1 Sol")
            .attack(AttackKind.PROJECTILE, DamageType.ENERGY, 180, 0.9, 1, 3)
            .base(s -> { s.projectileSpeed = 550; })
            .upgrade(750, null, "Splash radius 40; two orbiting moons (radius 60) deal 1 damage every 0.5 s on contact.",
                s -> { s.splashRadius = 40; s.set("moonCount", 2).set("moonRadius", 60).set("moonDamage", 1).set("moonInterval", 0.5)
                    .set("moonOrbitSpeed", 2.5).set("moonSize", 8); })
            .upgrade(800, "Solar Flare", "Hits BURN for 1 damage/s over 3 s.",
                s -> { s.hit.dotDps = 1; s.hit.dotDuration = 3; })
            .upgrade(1300, "Adaptive Plasma", "Each shot checks its target: x2 vs MOAB-class, +2 vs Fortified, +2 vs Ceramic-type, SONIC-like knockback vs Lead-type.",
                s -> { s.hit.multVsBoss = 2; s.hit.flatVsFortified = 2; s.hit.flatVsCeramic = 2; s.hit.knockbackVsLead = 20; })
            .ability(AbilityBuilder.of("Astra Beam", 60, "5 s continuous beam on the strongest enemy, 30 damage/s, x2 vs MOAB-class, ignores Fortified.")
                .p("duration", 5).p("dps", 30).p("bossMult", 2).p("tick", 0.25))
            .build();
    }
}
