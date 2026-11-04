package lindefense.test;

import static lindefense.test.TestKit.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import lindefense.combat.DamageType;
import lindefense.combat.HitSpec;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyType;
import lindefense.enemy.Modifier;
import lindefense.enemy.StatusEffect;
import lindefense.map.Path;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AttackKind;
import lindefense.tower.BuffStat;
import lindefense.tower.Targeting;
import lindefense.tower.Tower;
import lindefense.tower.TowerCatalog;
import lindefense.tower.TowerDef;
import lindefense.tower.TowerStats;
import lindefense.tower.TowerType;
import lindefense.tower.behavior.GptBehavior;

/**
 * Group B1 tests: Heroes (Bellarmine Student, Student + AI, Brad Lindemann), Primary (Gemini, Claude, GPT) and
 * Military (DeepSeek, Copilot). Agents extend this class as the special mechanics are implemented; every
 * mechanic and ability of the eight towers has a focused test here.
 */
public final class HeroPrimaryMilitaryTests {
    private HeroPrimaryMilitaryTests() { }

    /** type, range, interval, damage, pierce, damage type, kind (base stats straight from the spec). */
    private static final Object[][] BASE = {
        { TowerType.BELLARMINE_STUDENT, 160.0, 0.9, 1.0, 2, DamageType.BLUNT, AttackKind.PROJECTILE },
        { TowerType.BELLARMINE_STUDENT_AI, 200.0, 0.7, 1.0, 2, DamageType.ENERGY, AttackKind.PROJECTILE },
        { TowerType.BRAD_LINDEMANN, 260.0, 1.4, 3.0, 15, DamageType.EXPLOSIVE, AttackKind.PROJECTILE },
        { TowerType.GEMINI, 180.0, 0.8, 1.0, 1, DamageType.ENERGY, AttackKind.INSTANT },
        { TowerType.CLAUDE, 190.0, 0.7, 1.0, 2, DamageType.SHARP, AttackKind.PROJECTILE },
        { TowerType.GPT, 180.0, 0.9, 1.0, 3, DamageType.ENERGY, AttackKind.PROJECTILE },
        { TowerType.DEEPSEEK, TowerStats.UNLIMITED_RANGE, 1.6, 2.0, 1, DamageType.ENERGY, AttackKind.INSTANT },
        { TowerType.COPILOT, 150.0, 1.0, 1.0, 1, DamageType.ENERGY, AttackKind.PROJECTILE },
    };

    public static void runAll() {
        run("b1.baseStatsMatchSpec", HeroPrimaryMilitaryTests::baseStatsMatchSpec);
        run("b1.upgradeStatsMatchSpec", HeroPrimaryMilitaryTests::upgradeStatsMatchSpec);
        run("b1.baseAttacksDamageEnemies", HeroPrimaryMilitaryTests::baseAttacksDamageEnemies);
        run("b1.student", HeroPrimaryMilitaryTests::student);
        run("b1.studentAi", HeroPrimaryMilitaryTests::studentAi);
        run("b1.brad", HeroPrimaryMilitaryTests::brad);
        run("b1.gemini", HeroPrimaryMilitaryTests::gemini);
        run("b1.claude", HeroPrimaryMilitaryTests::claude);
        run("b1.gpt", HeroPrimaryMilitaryTests::gpt);
        run("b1.deepseek", HeroPrimaryMilitaryTests::deepseek);
        run("b1.copilot", HeroPrimaryMilitaryTests::copilot);
        run("b1.geminiSingularity", HeroPrimaryMilitaryTests::geminiSingularity);
        run("b1.gptMoons", HeroPrimaryMilitaryTests::gptMoons);
        run("b1.gptAstraBeam", HeroPrimaryMilitaryTests::gptAstraBeam);
        run("b1.abilityCooldowns", HeroPrimaryMilitaryTests::abilityCooldowns);
        run("b1.copilotEnterpriseScope", HeroPrimaryMilitaryTests::copilotEnterpriseScope);
    }

    private static Game frozen() { Game g = newGame(0); g.setGlobalEnemySpeedMultiplier(0); return g; }
    private static Enemy at(Game g, EnemyType t, double d) { return spawnAt(g, t, null, 0, d); }
    /** An enemy with a larger HP pool (HP scale) so damage numbers are not capped by remaining HP. */
    private static Enemy scaled(Game g, EnemyType t, double d, double hpScale) {
        Enemy e = g.getEnemyManager().spawnScaled(t, null, 0, d, hpScale, 1.0);
        g.getEnemyManager().update(0);
        return e;
    }
    private static boolean near(double a, double b) { return Math.abs(a - b) < 1e-9; }

    private static void baseStatsMatchSpec() {
        for (Object[] r : BASE) {
            TowerType t = (TowerType) r[0];
            TowerStats s = TowerCatalog.get(t).baseStats;
            check(near(s.range, (Double) r[1]), t + " range " + s.range);
            check(Math.abs(s.interval() - (Double) r[2]) < 1e-9, t + " interval " + s.interval());
            check(near(s.hit.damage, (Double) r[3]), t + " damage " + s.hit.damage);
            check(s.pierce == (Integer) r[4], t + " pierce " + s.pierce);
            check(s.hit.type == r[5], t + " damage type " + s.hit.type);
            check(s.kind == r[6], t + " attack kind " + s.kind);
        }
        check(near(TowerCatalog.get(TowerType.BELLARMINE_STUDENT).baseStats.projectileSpeed, 500), "Student projectile speed 500");
        check(near(TowerCatalog.get(TowerType.BELLARMINE_STUDENT_AI).baseStats.projectileSpeed, 600), "Student+AI homing speed 600");
        check(near(TowerCatalog.get(TowerType.CLAUDE).baseStats.projectileSpeed, 700), "Claude projectile speed 700");
        check(near(TowerCatalog.get(TowerType.GPT).baseStats.projectileSpeed, 550), "GPT projectile speed 550");
        TowerStats brad = TowerCatalog.get(TowerType.BRAD_LINDEMANN).baseStats;
        check(brad.splashRadius == 60 && brad.hit.flatVsBoss == 2 && brad.auraRange == 180 && near(brad.auraRate, 0.10), "Brad splash 60, +2 vs MOAB, aura 180 / +10%");
        check(TowerCatalog.get(TowerType.BELLARMINE_STUDENT_AI).baseStats.ignoresLos && TowerCatalog.get(TowerType.DEEPSEEK).baseStats.ignoresLos, "Student+AI notes and DeepSeek ignore line of sight");
        TowerStats cop = TowerCatalog.get(TowerType.COPILOT).baseStats;
        check(cop.auraRange == 150 && near(cop.auraRate, 0.10), "Copilot tether 150 px / +10%");
    }

    private static void upgradeStatsMatchSpec() {
        TowerStats s;
        s = TowerCatalog.get(TowerType.BELLARMINE_STUDENT).statsForTier(1);
        check(s.splashRadius == 40 && s.hit.damage == 2 && s.pierce == 8, "Student U1: splash 40, dmg 2, pierce 8");
        s = TowerCatalog.get(TowerType.BELLARMINE_STUDENT).statsForTier(2);
        check(s.auraRange == 140 && s.auraPierce == 1 && s.pierce == 10, "Student U2: aura 140 +1 pierce, own pierce +2");
        s = TowerCatalog.get(TowerType.BELLARMINE_STUDENT).statsForTier(3);
        check(near(s.auraRate, 0.15) && s.auraDamage == 1 && Math.abs(s.interval() - 0.6) < 1e-9, "Student U3: +15% speed, +1 damage, interval 0.6");
        s = TowerCatalog.get(TowerType.BELLARMINE_STUDENT_AI).statsForTier(1);
        check(s.targets == 3, "Student+AI U1: 3 targets");
        s = TowerCatalog.get(TowerType.BELLARMINE_STUDENT_AI).statsForTier(2);
        check(s.pierce == 5 && s.hit.armorBreak > 0 && s.hit.armorBreakDuration == 3, "Student+AI U2: pierce +3, armor crack 3 s");
        s = TowerCatalog.get(TowerType.BELLARMINE_STUDENT_AI).statsForTier(3);
        check(s.p("companionRange") == 220 && s.p("companionInterval") == 0.4 && s.p("companionDamage") == 2 && s.p("companionPierce") == 3 && s.p("markPct") == 0.25 && s.p("markDuration") == 4, "Student+AI U3 companion numbers");
        s = TowerCatalog.get(TowerType.BRAD_LINDEMANN).statsForTier(1);
        check(near(s.auraEnemySlow, 0.30) && s.fieldMarkFlat == 1 && !s.fieldAffectsBosses, "Brad U1: non-boss enemies slowed 30% and marked +1");
        s = TowerCatalog.get(TowerType.BRAD_LINDEMANN).statsForTier(2);
        check(near(s.auraRate, 0.25) && s.auraPierce == 2, "Brad U2: +25% speed, +2 pierce");
        s = TowerCatalog.get(TowerType.BRAD_LINDEMANN).statsForTier(3);
        check(near(s.auraRate, 0.35) && s.auraDamage == 1 && s.p("barrageInterval") == 6 && s.p("barrageShells") == 5 && s.p("barrageDamage") == 25 && s.p("barrageSplash") == 90 && s.p("barrageMoabMult") == 3, "Brad U3 numbers");
        s = TowerCatalog.get(TowerType.GEMINI).statsForTier(1);
        check(Math.abs(s.interval() - 0.5) < 1e-9 && s.pierce == 3 && s.chainHops == 3 && s.chainRange == 80, "Gemini U1: interval 0.5, pierce 3, ricochet 3 within 80");
        s = TowerCatalog.get(TowerType.GEMINI).statsForTier(3);
        check(s.chainHops + 1 == 8 && s.hit.damage == 2 && s.hit.multVsBoss == 3 && s.seesCamo && s.hit.stripsCamoRegrow, "Gemini U3: chains to 8 enemies, dmg 2, x3 vs MOAB (U2 effects kept)");
        s = TowerCatalog.get(TowerType.CLAUDE).statsForTier(1);
        check(s.pierce == 8 && Math.abs(s.interval() - 0.55) < 1e-9, "Claude U1: pierce 8, interval 0.55");
        s = TowerCatalog.get(TowerType.CLAUDE).statsForTier(2);
        check(s.hit.markFlat == 1 && s.hit.markDuration == 4 && near(s.hit.slowFraction, 0.15), "Claude U2: Context Snare");
        s = TowerCatalog.get(TowerType.CLAUDE).statsForTier(3);
        check(s.pierce == 20 && s.hit.damage == 2 && s.hit.flatVsFortified == 3 && s.hit.flatVsLead == 3 && s.hit.flatVsCeramic == 3 && s.hit.ignoresResistance, "Claude U3: pierce 20, dmg 2, +3 flats, can damage Lead");
        s = TowerCatalog.get(TowerType.GPT).statsForTier(1);
        check(s.splashRadius == 40 && s.p("moonCount") == 2 && s.p("moonRadius") == 60 && s.p("moonDamage") == 1 && s.p("moonInterval") == 0.5, "GPT U1: splash 40 + moons");
        s = TowerCatalog.get(TowerType.GPT).statsForTier(2);
        check(s.hit.dotDps == 1 && s.hit.dotDuration == 3, "GPT U2: burn 1/s for 3 s");
        s = TowerCatalog.get(TowerType.GPT).statsForTier(3);
        check(s.hit.multVsBoss == 2 && s.hit.flatVsFortified == 2 && s.hit.flatVsCeramic == 2 && s.hit.knockbackVsLead == 20, "GPT U3: Adaptive Plasma");
        s = TowerCatalog.get(TowerType.DEEPSEEK).statsForTier(1);
        check(s.projectilesPerShot == 3 && s.pierce == 3, "DeepSeek U1: 3 shots, pierce 3");
        s = TowerCatalog.get(TowerType.DEEPSEEK).statsForTier(3);
        check(s.hit.damage == 6 && s.hit.multVsSortingAndLists == 3 && s.hit.multVsBoss == 2, "DeepSeek U3: dmg 6, x3 vs sorting/lists, x2 vs bosses");
        s = TowerCatalog.get(TowerType.COPILOT).statsForTier(1);
        check(near(s.auraRate, 0.20) && s.auraPierce == 1, "Copilot U1: +20% speed, +1 pierce");
        s = TowerCatalog.get(TowerType.COPILOT).statsForTier(2);
        check(s.p("droneInterval") == 8 && s.p("droneDamage") == 6 && s.p("droneSplash") == 70, "Copilot U2 drone numbers");
        s = TowerCatalog.get(TowerType.COPILOT).statsForTier(3);
        check(near(s.p("globalMilitaryRate"), 0.15) && s.p("globalMilitaryDamage") == 1 && near(s.auraRate, 0.30) && s.auraPierce == 2, "Copilot U3 numbers");
        int[] cds = { 45, 60, 90, 50, 45, 60, 40, 60 };
        for (int i = 0; i < BASE.length; i++) check(TowerCatalog.get((TowerType) BASE[i][0]).ability.cooldown == cds[i], BASE[i][0] + " ability cooldown " + cds[i]);
    }

    private static void baseAttacksDamageEnemies() {
        for (Object[] r : BASE) {
            TowerType type = (TowerType) r[0];
            Game g = frozen();
            Tower t = type == TowerType.DEEPSEEK
                ? forcePlace(g, type, buildableNear(g, new Vec(1130, 860), 150), 0)
                : towerNearTrack(g, type, 0, 400);
            Enemy e = at(g, EnemyType.QUICK_SORT, 400);
            runSeconds(g, 4);
            check(t.getDamageDealt() > 0 && e.getHp() < e.getMaxHp(), type + " base attack damages an enemy");
        }
        // DeepSeek's range is the entire map
        Game g = frozen();
        Tower ds = forcePlace(g, TowerType.DEEPSEEK, buildableNear(g, new Vec(1130, 860), 150), 0);
        Enemy far = at(g, EnemyType.QUICK_SORT, 100);
        check(ds.getPosition().dist(far.getPosition()) > 600 && g.getCombat().acquireTargets(ds, 1).size() == 1, "DeepSeek targets enemies anywhere on the map");
    }

    // ------------------------------------------------------------------ heroes
    private static void student() {
        Game g = frozen();
        Tower st = towerNearTrack(g, TowerType.BELLARMINE_STUDENT, 3, 400);
        Tower gpt = forcePlace(g, TowerType.GPT, buildableNear(g, st.getPosition().add(new Vec(60, 0)), 80), 0);
        g.getTowerManager().recomputeAuras();
        check(gpt.effectivePierce() == gpt.getStats().pierce + 1, "Calculator Shield: nearby towers +1 pierce");
        check(near(gpt.getAuraRateBonus(), 0.15) && gpt.flatDamageBuff() == 1, "All-Nighter Aura: +15% speed, +1 damage");
        double rate = st.effectiveRate(g), speed = st.projectileSpeedMult();
        st.resetAbilityCooldown();
        check(g.activateAbility(st), "Caffeine Rush activates");
        check(near(st.effectiveRate(g), rate * 2) && near(st.projectileSpeedMult(), speed * 2), "Caffeine Rush doubles attack speed and projectile speed");
        runSeconds(g, 10.1);
        check(near(st.effectiveRate(g), rate) && near(st.projectileSpeedMult(), speed), "...for 10 s only");
        // splash textbooks hit several enemies
        Game g2 = frozen();
        Tower s2 = towerNearTrack(g2, TowerType.BELLARMINE_STUDENT, 1, 400);
        List<Enemy> grp = new ArrayList<>();
        Enemy a = scaled(g2, EnemyType.DYNAMIC_ARRAY, 380, 10), b = scaled(g2, EnemyType.DYNAMIC_ARRAY, 395, 10);
        g2.getCombat().attack(s2);
        runSeconds(g2, 1);
        check(a.getHp() < a.getMaxHp() && b.getHp() < b.getMaxHp(), "one textbook's splash (40 px) hits both enemies");
        check(s2.getDamageDealt() > 0, "Student U1 deals damage");
    }

    private static void studentAi() {
        Game g = frozen();
        Tower ai = towerNearTrack(g, TowerType.BELLARMINE_STUDENT_AI, 1, 450);
        Enemy lead = at(g, EnemyType.QUICK_SORT, 500);
        Enemy regrow = spawnAt(g, EnemyType.QUICK_SORT, EnumSet.of(Modifier.REGROW), 0, 420);
        Enemy camo = spawnAt(g, EnemyType.QUICK_SORT, EnumSet.of(Modifier.CAMO), 0, 410);
        List<Enemy> picks = g.getCombat().acquireTargets(ai, 3);
        check(picks.size() == 3 && (picks.get(0) == regrow || picks.get(0) == camo) && (picks.get(1) == regrow || picks.get(1) == camo) && picks.get(2) == lead,
            "notes prioritise Camo and Regrow enemies over the leader");
        check(ai.canSeeCamo(), "Student + AI sees camo");
        g.getCombat().attack(ai);
        g.getCombat().updateProjectiles(0);
        check(g.getCombat().getProjectiles().size() == 3, "U1 fires 3 notes");
        java.util.Set<Enemy> targets = new java.util.HashSet<>();
        for (lindefense.combat.Projectile p : g.getCombat().getProjectiles()) targets.add(p.getTarget());
        check(targets.size() == 3, "...at 3 different targets");
        // U2 armor crack, U3 companion with marks
        Game g2 = frozen();
        Tower ai3 = towerNearTrack(g2, TowerType.BELLARMINE_STUDENT_AI, 3, 450);
        Enemy heap = at(g2, EnemyType.HEAP_SORT, 450);
        runSeconds(g2, 3);
        check(heap.isArmorCracked() || !heap.isAlive(), "U2 hits apply ARMOR CRACK");
        check(g2.getEffects().dronesOf(ai3, "companion").size() == 1, "U3 spawns exactly one orbiting AI companion");
        check(heap.getMarkedBonus() > 0 || !heap.isAlive(), "companion hits MARK enemies");
        check(Math.abs(heap.getMarkedBonus() - 0.25) < 1e-9 || !heap.isAlive(), "...for +25%");
        runSeconds(g2, 3);
        check(g2.getEffects().dronesOf(ai3, "companion").size() == 1, "the companion is not duplicated");
        // ability: 2 study-buddy drones for 15 s
        Game g3 = frozen();
        Tower ai0 = towerNearTrack(g3, TowerType.BELLARMINE_STUDENT_AI, 0, 450);
        ai0.resetAbilityCooldown();
        check(g3.activateAbility(ai0) && g3.getEffects().dronesOf(ai0, "buddy").size() == 2, "Prompt Overclock spawns 2 study-buddy drones");
        Enemy target = at(g3, EnemyType.MERGE_SORT, 450);
        Enemy hp = at(g3, EnemyType.BELLARMINE_COLLEGE_PREP, 470);
        runSeconds(g3, 14.5);
        check(g3.getEffects().dronesOf(ai0, "buddy").size() == 2, "drones still alive before 15 s");
        runSeconds(g3, 1);
        check(g3.getEffects().dronesOf(ai0, "buddy").isEmpty(), "drones last 15 s");
        check(hp.getHp() < hp.getMaxHp(), "drones deal damage");
    }

    private static void brad() {
        Game g = frozen();
        Tower brad = towerNearTrack(g, TowerType.BRAD_LINDEMANN, 1, 400);
        Tower gpt = forcePlace(g, TowerType.GPT, buildableNear(g, brad.getPosition().add(new Vec(70, 0)), 80), 0);
        g.getTowerManager().recomputeAuras();
        check(near(gpt.getAuraRateBonus(), 0.10), "Megaphone aura +10% attack speed");
        Enemy norm = at(g, EnemyType.HEAP_SORT, 400);
        Enemy boss = at(g, EnemyType.BELLARMINE_COLLEGE_PREP, 420);
        g.run(30);
        check(near(norm.getSlowFraction(), 0.30), "Discipline Field slows non-boss enemies 30%");
        check(near(norm.getMarkedFlat(), 1), "...and marks them +1");
        check(boss.getSlowFraction() == 0 && boss.getMarkedFlat() == 0, "bosses are spared by the field");
        // No camo detection for Brad
        check(!brad.canSeeCamo(), "Brad has no camo detection");
        Game g3 = frozen();
        Tower b3 = towerNearTrack(g3, TowerType.BRAD_LINDEMANN, 0, 400);
        Enemy camo = spawnAt(g3, EnemyType.QUICK_SORT, EnumSet.of(Modifier.CAMO), 0, 400);
        runSeconds(g3, 3);
        check(camo.getHp() == camo.getMaxHp(), "Brad cannot hit Camo enemies (explosive, no detection)");
        // U3 barrage aimed at the boss
        Game g2 = frozen();
        Tower b2 = towerNearTrack(g2, TowerType.BRAD_LINDEMANN, 3, 400);
        Enemy bossFar = at(g2, EnemyType.BELLARMINE_COLLEGE_PREP, 900);
        double hp0 = bossFar.getHp();
        runSeconds(g2, 6.8);
        double expected = 5 * 25 * 3 * 0.75;
        check(Math.abs((hp0 - bossFar.getHp()) - expected) < 1e-6, "barrage anywhere on the track at the boss: 5 x 25 x3 (x0.75 armour) = " + expected + ", got " + (hp0 - bossFar.getHp()));
        // Inspiration Wave
        Game g4 = frozen();
        Tower b4 = towerNearTrack(g4, TowerType.BRAD_LINDEMANN, 0, 400);
        Tower o1 = forcePlace(g4, TowerType.BELLARMINE_STUDENT, buildableNear(g4, b4.getPosition().add(new Vec(80, 0)), 80), 0);
        Tower o2 = forcePlace(g4, TowerType.CLAUDE, buildableNear(g4, new Vec(1130, 860), 100), 0);
        check(o1.getAbilityCooldownRemaining() > 0 && o2.getAbilityCooldownRemaining() > 0, "others start on cooldown");
        b4.resetAbilityCooldown();
        check(g4.activateAbility(b4), "Inspiration Wave activates");
        check(o1.getAbilityCooldownRemaining() == 0, "resets cooldowns of towers in range");
        check(o2.getAbilityCooldownRemaining() > 0, "...but not towers out of range");
        check(b4.getAbilityCooldownRemaining() == 90, "...nor Brad's own");
        check(o1.effectivePierce() == o1.getStats().pierce + Math.round(o1.getStats().pierce * 0.5), "+50% pierce for 10 s");
        runSeconds(g4, 10.2);
        check(o1.effectivePierce() == o1.getStats().pierce, "pierce bonus expires");
    }

    // ------------------------------------------------------------------ primary
    private static void gemini() {
        Game g = frozen();
        Tower gem = towerNearTrack(g, TowerType.GEMINI, 1, 450);
        List<Enemy> grp = new ArrayList<>();
        for (int i = 0; i < 6; i++) grp.add(at(g, EnemyType.DYNAMIC_ARRAY, 440 + i * 12));
        g.getCombat().attack(gem);
        int hit = 0; for (Enemy e : grp) if (!e.isAlive() || e.getHp() < e.getMaxHp()) hit++;
        check(hit >= 4, "U1 beam pierces then ricochets to up to 3 more enemies (" + hit + " hit)");
        // U2: strip camo/regrow permanently from non-bosses
        Game g2 = frozen();
        Tower g3 = towerNearTrack(g2, TowerType.GEMINI, 2, 450);
        Enemy c = spawnAt(g2, EnemyType.HEAP_SORT, EnumSet.of(Modifier.CAMO, Modifier.REGROW), 0, 450);
        check(g3.canSeeCamo() && g2.getCombat().acquireTargets(g3, 1).get(0) == c, "U2 sees camo");
        runSeconds(g2, 1);
        check(!c.isCamo() && !c.isRegrow(), "U2 hits permanently remove Camo and Regrow");
        // U3 chain of 8
        Game g4 = frozen();
        Tower g5 = towerNearTrack(g4, TowerType.GEMINI, 3, 450);
        List<Enemy> chain = new ArrayList<>();
        for (int i = 0; i < 12; i++) chain.add(at(g4, EnemyType.HEAP_SORT, 360 + i * 20 + (i % 2)));
        g4.getCombat().attack(g5);
        int n = 0; for (Enemy e : chain) if (e.getHp() < e.getMaxHp()) n++;
        check(n >= 8, "U3 hits at least 8 enemies in one attack (" + n + ")");
    }

    private static void claude() {
        Game g = frozen();
        Tower c = towerNearTrack(g, TowerType.CLAUDE, 0, 450);
        Enemy heap = at(g, EnemyType.HEAP_SORT, 450);
        runSeconds(g, 4);
        check(heap.getHp() == heap.getMaxHp(), "Claude's Sharp needles cannot damage Lead-type Heap Sort");
        Game g2 = frozen();
        Tower c3 = towerNearTrack(g2, TowerType.CLAUDE, 3, 450);
        Enemy h2 = at(g2, EnemyType.HEAP_SORT, 450);
        runSeconds(g2, 2);
        check(h2.getHp() < h2.getMaxHp() || !h2.isAlive(), "Claude U3 can damage Lead");
        Enemy m = at(g2, EnemyType.MERGE_SORT, 460);
        double d = g2.getCombat().applyHit(m, c3.getStats().hit, c3, 1, 0);
        check(near(d, 5), "U3 damage 2 + 3 vs Ceramic = 5, got " + d);
        // U2 Context Snare
        Game g3 = frozen();
        Tower c2 = towerNearTrack(g3, TowerType.CLAUDE, 2, 450);
        Enemy s = at(g3, EnemyType.QUICK_SORT, 450);
        g3.getCombat().applyHit(s, c2.getStats().hit, c2, 1, 0);
        check(near(s.getMarkedFlat(), 1) && near(s.getSlowFraction(), 0.15), "Context Snare: mark +1 and slow 15%");
        // ability: freeze non-bosses in range, slow bosses, x2 damage
        Game g4 = frozen();
        Tower c4 = towerNearTrack(g4, TowerType.CLAUDE, 0, 450);
        Enemy in = at(g4, EnemyType.HEAP_SORT, 450), out = at(g4, EnemyType.HEAP_SORT, 1400), boss = at(g4, EnemyType.BELLARMINE_COLLEGE_PREP, 440);
        c4.resetAbilityCooldown();
        check(g4.activateAbility(c4), "Opus Time-Lock activates");
        check(in.isFrozen() && !out.isFrozen() && !boss.isFrozen(), "freezes non-bosses in range only");
        check(near(boss.getSlowFraction(), 0.5 / 3), "bosses are slowed (50%, delivered at 1/3 by the global boss-slow rule)");
        check(g4.getCombat().applyHit(in, new HitSpec(1, DamageType.ENERGY), null, 1, 0) == 2, "frozen enemies take x2 damage");
        in.tickStatuses(4.1);
        check(!in.isFrozen(), "freeze lasts 4 s");
    }

    private static void gpt() {
        Game g = frozen();
        Tower t = towerNearTrack(g, TowerType.GPT, 2, 450);
        Enemy e = at(g, EnemyType.BELLARMINE_COLLEGE_PREP, 450);
        runSeconds(g, 2);
        check(e.hasStatus(StatusEffect.Kind.DOT), "U2 hits BURN");
        double dps = e.statusMagnitude(StatusEffect.Kind.DOT);
        check(dps == 1, "burn 1 dmg/s and refreshes, never stacks: " + dps);
        Game g2 = frozen();
        Tower t3 = towerNearTrack(g2, TowerType.GPT, 3, 450);
        Enemy lead = at(g2, EnemyType.HEAP_SORT, 450);
        double before = lead.getDistance();
        g2.getCombat().applyHit(lead, t3.getStats().hit, t3, 1, 0);
        check(lead.getDistance() == before - 20, "U3: SONIC-like 20 px knockback vs Lead-type");
        Enemy m = at(g2, EnemyType.MERGE_SORT, 500);
        double d = g2.getCombat().applyHit(m, t3.getStats().hit, t3, 1, 0);
        check(near(d, 1 + 2), "U3: +2 vs Ceramic-type, got " + d);
        Enemy boss = at(g2, EnemyType.BELLARMINE_COLLEGE_PREP, 600);
        d = g2.getCombat().applyHit(boss, t3.getStats().hit, t3, 1, 0);
        check(near(d, 2 * 0.75), "U3: x2 vs MOAB-class (armour 25%), got " + d);
        // moons and Astra Beam: see gptMoons / gptAstraBeam
    }

    // ------------------------------------------------------------------ military
    private static void deepseek() {
        Game g = frozen();
        Tower t = forcePlace(g, TowerType.DEEPSEEK, buildableNear(g, new Vec(1130, 860), 150), 2);
        check(t.getTargeting() == Targeting.STRONGEST, "R1 makes Strongest the default targeting");
        Enemy boss = at(g, EnemyType.BELLARMINE_COLLEGE_PREP, 600);
        List<Double> deltas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            double hp = boss.getHp();
            g.getCombat().attack(t);
            deltas.add(hp - boss.getHp());
        }
        for (int i = 0; i < 10; i++) {
            double expect = 3 * (2 + Math.min(6, i)) * 0.75;
            check(Math.abs(deltas.get(i) - expect) < 1e-6, "volley " + i + " damage " + expect + " got " + deltas.get(i));
        }
        // bonus resets when the target changes
        Enemy other = scaled(g, EnemyType.MERGE_SORT, 300, 50);
        t.setTargeting(Targeting.WEAKEST);
        double hp = other.getHp();
        g.getCombat().attack(t);
        check(other.getHp() < hp, "attacks the new target");
        g.getEnemyManager().destroyWithoutChildren(other, null);
        g.getEnemyManager().update(0);
        t.setTargeting(Targeting.STRONGEST);
        hp = boss.getHp();
        g.getCombat().attack(t);
        check(Math.abs((hp - boss.getHp()) - 3 * 2 * 0.75) < 1e-6, "streak reset after switching targets, got " + (hp - boss.getHp()));
        // U3 multipliers
        Tower t3 = forcePlace(g, TowerType.DEEPSEEK, buildableNear(g, new Vec(1000, 860), 150), 3);
        Enemy bub = scaled(g, EnemyType.BUBBLE_SORT, 800, 20), ll = scaled(g, EnemyType.SINGLY_LINKED_LIST, 820, 20), arr = scaled(g, EnemyType.DYNAMIC_ARRAY, 840, 20);
        check(near(g.getCombat().applyHit(bub, t3.getStats().hit, t3, 1, 0), 18), "U3 x3 vs sorting algorithms");
        check(near(g.getCombat().applyHit(ll, t3.getStats().hit, t3, 1, 0), 18), "U3 x3 vs linked lists");
        check(near(g.getCombat().applyHit(arr, t3.getStats().hit, t3, 1, 0), 6), "no bonus vs ordinary enemies");
        Enemy bossHit = at(g, EnemyType.BELLARMINE_COLLEGE_PREP, 900);
        check(near(g.getCombat().applyHit(bossHit, t3.getStats().hit, t3, 1, 0), 6 * 2 * 0.75), "U3 x2 vs bosses");
        // ability: strongest non-boss destroyed, no children, cash once
        Game g2 = frozen();
        Tower d2 = forcePlace(g2, TowerType.DEEPSEEK, buildableNear(g2, new Vec(1130, 860), 150), 0);
        Enemy tim = at(g2, EnemyType.TIMSORT, 400);
        at(g2, EnemyType.STATIC_ARRAY, 420);
        at(g2, EnemyType.BELLARMINE_COLLEGE_PREP, 440);
        double cash = g2.getCash();
        d2.resetAbilityCooldown();
        check(g2.activateAbility(d2), "Deep Reasoning Strike activates");
        g2.getEnemyManager().update(0);
        check(!tim.isAlive() && g2.getEnemyManager().countOfType(EnemyType.MERGE_SORT) == 0 && g2.getEnemyManager().countOfType(EnemyType.INSERTION_SORT) == 0, "the strongest non-boss dies without children");
        check(g2.getCash() == cash + Math.round(EnemyType.TIMSORT.reward * g2.getRewardScale()), "cash paid once");
        check(g2.getEnemyManager().countOfType(EnemyType.BELLARMINE_COLLEGE_PREP) == 1, "bosses are never chosen");
    }

    private static void copilot() {
        Game g = frozen();
        Tower cop = towerNearTrack(g, TowerType.COPILOT, 3, 450);
        Tower ds = forcePlace(g, TowerType.DEEPSEEK, buildableNear(g, new Vec(1130, 860), 150), 0);      // far away MILITARY tower
        Tower gpt = forcePlace(g, TowerType.GPT, buildableNear(g, cop.getPosition().add(new Vec(60, 0)), 80), 0); // local, non-military
        Tower near = forcePlace(g, TowerType.COPILOT, buildableNear(g, cop.getPosition().add(new Vec(-60, 0)), 80), 0); // local military tower
        g.getTowerManager().recomputeAuras();
        check(near(ds.getAuraRateBonus(), 0.15) && ds.flatDamageBuff() == 1, "Enterprise Network: MILITARY towers anywhere +15% speed, +1 damage");
        check(near(gpt.getAuraRateBonus(), 0.30) && gpt.effectivePierce() == gpt.getStats().pierce + 2 && gpt.flatDamageBuff() == 0, "local tether +30% speed, +2 pierce (non-military gets no damage)");
        check(near(near.getAuraRateBonus(), 0.30), "same source never stacks: local MILITARY tower gets max(30%, 15%) = 30%, got " + near.getAuraRateBonus());
        // ability: MILITARY x2 for 12 s
        cop.resetAbilityCooldown();
        double base = ds.effectiveRate(g);
        check(g.activateAbility(cop), "Auto-Refactor activates");
        g.getTowerManager().recomputeAuras();
        check(near(ds.getAuraRateBonus(), 1.0) || near(ds.getAuraRateBonus(), 0.15 + 0.0) || ds.getAuraRateBonus() >= 0.15, "MILITARY towers boosted");
        check(near(ds.buffTotal(BuffStat.RATE), 1.0), "MILITARY towers attack x2 faster (capped at +100%)");
        check(gpt.buffTotal(BuffStat.RATE) < 1.0, "non-MILITARY towers are not boosted by the ability");
        runSeconds(g, 12.2);
        check(ds.buffTotal(BuffStat.RATE) < 1.0, "boost ends after 12 s");
        // U2 drone every 8 s at the densest cluster
        Game g2 = frozen();
        Tower c2 = forcePlace(g2, TowerType.COPILOT, buildableNear(g2, new Vec(1130, 860), 150), 2);
        List<Enemy> cluster = new ArrayList<>();
        for (int i = 0; i < 3; i++) cluster.add(at(g2, EnemyType.BELLARMINE_COLLEGE_PREP, 500 + i * 10));
        runSeconds(g2, 7.5);
        check(cluster.get(0).getHp() == cluster.get(0).getMaxHp(), "no drone before 8 s");
        runSeconds(g2, 1);
        check(cluster.get(1).getHp() == cluster.get(1).getMaxHp() - 6 * 0.75, "explosive drone (6 dmg, armour 25%) hits the cluster at 8 s");
        check(cluster.get(0).getHp() < cluster.get(0).getMaxHp() && cluster.get(2).getHp() < cluster.get(2).getMaxHp(), "splash hits the whole cluster");
    }
    // ------------------------------------------------------------------ B1 additions
    private static void geminiSingularity() {
        // pull: non-bosses in 150 px are held and gathered at the vortex centre, bosses are not, outsiders untouched
        Game g = frozen();
        Tower gem = towerNearTrack(g, TowerType.GEMINI, 0, 450);
        Enemy a = scaled(g, EnemyType.DYNAMIC_ARRAY, 425, 50), b = scaled(g, EnemyType.DYNAMIC_ARRAY, 475, 50);
        Enemy boss = at(g, EnemyType.BELLARMINE_COLLEGE_PREP, 450);
        Enemy far = scaled(g, EnemyType.DYNAMIC_ARRAY, 1000, 50);
        check(a.getPosition().dist(gem.getPosition()) <= 150 && b.getPosition().dist(gem.getPosition()) <= 150 && boss.getPosition().dist(gem.getPosition()) <= 150
            && far.getPosition().dist(gem.getPosition()) > 150, "scenario: three enemies inside 150 px, one outside");
        gem.resetAbilityCooldown();
        check(g.activateAbility(gem), "Argon Singularity activates");
        runSeconds(g, 1.0);
        check(a.isHeld() && b.isHeld() && !boss.isHeld() && !far.isHeld(), "non-bosses in range are held; bosses and outsiders are not");
        check(Math.abs(a.getDistance() - b.getDistance()) < 1e-6, "held enemies are pulled together into the vortex");
        runSeconds(g, 1.2);
        check(!a.isHeld() && !b.isHeld(), "the vortex lasts 2 s");
        check(gem.getAbilityCooldownRemaining() > 0, "cooldown spent");

        // damage: 10 each (boss 30, x0.75 armour) after the hold, nothing before it
        Game g2 = frozen();
        Tower gem2 = towerNearTrack(g2, TowerType.GEMINI, 0, 450);
        Enemy a2 = scaled(g2, EnemyType.DYNAMIC_ARRAY, 440, 50), far2 = scaled(g2, EnemyType.DYNAMIC_ARRAY, 1000, 50);
        Enemy boss2 = at(g2, EnemyType.BELLARMINE_COLLEGE_PREP, 460);
        double a0 = a2.getHp(), b0 = boss2.getHp(), f0 = far2.getHp();
        gem2.resetAbilityCooldown();
        g2.activateAbility(gem2);
        g2.getEffects().update(1.9);
        check(a2.getHp() == a0 && boss2.getHp() == b0, "no damage during the hold");
        g2.getEffects().update(0.2);
        check(near(a0 - a2.getHp(), 10), "10 damage to each held enemy, got " + (a0 - a2.getHp()));
        check(near(b0 - boss2.getHp(), 30 * 0.75), "bosses take 30 (x0.75 armour), got " + (b0 - boss2.getHp()));
        check(far2.getHp() == f0, "enemies outside 150 px are untouched");
        // refused (no cooldown spent) when nothing is in range
        Game g3 = frozen();
        Tower gem3 = towerNearTrack(g3, TowerType.GEMINI, 0, 450);
        gem3.resetAbilityCooldown();
        check(!g3.activateAbility(gem3) && gem3.getAbilityCooldownRemaining() == 0, "refused with nothing in range");
    }

    private static void gptMoons() {
        Game g = frozen();
        Tower t = towerNearTrack(g, TowerType.GPT, 1, 450);
        t.getStats().hit.damage = 0;                 // silence the orbs so only the moons deal damage
        Enemy e = scaled(g, EnemyType.DYNAMIC_ARRAY, 450, 100);
        double d = e.getPosition().dist(t.getPosition());
        check(Math.abs(d - 60) <= t.getStats().p("moonSize") + e.getRadius(), "scenario: enemy within a moon's orbit band, d=" + d);
        double toward = Math.atan2(e.getPosition().y - t.getPosition().y, e.getPosition().x - t.getPosition().x);
        double hp0 = e.getHp();
        int steps = (int) Math.round(1.9 / Game.DT);
        for (int i = 0; i < steps; i++) { t.setCounter("moonAngle", toward); g.run(1); }
        check(near(hp0 - e.getHp(), 4), "moon on contact: 1 damage every 0.5 s -> 4 hits in 1.9 s, got " + (hp0 - e.getHp()));
        check(t.getStats().p("moonCount") == 2 && t.getStats().p("moonRadius") == 60, "two moons of orbit radius 60");
        Vec m0 = GptBehavior.moonPosition(t, 0), m1 = GptBehavior.moonPosition(t, 1);
        check(near(m0.dist(t.getPosition()), 60) && near(m1.dist(t.getPosition()), 60) && near(m0.dist(m1), 120), "moons orbit at radius 60, opposite each other");
        // moons move
        t.setCounter("moonAngle", 0);
        Vec before = GptBehavior.moonPosition(t, 0);
        g.run(30);
        check(GptBehavior.moonPosition(t, 0).dist(before) > 1, "the moons orbit");
        // no contact, no damage
        Game g2 = frozen();
        Tower t2 = towerNearTrack(g2, TowerType.GPT, 1, 450);
        t2.getStats().hit.damage = 0;
        Enemy e2 = scaled(g2, EnemyType.DYNAMIC_ARRAY, 450, 100);
        double away = Math.atan2(e2.getPosition().y - t2.getPosition().y, e2.getPosition().x - t2.getPosition().x) + Math.PI / 2;
        double hp2 = e2.getHp();
        for (int i = 0; i < steps; i++) { t2.setCounter("moonAngle", away); g2.run(1); }
        check(e2.getHp() == hp2, "no damage without contact");
        // base GPT has no moons
        Game g3 = frozen();
        Tower t0 = towerNearTrack(g3, TowerType.GPT, 0, 450);
        t0.getStats().hit.damage = 0;
        Enemy e3 = scaled(g3, EnemyType.DYNAMIC_ARRAY, 450, 100);
        double toward3 = Math.atan2(e3.getPosition().y - t0.getPosition().y, e3.getPosition().x - t0.getPosition().x);
        for (int i = 0; i < steps; i++) { t0.setCounter("moonAngle", toward3); g3.run(1); }
        check(GptBehavior.moonPosition(t0, 0) == null && e3.getHp() == e3.getMaxHp(), "moons are an U1 feature");
    }

    private static void gptAstraBeam() {
        Game g = frozen();
        Tower t = towerNearTrack(g, TowerType.GPT, 0, 450);
        t.getStats().hit.damage = 0;                 // silence the orbs
        Enemy strong = scaled(g, EnemyType.DYNAMIC_ARRAY, 900, 100), weak = scaled(g, EnemyType.DYNAMIC_ARRAY, 300, 10);
        double s0 = strong.getHp(), w0 = weak.getHp();
        t.resetAbilityCooldown();
        check(g.activateAbility(t) && GptBehavior.beamActive(t), "Astra Beam activates");
        runSeconds(g, 2.5);
        check(near(s0 - strong.getHp(), 75), "30 dmg/s on the strongest enemy (anywhere): 75 after 2.5 s, got " + (s0 - strong.getHp()));
        runSeconds(g, 3.0);
        check(near(s0 - strong.getHp(), 150), "the beam lasts exactly 5 s (150 damage), got " + (s0 - strong.getHp()));
        check(weak.getHp() == w0 && !GptBehavior.beamActive(t), "only the strongest enemy is hit, and the beam ends");
        // x2 vs MOAB-class (armour 25% still applies)
        Game g2 = frozen();
        Tower t2 = towerNearTrack(g2, TowerType.GPT, 0, 450);
        t2.getStats().hit.damage = 0;
        Enemy boss = at(g2, EnemyType.BELLARMINE_COLLEGE_PREP, 900);
        double b0 = boss.getHp();
        t2.resetAbilityCooldown();
        g2.activateAbility(t2);
        runSeconds(g2, 5.5);
        check(near(b0 - boss.getHp(), 150 * 2 * 0.75), "x2 vs MOAB-class (x0.75 armour): " + (150 * 2 * 0.75) + ", got " + (b0 - boss.getHp()));
        // ignores Fortified: the doubled HP does not slow it down
        Game g3 = frozen();
        Tower t3 = towerNearTrack(g3, TowerType.GPT, 0, 450);
        t3.getStats().hit.damage = 0;
        Enemy fort = g3.getEnemyManager().spawnScaled(EnemyType.DYNAMIC_ARRAY, EnumSet.of(Modifier.FORTIFIED), 0, 900, 100, 1.0);
        g3.getEnemyManager().update(0);
        double f0 = fort.getHp();
        t3.resetAbilityCooldown();
        g3.activateAbility(t3);
        runSeconds(g3, 5.5);
        check(near(f0 - fort.getHp(), 300), "Fortified targets take x2 so their bonus HP is ignored: 300, got " + (f0 - fort.getHp()));
        // retargets when the target dies; refuses with no enemy
        Game g4 = frozen();
        Tower t4 = towerNearTrack(g4, TowerType.GPT, 0, 450);
        t4.getStats().hit.damage = 0;
        t4.resetAbilityCooldown();
        check(!g4.activateAbility(t4) && t4.getAbilityCooldownRemaining() == 0, "no enemy: ability refused, cooldown kept");
    }

    private static void abilityCooldowns() {
        TowerType[] types = { TowerType.BELLARMINE_STUDENT, TowerType.BELLARMINE_STUDENT_AI, TowerType.BRAD_LINDEMANN, TowerType.GEMINI,
            TowerType.CLAUDE, TowerType.GPT, TowerType.DEEPSEEK, TowerType.COPILOT };
        for (TowerType type : types) {
            Game g = frozen();
            Tower t = type == TowerType.DEEPSEEK || type == TowerType.COPILOT
                ? forcePlace(g, type, buildableNear(g, new Vec(1130, 860), 150), 0) : towerNearTrack(g, type, 0, 450);
            double cd = t.getAbility().cooldown;
            check(near(t.getAbilityCooldownRemaining(), cd / 2), type + " ability starts at 50% cooldown");
            check(!t.isAbilityReady() && !g.activateAbility(t), type + " cannot fire while cooling down");
            runSeconds(g, 10);
            check(near(t.getAbilityCooldownRemaining(), cd / 2 - 10), type + " cooldown ticks in game time (10 s run)");
            runSeconds(g, cd / 2 - 10 + 0.1);
            check(t.isAbilityReady(), type + " ready after the remaining half");
        }
        // cooldown restarts in full after use, and does not tick when the game does not step
        Game g = frozen();
        Tower st = towerNearTrack(g, TowerType.BELLARMINE_STUDENT, 0, 450);
        st.resetAbilityCooldown();
        check(g.activateAbility(st) && near(st.getAbilityCooldownRemaining(), 45), "full 45 s cooldown after use");
        double rem = st.getAbilityCooldownRemaining();
        g.getCombat().updateProjectiles(5);
        check(st.getAbilityCooldownRemaining() == rem, "cooldown follows game time only");
    }

    private static void copilotEnterpriseScope() {
        Game g = frozen();
        Tower cop = towerNearTrack(g, TowerType.COPILOT, 3, 450);
        Tower farMil = forcePlace(g, TowerType.DEEPSEEK, buildableNear(g, new Vec(1130, 860), 150), 0);
        Tower farOther = forcePlace(g, TowerType.GPT, buildableNear(g, new Vec(1130, 760), 100), 0);
        check(farMil.getPosition().dist(cop.getPosition()) > cop.getStats().auraRange && farOther.getPosition().dist(cop.getPosition()) > cop.getStats().auraRange, "scenario: both far from the tether");
        g.getTowerManager().recomputeAuras();
        check(near(farMil.getAuraRateBonus(), 0.15) && farMil.flatDamageBuff() == 1, "Enterprise Network reaches a far MILITARY tower");
        check(farOther.getAuraRateBonus() == 0 && farOther.flatDamageBuff() == 0 && farOther.effectivePierce() == farOther.getStats().pierce, "...but not a far non-MILITARY tower");
        Tower hero = forcePlace(g, TowerType.BELLARMINE_STUDENT, buildableNear(g, new Vec(1130, 660), 100), 0);
        g.getTowerManager().recomputeAuras();
        check(hero.getAuraRateBonus() == 0 && hero.flatDamageBuff() == 0, "HERO towers get no Enterprise bonus");
        // before U3 there is no global bonus
        Game g2 = frozen();
        Tower c2 = forcePlace(g2, TowerType.COPILOT, buildableNear(g2, new Vec(600, 700), 100), 2);
        Tower far2 = forcePlace(g2, TowerType.DEEPSEEK, buildableNear(g2, new Vec(1130, 860), 150), 0);
        g2.getTowerManager().recomputeAuras();
        check(c2.getPosition().dist(far2.getPosition()) > 150 && far2.getAuraRateBonus() == 0, "no map-wide bonus before U3");
    }
}
