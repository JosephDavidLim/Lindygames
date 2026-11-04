package lindefense.tower.data;

import java.util.Arrays;
import java.util.List;
import lindefense.combat.DamageType;
import lindefense.tower.AttackKind;
import lindefense.tower.TowerDef;
import lindefense.tower.TowerType;
import lindefense.tower.data.TowerBuilder.AbilityBuilder;

/**
 * Spec 3.1 HEROES: Bellarmine Student, Bellarmine Student + AI, Brad Lindemann. Owner: agent B1.
 * Distances px, intervals seconds. Parameters (read with {@code stats.p("name")}):
 * <ul>
 *   <li>Student + AI: {@code prioritizeCamoRegrow}; U3 companion {@code companionRange, companionInterval,
 *       companionDamage, companionPierce, companionOrbit, markPct, markDuration}.</li>
 *   <li>Brad: U1 {@code fieldSlow}/{@code fieldMarkFlat} are typed field stats; U3 {@code barrageInterval,
 *       barrageShells, barrageDamage, barrageSplash, barrageMoabMult}.</li>
 * </ul>
 */
public final class HeroTowers {
    private HeroTowers() { }

    public static List<TowerDef> all() { return Arrays.asList(student(), studentAi(), brad()); }

    static TowerDef student() {
        return TowerBuilder.of(TowerType.BELLARMINE_STUDENT, "Melee-Ranged Hybrid", 550)
            .description("Hero. Throws pencils and small books; later heavy textbooks with splash, and an aura that boosts nearby towers. Blunt damage, no camo detection.")
            .tiers("Backpack Toss", "Textbook Smash", "Calculator Shield", "All-Nighter Aura")
            .attack(AttackKind.PROJECTILE, DamageType.BLUNT, 160, 0.9, 1, 2)
            .base(s -> { s.projectileSpeed = 500; })
            .upgrade(350, "Textbook Smash", "Heavy textbooks: splash radius 40, damage 2, pierce 8.",
                s -> { s.splashRadius = 40; s.hit.damage = 2; s.pierce = 8; })
            .upgrade(600, "Calculator Shield", "Aura radius 140 gives nearby towers +1 pierce; own pierce +2.",
                s -> { s.auraRange = 140; s.auraPierce = 1; s.pierce += 2; })
            .upgrade(1100, "All-Nighter Aura", "Aura also gives +15% attack speed and +1 damage; own interval 0.6.",
                s -> { s.auraRate = 0.15; s.auraDamage = 1; s.interval(0.6); })
            .ability(AbilityBuilder.of("Caffeine Rush", 45, "Doubles own attack speed and projectile speed for 10 s.")
                .p("duration", 10).p("rateMult", 2).p("projectileSpeedMult", 2))
            .build();
    }

    static TowerDef studentAi() {
        return TowerBuilder.of(TowerType.BELLARMINE_STUDENT_AI, "Autonomous Hybrid", 800)
            .description("Hero. Homing laser-etched notes that prioritise Camo and Regrow enemies and ignore line-of-sight blockers. Built-in camo detection.")
            .tiers("Copilot Assist", "Automated Essay", "Neural Backpack", "Autonomous Scholar")
            .attack(AttackKind.PROJECTILE, DamageType.ENERGY, 200, 0.7, 1, 2)
            .base(s -> { s.projectileSpeed = 600; s.seesCamo = true; s.ignoresLos = true; s.set("prioritizeCamoRegrow", 1); })
            .upgrade(500, "Automated Essay", "Each attack fires 3 homing notes at 3 different targets.",
                s -> { s.targets = 3; })
            .upgrade(850, "Neural Backpack", "Pierce +3; hits apply ARMOR CRACK for 3 s.",
                s -> { s.pierce += 3; s.hit.armorBreak = 1; s.hit.armorBreakDuration = 3; })
            .upgrade(1500, "Autonomous Scholar", "Orbiting AI companion attacks on its own (range 220, interval 0.4, damage 2, pierce 3, camo); its hits MARK enemies +25% damage taken for 4 s.",
                s -> { s.set("companionRange", 220).set("companionInterval", 0.4).set("companionDamage", 2).set("companionPierce", 3)
                    .set("companionOrbit", 45).set("markPct", 0.25).set("markDuration", 4); })
            .ability(AbilityBuilder.of("Prompt Overclock", 60, "Spawns 2 study-buddy drones for 15 s (interval 0.2, damage 3, x3 vs Lead-type and Ceramic-type).")
                .p("drones", 2).p("duration", 15).p("interval", 0.2).p("damage", 3).p("armoredMult", 3).p("range", 200).p("orbit", 40))
            .build();
    }

    static TowerDef brad() {
        return TowerBuilder.of(TowerType.BRAD_LINDEMANN, "Commander Support & Heavy Artillery", 2000)
            .description("Hero. The strongest and most expensive unit in the game: a megaphone aura boosts nearby towers while shockwave shells pound the track. No camo detection.")
            .tiers("Megaphone Shouted Orders", "Discipline Field", "Tactical Megaphone", "Supreme Commander Strike")
            .attack(AttackKind.PROJECTILE, DamageType.EXPLOSIVE, 260, 1.4, 3, 15)
            .base(s -> { s.splashRadius = 60; s.projectileSpeed = 450; s.hit.flatVsBoss = 2; s.auraRange = 180; s.auraRate = 0.10; })
            .upgrade(1500, "Discipline Field", "Non-boss enemies in range are SLOWED 30% and MARKED (+1 damage taken).",
                s -> { s.auraEnemySlow = 0.30; s.fieldMarkFlat = 1; s.fieldAffectsBosses = false; })
            .upgrade(3000, "Tactical Megaphone", "Aura becomes +25% attack speed and +2 pierce.",
                s -> { s.auraRate = 0.25; s.auraPierce = 2; })
            .upgrade(6000, "Supreme Commander Strike", "Every 6 s a 5-shell barrage hits anywhere on the track, aimed at the boss if present, otherwise the densest cluster (damage 25 each, splash 90, x3 vs MOAB-class). Aura becomes +35% attack speed and +1 damage.",
                s -> { s.auraRate = 0.35; s.auraDamage = 1;
                    s.set("barrageInterval", 6).set("barrageShells", 5).set("barrageDamage", 25).set("barrageSplash", 90).set("barrageMoabMult", 3); })
            .ability(AbilityBuilder.of("Inspiration Wave", 90, "Resets ability cooldowns of all towers in range (not Brad's own) and gives them +50% pierce for 10 s.")
                .p("pierceBonusPct", 0.5).p("duration", 10))
            .build();
    }
}
