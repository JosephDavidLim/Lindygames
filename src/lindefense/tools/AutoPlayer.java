package lindefense.tools;

import java.util.ArrayList;
import java.util.List;
import lindefense.map.GameMap;
import lindefense.map.MapCatalog;
import lindefense.map.Path;
import lindefense.sim.Difficulty;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Tower;
import lindefense.tower.TowerCatalog;
import lindefense.tower.TowerType;

/**
 * Headless balance probe: a simple greedy bot plays the campaign on every map
 * with a fixed build order, upgrading when affordable and firing abilities
 * during bosses/rushes. Prints the wave reached, lives and economy so the
 * difficulty curve can be tuned. Usage: {@code java -cp build lindefense.tools.AutoPlayer [maxWaves]}.
 */
public final class AutoPlayer {
    private AutoPlayer() { }

    /** Build order (type, max count). Repeats once exhausted. */
    private static final Object[][] ORDER = {
        { TowerType.GPT, 2 }, { TowerType.CLAUDE, 1 }, { TowerType.QWEN, 1 }, { TowerType.GEMINI, 1 },
        { TowerType.PERPLEXITY, 1 }, { TowerType.DEEPSEEK, 1 }, { TowerType.SUNO, 1 }, { TowerType.SORA, 1 },
        { TowerType.COHERE, 1 }, { TowerType.LLAMA, 1 }, { TowerType.MISTRAL, 1 }, { TowerType.COPILOT, 1 },
        { TowerType.BRAD_LINDEMANN, 1 }, { TowerType.GLM, 1 }, { TowerType.KIMI, 1 }, { TowerType.MINIMAX, 1 },
        { TowerType.BELLARMINE_STUDENT_AI, 1 }, { TowerType.PERPLEXITY, 2 }, { TowerType.DEEPSEEK, 3 }, { TowerType.COHERE, 3 },
        { TowerType.GEMINI, 3 }, { TowerType.SORA, 2 }, { TowerType.GROK, 1 }, { TowerType.DALLE, 1 }, { TowerType.BELLARMINE_STUDENT, 1 },
        { TowerType.DEEPSEEK, 6 }, { TowerType.COHERE, 6 }, { TowerType.GLM, 3 }, { TowerType.SUNO, 3 }, { TowerType.QWEN, 4 },
        { TowerType.BRAD_LINDEMANN, 2 }, { TowerType.PERPLEXITY, 4 }, { TowerType.DEEPSEEK, 12 }, { TowerType.COHERE, 12 }, { TowerType.GEMINI, 8 },
        { TowerType.SORA, 5 }, { TowerType.GLM, 6 }, { TowerType.KIMI, 4 }, { TowerType.MINIMAX, 4 }, { TowerType.SUNO, 6 }, { TowerType.DEEPSEEK, 30 },
    };

    public static void main(String[] args) {
        int maxWaves = args.length > 0 ? Integer.parseInt(args[0]) : 100;
        int only = args.length > 1 ? Integer.parseInt(args[1]) : -1;
        for (int m = 0; m < MapCatalog.MAP_COUNT; m++) if (only < 0 || only == m) play(m, maxWaves, args.length > 2);
    }

    /** Difficulty from {@code -Dbot.difficulty=O1|ON|ON2|ONN} (falls back to -Dlindefense.difficulty, then O(1)). */
    private static Difficulty botDifficulty() {
        Difficulty d = Difficulty.parse(System.getProperty("bot.difficulty"));
        return d != null ? d : Difficulty.defaultMode();
    }

    static void play(int mapIdx, int maxWaves, boolean verbose) {
        Difficulty diff = botDifficulty();
        Game g = new Game(MapCatalog.create(mapIdx), Long.getLong("bot.seed", 1234L), diff);
        GameMap map = g.getMap();
        List<Vec> samples = new ArrayList<>();
        for (Path r : map.getRoutes()) for (double d = 0; d < r.getTotalLength(); d += 20) samples.add(r.positionAt(d));
        List<Vec> spots = new ArrayList<>();
        for (int x = 30; x < GameMap.WIDTH; x += 15) for (int y = 30; y < GameMap.HEIGHT; y += 15) {
            Vec p = new Vec(x, y);
            if (map.isBuildable(p, 20)) spots.add(p);
        }
        long t0 = System.currentTimeMillis();
        int ticks = 0;
        int lastReport = 0;
        StringBuilder sb = new StringBuilder();
        while (g.getState() == Game.State.PLAYING && ticks < 60 * 60 * Integer.getInteger("bot.maxMinutes", 90)) {
            if (ticks % 30 == 0) manage(g, spots, samples);
            if (g.getWaveManager().canStartNextWave() && g.getWave() < maxWaves) {
                // short prep: wait until projectiles clear, then go
                g.startNextWave();
            } else if (g.getWaveManager().canStartNextWave()) {
                break;
            }
            g.step(Game.DT);
            g.getEvents().clear();
            ticks++;
            if (g.getLastCompletedWave() != lastReport && g.getLastCompletedWave() % Integer.getInteger("bot.reportEvery", 10) == 0) {
                lastReport = g.getLastCompletedWave();
                sb.append(String.format("    after wave %3d: lives %3d cash %7.0f towers %2d (T4: %d)%n", lastReport, g.getLives(), g.getCash(),
                    g.getTowerManager().getPlayerTowers().size(), countT3(g)));
            }
        }
        System.out.printf("[%s] ", diff.label);
        System.out.printf("%s: state=%s reached wave %d (completed %d), lives=%d, cash=%.0f, towers=%d, sim %.0fs, real %dms%n",
            map.getName(), g.getState(), g.getWave(), g.getLastCompletedWave(), g.getLives(), g.getCash(),
            g.getTowerManager().getPlayerTowers().size(), g.getTime(), System.currentTimeMillis() - t0);
        System.out.print(sb);
        if (g.getState() == Game.State.PLAYING) {
            System.out.println("    still playing: toSpawn=" + g.getWaveManager().getRemainingToSpawn() + " alive=" + g.getEnemyManager().getAliveCount()
                + " projectiles=" + g.getCombat().getProjectiles().size());
            for (lindefense.combat.Projectile pr : g.getCombat().getProjectiles()) System.out.println("      " + pr.getSource().getType() + " pos=" + pr.getPosition() + " vel=" + pr.getVelocity() + " target=" + pr.getTarget());
        }
        if (!g.getEnemyManager().getEnemies().isEmpty()) {
            System.out.println("    remaining enemies (" + g.getEnemyManager().getEnemies().size() + "), wave time " + g.getWaveManager().getWaveTime() + ":");
            int shown = 0;
            for (lindefense.enemy.Enemy e : g.getEnemyManager().getEnemies()) {
                if (shown++ > 12) break;
                System.out.println("      " + e + " route=" + e.getRoute().getName() + " stored=" + e.getStoredEnemies().size()
                    + " stunned=" + e.isStunned() + " slow=" + e.getSlowFraction() + " speed=" + e.currentSpeed(1) + " pos=" + e.getPosition());
            }
        }
        if (verbose) for (String l : g.getLog()) System.out.println("      " + l);
    }

    private static int countT3(Game g) {
        int n = 0;
        for (Tower t : g.getTowerManager().getPlayerTowers()) if (t.isSuperclass()) n++;
        return n;
    }

    private static void manage(Game g, List<Vec> spots, List<Vec> samples) {
        // abilities when bosses or crowds are present
        int alive = g.getEnemyManager().getEnemies().size();
        boolean boss = !g.getActiveBosses().isEmpty();
        if (boss || alive > 25) {
            for (Tower t : new ArrayList<>(g.getTowerManager().getTowers())) if (t.isAbilityReady() && !t.isTemporary()) g.activateAbility(t);
        }
        // decide next purchase
        TowerType want = null;
        for (Object[] o : ORDER) {
            TowerType tt = (TowerType) o[0];
            int max = (Integer) o[1];
            int have = 0;
            for (Tower t : g.getTowerManager().getPlayerTowers()) if (t.getType() == tt) have++;
            if (have < max) { want = tt; break; }
        }
        // prefer upgrading the cheapest available upgrade when it is affordable and cheaper than next tower
        Tower bestUp = null; int bestCost = Integer.MAX_VALUE;
        for (Tower t : g.getTowerManager().getPlayerTowers()) {
            if (t.canUpgrade() && t.nextUpgrade().cost < bestCost) { bestCost = t.nextUpgrade().cost; bestUp = t; }
        }
        int wantCost = want == null ? Integer.MAX_VALUE : TowerCatalog.get(want).baseCost;
        int towers = g.getTowerManager().getPlayerTowers().size();
        int cap = Integer.getInteger("bot.maxTowers", 999);
        if (towers >= cap) want = null;
        boolean upgradeFirst = bestUp != null && (bestCost <= wantCost || towers >= 6 + g.getWave() / 8);
        if (upgradeFirst && g.getCash() >= bestCost) { g.upgradeTower(bestUp); return; }
        if (want != null && g.getCash() >= wantCost) {
            Vec p = bestSpot(g, want, spots, samples);
            if (p != null) g.placeTower(want, p);
        }
    }

    private static Vec bestSpot(Game g, TowerType type, List<Vec> spots, List<Vec> samples) {
        double range = TowerCatalog.get(type).baseStats.range;
        Vec best = null; double bestScore = -1;
        if (Boolean.getBoolean("bot.random")) {
            // human-ish: a random buildable spot that is at least within range of the track
            for (int tries = 0; tries < 200; tries++) {
                Vec p = spots.get(g.getRng().nextInt(spots.size()));
                if (!g.getTowerManager().canPlace(type, p)) continue;
                int cover = 0;
                for (Vec s : samples) if (s.distSq(p) <= range * range) cover++;
                if (cover >= 3) return p;
            }
            return null;
        }
        for (int i = 0; i < spots.size(); i += 3) {
            Vec p = spots.get(i);
            if (!g.getTowerManager().canPlace(type, p)) continue;
            double score = 0;
            for (Vec s : samples) if (s.distSq(p) <= range * range) score += 1;
            // support towers want neighbours
            if (TowerCatalog.get(type).baseStats.auraRange > 0) {
                for (Tower t : g.getTowerManager().getPlayerTowers()) if (t.getPosition().dist(p) <= TowerCatalog.get(type).baseStats.auraRange) score += 15;
            } else {
                for (Tower t : g.getTowerManager().getPlayerTowers()) if (t.getStats().auraRange > 0 && t.getPosition().dist(p) <= t.getStats().auraRange) score += 10;
            }
            if (score > bestScore) { bestScore = score; best = p; }
        }
        return best;
    }
}
