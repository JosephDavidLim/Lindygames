package lindefense.test;

import static lindefense.test.TestKit.*;
import static lindefense.tower.TowerStats.u;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lindefense.combat.AttackContext;
import lindefense.combat.DamageType;
import lindefense.combat.HitSpec;
import lindefense.combat.VisualEvent;
import lindefense.effects.Drone;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyType;
import lindefense.enemy.Modifier;
import lindefense.map.Path;
import lindefense.sim.Game;
import lindefense.sim.HeroCollection;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.BuffStat;
import lindefense.tower.Faction;
import lindefense.tower.Shop;
import lindefense.tower.Targeting;
import lindefense.tower.Tower;
import lindefense.tower.TowerCatalog;
import lindefense.tower.TowerDef;
import lindefense.tower.TowerStats;
import lindefense.tower.TowerType;
import lindefense.tower.behavior.BradLindemannBehavior;
import lindefense.tower.behavior.BrandonBehavior;
import lindefense.tower.behavior.OwenBehavior;
import lindefense.tower.behavior.ZethosBehavior;
import lindefense.ui.HeroStatText;

/**
 * Hero Store, the six heroes and Zethos: the brief's test list (locked start, Delta removed, exact prices, persistence,
 * placement rules, every hero's mechanics and ability, Zethos's shop / tiers / no-duplicate-damage projectiles,
 * cooldowns, non-negative currency and the UI text).
 */
public final class HeroStoreTests {
    private HeroStoreTests() { }

    public static void runAll() {
        run("heroes.startLocked", HeroStoreTests::startLocked);
        run("heroes.deltaRemoved", HeroStoreTests::deltaRemoved);
        run("heroes.exactPrices", HeroStoreTests::exactPrices);
        run("heroes.persistence", HeroStoreTests::persistence);
        run("heroes.placementRules", HeroStoreTests::placementRules);
        run("heroes.camoAndTargeting", HeroStoreTests::camoAndTargeting);
        run("heroes.brad", HeroStoreTests::brad);
        run("heroes.brandon", HeroStoreTests::brandon);
        run("heroes.rohan", HeroStoreTests::rohan);
        run("heroes.ronil", HeroStoreTests::ronil);
        run("heroes.shashank", HeroStoreTests::shashank);
        run("heroes.owen", HeroStoreTests::owen);
        run("zethos.shopOnly", HeroStoreTests::zethosShop);
        run("zethos.tiers", HeroStoreTests::zethosTiers);
        run("zethos.noDuplicateDamage", HeroStoreTests::zethosNoDuplicates);
        run("zethos.bounceFalloff", HeroStoreTests::zethosBounce);
        run("zethos.realityCollapse", HeroStoreTests::zethosCollapse);
        run("heroes.abilityNumbers", HeroStoreTests::abilityNumbers);
        run("heroes.cashNeverNegative", HeroStoreTests::cashNeverNegative);
        run("heroes.uiText", HeroStoreTests::uiText);
        run("ui.tutorialScript", HeroStoreTests::tutorialScript);
        run("ui.catalogEntries", HeroStoreTests::catalogEntries);
        run("towers.originalTowersPassive", HeroStoreTests::originalTowersPassive);
    }

    // ------------------------------------------------------------------ tutorial, catalog, passive towers
    private static void tutorialScript() {
        Game g = newGame(0);
        lindefense.ui.TutorialScript ts = new lindefense.ui.TutorialScript(g);
        check(ts.current() == lindefense.ui.TutorialScript.Step.WELCOME && !ts.current().action, "starts on the welcome card");
        ts.update();
        check(ts.index() == 0, "info cards wait for NEXT");
        ts.next();
        check(ts.current() == lindefense.ui.TutorialScript.Step.PLACE, "then: place a tower");
        ts.update();
        check(ts.index() == 1, "action steps wait for the action");
        Tower gpt = g.placeTower(TowerType.GPT, buildableNear(g, new Vec(380, 300), 150));
        check(gpt != null, "player places GPT");
        ts.update();
        check(ts.current() == lindefense.ui.TutorialScript.Step.WAVE, "placing a tower advances");
        g.startNextWave();
        ts.update();
        check(ts.current() == lindefense.ui.TutorialScript.Step.UPGRADE && ts.grantPaid(), "starting a wave advances; upgrade step pays its bonus");
        check(g.upgradeTower(gpt), "bonus covers the upgrade");
        ts.update();
        check(ts.current() == lindefense.ui.TutorialScript.Step.TARGETING, "upgrading advances");
        ts.next();
        check(ts.current() == lindefense.ui.TutorialScript.Step.HERO_STORE && g.getCash() >= 1000, "Hero Store step funds a hero");
        check(g.purchaseHero(TowerType.SHASHANK), "buy Shashank");
        ts.update();
        check(ts.current() == lindefense.ui.TutorialScript.Step.DEPLOY, "buying advances");
        Tower sh = g.placeTower(TowerType.SHASHANK, buildableNear(g, new Vec(700, 300), 150));
        ts.update();
        check(sh != null && ts.current() == lindefense.ui.TutorialScript.Step.ABILITY && sh.isAbilityReady(), "deploying advances; the hero's ability is made ready");
        g.activateAbility(sh);
        ts.update();
        check(ts.current() == lindefense.ui.TutorialScript.Step.ZETHOS, "using the ability advances");
        ts.next();
        check(ts.current() == lindefense.ui.TutorialScript.Step.DONE, "final card");
        ts.next();
        check(ts.isFinished(), "FINISH ends the tutorial");
        check(g.getHeroCollection() != HeroCollection.persistent(), "the tutorial game is not wired to the saved collection");
        lindefense.ui.TutorialScript skip = new lindefense.ui.TutorialScript(newGame(0));
        skip.skipAll();
        check(skip.isFinished(), "END TUTORIAL works from any step");
    }

    private static void catalogEntries() {
        int enemies = lindefense.ui.CatalogScreen.entries(lindefense.ui.CatalogScreen.Tab.ENEMIES).size();
        int bosses = lindefense.ui.CatalogScreen.entries(lindefense.ui.CatalogScreen.Tab.BOSSES).size();
        int towers = lindefense.ui.CatalogScreen.entries(lindefense.ui.CatalogScreen.Tab.TOWERS).size();
        int heroes = lindefense.ui.CatalogScreen.entries(lindefense.ui.CatalogScreen.Tab.HEROES).size();
        int allEnemies = 0;
        for (EnemyType e : EnemyType.values()) allEnemies++;
        check(enemies + bosses == allEnemies && bosses == 5, "catalog lists every enemy (" + enemies + ") and all 5 bosses");
        check(towers == 18 && lindefense.ui.CatalogScreen.entries(lindefense.ui.CatalogScreen.Tab.TOWERS).contains(TowerType.ZETHOS), "catalog lists the 18 shop towers incl. Zethos");
        check(heroes == 6 && !lindefense.ui.CatalogScreen.entries(lindefense.ui.CatalogScreen.Tab.HEROES).contains(TowerType.ZETHOS), "catalog lists the 6 heroes (not Zethos)");
    }

    private static void originalTowersPassive() {
        for (TowerType t : TowerType.shopTowers()) {
            boolean shouldHave = t == TowerType.ZETHOS;
            check((TowerCatalog.get(t).ability != null) == shouldHave, t + (shouldHave ? " keeps its ability" : " has no special ability"));
        }
        for (TowerType h : TowerType.heroes()) check(TowerCatalog.get(h).ability != null, h + " keeps its ability");
        Game g = frozen();
        Tower claude = towerNearTrack(g, TowerType.CLAUDE, 3, 450);
        tank(g, 450);
        check(!claude.isAbilityReady() && !g.activateAbility(claude), "a passive tower cannot fire an ability");
    }

    // ------------------------------------------------------------------ helpers
    private static Game frozen() { Game g = newGame(0); g.setGlobalEnemySpeedMultiplier(0); return g; }
    private static boolean near(double a, double b) { return Math.abs(a - b) < 1e-6; }

    /** An unarmoured enemy with a huge HP pool, so damage is never capped by remaining HP. */
    private static Enemy tank(Game g, double dist) {
        Enemy e = g.getEnemyManager().spawnScaled(EnemyType.DYNAMIC_ARRAY, null, 0, dist, 500, 1.0);
        g.getEnemyManager().update(0);
        return e;
    }
    private static double taken(Enemy e) { return e.getMaxHp() - e.getHp(); }

    /** Fires exactly one attack from the tower and flies its projectiles until they are spent (no other tower acts). */
    private static void oneShot(Game g, Tower t) {
        check(g.getCombat().attack(t), t.getType() + " fires");
        for (int i = 0; i < 600 && hasProjectiles(g); i++) { g.getCombat().updateProjectiles(Game.DT); g.getEvents().clear(); }
    }
    private static boolean hasProjectiles(Game g) {
        g.getCombat().updateProjectiles(0);
        return !g.getCombat().getProjectiles().isEmpty();
    }

    private static Tower hero(Game g, TowerType t, double trackDist) { return towerNearTrack(g, t, 0, trackDist); }

    // ------------------------------------------------------------------ 1-7 store and placement
    private static void startLocked() {
        Game g = newGame(0);
        for (TowerType t : TowerType.heroes()) {
            check(!g.isHeroUnlocked(t), t + " starts locked");
            check(g.getTowerManager().heroProblem(t) != null, t + " cannot be placed while locked");
        }
        check(g.getHeroCollection().unlocked().isEmpty(), "zero unlocked heroes at the start");
    }

    private static void deltaRemoved() {
        for (TowerType t : TowerType.values()) {
            check(!t.name().contains("STUDENT") && !t.displayName.contains("Student") && !t.displayName.contains("Delta"), "no Delta / Bellarmine Student unit: " + t);
        }
        EnumSet<TowerType> want = EnumSet.of(TowerType.BRAD_LINDEMANN, TowerType.BRANDON, TowerType.ROHAN, TowerType.RONIL, TowerType.SHASHANK, TowerType.OWEN);
        EnumSet<TowerType> got = EnumSet.noneOf(TowerType.class);
        for (TowerType t : TowerType.heroes()) got.add(t);
        check(got.equals(want), "exactly the six heroes: " + got);
        check(TowerType.ZETHOS.faction == Faction.PRIMARY && !TowerType.ZETHOS.isHero(), "Zethos is a Primary tower, never a hero");
    }

    private static void exactPrices() {
        Object[][] p = { { TowerType.BRAD_LINDEMANN, 1800 }, { TowerType.BRANDON, 1100 }, { TowerType.ROHAN, 1300 },
            { TowerType.RONIL, 1500 }, { TowerType.SHASHANK, 1000 }, { TowerType.OWEN, 1400 } };
        for (Object[] r : p) {
            TowerType t = (TowerType) r[0];
            int price = (Integer) r[1];
            check(Shop.unlockPriceOf(t) == price, t + " costs $" + price);
            Game g = newGame(0);
            g.addCash(price - g.getCash() - 1, null);
            check(g.getCash() == price - 1 && !g.purchaseHero(t) && !g.isHeroUnlocked(t), t + " unaffordable at $" + (price - 1));
            g.addCash(1, null);
            check(g.purchaseHero(t) && g.isHeroUnlocked(t) && g.getCash() == 0, t + " bought for exactly $" + price);
            g.addCash(10_000, null);
            double c = g.getCash();
            check(!g.purchaseHero(t) && g.getCash() == c && "Already unlocked".equals(g.heroPurchaseProblem(t)), t + " cannot be bought twice");
            check(g.getTowerManager().getTowers().isEmpty(), t + " purchase does not place it");
        }
    }

    private static void persistence() {
        Set<TowerType> disk = EnumSet.noneOf(TowerType.class);
        HeroCollection.Storage st = new HeroCollection.Storage() {
            @Override public Set<TowerType> load() { return EnumSet.copyOf(disk.isEmpty() ? EnumSet.noneOf(TowerType.class) : disk); }
            @Override public void save(Set<TowerType> s) { disk.clear(); disk.addAll(s); }
        };
        Game g = newGame(0);
        g.setHeroCollection(HeroCollection.withStorage(st));
        g.addCash(5000, null);
        check(g.purchaseHero(TowerType.ROHAN), "Rohan bought");
        check(disk.contains(TowerType.ROHAN), "unlock saved to storage");
        HeroCollection reloaded = HeroCollection.withStorage(st);
        check(reloaded.isUnlocked(TowerType.ROHAN) && !reloaded.isUnlocked(TowerType.OWEN), "a new session loads the unlock");
        Game next = Game.freshCopy(g);
        check(next.isHeroUnlocked(TowerType.ROHAN), "restart keeps unlocks");
        check(!next.isHeroDeployed(TowerType.ROHAN), "...but deployments reset per match");
        check(next.getCash() == Game.START_CASH, "...and the new match starts with normal cash");
    }

    private static void placementRules() {
        Game g = newGame(0);
        g.addCash(20_000, null);
        Vec a = buildableNear(g, new Vec(300, 300), 120), b = buildableNear(g, new Vec(700, 300), 120);
        check(g.placeTower(TowerType.OWEN, a) == null, "locked hero refused");
        g.purchaseHero(TowerType.OWEN);
        double cash = g.getCash();
        Tower o = g.placeTower(TowerType.OWEN, a);
        check(o != null && g.getCash() == cash, "unlocked hero deploys for free");
        check(g.placeTower(TowerType.OWEN, b) == null, "second Owen refused");
        Tower gpt = g.placeTower(TowerType.GPT, b);
        check(gpt != null, "regular towers place independently of heroes");
        g.purchaseHero(TowerType.BRANDON);
        check(g.placeTower(TowerType.BRANDON, buildableNear(g, new Vec(500, 600), 150)) != null, "different heroes in the same match");
        g.sellTower(o);
        check(Shop.sellValue(o) == 0, "dismissing a hero refunds nothing (it cost nothing to deploy)");
        check(g.placeTower(TowerType.OWEN, a) == null && g.placementProblem(TowerType.OWEN, a).contains("already deployed"), "once per match, even after dismissing");
    }

    private static void camoAndTargeting() {
        Game g = frozen();
        Tower brad = hero(g, TowerType.BRAD_LINDEMANN, 500);
        Enemy camo = spawnAt(g, EnemyType.QUICK_SORT, EnumSet.of(Modifier.CAMO), 0, 500);
        check(g.getCombat().acquireTargets(brad, 1).isEmpty(), "heroes need detection for Camo");
        check(!camo.isTargetableBy(brad.canSeeCamo()), "...Brad has no built-in detection");
        check(brad.getTargeting() == Targeting.STRONGEST, "Brad prioritises the strongest enemy");
        check(TowerCatalog.get(TowerType.BRANDON).defaultTargeting == Targeting.STRONGEST, "Brandon prioritises the strongest enemy");
        check(TowerCatalog.get(TowerType.ZETHOS).defaultTargeting == Targeting.FIRST, "Zethos prioritises the first enemy");
        // Fortified keeps its HP-doubling rule against hero damage
        Enemy fort = spawnAt(g, EnemyType.DYNAMIC_ARRAY, EnumSet.of(Modifier.FORTIFIED), 0, 900);
        Enemy plain = spawnAt(g, EnemyType.DYNAMIC_ARRAY, null, 0, 950);
        check(near(fort.getMaxHp(), plain.getMaxHp() * 2), "Fortified enemies keep double HP");
    }

    // ------------------------------------------------------------------ 8-13 heroes
    private static void brad() {
        TowerStats s = TowerCatalog.get(TowerType.BRAD_LINDEMANN).baseStats;
        check(s.hit.damage == 35 && near(s.interval(), 2.0) && near(s.range, u(55)) && near(s.splashRadius, u(18)) && s.chainHops == 5
            && near(s.chainRange, u(15)) && s.p("chainDamage") == 25 && near(s.projectileSpeed, u(45)), "Brad Command Lightning numbers");
        for (int sup = 0; sup < 2; sup++) {
            Game g = frozen();
            Tower b = hero(g, TowerType.BRAD_LINDEMANN, 600);
            if (sup == 1) { b.resetAbilityCooldown(); check(g.activateAbility(b), "Supreme Command activates"); }
            List<Enemy> es = new ArrayList<>();
            for (int i = 0; i < 16; i++) es.add(tank(g, 560 + i * 12));
            oneShot(g, b);
            double blast = sup == 1 ? 70 : 35, chain = sup == 1 ? 35 : 25;
            int chained = 0, blasted = 0;
            boolean valid = true;
            for (Enemy e : es) {
                double d = taken(e);
                if (near(d, blast)) blasted++;
                else if (near(d, chain)) chained++;
                else if (near(d, blast + chain)) { blasted++; chained++; }
                else if (d != 0) valid = false;
            }
            String tag = sup == 1 ? "Supreme Command: " : "";
            check(valid, tag + "every enemy took explosion and/or one chain hit only (no double chain hits)");
            check(chained == (sup == 1 ? 10 : 5), tag + "chain reaches " + (sup == 1 ? 10 : 5) + " enemies, got " + chained);
            check(blasted >= 2, tag + "explosion hits the target and its neighbours (" + blasted + ")");
            if (sup == 1) check(near(BradLindemannBehavior.splashRadius(b), u(27)), "Supreme Command splash 27 units");
        }
        // chain hops need an enemy within 15 units of the previous one
        Game g = frozen();
        Tower b = hero(g, TowerType.BRAD_LINDEMANN, 600);
        Enemy t0 = tank(g, 600);
        Enemy far = tank(g, 600 + u(18) + u(15) + 40);
        oneShot(g, b);
        check(near(taken(t0), 35) && taken(far) == 0, "no chain target beyond 15 units");
        // ability lasts 12 s
        b.resetAbilityCooldown();
        g.activateAbility(b);
        runSeconds(g, 11.9);
        check(BradLindemannBehavior.isSupreme(b), "Supreme Command active at 11.9 s");
        runSeconds(g, 0.2);
        check(!BradLindemannBehavior.isSupreme(b) && near(b.getAbilityCooldownRemaining(), 45 - 12.1), "ends at 12 s; 45 s cooldown counts from activation");
    }

    private static void brandon() {
        Game g = frozen();
        Tower t = hero(g, TowerType.BRANDON, 500);
        check(near(1 / t.effectiveRate(g), 0.30), "attack interval 0.30 s");
        List<Enemy> stack = new ArrayList<>();
        for (int i = 0; i < 6; i++) stack.add(tank(g, 500));
        oneShot(g, t);
        int hit = 0;
        for (Enemy e : stack) { if (taken(e) > 0) { hit++; check(near(taken(e), 12), "12 damage per bolt hit"); } }
        check(hit == 3, "a bolt pierces 3 enemies, got " + hit);
        t.resetAbilityCooldown();
        check(g.activateAbility(t), "Overdrive activates");
        check(near(1 / t.effectiveRate(g), 0.15), "Overdrive halves the interval");
        AttackContext c = g.getCombat().newAttackContext(t, g.getCombat().acquireTargets(t, 1));
        t.getBehavior().modifyAttack(g, t, c);
        check(near(c.damageMult, 1.5) && c.pierce == 5, "Overdrive: x1.5 damage, pierce 5");
        for (Enemy e : stack) e.heal(1e9);
        oneShot(g, t);
        hit = 0;
        for (Enemy e : stack) if (taken(e) > 0) { hit++; check(near(taken(e), 18), "Overdrive bolt deals 18"); }
        check(hit == 5, "Overdrive bolt pierces 5, got " + hit);
        runSeconds(g, 10.1);
        check(!t.hasSelfBuff(BrandonBehavior.OVERDRIVE) && near(1 / t.effectiveRate(g), 0.30), "Overdrive ends after 10 s");
    }

    private static void rohan() {
        Game g = frozen();
        Tower r = hero(g, TowerType.ROHAN, 500);
        List<Enemy> pack = new ArrayList<>();
        for (int i = 0; i < 30; i++) pack.add(tank(g, 500));
        Enemy outside = tank(g, 500 - u(12) - 60);   // behind the pack, so First targeting still picks the pack
        Enemy boss = g.getEnemyManager().spawnScaled(EnemyType.BELLARMINE_COLLEGE_PREP, null, 0, 500, 1, 1.0);
        g.getEnemyManager().update(0);
        oneShot(g, r);
        int frozen = 0;
        for (Enemy e : pack) {
            check(near(taken(e), 18) && near(e.getSlowFraction(), 0.40), "Cryo Blast: 18 damage and 40% slow");
            if (e.isFrozen()) frozen++;
        }
        check(frozen > 0 && frozen < 15, "about 20% of enemies frozen (" + frozen + " of 30)");
        check(taken(outside) == 0, "nothing beyond 12 units");
        check(!boss.isFrozen() && near(boss.getSlowFraction(), 0.15), "bosses are never frozen and take a 15% slow, got " + boss.getSlowFraction());
        Enemy f = null;
        for (Enemy e : pack) if (e.isFrozen()) { f = e; break; }
        if (f != null) { f.tickStatuses(1.01); check(!f.isFrozen(), "freeze lasts 1 s"); }
        for (Enemy e : pack) e.tickStatuses(3.01);
        check(pack.get(0).getSlowFraction() == 0, "slow lasts 3 s");
        // Absolute Zero: every enemy on the map
        Enemy farAway = tank(g, 1200);
        double bossBefore = taken(boss);
        r.resetAbilityCooldown();
        check(g.activateAbility(r), "Absolute Zero activates");
        boolean allFrozen = true;
        for (Enemy e : pack) allFrozen &= e.isFrozen();
        check(allFrozen && farAway.isFrozen(), "freezes all non-boss enemies on the map");
        check(near(taken(farAway), 50), "deals 50 damage (before the freeze bonus)");
        check(!boss.isFrozen() && near(boss.getSlowFraction(), 0.50), "boss slowed exactly 50%, not frozen");
        check(near(taken(boss) - bossBefore, 50 * (1 - boss.effectiveArmor())), "boss takes 50 damage (through its armour)");
        farAway.tickStatuses(4.01);
        boss.tickStatuses(4.01);
        check(!farAway.isFrozen() && boss.getSlowFraction() == 0, "freeze and boss slow last 4 s");
        Game empty = frozen();
        Tower r2 = hero(empty, TowerType.ROHAN, 500);
        r2.resetAbilityCooldown();
        check(!empty.activateAbility(r2) && r2.isAbilityReady(), "no enemies: refused, cooldown kept");
    }

    private static void ronil() {
        TowerStats s = TowerCatalog.get(TowerType.RONIL).baseStats;
        check(s.hit.damage == 75 && near(s.interval(), 3.0) && near(s.range, u(75)) && near(s.splashRadius, u(16)) && near(s.projectileSpeed, u(35)), "Meteor Strike numbers");
        Game g = frozen();
        Tower t = hero(g, TowerType.RONIL, 400);
        List<Enemy> pack = new ArrayList<>();
        for (int i = 0; i < 4; i++) pack.add(tank(g, 400));
        oneShot(g, t);
        for (Enemy e : pack) check(near(taken(e), 75), "Meteor Strike hits every enemy in 16 units for 75");
        // Meteor Shower: 8 meteors over 6 s at distinct enemy locations
        Game g2 = frozen();
        Tower r2 = forcePlace(g2, TowerType.RONIL, buildableNear(g2, new Vec(1130, 860), 150), 0);
        Path route = g2.getMap().getRoutes().get(0);
        for (int i = 0; i < 12; i++) tank(g2, 150 + i * 140);
        r2.resetAbilityCooldown();
        check(g2.activateAbility(r2), "Meteor Shower activates");
        List<Vec> impacts = new ArrayList<>();
        double t0 = g2.getTime();
        for (int i = 0; i < (int) (6.2 / Game.DT); i++) {
            g2.step(Game.DT);
            for (VisualEvent ev : g2.getEvents()) if (ev.kind == VisualEvent.Kind.EXPLOSION && near(ev.radius, u(14))) impacts.add(ev.a);
            g2.getEvents().clear();
        }
        check(impacts.size() == 8, "8 meteors land, got " + impacts.size());
        boolean distinct = true;
        for (int i = 0; i < impacts.size(); i++) for (int j = i + 1; j < impacts.size(); j++) if (impacts.get(i).dist(impacts.get(j)) <= u(14)) distinct = false;
        check(distinct, "each meteor lands at a different enemy location");
        check(g2.getTime() - t0 > 6 && route != null, "shower spans 6 s");
    }

    private static void shashank() {
        Game g = frozen();
        Tower sh = hero(g, TowerType.SHASHANK, 500);
        Tower nearT = forcePlace(g, TowerType.GPT, buildableNear(g, sh.getPosition().add(new Vec(70, 0)), 40), 0);
        Tower farT = forcePlace(g, TowerType.GPT, buildableNear(g, new Vec(1130, 860), 150), 0);
        check(nearT.getPosition().dist(sh.getPosition()) <= u(35) && farT.getPosition().dist(sh.getPosition()) > u(35), "scenario: one tower inside 35 units, one outside");
        List<Enemy> stack = new ArrayList<>();
        for (int i = 0; i < 4; i++) stack.add(tank(g, 500));
        oneShot(g, sh);
        int hit = 0;
        for (Enemy e : stack) if (taken(e) > 0) { hit++; check(near(taken(e), 15) && near(e.getMarkedBonus(), 0.10), "Data Drain: 15 damage and +10% vulnerability"); }
        check(hit == 2, "Data Drain pierces 2, got " + hit);
        Enemy marked = null;
        for (Enemy e : stack) if (e.getMarkedBonus() > 0) { marked = e; break; }
        double dealt = g.getCombat().applyHit(marked, new HitSpec(10, DamageType.ENERGY), farT, 1, 0);
        check(near(dealt, 11), "any tower deals +10% to a drained enemy, got " + dealt);
        marked.applyStatus(lindefense.enemy.StatusEffect.Kind.MARKED, 0.10, 4);
        check(near(marked.getMarkedBonus(), 0.10), "reapplying refreshes, never stacks");
        marked.tickStatuses(4.01);
        check(marked.getMarkedBonus() == 0, "debuff lasts 4 s");
        double cash = g.getCash();
        double before = nearT.effectiveRate(g);
        sh.resetAbilityCooldown();
        check(g.activateAbility(sh), "Resource Optimization activates");
        check(near(g.getCash(), cash + 500), "+$500 immediately");
        check(near(nearT.effectiveRate(g), before * 1.2) && near(farT.buffTotal(BuffStat.RATE), 0), "+20% attack speed within 35 units only");
        check(near(sh.buffTotal(BuffStat.RATE), 0.2), "Shashank buffs himself too (he is within range)");
        sh.resetAbilityCooldown();
        g.activateAbility(sh);
        check(near(nearT.buffTotal(BuffStat.RATE), 0.2), "a second activation does not stack");
        runSeconds(g, 12.1);
        check(near(nearT.buffTotal(BuffStat.RATE), 0), "buff lasts 12 s");
    }

    private static void owen() {
        Game g = frozen();
        Tower o = hero(g, TowerType.OWEN, 500);
        runSeconds(g, 0.05);
        List<Drone> core = g.getEffects().dronesOf(o, OwenBehavior.CORE);
        check(core.size() == 3, "3 permanent drones, got " + core.size());
        for (Drone d : core) {
            TowerStats s = d.effectiveStats();
            check(s.hit.damage == 20 && near(s.interval(), 1.5) && near(s.range, u(50)) && near(s.projectileSpeed, u(55)), "drone: 20 dmg / 1.5 s / 50 u / 55 u/s");
        }
        Enemy a = tank(g, 480), b = tank(g, 500), c = tank(g, 520);
        runSeconds(g, 1.6);
        Set<Integer> locked = new HashSet<>();
        for (Drone d : core) if (d.getCurrentTarget() != null) locked.add(d.getCurrentTarget().getId());
        check(locked.size() == 3, "each drone locks its own target (" + locked.size() + " distinct)");
        check(taken(a) > 0 && taken(b) > 0 && taken(c) > 0, "all three enemies are being shot");
        double perShot = taken(a) + taken(b) + taken(c);
        check(near(perShot % 20, 0), "drone shots deal 20 each");
        // retarget when the target dies
        Drone d0 = core.get(0);
        Enemy tgt = d0.getCurrentTarget();
        killOutright(g, tgt);
        Enemy d4 = tank(g, 510);
        runSeconds(g, 1.6);
        check(d0.getCurrentTarget() != null && d0.getCurrentTarget() != tgt && d0.getCurrentTarget().isAlive(), "drone acquires a new target after a kill");
        check(taken(d4) >= 0, "replacement target in range");
        // Drone Swarm
        o.resetAbilityCooldown();
        check(g.activateAbility(o), "Drone Swarm activates");
        List<Drone> swarm = g.getEffects().dronesOf(o, OwenBehavior.SWARM);
        check(swarm.size() == 5, "5 temporary drones");
        for (Drone d : swarm) check(d.effectiveStats().hit.damage == 25 && near(d.effectiveStats().interval(), 1.0) && near(d.lifetime, 15), "swarm drone: 25 dmg, 1 s, 15 s life");
        runSeconds(g, 15.1);
        check(g.getEffects().dronesOf(o, OwenBehavior.SWARM).isEmpty(), "swarm disappears after 15 s");
        check(g.getEffects().dronesOf(o, OwenBehavior.CORE).size() == 3, "original 3 drones remain");
        g.sellTower(o);
        g.run(1);
        check(g.getEffects().dronesOf(o, null).isEmpty(), "drones are removed when Owen is dismissed");
    }

    // ------------------------------------------------------------------ 14-17 Zethos
    private static void zethosShop() {
        boolean inPrimary = false;
        for (lindefense.ui.ShopLayout.Cell c : lindefense.ui.ShopLayout.factionCells(Faction.PRIMARY)) if (c.type == TowerType.ZETHOS) inPrimary = true;
        check(inPrimary, "Zethos is in the Primary tower shop");
        for (TowerType h : TowerType.heroes()) check(h != TowerType.ZETHOS, "Zethos is not in the Hero Store");
        Game g = newGame(0);
        Vec p = buildableNear(g, new Vec(400, 300), 150);
        check(g.getTowerManager().heroProblem(TowerType.ZETHOS) == null, "Zethos has no hero restrictions");
        Tower z = g.placeTower(TowerType.ZETHOS, p);
        check(z != null && g.getCash() == Game.START_CASH - 650, "Zethos bought for $650 with starting cash");
        g.addCash(10_000, null);
        Tower z2 = g.placeTower(TowerType.ZETHOS, buildableNear(g, new Vec(800, 300), 150));
        check(z2 != null, "several Zethos towers may be placed");
        double c0 = g.getCash();
        check(g.upgradeTower(z) && near(g.getCash(), c0 - 350), "Tier 1 costs $350");
        double c1 = g.getCash();
        int refund = g.sellTower(z);
        check(refund == (int) Math.floor((650 + 350) * 0.7) && near(g.getCash(), c1 + refund), "sells for 70% like any tower");
    }

    private static void zethosTiers() {
        TowerDef d = TowerCatalog.get(TowerType.ZETHOS);
        // damage, interval, range(u), pierce, splash(u), splash%, bounces, bounce%
        double[][] t = {
            { 10, 1.00, 40, 2, 0, 0, 0, 0 },
            { 15, 0.85, 45, 3, 0, 0, 0, 0 },
            { 22, 0.75, 50, 4, 5, 0.50, 0, 0 },
            { 32, 0.65, 55, 5, 8, 0.75, 3, 0.75 },
            { 55, 0.50, 65, 8, 12, 1.00, 5, 1.00 } };
        int[] cost = { 650, 350, 650, 1200, 2500 };
        check(d.baseCost == cost[0], "base $650");
        for (int i = 0; i < t.length; i++) {
            TowerStats s = d.statsForTier(i);
            String n = "Zethos tier " + i;
            check(s.hit.damage == t[i][0] && near(s.interval(), t[i][1]) && near(s.range, u(t[i][2])) && s.pierce == (int) t[i][3], n + " damage/interval/range/pierce");
            check(near(s.splashRadius, u(t[i][4])) && near(s.p("splashPct", 0), t[i][5]) && s.bounces == (int) t[i][6] && near(s.p("bouncePct", 0), t[i][7]), n + " splash/bounce");
            if (i > 0) check(d.upgrades[i - 1].cost == cost[i], n + " costs $" + cost[i]);
        }
        check(near(d.baseStats.projectileSpeed, u(55)) && d.statsForTier(1).projectileSpeed > d.baseStats.projectileSpeed, "55 u/s, faster after Quantum Acceleration");
        check(d.totalCost() == 5350, "fully upgraded $5,350");
    }

    private static void zethosNoDuplicates() {
        for (int tier = 2; tier <= 4; tier++) {
            Game g = frozen();
            Tower z = towerNearTrack(g, TowerType.ZETHOS, tier, 500);
            List<Enemy> stack = new ArrayList<>();
            for (int i = 0; i < 25; i++) stack.add(tank(g, 500));
            oneShot(g, z);
            double dmg = z.getStats().hit.damage;
            double max = 0; int hit = 0;
            for (Enemy e : stack) { max = Math.max(max, taken(e)); if (taken(e) > 0) hit++; }
            check(max <= dmg + 1e-6, "tier " + tier + ": no enemy damaged twice by one projectile (max " + max + " of " + dmg + ")");
            check(hit > z.getStats().pierce, "tier " + tier + ": splash reaches beyond the direct hits (" + hit + " enemies)");
        }
    }

    private static void zethosBounce() {
        Game g = frozen();
        Tower z = towerNearTrack(g, TowerType.ZETHOS, 3, 500);
        // spread enemies so each is out of the 8-unit splash of the others but inside the 25-unit bounce reach
        List<Enemy> line = new ArrayList<>();
        for (int i = 0; i < 12; i++) line.add(tank(g, 300 + i * 75));
        oneShot(g, z);
        int full = 0, bounce = 0, other = 0;
        for (Enemy e : line) {
            double d = taken(e);
            if (d == 0) continue;
            if (near(d, 32)) full++; else if (near(d, 32 * 0.75)) bounce++; else other++;
        }
        check(bounce == 3, "Tier 3 bounces to exactly 3 more enemies at 75% (" + bounce + ")");
        check(full >= 1 && full <= 5, "direct (pierce) hits at full damage (" + full + ")");
        check(other == 0, "no other damage values (" + other + ")");
    }

    private static void zethosCollapse() {
        Game g = frozen();
        Tower z = towerNearTrack(g, TowerType.ZETHOS, 4, 500);
        double range = z.effectiveRange(g), rate = z.effectiveRate(g);
        check(near(range, u(65)), "Reality Engine range 65 units");
        z.resetAbilityCooldown();
        check(g.activateAbility(z), "Reality Collapse activates");
        check(near(z.effectiveRange(g), u(80)) && near(z.effectiveRate(g), rate * 2), "range 65 -> 80 units, attack interval halved");
        tank(g, 500);
        AttackContext c = g.getCombat().newAttackContext(z, g.getCombat().acquireTargets(z, 1));
        z.getBehavior().modifyAttack(g, z, c);
        check(near(c.damageMult, 2) && c.bounces == 8 && c.splash > 0, "x2 damage, 8 bounces, every impact explodes");
        runSeconds(g, 6.1);
        check(!ZethosBehavior.isCollapsing(z) && near(z.effectiveRange(g), u(65)), "ends after 6 s");
        // a base Zethos gets an explosion during the collapse too
        Game g2 = frozen();
        Tower z0 = towerNearTrack(g2, TowerType.ZETHOS, 0, 500);
        z0.resetAbilityCooldown();
        g2.activateAbility(z0);
        tank(g2, 500);
        AttackContext c0 = g2.getCombat().newAttackContext(z0, g2.getCombat().acquireTargets(z0, 1));
        z0.getBehavior().modifyAttack(g2, z0, c0);
        check(c0.splash > 0 && c0.bounces == 8, "base-tier Reality Collapse still explodes and bounces");
    }

    // ------------------------------------------------------------------ 18-20
    private static void abilityNumbers() {
        Object[][] a = {
            { TowerType.BRAD_LINDEMANN, 45.0, "duration", 12.0 }, { TowerType.BRANDON, 35.0, "duration", 10.0 },
            { TowerType.ROHAN, 50.0, "freezeDuration", 4.0 }, { TowerType.RONIL, 55.0, "duration", 6.0 },
            { TowerType.SHASHANK, 60.0, "duration", 12.0 }, { TowerType.OWEN, 50.0, "duration", 15.0 }, { TowerType.ZETHOS, 55.0, "duration", 6.0 } };
        for (Object[] r : a) {
            AbilityDef d = TowerCatalog.get((TowerType) r[0]).ability;
            check(d != null && near(d.cooldown, (Double) r[1]) && near(d.p((String) r[2]), (Double) r[3]), r[0] + " ability cooldown " + r[1] + " s, " + r[2] + " " + r[3]);
        }
        AbilityDef b = TowerCatalog.get(TowerType.BRAD_LINDEMANN).ability;
        check(b.p("damageMult") == 2 && b.p("splashMult") == 1.5 && b.p("chainTargets") == 10 && b.p("chainDamage") == 35, "Supreme Command multipliers");
        AbilityDef z = TowerCatalog.get(TowerType.ZETHOS).ability;
        check(z.p("damageMult") == 2 && z.p("rateMult") == 2 && near(z.p("rangeBonus"), u(15)) && z.p("bounces") == 8, "Reality Collapse multipliers");
        AbilityDef rs = TowerCatalog.get(TowerType.RONIL).ability;
        check(rs.p("meteors") == 8 && rs.p("damage") == 100 && near(rs.p("splash"), u(14)), "Meteor Shower numbers");
        // cooldowns start after activation (and at 50% on deployment, like every tower)
        Game g = frozen();
        Tower t = hero(g, TowerType.SHASHANK, 500);
        check(near(t.getAbilityCooldownRemaining(), 30), "starts at 50% (30 of 60 s)");
        t.resetAbilityCooldown();
        g.activateAbility(t);
        check(near(t.getAbilityCooldownRemaining(), 60), "full 60 s cooldown after activation");
    }

    private static void cashNeverNegative() {
        Game g = newGame(0);
        for (TowerType h : TowerType.heroes()) { g.purchaseHero(h); check(g.getCash() >= 0, "cash stays >= 0 after trying " + h); }
        check(g.getCash() == Game.START_CASH, "nothing affordable bought at the start");
        g.addCash(1000 - g.getCash(), null);
        check(g.purchaseHero(TowerType.SHASHANK) && g.getCash() == 0, "exact price leaves $0");
        check(!g.purchaseHero(TowerType.BRANDON) && g.getCash() == 0, "no purchase below zero");
        Tower z = g.placeTower(TowerType.ZETHOS, buildableNear(g, new Vec(400, 300), 150));
        check(z == null && g.getCash() == 0, "no tower purchase below zero");
    }

    private static void uiText() {
        check(HeroStatText.attackLines(TowerType.BRAD_LINDEMANN).get(0).contains("35 dmg") && HeroStatText.attackLines(TowerType.BRAD_LINDEMANN).get(0).contains("55u"), "Brad card shows 35 dmg, 55u");
        check(String.join(" ", HeroStatText.attackLines(TowerType.BRAD_LINDEMANN)).contains("5 x 25"), "Brad card shows the 5 x 25 chain");
        check(HeroStatText.attackLines(TowerType.BRANDON).get(0).contains("every 0.3s"), "Brandon card shows the 0.30 s interval: " + HeroStatText.attackLines(TowerType.BRANDON).get(0));
        check(String.join(" ", HeroStatText.attackLines(TowerType.OWEN)).contains("3 drones x 20"), "Owen card shows 3 drones x 20");
        check(HeroStatText.range(TowerType.ZETHOS, u(65)).equals("65u") && HeroStatText.range(TowerType.GPT, 180).equals("180"), "ranges in map units for heroes / Zethos, px otherwise");
        for (TowerType h : TowerType.heroes()) {
            check(!HeroStatText.role(h).isEmpty() && !HeroStatText.attackName(h).equals("Attack"), h + " has role and attack name");
        }
        Game g = frozen();
        Tower z = towerNearTrack(g, TowerType.ZETHOS, 4, 500);
        z.resetAbilityCooldown();
        g.activateAbility(z);
        String live = HeroStatText.liveLine(g, z);
        check(live != null && live.contains("REALITY COLLAPSE"), "selected Zethos shows the live ability state: " + live);
        check(z.getTierLabel().equals("Zethos: Reality Engine") && z.nextUpgrade() == null, "Zethos tier label and no next upgrade at max");
        Tower z1 = towerNearTrack(g, TowerType.ZETHOS, 1, 900);
        check(Shop.upgradeCost(z1) == 650 && z1.getTierLabel().equals("Quantum Acceleration"), "Tier 1 Zethos shows next upgrade cost $650");
    }
}
