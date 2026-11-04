package lindefense.test;

import static lindefense.test.TestKit.*;

import java.util.ArrayList;
import java.util.List;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyType;
import lindefense.map.GameMap;
import lindefense.map.Path;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Tower;
import lindefense.tower.TowerCatalog;
import lindefense.tower.TowerType;

/**
 * Tower spec section 3.7 balance targets, verified headlessly:
 * (a) Brad alone beats a MOAB but not a BFB, (b) no other fully upgraded tower solos a BFB, (c) Brad is the most
 * expensive tower by far, (d) every tower contributes in waves 60+ of a mixed build, (e) ability cooldowns run in game time.
 */
public final class BalanceTests {
    private BalanceTests() { }

    /** Single-lane map used for solo duels (Legacy Monolith). */
    private static final int DUEL_MAP = 1;

    public static void runAll() {
        run("balance.hpNeverDecreases", BalanceTests::hpNeverDecreases);
        run("balance.bradBeatsMoabNotBfb", BalanceTests::bradDuel);
        run("balance.noSoloBfb", BalanceTests::noSoloBfb);
        run("balance.bradCostRanking", BalanceTests::costRanking);
        run("balance.everyTowerContributesLate", BalanceTests::everyTowerContributes);
        run("balance.abilityCooldownsInGameTime", BalanceTests::cooldownsInGameTime);
    }

    /** Fraction of the boss's HP left after it has walked the whole route past one fully upgraded tower (0 = killed). */
    public static double duel(TowerType type, EnemyType boss) { return duel(type, boss, 1.0); }

    /** Same duel with the boss spawned at the given HP scale (e.g. {@code WaveData.hpScale(wave)} for the HP met in the campaign). */
    public static double duel(TowerType type, EnemyType boss, double hpScale) {
        Game g = newGame(DUEL_MAP);
        g.setInvincible(true);
        Path route = g.getMap().getRoutes().get(0);
        Tower t = forcePlace(g, type, bestSpot(g, type), TowerCatalog.get(type).upgrades.length);
        Enemy e = g.getEnemyManager().spawnScaled(boss, null, 0, 0, hpScale, 1.0);
        g.getEnemyManager().update(0);
        double limit = 400;   // simulated seconds: far longer than the walk
        for (double time = 0; time < limit && e.isAlive() && !e.hasLeaked() && e.getDistance() < route.getTotalLength() - 1; time += Game.DT) {
            if (t.getAbility() != null && t.isAbilityReady()) {
                if (t.getAbility().needsTarget) g.activateAbility(t, e.getPosition()); else g.activateAbility(t);
            }
            g.step(Game.DT);
            g.getEvents().clear();
        }
        return e.isAlive() ? e.getHp() / e.getMaxHp() : 0;
    }

    /** The buildable spot that covers the most of the route within the tower's range. */
    private static Vec bestSpot(Game g, TowerType type) {
        double range = Math.min(TowerCatalog.get(type).baseStats.range, 400);
        Path r = g.getMap().getRoutes().get(0);
        g.addCash(1_000_000, null);   // so only geometry (not price) can reject a spot
        Vec best = null; int bestN = -1;
        for (int x = 30; x < GameMap.WIDTH; x += 15) for (int y = 30; y < GameMap.HEIGHT; y += 15) {
            Vec p = new Vec(x, y);
            if (!g.getMap().isBuildable(p, 20) || g.getTowerManager().placementProblem(type, p) != null) continue;
            int n = 0;
            for (double d = 0; d < r.getTotalLength(); d += 20) if (r.positionAt(d).distSq(p) <= range * range) n++;
            if (n > bestN) { bestN = n; best = p; }
        }
        return best;
    }

    private static void bradDuel() {
        // base HP, then the HP actually met in the campaign: MOAB from wave 40 to 100, BFB from wave 60 to 99
        int[] moabWaves = { 40, 60, 80, 100 }, bfbWaves = { 60, 70, 80, 90, 99 };
        double moab0 = duel(TowerType.BRAD_LINDEMANN, EnemyType.BELLARMINE_COLLEGE_PREP), bfb0 = duel(TowerType.BRAD_LINDEMANN, EnemyType.SW_RESOURCE_CONSULTANT);
        check(moab0 == 0, "base HP: Brad alone defeats a MOAB (HP left " + pct(moab0) + ")");
        check(bfb0 > 0, "base HP: Brad alone does NOT defeat a BFB (HP left " + pct(bfb0) + ")");
        StringBuilder sb = new StringBuilder("      Brad solo: base MOAB " + pct(moab0) + " BFB " + pct(bfb0) + " left; campaign MOAB");
        for (int w : moabWaves) {
            double left = duel(TowerType.BRAD_LINDEMANN, EnemyType.BELLARMINE_COLLEGE_PREP, lindefense.wave.WaveData.hpScale(w));
            sb.append(" w").append(w).append('=').append(pct(left));
            check(left == 0, "campaign wave " + w + ": Brad alone defeats a MOAB (HP left " + pct(left) + ")");
        }
        sb.append("; BFB");
        for (int w : bfbWaves) {
            double left = duel(TowerType.BRAD_LINDEMANN, EnemyType.SW_RESOURCE_CONSULTANT, lindefense.wave.WaveData.hpScale(w));
            sb.append(" w").append(w).append('=').append(pct(left));
            check(left > 0, "campaign wave " + w + ": Brad alone does NOT defeat a BFB (HP left " + pct(left) + ")");
        }
        System.out.println(sb);
    }

    private static void noSoloBfb() {
        for (int wave : new int[] { 1, 60, 99 }) {      // base HP, the BFB's debut wave, and its last appearance
            double scale = wave == 1 ? 1.0 : lindefense.wave.WaveData.hpScale(wave);
            StringBuilder sb = new StringBuilder("      solo BFB hp left (" + (wave == 1 ? "base" : "wave " + wave) + "):");
            for (TowerType tt : TowerType.purchasable()) {
                if (tt.isHero()) continue;
                double left = duel(tt, EnemyType.SW_RESOURCE_CONSULTANT, scale);
                sb.append(' ').append(tt).append('=').append(pct(left));
                check(left > 0, tt + " fully upgraded must not solo an SW Resource Consultant BFB (" + (wave == 1 ? "base" : "wave " + wave) + ")");
            }
            if (wave == 1 || wave == 60) System.out.println(sb);
        }
    }

    private static void hpNeverDecreases() {
        for (int w = 1; w < lindefense.wave.WaveData.WAVE_COUNT; w++)
            check(lindefense.wave.WaveData.hpScale(w + 1) >= lindefense.wave.WaveData.hpScale(w), "HP scale never decreases (wave " + w + ")");
        check(lindefense.wave.WaveData.hpScale(100) > lindefense.wave.WaveData.hpScale(80) && lindefense.wave.WaveData.hpScale(80) > lindefense.wave.WaveData.hpScale(60), "HP grows after wave 60 and again after wave 80");
        double palantir = EnemyType.PALANTIR.baseHp;
        for (EnemyType t : EnemyType.values()) if (t.isBoss() && t != EnemyType.PALANTIR) check(palantir > 2 * t.baseHp * lindefense.wave.WaveData.hpScale(100) * 2, "Palantir clearly outweighs even a Fortified " + t);
    }

    private static String pct(double f) { return Math.round(f * 1000) / 10.0 + "%"; }

    private static void costRanking() {
        int brad = TowerCatalog.get(TowerType.BRAD_LINDEMANN).totalCostForTier(3);
        check(brad == 12_500, "Brad total is $12,500: " + brad);
        int second = 0; TowerType which = null;
        for (TowerType tt : TowerType.purchasable()) {
            if (tt == TowerType.BRAD_LINDEMANN) continue;
            int c = TowerCatalog.get(tt).totalCostForTier(3);
            if (c > second) { second = c; which = tt; }
        }
        check(brad > 2 * second, "Brad ($" + brad + ") is more than twice the next most expensive tower (" + which + " $" + second + ")");
    }

    private static void everyTowerContributes() {
        // One of every tower, fully upgraded, in a mixed build fighting real waves 60-64 at O(N^N) strength (so nothing is
        // overkilled before it reaches the back of the build). Abilities are not used. Every attacker must deal damage or
        // score kills; Perplexity (pure economy) must pay its round-end income.
        Game g = new Game(lindefense.map.MapCatalog.create(DUEL_MAP), 99L, lindefense.sim.Difficulty.ONN);
        g.setInvincible(true);
        List<Tower> build = new ArrayList<>();
        List<TowerType> order = new ArrayList<>(java.util.Arrays.asList(TowerType.purchasable()));
        order.sort((x, y) -> Double.compare(TowerCatalog.get(x).baseStats.range, TowerCatalog.get(y).baseStats.range));   // short-range towers get the closest spots
        for (TowerType tt : order) build.add(forcePlaceBest(g, tt));
        while (g.getWave() < 59) { g.getWaveManager().startNextWave(); g.getWaveManager().endWave(); }
        for (int w = 60; w <= 64; w++) {
            check(g.getWaveManager().startNextWave(), "wave " + w + " starts");
            for (int tick = 0; tick < 60 * 400 && (g.getWaveManager().isWaveActive() || tick < 60); tick++) {
                g.step(Game.DT);
                g.getEvents().clear();
            }
        }
        List<TowerType> silent = new ArrayList<>();
        StringBuilder sb = new StringBuilder("      waves 60-64 damage:");
        for (Tower t : build) {
            sb.append(' ').append(t.getType()).append('=').append(Math.round(t.getDamageDealt())).append('/').append(t.getKills());
            boolean active = t.getDamageDealt() > 0 || t.getKills() > 0;
            if (t.getType() == TowerType.PERPLEXITY) active = t.getBehavior().onRoundEnd(g, t) > 0;
            if (!active) silent.add(t.getType());
        }
        System.out.println(sb);
        check(build.size() == 20, "the mixed build has all 20 towers (" + build.size() + ")");
        check(silent.isEmpty(), "every tower contributed in waves 60+ (silent: " + silent + ")");
    }

    /** Places a fully upgraded tower on the free buildable spot nearest the shared kill zone (route distance 1500). */
    private static Tower forcePlaceBest(Game g, TowerType type) {
        g.addCash(1_000_000, null);
        Vec zone = g.getMap().getRoutes().get(0).positionAt(1500);
        Vec best = null; double bestD = Double.MAX_VALUE;
        for (int x = 30; x < GameMap.WIDTH; x += 15) for (int y = 30; y < GameMap.HEIGHT; y += 15) {
            Vec p = new Vec(x, y);
            if (!g.getMap().isBuildable(p, 20) || g.getTowerManager().placementProblem(type, p) != null) continue;
            double d = p.dist(zone);
            if (d < bestD) { bestD = d; best = p; }
        }
        return forcePlace(g, type, best, TowerCatalog.get(type).upgrades.length);
    }

    private static void cooldownsInGameTime() {
        Game g = newGame(DUEL_MAP);
        Tower t = forcePlace(g, TowerType.CLAUDE, bestSpot(g, TowerType.CLAUDE), 0);
        double start = t.getAbilityCooldownRemaining();
        check(Math.abs(start - t.getAbility().cooldown * 0.5) < 1e-9, "starts at 50% cooldown");
        g.setSpeed(3);
        for (int i = 0; i < 6; i++) g.advance(1.0 / 6);   // one real second at 3x = three game seconds
        check(Math.abs(start - t.getAbilityCooldownRemaining() - g.getTime()) < 1e-6, "cooldown drops by elapsed game time (" + g.getTime() + " s)");
        check(g.getTime() > 2.9 && g.getTime() < 3.1, "fast-forward speeds game time up (" + g.getTime() + ")");
    }
}
