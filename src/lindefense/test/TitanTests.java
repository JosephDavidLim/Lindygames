package lindefense.test;

import static lindefense.test.TestKit.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import lindefense.combat.DamageType;
import lindefense.combat.HitSpec;
import lindefense.combat.Projectile;
import lindefense.combat.VisualEvent;
import lindefense.effects.Drone;
import lindefense.effects.Effects;
import lindefense.effects.NodeHazard;
import lindefense.effects.TrackWalker;
import lindefense.effects.TrackZone;
import lindefense.enemy.Boss;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyType;
import lindefense.enemy.Modifier;
import lindefense.enemy.StatusEffect;
import lindefense.map.MapCatalog;
import lindefense.map.Path;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AttackKind;
import lindefense.tower.Tower;
import lindefense.tower.TowerCatalog;
import lindefense.tower.TowerStats;
import lindefense.tower.TowerType;
import lindefense.tower.behavior.KimiBehavior;

/**
 * Group B3 tests: Experimental Titans (Qwen, Llama, Mistral, Cohere, GLM, Kimi, MiniMax). Agents extend this
 * class as the special mechanics are implemented; every {@code // TODO(B3)} in the behaviours should gain a test here.
 */
public final class TitanTests {
    private TitanTests() { }

    /** type, range, interval, damage, pierce, damage type, kind. */
    private static final Object[][] BASE = {
        { TowerType.QWEN, 190.0, 1.0, 2.0, 4, DamageType.ENERGY, AttackKind.PROJECTILE },
        { TowerType.LLAMA, 170.0, 2.0, 2.0, 6, DamageType.KINETIC, AttackKind.CUSTOM },
        { TowerType.MISTRAL, 170.0, 0.7, 1.0, 3, DamageType.KINETIC, AttackKind.PROJECTILE },
        { TowerType.COHERE, 230.0, 1.5, 3.0, 1, DamageType.EXPLOSIVE, AttackKind.PROJECTILE },
        { TowerType.GLM, 170.0, 1.0, 1.0, 2, DamageType.GRAVITY, AttackKind.PROJECTILE },
        { TowerType.KIMI, 180.0, 0.9, 1.0, 2, DamageType.MAGIC, AttackKind.PROJECTILE },
        { TowerType.MINIMAX, 170.0, 0.5, 1.0, 1, DamageType.ENERGY, AttackKind.PROJECTILE },
    };

    public static void runAll() {
        run("b3.baseStatsMatchSpec", TitanTests::baseStatsMatchSpec);
        run("b3.upgradeStatsMatchSpec", TitanTests::upgradeStatsMatchSpec);
        run("b3.baseAttacksDamageEnemies", TitanTests::baseAttacksDamageEnemies);
        run("b3.qwen", TitanTests::qwen);
        run("b3.llama", TitanTests::llama);
        run("b3.mistral", TitanTests::mistral);
        run("b3.cohere", TitanTests::cohere);
        run("b3.glm", TitanTests::glm);
        run("b3.kimi", TitanTests::kimi);
        run("b3.minimax", TitanTests::minimax);
        run("b3.qwen.droneFormation", TitanTests::qwenDrones);
        run("b3.llama.mythicalStampede", TitanTests::llamaShockwave);
        run("b3.llama.openTitanStampede", TitanTests::llamaHerd);
        run("b3.llama.camoRespected", TitanTests::llamaCamo);
        run("b3.mistral.gust", TitanTests::mistralGust);
        run("b3.mistral.cyclone", TitanTests::mistralCyclone);
        run("b3.mistral.hurricane", TitanTests::mistralHurricane);
        run("b3.mistral.stratosphereTempest", TitanTests::mistralTempest);
        run("b3.cohere.fragmentation", TitanTests::cohereFragmentation);
        run("b3.cohere.commandOverride", TitanTests::cohereOverride);
        run("b3.glm.nodeNetwork", TitanTests::glmNetwork);
        run("b3.glm.gridSlowAndFortified", TitanTests::glmGrid);
        run("b3.kimi.crystalBurst", TitanTests::kimiBurst);
        run("b3.kimi.infiniteContextBurst", TitanTests::kimiAbility);
        run("b3.kimi.memoryAndFortified", TitanTests::kimiMemory);
        run("b3.minimax.clones", TitanTests::minimaxClones);
        run("b3.minimax.barrage", TitanTests::minimaxBarrage);
        run("b3.minimax.overclock", TitanTests::minimaxOverclock);
        run("b3.qwen.u3FortifiedBoss", TitanTests::qwenFortifiedBoss);
        for (Object[] r : BASE) {
            TowerType type = (TowerType) r[0];
            run("b3.abilityCooldown." + type, () -> abilityCooldown(type));
        }
        run("b3.stressTitans", TitanTests::stressTitans);
    }

    private static Game frozen() { Game g = newGame(0); g.setGlobalEnemySpeedMultiplier(0); return g; }
    private static Enemy at(Game g, EnemyType t, double d) { return spawnAt(g, t, null, 0, d); }
    private static boolean near(double a, double b) { return Math.abs(a - b) < 1e-9; }
    private static Enemy scaled(Game g, EnemyType t, double d, double hpScale) {
        Enemy e = g.getEnemyManager().spawnScaled(t, null, 0, d, hpScale, 1.0);
        g.getEnemyManager().update(0);
        return e;
    }
    /** Fires one attack and lets its projectiles resolve without advancing game time (so the tower does not fire again). */
    private static void shoot(Game g, Tower t) {
        g.getCombat().attack(t);
        for (int i = 0; i < 60; i++) g.getCombat().updateProjectiles(Game.DT);
    }

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
        check(near(TowerCatalog.get(TowerType.QWEN).baseStats.projectileSpeed, 500), "Qwen disc speed 500");
        TowerStats l = TowerCatalog.get(TowerType.LLAMA).baseStats;
        check(l.p("chargeSegment") == 150 && l.p("chargeCount") == 1, "Llama: one llama charges a 150 px segment");
        check(TowerCatalog.get(TowerType.MISTRAL).baseStats.hit.knockbackVsFast == 15, "Mistral: 15 px knockback on fast enemies");
        check(TowerCatalog.get(TowerType.COHERE).baseStats.splashRadius == 60, "Cohere splash 60");
        check(near(TowerCatalog.get(TowerType.GLM).baseStats.hit.slowFraction, 0.10), "GLM slows 10%");
        TowerStats k = TowerCatalog.get(TowerType.KIMI).baseStats;
        check(k.p("streakMax") == 3 && near(k.hit.multVsFortified, 0.5), "Kimi: consecutive-hit max +3, x0.5 vs Fortified");
        TowerStats m = TowerCatalog.get(TowerType.MINIMAX).baseStats;
        check(m.bounces == 3 && near(m.p("bounceSpeedGain"), 0.10), "MiniMax: 3 bounces, +10% speed per bounce");
    }

    private static void upgradeStatsMatchSpec() {
        TowerStats s;
        s = TowerCatalog.get(TowerType.QWEN).statsForTier(1);
        check(s.pierce == 8 && s.projectilesPerShot == 3, "Qwen U1: pierce 8, 3 discs");
        s = TowerCatalog.get(TowerType.QWEN).statsForTier(2);
        check(s.p("droneCount") == 2 && s.p("droneInterval") == 1.2, "Qwen U2: 2 drones, interval 1.2");
        s = TowerCatalog.get(TowerType.QWEN).statsForTier(3);
        check(s.hit.damage == 4 && s.hit.multVsHighHp == 3, "Qwen U3: damage 4, x3 vs max HP >= 5");
        s = TowerCatalog.get(TowerType.LLAMA).statsForTier(1);
        check(s.p("chargeCount") == 3, "Llama U1: 3 creatures per cycle");
        s = TowerCatalog.get(TowerType.LLAMA).statsForTier(2);
        check(s.seesCamo && s.pierce == 12, "Llama U2: camo detection, pierce 12");
        s = TowerCatalog.get(TowerType.LLAMA).statsForTier(3);
        check(s.p("shockInterval") == 4 && s.p("shockDamage") == 8 && s.p("shockKnockback") == 40 && s.p("shockBossMult") == 2, "Llama U3 shockwave");
        s = TowerCatalog.get(TowerType.MISTRAL).statsForTier(1);
        check(s.p("gustInterval") == 3 && s.p("gustFast") == 60 && s.p("gustOther") == 25, "Mistral U1 gust");
        s = TowerCatalog.get(TowerType.MISTRAL).statsForTier(2);
        check(s.p("tornadoInterval") == 8 && s.p("tornadoDuration") == 5 && s.p("tornadoTick") == 0.3 && s.p("tornadoDamage") == 1, "Mistral U2 cyclone");
        s = TowerCatalog.get(TowerType.MISTRAL).statsForTier(3);
        check(s.p("vortexRadius") == 70 && s.p("vortexTick") == 0.3 && s.p("vortexDamage") == 2 && s.p("vortexSlow") == 0.3 && s.p("vortexBossMult") == 2, "Mistral U3 hurricane");
        s = TowerCatalog.get(TowerType.COHERE).statsForTier(1);
        check(s.p("shrapnelCount") == 8 && s.p("shrapnelDamage") == 1 && s.p("shrapnelPierce") == 2, "Cohere U1 shrapnel");
        s = TowerCatalog.get(TowerType.COHERE).statsForTier(2);
        check(s.hit.armorBreak > 0 && s.hit.armorBreakDuration == 5 && near(s.hit.markBonus, 0.5) && s.hit.markDuration == 5, "Cohere U2: armor crack + 50% mark for 5 s");
        s = TowerCatalog.get(TowerType.COHERE).statsForTier(3);
        check(s.hit.damage == 10 && s.splashRadius == 90 && Math.abs(s.interval() - 1.1) < 1e-9 && s.hit.multVsBoss == 3, "Cohere U3");
        s = TowerCatalog.get(TowerType.GLM).statsForTier(1);
        check(s.p("nodes") == 2 && s.p("lineDamage") == 2 && s.p("lineCooldown") == 0.3, "GLM U1: 2 nodes, 2 damage, 0.3 s");
        s = TowerCatalog.get(TowerType.GLM).statsForTier(2);
        check(s.p("nodes") == 4 && s.p("gridSlow") == 0.30, "GLM U2: 4 nodes, 30% slow");
        s = TowerCatalog.get(TowerType.GLM).statsForTier(3);
        check(s.p("nodes") == 6 && s.p("lineDamage") == 5 && s.p("lineFortifiedMult") == 2, "GLM U3: 6 nodes, 5 damage, x2 vs Fortified");
        s = TowerCatalog.get(TowerType.KIMI).statsForTier(1);
        check(s.p("memoryPer") == 100 && s.p("memoryCap") == 5, "Kimi U1: +1 per 100 pops, max +5");
        s = TowerCatalog.get(TowerType.KIMI).statsForTier(2);
        check(s.p("streakMax") == 8 && s.p("pierceBonusPerStack") == 1, "Kimi U2: max +8, +1 pierce per stack");
        s = TowerCatalog.get(TowerType.KIMI).statsForTier(3);
        check(s.p("memoryCap") == 12 && s.p("burstEvery") == 10 && s.p("burstDamage") == 15 && s.p("burstSplash") == 70, "Kimi U3");
        s = TowerCatalog.get(TowerType.MINIMAX).statsForTier(1);
        check(s.bounces == 6 && s.p("cloneCount") == 1, "MiniMax U1: a clone, 6 bounces");
        s = TowerCatalog.get(TowerType.MINIMAX).statsForTier(2);
        check(s.clones == 2, "MiniMax U2: packets split into 2");
        s = TowerCatalog.get(TowerType.MINIMAX).statsForTier(3);
        check(s.p("cloneCount") == 3 && s.p("barrageInterval") == 6 && s.p("barrageCount") == 20, "MiniMax U3: 3 clones, barrage of 20 every 6 s");
        int[] cds = { 90, 90, 75, 80, 90, 100, 60 };
        for (int i = 0; i < BASE.length; i++) check(TowerCatalog.get((TowerType) BASE[i][0]).ability.cooldown == cds[i], BASE[i][0] + " ability cooldown " + cds[i]);
        check(TowerCatalog.get(TowerType.MISTRAL).ability.needsTarget, "Stratosphere Tempest needs a player-chosen bend");
    }

    private static void baseAttacksDamageEnemies() {
        for (Object[] r : BASE) {
            TowerType type = (TowerType) r[0];
            Game g = frozen();
            Tower t = towerNearTrack(g, type, 0, 400);
            Enemy e = at(g, EnemyType.QUICK_SORT, 400);
            runSeconds(g, 5);
            check(t.getDamageDealt() > 0 && e.getHp() < e.getMaxHp(), type + " base attack damages an enemy");
        }
    }

    private static void qwen() {
        Game g = frozen();
        Tower q = towerNearTrack(g, TowerType.QWEN, 1, 400);
        Enemy e = scaled(g, EnemyType.DYNAMIC_ARRAY, 400, 30);
        g.getCombat().attack(q);
        g.getCombat().updateProjectiles(0);
        check(g.getCombat().getProjectiles().size() == 3, "U1 fires 3 discs per attack");
        // U3: x3 vs max HP >= 5
        Game g2 = frozen();
        Tower q3 = towerNearTrack(g2, TowerType.QWEN, 3, 400);
        Enemy small = scaled(g2, EnemyType.STATIC_ARRAY, 400, 30), big = scaled(g2, EnemyType.MERGE_SORT, 410, 30);
        check(near(g2.getCombat().applyHit(small, q3.getStats().hit, q3, 1, 0), 4), "damage 4 vs small enemies");
        check(near(g2.getCombat().applyHit(big, q3.getStats().hit, q3, 1, 0), 12), "x3 vs enemies with base HP >= 5");
        // ability: 20 to all, strip Fortified from airships (not Palantir), Palantir loses its phase shield 10 s
        Game g3 = frozen();
        Tower q0 = towerNearTrack(g3, TowerType.QWEN, 0, 400);
        Enemy moab = spawnAt(g3, EnemyType.BELLARMINE_COLLEGE_PREP, EnumSet.of(Modifier.FORTIFIED), 0, 900);
        Boss pal = (Boss) spawnAt(g3, EnemyType.PALANTIR, EnumSet.of(Modifier.FORTIFIED), 0, 1000);
        Enemy norm = scaled(g3, EnemyType.DYNAMIC_ARRAY, 1100, 30);
        double h1 = norm.getHp(), hm = moab.getHp(), hp = pal.getHp();
        q0.resetAbilityCooldown();
        check(g3.activateAbility(q0), "Cosmic Sovereign Burst activates");
        check(near(h1 - norm.getHp(), 20), "20 damage to all enemies");
        check(!moab.isFortified(), "Fortified removed from airships");
        check(pal.isFortified(), "...except Palantir");
        check(pal.isPhaseShieldDown() && !pal.isControlImmune() && hp - pal.getHp() > 0, "Palantir loses its phase shield and takes the wave");
        runSeconds(g3, 10.5);
        check(pal.isControlImmune(), "Palantir's shield returns after 10 s");
    }

    private static void llama() {
        Game g = frozen();
        Tower l = towerNearTrack(g, TowerType.LLAMA, 0, 450);
        check(!g.getCombat().attack(l), "no charge without an enemy in range");
        Enemy e = scaled(g, EnemyType.HEAP_SORT, 450, 5);
        check(g.getCombat().attack(l), "a llama charges when an enemy is in range");
        check(g.getEffects().getWalkers().size() == 1, "one llama at base");
        check(g.getEffects().getWalkers().get(0).route == e.getRoute(), "the llama runs on the enemy's route");
        runSeconds(g, 2.5);
        check(e.getHp() < e.getMaxHp(), "the llama's contact damages the enemy");
        check(l.getDamageDealt() >= 2, "...credited to the Llama tower");
        // U1: 3 creatures per cycle; U2: camo detection + pierce 12
        Game g2 = frozen();
        Tower l1 = towerNearTrack(g2, TowerType.LLAMA, 1, 450);
        scaled(g2, EnemyType.HEAP_SORT, 450, 5);
        g2.getCombat().attack(l1);
        check(g2.getEffects().getWalkers().size() == 3, "U1 sends 3 charging creatures");
        Game g3 = frozen();
        Tower l2 = towerNearTrack(g3, TowerType.LLAMA, 2, 450);
        Enemy camo = spawnAt(g3, EnemyType.HEAP_SORT, EnumSet.of(Modifier.CAMO), 0, 450);
        check(l2.canSeeCamo() && g3.getCombat().attack(l2), "U2 sees camo and charges at it");
        check(g3.getEffects().getWalkers().get(0).pierce == 12 && g3.getEffects().getWalkers().get(0).detection, "U2 spectral creatures: pierce 12 and camo detection");
        // pierce 6 at base: only 6 enemies per llama
        Game g4 = frozen();
        Tower lb = towerNearTrack(g4, TowerType.LLAMA, 0, 450);
        Enemy[] grp = new Enemy[8];
        for (int i = 0; i < 8; i++) grp[i] = spawnAt(g4, EnemyType.HEAP_SORT, null, 0, 400 + i * 12);
        g4.getCombat().attack(lb);
        g4.run(3 * 60);
        int hit = 0; for (Enemy x : grp) if (x.getHp() < x.getMaxHp() || !x.isAlive()) hit++;
        check(hit >= 6 && hit <= 7, "base llama pierce 6 (heap shield aside): " + hit);
    }

    private static void mistral() {
        Game g = frozen();
        Tower m = towerNearTrack(g, TowerType.MISTRAL, 0, 400);
        Enemy fast = scaled(g, EnemyType.SINGLY_LINKED_LIST, 500, 20), shell = scaled(g, EnemyType.SHELL_SORT, 520, 20), slow = scaled(g, EnemyType.MERGE_SORT, 540, 20);
        Enemy boss = at(g, EnemyType.BELLARMINE_COLLEGE_PREP, 560);
        HitSpec h = m.getStats().hit;
        g.getCombat().applyHit(fast, h, m, 1, 0);
        g.getCombat().applyHit(shell, h, m, 1, 0);
        g.getCombat().applyHit(slow, h, m, 1, 0);
        g.getCombat().applyHit(boss, h, m, 1, 0);
        check(fast.getDistance() == 485, "Linked Lists are knocked back 15 px");
        check(shell.getDistance() == 505, "any enemy at 2.0x speed or more is knocked back 15 px");
        check(slow.getDistance() == 540, "slow enemies are not knocked back");
        check(boss.getDistance() == 560, "bosses are immune to knockback");
    }

    private static void cohere() {
        Game g = frozen();
        Tower c = towerNearTrack(g, TowerType.COHERE, 0, 400);
        Enemy a = scaled(g, EnemyType.DYNAMIC_ARRAY, 400, 20), b = scaled(g, EnemyType.DYNAMIC_ARRAY, 430, 20), far = scaled(g, EnemyType.DYNAMIC_ARRAY, 900, 20);
        g.getCombat().attack(c);
        for (int i = 0; i < 80; i++) g.getCombat().updateProjectiles(Game.DT);
        check(a.getHp() < a.getMaxHp() && b.getHp() < b.getMaxHp() && far.getHp() == far.getMaxHp(), "heavy shell splash hits the group but not far enemies");
        Enemy camo = spawnAt(g, EnemyType.QUICK_SORT, EnumSet.of(Modifier.CAMO), 0, 410);
        check(g.getCombat().applyHit(camo, c.getStats().hit, c, 1, 0) == 0, "Cohere (no detection) cannot pop Camo");
        Game g2 = frozen();
        Tower c2 = towerNearTrack(g2, TowerType.COHERE, 2, 400);
        Enemy e = scaled(g2, EnemyType.HEAP_SORT, 400, 20);
        g2.getCombat().applyHit(e, c2.getStats().hit, c2, 1, 0);
        check(e.isArmorCracked() && near(e.getMarkedBonus(), 0.5), "U2 hits apply ARMOR CRACK and +50% damage taken");
        e.tickStatuses(5.1);
        check(!e.isArmorCracked() && e.getMarkedBonus() == 0, "...for 5 s");
        Tower c3 = forcePlace(g2, TowerType.COHERE, buildableNear(g2, new Vec(1100, 840), 100), 3);
        Enemy boss = at(g2, EnemyType.BELLARMINE_COLLEGE_PREP, 700);
        check(near(g2.getCombat().applyHit(boss, c3.getStats().hit, c3, 1, 0), 10 * 3 * 0.75), "U3: damage 10, x3 vs MOAB-class");
    }

    private static void glm() {
        Game g = frozen();
        Tower t = towerNearTrack(g, TowerType.GLM, 0, 400);
        Enemy e = scaled(g, EnemyType.HEAP_SORT, 400, 20);
        g.getCombat().applyHit(e, t.getStats().hit, t.getType() == TowerType.GLM ? t : null, 1, 0);
        check(near(e.getSlowFraction(), 0.10), "GRAVITY pebble slows 10% on hit");
        // ability: stun all non-boss enemies 5 s, bosses 1.5 s
        Game g2 = frozen();
        Tower t2 = towerNearTrack(g2, TowerType.GLM, 0, 400);
        Enemy n = at(g2, EnemyType.QUICK_SORT, 900), boss = at(g2, EnemyType.BELLARMINE_COLLEGE_PREP, 1000);
        t2.resetAbilityCooldown();
        check(g2.activateAbility(t2), "Matrix Overload activates");
        check(n.isStunned() && boss.isStunned(), "all enemies on the track are stunned");
        n.tickStatuses(4.9);
        boss.tickStatuses(1.4);
        check(n.isStunned() && boss.isStunned(), "...still stunned just before the limits");
        n.tickStatuses(0.2);
        boss.tickStatuses(0.2);
        check(!n.isStunned() && !boss.isStunned(), "non-bosses stunned 5 s, bosses 1.5 s");
    }

    private static void kimi() {
        Game g = frozen();
        Tower k = towerNearTrack(g, TowerType.KIMI, 0, 400);
        Enemy e = scaled(g, EnemyType.BELLARMINE_COLLEGE_PREP, 400, 100);
        double[] expected = { 1, 2, 3, 4, 4, 4 };
        for (int i = 0; i < expected.length; i++) {
            double hp = e.getHp();
            shoot(g, k);
            double d = (hp - e.getHp()) / 0.75;
            check(near(d, expected[i]), "consecutive hit " + (i + 1) + " deals " + expected[i] + " (max +3), got " + d);
        }
        // memory (U1): +1 damage per 100 pops
        Tower k1 = forcePlace(g, TowerType.KIMI, buildableNear(g, new Vec(1100, 840), 100), 1);
        check(KimiBehavior.memoryStacks(k1) == 0, "no memory at start");
        for (int i = 0; i < 250; i++) k1.addKill();
        check(KimiBehavior.memoryStacks(k1) == 2, "250 pops = +2 permanent damage");
        Game g2 = frozen();
        Tower k2 = towerNearTrack(g2, TowerType.KIMI, 1, 400);
        for (int i = 0; i < 300; i++) k2.addKill();
        Enemy boss = scaled(g2, EnemyType.BELLARMINE_COLLEGE_PREP, 400, 100);
        double hp = boss.getHp();
        shoot(g2, k2);
        check(near((hp - boss.getHp()) / 0.75, 1 + 3), "memory +3 adds to the first shot: 1 + 3");
        // x0.5 vs Fortified
        Enemy fort = spawnAt(g2, EnemyType.QUICK_SORT, EnumSet.of(Modifier.FORTIFIED), 0, 380);
        check(near(g2.getCombat().applyHit(fort, new HitSpec(4, DamageType.MAGIC), null, 1, 0), 4), "(control: neutral hit)");
        HitSpec kimiHit = k2.getStats().hit;
        Enemy fort2 = scaled(g2, EnemyType.QUICK_SORT, 390, 20);
        g2.getCombat().applyHit(fort2, new HitSpec(1, DamageType.TRUE), null, 1, 0);
        Enemy fortified = g2.getEnemyManager().spawnScaled(EnemyType.QUICK_SORT, EnumSet.of(Modifier.FORTIFIED), 0, 395, 20, 1);
        g2.getEnemyManager().update(0);
        check(near(g2.getCombat().applyHit(fortified, kimiHit, k2, 1, 0), 0.5), "all Kimi damage is x0.5 vs Fortified");
        // U2: +1 pierce per stack
        Game g3 = frozen();
        Tower k3 = towerNearTrack(g3, TowerType.KIMI, 2, 400);
        Enemy tgt = scaled(g3, EnemyType.BELLARMINE_COLLEGE_PREP, 400, 100);
        shoot(g3, k3); shoot(g3, k3); shoot(g3, k3);
        g3.getCombat().attack(k3);
        g3.getCombat().updateProjectiles(0);
        List<Projectile> ps = g3.getCombat().getProjectiles();
        check(!ps.isEmpty() && ps.get(ps.size() - 1).getShot().pierce == k3.getStats().pierce + 3, "U2: +1 pierce per stack (3 stacks)");
    }

    private static void minimax() {
        Game g = frozen();
        Tower m = towerNearTrack(g, TowerType.MINIMAX, 0, 400);
        Enemy a = scaled(g, EnemyType.HEAP_SORT, 400, 20), b = scaled(g, EnemyType.HEAP_SORT, 480, 20), c = scaled(g, EnemyType.HEAP_SORT, 560, 20);
        g.getCombat().attack(m);
        double speed0 = m.getStats().projectileSpeed;
        Projectile p = null;
        for (int i = 0; i < 200 && (p == null || p.getBouncesLeft() == m.getStats().bounces); i++) {
            g.getCombat().updateProjectiles(Game.DT);
            if (!g.getCombat().getProjectiles().isEmpty()) p = g.getCombat().getProjectiles().get(0);
        }
        check(p != null && p.getBouncesLeft() == m.getStats().bounces - 1, "the packet bounced once");
        check(p != null && Math.abs(p.getVelocity().length() - speed0 * 1.1) < 1e-6, "+10% speed per bounce: " + (p == null ? 0 : p.getVelocity().length()));
        for (int i = 0; i < 400; i++) g.getCombat().updateProjectiles(Game.DT);
        int hit = 0; for (Enemy e : new Enemy[] { a, b, c }) if (e.getHp() < e.getMaxHp()) hit++;
        check(hit == 3, "one packet bounced across all three enemies, hit " + hit);
        // U2: splits into 2 on its first hit
        Game g2 = frozen();
        Tower m2 = towerNearTrack(g2, TowerType.MINIMAX, 2, 400);
        scaled(g2, EnemyType.HEAP_SORT, 400, 20); scaled(g2, EnemyType.HEAP_SORT, 430, 20); scaled(g2, EnemyType.HEAP_SORT, 460, 20);
        g2.getCombat().attack(m2);
        int max = 0;
        for (int i = 0; i < 100; i++) { g2.getCombat().updateProjectiles(Game.DT); max = Math.max(max, g2.getCombat().getProjectiles().size()); }
        check(max >= 2, "U2 packet splits into 2 on its first hit (peak " + max + " packets)");
    }

    // ------------------------------------------------------------------ helpers for the mechanic tests
    private static final EnumSet<Modifier> CAMO = EnumSet.of(Modifier.CAMO);

    private static Vec bend(Game g) { return g.getMap().getRoutes().get(0).getWaypoints().get(1); }

    /** Spawns a big-HP enemy on the track point nearest {@code p}. */
    private static Enemy atPoint(Game g, EnemyType type, EnumSet<Modifier> mods, Vec p, double hpScale) {
        Effects.TrackPoint tp = g.getEffects().nearestTrackPoint(p);
        Enemy e = g.getEnemyManager().spawnScaled(type, mods, tp.routeIndex, tp.distance, hpScale, 1.0);
        g.getEnemyManager().update(0);
        return e;
    }

    private static Enemy camoAt(Game g, EnemyType type, double dist, double hpScale) {
        Enemy e = g.getEnemyManager().spawnScaled(type, CAMO, 0, dist, hpScale, 1.0);
        g.getEnemyManager().update(0);
        return e;
    }

    /** Steps the sim while holding the given enemies exactly where they are (Gust knockback would otherwise move them). */
    private static void runPinned(Game g, double seconds, Enemy... es) {
        double[] d = new double[es.length];
        for (int i = 0; i < es.length; i++) d[i] = es[i].getDistance();
        for (int n = (int) Math.round(seconds / Game.DT); n > 0; n--) {
            for (int i = 0; i < es.length; i++) es[i].setDistance(d[i]);
            g.step(Game.DT);
        }
    }

    private static String zoneKinds(Game g) {
        StringBuilder sb = new StringBuilder();
        for (TrackZone z : g.getEffects().getZones()) sb.append(z.kind).append(z.permanent ? "(perm) " : " ");
        return sb.toString();
    }

    private static int countZones(Game g, String kind) {
        int n = 0;
        for (TrackZone z : g.getEffects().getZones()) if (z.isAlive() && kind.equals(z.kind)) n++;
        return n;
    }

    // ------------------------------------------------------------------ Qwen
    private static void qwenDrones() {
        Game g1 = frozen();
        Tower q1 = towerNearTrack(g1, TowerType.QWEN, 1, 400);
        g1.run(120);
        check(g1.getEffects().dronesOf(q1, "qwen").isEmpty(), "no drones before U2");

        Game g = frozen();
        Tower q = towerNearTrack(g, TowerType.QWEN, 2, 400);
        g.run(120);
        List<Drone> ds = g.getEffects().dronesOf(q, "qwen");
        check(ds.size() == 2, "U2 keeps 2 patrol drones: " + ds.size());
        for (Drone d : ds) {
            check(d.mode == Drone.Mode.PATROL && !d.mirrorOwner, "drones patrol with their own stats");
            check(d.getPosition().dist(q.getPosition()) > q.getStats().range, "drones patrol OTHER track segments (outside the tower's range)");
            TowerStats st = d.stats;
            check(near(st.interval(), 1.2) && near(st.hit.damage, 2) && st.pierce == 8 && st.projectilesPerShot == 3
                && near(st.range, 190) && st.hit.type == DamageType.ENERGY && near(st.projectileSpeed, 500), "drone interval 1.2, same disc stats");
        }
        check(ds.get(0).getPosition().dist(ds.get(1).getPosition()) > 20, "the two drones cover different segments");
        // a drone shoots enemies near it, credited to Qwen; the tower itself is out of range
        Enemy victim = atPoint(g, EnemyType.DYNAMIC_ARRAY, null, ds.get(0).getPosition(), 300);
        check(victim.getPosition().dist(q.getPosition()) > q.getStats().range, "(control) the victim is outside the tower's own range");
        g.run(180);
        check(victim.getHp() < victim.getMaxHp() && q.getDamageDealt() > 0, "a drone's discs damage the enemy and credit Qwen");
        // upgrade: drones follow the tower's tier (damage 4 / x3), count stays 2, none leak
        g.getTowerManager().upgrade(q);
        g.run(10);
        ds = g.getEffects().dronesOf(q, "qwen");
        check(ds.size() == 2 && ds.get(0).stats.hit.damage == 4 && ds.get(0).stats.hit.multVsHighHp == 3, "U3 drones copy the new disc stats; still 2 drones");
        check(near(ds.get(0).stats.interval(), 1.2), "...at interval 1.2");
        g.sellTower(q);
        g.run(5);
        check(g.getEffects().getDrones().isEmpty(), "selling Qwen removes its drones");
    }

    private static void qwenFortifiedBoss() {
        Game g = frozen();
        Tower q = towerNearTrack(g, TowerType.QWEN, 3, 400);
        Enemy plain = at(g, EnemyType.BELLARMINE_COLLEGE_PREP, 900);
        Enemy fort = spawnAt(g, EnemyType.BELLARMINE_COLLEGE_PREP, EnumSet.of(Modifier.FORTIFIED), 0, 1000);
        double a = g.getCombat().applyHit(plain, q.getStats().hit, q, 1, 0), b = g.getCombat().applyHit(fort, q.getStats().hit, q, 1, 0);
        check(near(a, 4 * 3 * 0.75 * 0.75 / 0.75) || a > 0, "boss hit lands");
        check(near(b / a, 1.0), "x3 applies to plain and Fortified MOAB-class alike (" + a + " / " + b + ")");
        check(near(a, 4 * 3 * 0.75), "U3: 4 x3 vs MOAB-class, after 25% armour");
    }

    // ------------------------------------------------------------------ Llama
    private static void llamaShockwave() {
        Game g = frozen();
        Tower l = towerNearTrack(g, TowerType.LLAMA, 3, 400);
        Enemy near1 = scaled(g, EnemyType.DYNAMIC_ARRAY, 400, 100), near2 = scaled(g, EnemyType.DYNAMIC_ARRAY, 360, 100);
        Enemy boss = at(g, EnemyType.BELLARMINE_COLLEGE_PREP, 440);
        Enemy far = scaled(g, EnemyType.DYNAMIC_ARRAY, 900, 100);
        double mult = l.damageMultiplier(g);
        l.getBehavior().update(g, l, 3.9);
        check(near1.getHp() == near1.getMaxHp(), "no shockwave before 4 s");
        l.getBehavior().update(g, l, 0.2);
        check(near(near1.getMaxHp() - near1.getHp(), 8 * mult), "shockwave deals 8: " + (near1.getMaxHp() - near1.getHp()));
        check(near(near1.getDistance(), 360) && near(near2.getDistance(), 320), "KNOCKBACK 40 px");
        check(near(boss.getMaxHp() - boss.getHp(), 8 * 2 * mult * 0.75), "x2 vs MOAB-class (after armour): " + (boss.getMaxHp() - boss.getHp()));
        check(boss.getDistance() == 440, "bosses are not knocked back");
        check(far.getHp() == far.getMaxHp() && far.getDistance() == 900, "enemies outside the range are untouched");
        l.getBehavior().update(g, l, 4.0);
        check(near(near1.getDistance(), 320), "uncapped: a second wave pushes another 40 px immediately (past the knockback budget)");
        // not before U3
        Game g2 = frozen();
        Tower l2 = towerNearTrack(g2, TowerType.LLAMA, 2, 400);
        Enemy e = scaled(g2, EnemyType.DYNAMIC_ARRAY, 400, 100);
        l2.getBehavior().update(g2, l2, 10);
        check(e.getHp() == e.getMaxHp(), "U2 has no shockwave");
        // it runs from game time: 4 s of the sim triggers it
        Game g3 = frozen();
        Tower l3 = towerNearTrack(g3, TowerType.LLAMA, 3, 400);
        Enemy e3 = camoAt(g3, EnemyType.DYNAMIC_ARRAY, 400, 100);
        runSeconds(g3, 4.1);
        check(e3.getDistance() <= 360 + 1e-9, "the shockwave fires from the sim loop every 4 s (and hits camo: U2+ detection)");
    }

    private static void llamaHerd() {
        Game g = frozen();
        Tower l = towerNearTrack(g, TowerType.LLAMA, 0, 400);
        Enemy a = scaled(g, EnemyType.DYNAMIC_ARRAY, 900, 100), b = scaled(g, EnemyType.DYNAMIC_ARRAY, 1100, 100);
        Enemy boss = at(g, EnemyType.BELLARMINE_COLLEGE_PREP, 1000);
        Enemy camo = camoAt(g, EnemyType.DYNAMIC_ARRAY, 1050, 100);
        g.addCash(0, null);
        l.resetAbilityCooldown();
        check(g.activateAbility(l), "Open Titan Stampede activates");
        int routes = g.getMap().getRoutes().size();
        check(g.getEffects().getWalkers().size() == routes, "one herd per route (" + routes + ")");
        TrackWalker w = g.getEffects().getWalkers().get(0);
        check(w.kind.equals("herd") && w.distance == w.route.getTotalLength() && w.endDistance == 0, "the herd runs exit -> entrance");
        runSeconds(g, 30);
        check(near(a.getMaxHp() - a.getHp(), 15) && near(b.getMaxHp() - b.getHp(), 15) && near(camo.getMaxHp() - camo.getHp(), 15),
            "15 damage to every enemy it touches, once each (camo included): " + (a.getMaxHp() - a.getHp()));
        check(near(boss.getMaxHp() - boss.getHp(), 60 * 0.75), "bosses take 60 (after 25% armour): " + (boss.getMaxHp() - boss.getHp()));
        check(near(boss.getDistance(), 1000 - 50), "bosses are knocked back 50 px: " + boss.getDistance());
        check(g.getEffects().getWalkers().isEmpty(), "the herd expires at the entrance");
        check(near(l.getAbilityCooldownRemaining(), 90) || l.getAbilityCooldownRemaining() < 90 && l.getAbilityCooldownRemaining() > 55, "cooldown spent");
    }

    private static void llamaCamo() {
        // base Llama (no detection): its creatures do not touch camo enemies; U2 does
        Game g = frozen();
        Tower l = towerNearTrack(g, TowerType.LLAMA, 0, 450);
        Enemy seen = scaled(g, EnemyType.DYNAMIC_ARRAY, 440, 50), camo = camoAt(g, EnemyType.DYNAMIC_ARRAY, 460, 50);
        g.getCombat().attack(l);
        runSeconds(g, 0.5);
        check(seen.getHp() < seen.getMaxHp() && camo.getHp() == camo.getMaxHp(), "base creatures cannot hurt Camo");
        Game g2 = frozen();
        Tower l2 = towerNearTrack(g2, TowerType.LLAMA, 2, 450);
        Enemy camo2 = camoAt(g2, EnemyType.DYNAMIC_ARRAY, 460, 50);
        g2.getCombat().attack(l2);
        runSeconds(g2, 0.5);
        check(camo2.getHp() < camo2.getMaxHp(), "Illusion Pack creatures hit Camo");
    }

    // ------------------------------------------------------------------ Mistral
    private static void mistralGust() {
        Game g = frozen();
        Tower m = towerNearTrack(g, TowerType.MISTRAL, 1, 400);
        Enemy fast = scaled(g, EnemyType.SINGLY_LINKED_LIST, 450, 50), other = scaled(g, EnemyType.MERGE_SORT, 360, 50);
        Enemy boss = at(g, EnemyType.BELLARMINE_COLLEGE_PREP, 420), far = scaled(g, EnemyType.DYNAMIC_ARRAY, 800, 50);
        m.getBehavior().update(g, m, 2.9);
        check(fast.getDistance() == 450, "no gust before 3 s");
        m.getBehavior().update(g, m, 0.2);
        check(near(fast.getDistance(), 390), "Gust: fast enemies back 60 px: " + fast.getDistance());
        check(near(other.getDistance(), 335), "...other enemies 25 px: " + other.getDistance());
        check(boss.getDistance() == 420, "...bosses not at all");
        check(far.getDistance() == 800, "...nothing out of range");
        // U0 has no gust
        Game g0 = frozen();
        Tower m0 = towerNearTrack(g0, TowerType.MISTRAL, 0, 400);
        Enemy e0 = scaled(g0, EnemyType.DYNAMIC_ARRAY, 400, 50);
        m0.getBehavior().update(g0, m0, 9);
        check(e0.getDistance() == 400, "base Mistral has no gust");
    }

    private static void mistralCyclone() {
        Game g = frozen();
        Tower m = towerNearTrack(g, TowerType.MISTRAL, 2, 400);
        m.getBehavior().update(g, m, 8.1);
        check(countZones(g, "tornado") == 0, "no enemy in range: no tornado, the cyclone stays ready");
        Enemy decoy = scaled(g, EnemyType.DYNAMIC_ARRAY, 400, 1000);
        Enemy victim = camoAt(g, EnemyType.DYNAMIC_ARRAY, 400, 1000);   // Mistral's own shots cannot touch it
        m.getBehavior().update(g, m, 0.01);
        check(countZones(g, "tornado") == 1, "a tornado appears on the first enemy in range");
        TrackZone z = g.getEffects().zonesOf(m, "tornado").get(0);
        check(near(z.radius, 45) && near(z.duration, 5) && near(z.tickInterval, 0.3) && !z.permanent, "tornado 5 s, tick 0.3");
        check(z.center.dist(decoy.getPosition()) < 1e-6, "...centred on the enemy, i.e. on the track");
        double hp = victim.getHp();
        runPinned(g, 5.2, victim, decoy);
        double dealt = hp - victim.getHp();
        check(dealt >= 15 && dealt <= 17, "1 damage every 0.3 s for 5 s: " + dealt);
        check(countZones(g, "tornado") == 0 || g.getEffects().zonesOf(m, "tornado").get(0).getRemaining() > 4, "the first tornado expired after 5 s");
        // not before U2
        Game g1 = frozen();
        Tower m1 = towerNearTrack(g1, TowerType.MISTRAL, 1, 400);
        scaled(g1, EnemyType.DYNAMIC_ARRAY, 400, 1000);
        m1.getBehavior().update(g1, m1, 20);
        check(countZones(g1, "tornado") == 0, "U1 has no tornado");
    }

    private static void mistralHurricane() {
        Game g = frozen();
        Vec b = bend(g);
        Tower m = forcePlace(g, TowerType.MISTRAL, buildableNear(g, b.add(new Vec(0, -60)), 80), 3);
        check(m.getPosition().dist(b) <= m.getStats().range, "(setup) a bend is in range");
        runSeconds(g, 1);
        check(countZones(g, "vortex") == 1, "U3 creates one vortex");
        TrackZone z = g.getEffects().zonesOf(m, "vortex").get(0);
        check(z.permanent && near(z.radius, 70) && near(z.tickInterval, 0.3), "permanent, radius 70, tick 0.3");
        boolean atBend = false;
        for (Vec w : g.getMap().getRoutes().get(0).getWaypoints()) if (w.dist(z.center) < 1e-6) atBend = true;
        check(atBend, "the vortex sits on a track bend");
        Enemy e = atPoint(g, EnemyType.DYNAMIC_ARRAY, CAMO, z.center, 300);
        Enemy boss = atPoint(g, EnemyType.BELLARMINE_COLLEGE_PREP, CAMO, z.center, 1);
        double hp = e.getHp(), bhp = boss.getHp();
        runPinned(g, 3, e, boss);
        double dealt = hp - e.getHp(), bdealt = bhp - boss.getHp();
        check(dealt >= 18 && dealt <= 22, "2 damage every 0.3 s: " + dealt);
        check(near(bdealt / dealt, 2 * 0.75), "x2 vs MOAB-class (after armour): " + bdealt / dealt);
        check(near(e.getSlowFraction(), 0.30), "SLOW 30%: " + e.getSlowFraction());
        check(near(boss.getSlowFraction(), 0.10), "bosses get a third of the slow: " + boss.getSlowFraction());
        runSeconds(g, 20);
        check(countZones(g, "vortex") == 1, "the vortex is permanent and never duplicated");
        g.sellTower(m);
        runSeconds(g, 0.2);
        check(countZones(g, "vortex") == 0, "selling Mistral removes the vortex");
    }

    private static void mistralTempest() {
        Game g = frozen();
        Tower m = towerNearTrack(g, TowerType.MISTRAL, 0, 400);
        Vec b = bend(g);
        Vec click = b.add(new Vec(35, 25));
        check(m.getAbility().needsTarget, "Tempest is player-targeted");
        check(!g.activateAbility(m), "refused without a target point");
        check(!g.activateAbility(m, click), "locked at 50% cooldown");
        check(near(m.getAbilityCooldownRemaining(), 37.5), "starts at 50% cooldown");
        runSeconds(g, 37.6);
        check(m.isAbilityReady(), "ready after 37.5 s of game time");
        Enemy boss = camoAt(g, EnemyType.BELLARMINE_COLLEGE_PREP, g.getMap().getRoutes().get(0).nearestDistanceAlong(b), 1);
        Enemy norm = camoAt(g, EnemyType.DYNAMIC_ARRAY, g.getMap().getRoutes().get(0).nearestDistanceAlong(b), 300);
        double bd = boss.getDistance(), bhp = boss.getHp(), nhp = norm.getHp();
        check(g.activateAbility(m, click), "activates at the chosen point");
        check(near(m.getAbilityCooldownRemaining(), 75), "cooldown 75 s");
        check(countZones(g, "tempest") == 1, "one tempest zone");
        TrackZone z = g.getEffects().zonesOf(m, "tempest").get(0);
        check(z.center.dist(b) < 1e-6, "snapped to the bend nearest the clicked point");
        check(z.bossesOnly && near(z.duration, 15) && near(z.radius, 90) && near(z.tickInterval, 0.3), "bosses only, 15 s, radius 90, tick 0.3");
        check(near(boss.getDistance(), bd - 100), "MOAB-class pushed back 100 px: " + (bd - boss.getDistance()));
        boss.setDistance(bd);   // the airship walks back into the storm
        runPinned(g, 15.5, boss);
        double ticks = (bhp - boss.getHp()) / (10 * 0.75);
        check(ticks >= 48 && ticks <= 51, "10 damage every 0.3 s to airships (" + ticks + " ticks)");
        check(near(boss.getDistance(), bd), "...pushed once per cast, not again on every tick");
        check(norm.getHp() == nhp && norm.getDistance() == bd, "ordinary enemies are not affected");
        check(countZones(g, "tempest") == 0, "the tempest ends after 15 s");
        // a click far from any bend snaps to the nearest track point
        m.resetAbilityCooldown();
        check(g.activateAbility(m, new Vec(10, 10)), "a click anywhere is accepted and snapped");
        check(g.getEffects().nearestTrackPoint(g.getEffects().zonesOf(m, "tempest").get(0).center).position.dist(g.getEffects().zonesOf(m, "tempest").get(0).center) < 1e-6, "...onto the track");
    }

    // ------------------------------------------------------------------ Cohere
    private static void cohereFragmentation() {
        Game g = frozen();
        Tower c = towerNearTrack(g, TowerType.COHERE, 1, 400);
        Enemy target = scaled(g, EnemyType.DYNAMIC_ARRAY, 400, 300);
        List<Enemy> ring = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            ring.add(scaled(g, EnemyType.DYNAMIC_ARRAY, 400 + i * 15, 300));
            ring.add(scaled(g, EnemyType.DYNAMIC_ARRAY, 400 - i * 15, 300));
        }
        g.getEvents().clear();
        g.getCombat().attack(c);
        for (int i = 0; i < 90; i++) g.getCombat().updateProjectiles(Game.DT);
        int beams = 0;
        for (VisualEvent ev : g.getEvents()) if (ev.kind == VisualEvent.Kind.BEAM) beams++;
        check(beams == 8, "one burst of 8 shrapnel per shell, however many enemies the blast hits: " + beams);
        double shell = 3 * 0.75;
        int shrapnelHits = 0;
        for (Enemy e : ring) if (e.getMaxHp() - e.getHp() > shell + 1e-6) shrapnelHits++;
        check(shrapnelHits > 0, "shrapnel damages enemies around the impact beyond the shell: " + shrapnelHits);
        double maxDmg = 0;
        for (Enemy e : ring) maxDmg = Math.max(maxDmg, e.getMaxHp() - e.getHp());
        check(maxDmg >= shell + 0.75 - 1e-6, "shrapnel damage 1 on top of the shell: " + maxDmg);
        check(target.getHp() < target.getMaxHp(), "(the struck enemy is hit)");
        // no shrapnel before U1
        Game g0 = frozen();
        Tower c0 = towerNearTrack(g0, TowerType.COHERE, 0, 400);
        scaled(g0, EnemyType.DYNAMIC_ARRAY, 400, 300);
        g0.getEvents().clear();
        g0.getCombat().attack(c0);
        for (int i = 0; i < 90; i++) g0.getCombat().updateProjectiles(Game.DT);
        int b0 = 0;
        for (VisualEvent ev : g0.getEvents()) if (ev.kind == VisualEvent.Kind.BEAM) b0++;
        check(b0 == 0, "base Cohere has no shrapnel");
        check(c.getStats().p("shrapnelPierce") == 2 && c.getStats().p("shrapnelDamage") == 1, "shrapnel: damage 1, pierce 2");
    }

    private static void cohereOverride() {
        Game g = frozen();
        Tower c = towerNearTrack(g, TowerType.COHERE, 0, 400);
        Enemy a = camoAt(g, EnemyType.DYNAMIC_ARRAY, 400, 500), b = camoAt(g, EnemyType.DYNAMIC_ARRAY, 402, 500);
        Enemy moab = g.getEnemyManager().spawnScaled(EnemyType.BELLARMINE_COLLEGE_PREP, CAMO, 0, 404, 1.0, 1.0);
        g.getEnemyManager().update(0);
        Enemy d = scaled(g, EnemyType.DYNAMIC_ARRAY, 520, 500);
        Enemy bfb = spawnAt(g, EnemyType.SW_RESOURCE_CONSULTANT, null, 0, 395);
        Enemy zomg = spawnAt(g, EnemyType.THREE_COM, null, 0, 406);
        Enemy ddt = spawnAt(g, EnemyType.LOCKHEED_MARTIN, null, 0, 408);
        Enemy bad = spawnAt(g, EnemyType.PALANTIR, null, 0, 410);
        c.resetAbilityCooldown();
        check(g.activateAbility(c), "Command Override activates");
        check(a.isReversed() && b.isReversed() && moab.isReversed(), "the 3 nearest non-boss enemies reverse (Bellarmine College Prep allowed)");
        check(!d.isReversed(), "...only 3 of them");
        check(!bfb.isReversed() && !zomg.isReversed() && !ddt.isReversed() && !bad.isReversed(), "BFB, ZOMG, DDT and BAD are excluded");
        check(near(a.getReverseDps(), 10), "10 damage/s on contact");
        double hpA = a.getHp(), hpD = d.getHp();
        runSeconds(g, 1.0);
        double lost = hpA - a.getHp();
        check(lost >= 19 && lost <= 21, "each reversed enemy takes 10 dmg/s from the two others it touches: " + lost);
        check(d.getHp() == hpD || d.getHp() < hpD, "(control) the far enemy is not reversed");
        runSeconds(g, 7.2);
        check(!a.isReversed() && !b.isReversed() && !moab.isReversed(), "reversal ends after 8 s");
        // with nothing to reverse the ability is refused (no cooldown spent)
        Game g2 = frozen();
        Tower c2 = towerNearTrack(g2, TowerType.COHERE, 0, 400);
        spawnAt(g2, EnemyType.SW_RESOURCE_CONSULTANT, null, 0, 900);
        c2.resetAbilityCooldown();
        check(!g2.activateAbility(c2) && c2.isAbilityReady(), "no eligible enemy: refused, cooldown kept");
        // reversed enemies really walk backwards
        Game g3 = newGame(0);
        Tower c3 = towerNearTrack(g3, TowerType.COHERE, 0, 400);
        Enemy w = spawnAt(g3, EnemyType.DYNAMIC_ARRAY, CAMO, 0, 800);
        c3.resetAbilityCooldown();
        g3.activateAbility(c3);
        runSeconds(g3, 2);
        check(w.getDistance() < 800, "a reversed enemy moves toward the entrance: " + w.getDistance());
    }

    // ------------------------------------------------------------------ GLM
    private static void glmNetwork() {
        Game g = frozen();
        Tower t = towerNearTrack(g, TowerType.GLM, 0, 400);
        runSeconds(g, 1);
        check(g.getEffects().getHazards().isEmpty(), "no nodes at base");
        g.getTowerManager().upgrade(t);
        check(g.getEffects().getHazards().size() == 1, "U1 places a network");
        NodeHazard h = g.getEffects().getHazards().get(0);
        check(h.nodes.size() == 2 && h.links.size() == 1 && h.region == null, "2 nodes joined by one laser line");
        for (Vec n : h.nodes) {
            check(n.dist(t.getPosition()) <= t.getStats().range + 40, "nodes are placed around the track within the tower's range");
            check(Math.abs(g.getMap().getRoutes().get(0).distanceToPoint(n) - 30) < 8 || g.getEffects().nearestTrackPoint(n).position.dist(n) < 45, "...on either side of the track");
        }
        Vec mid = Vec.lerp(h.nodes.get(0), h.nodes.get(1), 0.5);
        Enemy e = atPoint(g, EnemyType.DYNAMIC_ARRAY, CAMO, mid, 300);
        double hp = e.getHp();
        runSeconds(g, 0.25);
        check(near(hp - e.getHp(), 2), "enemy crossing the line takes 2: " + (hp - e.getHp()));
        runSeconds(g, 0.75);
        double dealt = hp - e.getHp();
        check(dealt >= 6 && dealt <= 10, "...at most once every 0.3 s (about 4 hits in 1 s): " + dealt);
        // upgrade replaces the network (no leaks)
        g.getTowerManager().upgrade(t);
        check(countAlive(g.getEffects().getHazards()) == 1, "U2 replaces the U1 network");
        NodeHazard h2 = g.getEffects().getHazards().get(g.getEffects().getHazards().size() - 1);
        check(h2.nodes.size() == 4 && h2.region != null && near(h2.regionSlow, 0.30), "U2: 4 nodes forming a slowing grid");
        g.getTowerManager().upgrade(t);
        g.run(1);
        check(countAlive(g.getEffects().getHazards()) == 1, "U3 replaces the U2 network");
        NodeHazard h3 = g.getEffects().getHazards().get(g.getEffects().getHazards().size() - 1);
        check(h3.nodes.size() == 6 && near(h3.lineHit.damage, 5) && near(h3.lineHit.multVsFortified, 2) && near(h3.regionSlow, 0.30), "U3: 6 nodes, line damage 5, x2 vs Fortified, keeps the slow");
        check(h3.links.size() > h3.nodes.size(), "U3 grid has interior links as well as the perimeter");
        g.sellTower(t);
        g.run(5);
        check(countAlive(g.getEffects().getHazards()) == 0, "selling GLM removes its nodes");
    }

    private static int countAlive(List<NodeHazard> l) {
        int n = 0;
        for (NodeHazard h : l) if (h.isAlive()) n++;
        return n;
    }

    private static void glmGrid() {
        Game g = frozen();
        Tower t = towerNearTrack(g, TowerType.GLM, 3, 400);
        g.run(30);
        NodeHazard h = g.getEffects().getHazards().get(0);
        int[] rung = null;
        for (int[] l : h.links) if (l[0] == 1 && l[1] == 4) rung = l;
        check(rung != null, "(setup) interior rung present");
        Vec centre = Vec.lerp(h.nodes.get(1), h.nodes.get(4), 0.5);
        Enemy plain = atPoint(g, EnemyType.DYNAMIC_ARRAY, CAMO, centre, 300);
        Enemy fort = atPoint(g, EnemyType.DYNAMIC_ARRAY, EnumSet.of(Modifier.CAMO, Modifier.FORTIFIED), centre, 300);
        Enemy boss = atPoint(g, EnemyType.BELLARMINE_COLLEGE_PREP, CAMO, centre, 1);
        check(NodeHazard.polygonContains(h.region, centre) || plain.getPosition().dist(centre) < 40, "(setup) the web covers the track stretch");
        double p0 = plain.getHp(), f0 = fort.getHp();
        runSeconds(g, 0.05);
        check(near(p0 - plain.getHp(), 5) || p0 - plain.getHp() >= 5, "U3 line damage 5: " + (p0 - plain.getHp()));
        check(near((f0 - fort.getHp()) / (p0 - plain.getHp()), 2), "x2 vs Fortified: " + (f0 - fort.getHp()));
        check(near(plain.getSlowFraction(), 0.30), "enemies inside the web are SLOWED 30%: " + plain.getSlowFraction());
        check(near(boss.getSlowFraction(), 0.10), "bosses get 1/3 of the slow: " + boss.getSlowFraction());
        // enemies away from the web are neither slowed nor hurt
        Enemy away = camoAt(g, EnemyType.DYNAMIC_ARRAY, 1000, 300);
        runSeconds(g, 1);
        check(away.getHp() == away.getMaxHp() && away.getSlowFraction() == 0, "outside the web nothing happens");
    }

    // ------------------------------------------------------------------ Kimi
    private static void kimiBurst() {
        Game g = frozen();
        Tower k = towerNearTrack(g, TowerType.KIMI, 3, 400);
        Enemy target = scaled(g, EnemyType.BELLARMINE_COLLEGE_PREP, 400, 1000);
        Enemy n = camoAt(g, EnemyType.DYNAMIC_ARRAY, 430, 1000);
        Enemy f = g.getEnemyManager().spawnScaled(EnemyType.DYNAMIC_ARRAY, EnumSet.of(Modifier.CAMO, Modifier.FORTIFIED), 0, 410, 1000, 1.0);
        g.getEnemyManager().update(0);
        for (int i = 0; i < 9; i++) shoot(g, k);
        check(n.getHp() == n.getMaxHp(), "(control) shards cannot touch Camo, so only the burst can: " + (n.getMaxHp() - n.getHp()));
        shoot(g, k);
        double mult = k.damageMultiplier(g);
        check(near(n.getMaxHp() - n.getHp(), 15 * mult), "the 10th attack fires a crystal burst, 15 damage in a 70 px splash: " + (n.getMaxHp() - n.getHp()));
        check(near(f.getMaxHp() - f.getHp(), 15 * 0.5 * mult), "...x0.5 vs Fortified: " + (f.getMaxHp() - f.getHp()));
        double after = n.getHp();
        for (int i = 0; i < 9; i++) shoot(g, k);
        check(n.getHp() == after, "no burst on attacks 11-19");
        shoot(g, k);
        check(near(after - n.getHp(), 15 * mult), "...and again on the 20th");
        Enemy outside = camoAt(g, EnemyType.DYNAMIC_ARRAY, 400 + 100, 1000);
        for (int i = 0; i < 10; i++) shoot(g, k);
        check(outside.getHp() == outside.getMaxHp(), "burst splash is 70 px");
        // not before U3
        Game g2 = frozen();
        Tower k2 = towerNearTrack(g2, TowerType.KIMI, 2, 400);
        scaled(g2, EnemyType.BELLARMINE_COLLEGE_PREP, 400, 1000);
        Enemy n2 = camoAt(g2, EnemyType.DYNAMIC_ARRAY, 420, 1000);
        for (int i = 0; i < 12; i++) shoot(g2, k2);
        check(n2.getHp() == n2.getMaxHp(), "U2 has no crystal burst");
    }

    private static void kimiAbility() {
        Game g = frozen();
        Tower k = towerNearTrack(g, TowerType.KIMI, 1, 400);
        for (int i = 0; i < 300; i++) k.addKill();
        Enemy a = scaled(g, EnemyType.DYNAMIC_ARRAY, 900, 1000), b = scaled(g, EnemyType.QUICK_SORT, 1000, 1000);
        Enemy boss = at(g, EnemyType.BELLARMINE_COLLEGE_PREP, 1100);
        Enemy fort = g.getEnemyManager().spawnScaled(EnemyType.DYNAMIC_ARRAY, EnumSet.of(Modifier.FORTIFIED), 0, 950, 1000, 1.0);
        Enemy fortBoss = spawnAt(g, EnemyType.THREE_COM, EnumSet.of(Modifier.FORTIFIED), 0, 1050);
        g.getEnemyManager().update(0);
        k.resetAbilityCooldown();
        double ha = a.getHp(), hb = b.getHp(), hboss = boss.getHp(), hf = fort.getHp(), hfb = fortBoss.getHp();
        check(g.activateAbility(k), "Infinite Context Burst activates");
        check(near(ha - a.getHp(), 15) && near(hb - b.getHp(), 15), "memory stacks (3) x 5 = 15 to every non-Fortified enemy: " + (ha - a.getHp()));
        check(near(hboss - boss.getHp(), 7.5), "bosses take half: " + (hboss - boss.getHp()));
        check(fort.getHp() == hf && fortBoss.getHp() == hfb, "Fortified enemies (and Fortified bosses) are immune");
        check(near(k.getAbilityCooldownRemaining(), 100), "cooldown 100 s");
        // cap: U1 +5, U3 +12
        Game g2 = frozen();
        Tower k1 = towerNearTrack(g2, TowerType.KIMI, 1, 400);
        for (int i = 0; i < 5000; i++) k1.addKill();
        check(KimiBehavior.memoryStacks(k1) == 5, "U1 memory capped at +5");
        Enemy e = scaled(g2, EnemyType.DYNAMIC_ARRAY, 900, 1000);
        k1.resetAbilityCooldown();
        double h = e.getHp();
        g2.activateAbility(k1);
        check(near(h - e.getHp(), 25), "damage = stacks x 5 = 25");
        Game g3 = frozen();
        Tower k3 = towerNearTrack(g3, TowerType.KIMI, 3, 400);
        for (int i = 0; i < 5000; i++) k3.addKill();
        check(KimiBehavior.memoryStacks(k3) == 12, "U3 memory capped at +12");
        Enemy e3 = scaled(g3, EnemyType.DYNAMIC_ARRAY, 900, 1000);
        k3.resetAbilityCooldown();
        h = e3.getHp();
        g3.activateAbility(k3);
        check(near(h - e3.getHp(), 60), "12 stacks x 5 = 60");
    }

    private static void kimiMemory() {
        Game g = frozen();
        Tower k = towerNearTrack(g, TowerType.KIMI, 1, 400);
        Enemy e = scaled(g, EnemyType.DYNAMIC_ARRAY, 400, 1);
        g.getCombat().applyHit(e, new HitSpec(1e9, DamageType.TRUE), k, 1, 0);
        g.getEnemyManager().update(0);
        check(k.getKills() == 1, "a pop credited to Kimi is counted as memory: " + k.getKills());
        for (int i = 0; i < 199; i++) k.addKill();
        check(KimiBehavior.memoryStacks(k) == 2, "200 pops = +2");
        // memory damage is also x0.5 vs Fortified
        Enemy fort = g.getEnemyManager().spawnScaled(EnemyType.DYNAMIC_ARRAY, EnumSet.of(Modifier.FORTIFIED), 0, 380, 300, 1.0);
        g.getEnemyManager().update(0);
        double dealt = g.getCombat().applyHit(fort, k.getStats().hit, k, 1, 2);
        check(near(dealt, (1 + 2) * 0.5), "all Kimi damage (incl. memory) is x0.5 vs Fortified: " + dealt);
        // base Kimi has no memory
        Game g0 = frozen();
        Tower k0 = towerNearTrack(g0, TowerType.KIMI, 0, 400);
        for (int i = 0; i < 1000; i++) k0.addKill();
        check(KimiBehavior.memoryStacks(k0) == 0, "no memory before U1");
    }

    // ------------------------------------------------------------------ MiniMax
    private static void minimaxClones() {
        for (int tier : new int[] { 0, 1, 2, 3 }) {
            Game g = frozen();
            Tower m = towerNearTrack(g, TowerType.MINIMAX, tier, 400);
            scaled(g, EnemyType.HEAP_SORT, 400, 100000);
            int peak = 0;
            for (int i = 0; i < 4; i++) { g.step(Game.DT); peak = Math.max(peak, g.getCombat().getProjectiles().size()); }
            int clones = g.getEffects().dronesOf(m, "clone").size();
            int expected = tier == 0 ? 0 : tier < 3 ? 1 : 3;
            check(clones == expected, "tier " + tier + " has " + expected + " clones: " + clones);
            check(peak == 1 + expected, "tier " + tier + ": tower + clones fire together (" + peak + " packets)");
            for (Drone d : g.getEffects().dronesOf(m, "clone")) {
                check(d.mode == Drone.Mode.ORBIT && d.stats.bounces == m.getStats().bounces && d.stats.clones == m.getStats().clones, "clone copies the base attack (bounces / splitting)");
                check(near(d.stats.hit.damage, 1), "clone damage 1");
            }
        }
        Game g = frozen();
        Tower m = towerNearTrack(g, TowerType.MINIMAX, 1, 400);
        g.run(5);
        check(g.getEffects().getDrones().size() == 1, "one clone only");
        g.getTowerManager().upgrade(m);
        g.getTowerManager().upgrade(m);
        g.run(5);
        check(g.getEffects().dronesOf(m, "clone").size() == 3 && g.getEffects().getDrones().size() == 3, "U3: 3 clones, none leaked");
        g.sellTower(m);
        g.run(3);
        check(g.getEffects().getDrones().isEmpty(), "selling removes the clones");
    }

    private static void minimaxBarrage() {
        Game g = frozen();
        Tower m = towerNearTrack(g, TowerType.MINIMAX, 3, 400);
        m.getBehavior().update(g, m, 6.0);
        g.getCombat().updateProjectiles(0);
        check(g.getCombat().getProjectiles().isEmpty(), "no barrage without a target; it waits for one");
        scaled(g, EnemyType.HEAP_SORT, 400, 100000);
        m.getBehavior().update(g, m, 0.01);
        g.getCombat().updateProjectiles(0);
        check(g.getCombat().getProjectiles().size() == 20, "a barrage of 20 packets: " + g.getCombat().getProjectiles().size());
        check(g.getCombat().getProjectiles().get(0).getBouncesLeft() == 6, "...base packets (6 bounces)");
        for (int i = 0; i < 600; i++) g.getCombat().updateProjectiles(Game.DT);
        check(g.getCombat().getProjectiles().isEmpty(), "all packets expire");
        m.getBehavior().update(g, m, 5.9);
        g.getCombat().updateProjectiles(0);
        check(g.getCombat().getProjectiles().isEmpty(), "next barrage 6 s later, not before");
        m.getBehavior().update(g, m, 0.2);
        g.getCombat().updateProjectiles(0);
        check(g.getCombat().getProjectiles().size() == 20, "...and it fires every 6 s");
        // spread over several targets
        Game g2 = frozen();
        Tower m2 = towerNearTrack(g2, TowerType.MINIMAX, 3, 400);
        Enemy a = scaled(g2, EnemyType.HEAP_SORT, 380, 100000), b = scaled(g2, EnemyType.HEAP_SORT, 420, 100000);
        m2.getBehavior().update(g2, m2, 6.0);
        g2.getCombat().updateProjectiles(0);
        int toA = 0, toB = 0;
        for (Projectile p : g2.getCombat().getProjectiles()) { if (p.getTarget() == a) toA++; if (p.getTarget() == b) toB++; }
        check(toA == 10 && toB == 10, "20 packets spread over the targets in range: " + toA + "/" + toB);
        // not before U3
        Game g3 = frozen();
        Tower m3 = towerNearTrack(g3, TowerType.MINIMAX, 2, 400);
        scaled(g3, EnemyType.HEAP_SORT, 400, 100000);
        m3.getBehavior().update(g3, m3, 20);
        g3.getCombat().updateProjectiles(0);
        check(g3.getCombat().getProjectiles().isEmpty(), "U2 has no barrage");
    }

    private static void minimaxOverclock() {
        Game g = frozen();
        Tower m = towerNearTrack(g, TowerType.MINIMAX, 1, 400);
        scaled(g, EnemyType.HEAP_SORT, 400, 100000);
        check(near(m.getAbilityCooldownRemaining(), 30), "starts at 50% cooldown");
        check(!g.activateAbility(m), "locked at start");
        runSeconds(g, 30.1);
        double rate = m.effectiveRate(g);
        check(g.activateAbility(m), "Scale Factor Overclock activates");
        check(near(m.getAbilityCooldownRemaining(), 60), "cooldown 60 s");
        check(near(m.effectiveRate(g) / rate, 3), "fire rate x3: " + m.effectiveRate(g) / rate);
        g.run(2);
        for (Drone d : g.getEffects().dronesOf(m, "clone")) {
            check(near(d.stats.rate / m.getStats().rate, 3) && d.stats.bounces == 18, "clones also fire x3 faster with x3 bounces (" + d.stats.bounces + ")");
        }
        g.getCombat().attack(m);
        g.getCombat().updateProjectiles(0);
        int maxBounces = 0;
        for (Projectile p : g.getCombat().getProjectiles()) maxBounces = Math.max(maxBounces, p.getBouncesLeft());
        check(maxBounces == 18, "bounce count x3 (6 -> 18): " + maxBounces);
        runSeconds(g, 10.2);
        check(near(m.effectiveRate(g) / rate, 1), "overclock ends after 10 s");
        for (Drone d : g.getEffects().dronesOf(m, "clone")) check(d.stats.bounces == 6 && near(d.stats.rate, m.getStats().rate), "clones return to normal");
        g.getCombat().attack(m);
        g.getCombat().updateProjectiles(0);
        maxBounces = 0;
        for (Projectile p : g.getCombat().getProjectiles()) maxBounces = Math.max(maxBounces, p.getBouncesLeft());
        check(maxBounces == 6, "bounces back to 6: " + maxBounces);
    }

    // ------------------------------------------------------------------ ability cooldown rules (spec 3.0)
    private static void abilityCooldown(TowerType type) {
        Game g = frozen();
        Tower t = towerNearTrack(g, type, 0, 400);
        scaled(g, EnemyType.DYNAMIC_ARRAY, 900, 1000);
        double cd = t.getAbility().cooldown;
        Vec pt = bend(g);
        check(near(t.getAbilityCooldownRemaining(), cd * 0.5), type + " starts at 50% cooldown: " + t.getAbilityCooldownRemaining());
        boolean early = t.getAbility().needsTarget ? g.activateAbility(t, pt) : g.activateAbility(t);
        check(!early, type + " cannot be used at 50%");
        g.setSpeed(2);
        double realSeconds = 0;
        while (g.getTime() < cd / 2 - 1 && realSeconds < cd) { g.advance(0.2); realSeconds += 0.2; }
        check(!t.isAbilityReady(), type + " not ready one second early");
        while (!t.isAbilityReady() && realSeconds < cd) { g.advance(0.2); realSeconds += 0.2; }
        check(t.isAbilityReady() && realSeconds < cd / 4 + 1, type + " cooldown runs on game time (2x speed): " + realSeconds + " real s for " + g.getTime() + " game s");
        boolean ok = t.getAbility().needsTarget ? g.activateAbility(t, pt) : g.activateAbility(t);
        check(ok && near(t.getAbilityCooldownRemaining(), cd), type + " fires and restarts the full cooldown");
        check(!(t.getAbility().needsTarget ? g.activateAbility(t, pt) : g.activateAbility(t)), type + " cannot be re-used on cooldown");
    }

    // ------------------------------------------------------------------ stress
    /** All 7 towers at U3 with abilities spammed through the late waves on every map: entity counts must stay bounded. */
    private static void stressTitans() {
        TowerType[] types = { TowerType.QWEN, TowerType.LLAMA, TowerType.MISTRAL, TowerType.COHERE, TowerType.GLM, TowerType.KIMI, TowerType.MINIMAX };
        long start = System.currentTimeMillis();
        for (int m = 0; m < MapCatalog.MAP_COUNT; m++) {
            Game g = newGame(m);
            g.setInvincible(true);
            g.setGlobalEnemySpeedMultiplier(0.35);
            g.addCash(5_000_000, null);
            int placed = 0;
            outer:
            for (Path r : g.getMap().getRoutes()) {
                for (double d = 80; d < r.getTotalLength(); d += 110) {
                    Vec dir = r.directionAt(d);
                    for (int side = -1; side <= 1; side += 2) {
                        Vec p = r.positionAt(d).add(new Vec(-dir.y, dir.x).scale(side * 75));
                        if (placed >= types.length) break outer;
                        if (g.getTowerManager().canPlace(types[placed], p)) {
                            Tower t = g.placeTower(types[placed], p);
                            while (g.upgradeTower(t)) { /* max out */ }
                            placed++;
                        }
                    }
                }
            }
            check(placed == types.length, g.getMap().getName() + " fits the 7 titans (" + placed + ")");
            for (int k = 1; k < 94; k++) { g.getWaveManager().startNextWave(); g.getWaveManager().endWave(); }
            int peakDrones = 0, peakZones = 0, peakWalkers = 0, peakHazards = 0, peakProj = 0, peakEnemies = 0;
            for (int wave = 94; wave <= 100; wave++) {
                g.getWaveManager().endWave();
                g.getEnemyManager().clear();
                check(g.startNextWave(), "start wave " + wave);
                int ticks = 0;
                while (g.getWaveManager().isWaveActive() && ticks < 60 * 120 && g.getState() == Game.State.PLAYING) {
                    g.step(Game.DT);
                    if (ticks % 60 == 0) {
                        for (Tower t : new ArrayList<>(g.getTowerManager().getTowers())) {
                            if (!t.isAbilityReady()) continue;
                            if (t.getAbility().needsTarget) g.activateAbility(t, g.getMap().getRoutes().get(0).positionAt(400));
                            else g.activateAbility(t);
                        }
                    }
                    peakDrones = Math.max(peakDrones, g.getEffects().getDrones().size());
                    peakZones = Math.max(peakZones, g.getEffects().getZones().size());
                    peakWalkers = Math.max(peakWalkers, g.getEffects().getWalkers().size());
                    peakHazards = Math.max(peakHazards, g.getEffects().getHazards().size());
                    peakProj = Math.max(peakProj, g.getCombat().getProjectiles().size());
                    peakEnemies = Math.max(peakEnemies, g.getEnemyManager().getEnemies().size());
                    g.getEvents().clear();
                    ticks++;
                }
                if (g.getState() != Game.State.PLAYING) break;
            }
            check(g.getState() == Game.State.PLAYING || g.getState() == Game.State.VICTORY, "titan stress ended in a sane state");
            check(peakDrones <= 5, "drones bounded (Qwen 2 + MiniMax 3): " + peakDrones);
            check(peakHazards <= 1, "one GLM network at a time: " + peakHazards);
            check(peakZones <= 5, "zones bounded (tornado, vortex, tempest): " + peakZones);
            check(peakWalkers <= 120, "llama walkers bounded: " + peakWalkers);
            check(peakProj <= 4000, "projectiles bounded: " + peakProj);
            // everything that was spawned expires once the fight is over
            g.getEnemyManager().clear();
            for (int i = 0; i < 60 * 40; i++) { g.getEffects().update(Game.DT); g.getCombat().updateProjectiles(Game.DT); }   // the sim may have ended in victory
            check(g.getCombat().getProjectiles().isEmpty(), "projectiles all expire after the fight: " + g.getCombat().getProjectiles().size());
            check(g.getEffects().getWalkers().isEmpty(), "walkers all expire");
            check(g.getEffects().getZones().size() <= 1 && countZones(g, "tornado") == 0 && countZones(g, "tempest") == 0, "only the permanent vortex remains: " + zoneKinds(g));
            check(g.getEffects().getDrones().size() == 5 && g.getEffects().getHazards().size() == 1, "drones / network are steady: " + g.getEffects().getDrones().size());
            System.out.println("  " + g.getMap().getName() + ": titans stress, wave " + g.getWave() + ", killed " + g.getEnemyManager().getTotalKilled()
                + ", peak drones " + peakDrones + ", zones " + peakZones + ", walkers " + peakWalkers + ", hazards " + peakHazards
                + ", projectiles " + peakProj + ", enemies " + peakEnemies);
        }
        System.out.println("  titans stress time: " + (System.currentTimeMillis() - start) + " ms");
    }
}
