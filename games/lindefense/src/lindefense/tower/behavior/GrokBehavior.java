package lindefense.tower.behavior;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lindefense.combat.AttackContext;
import lindefense.combat.Clusters;
import lindefense.combat.HitContext;
import lindefense.combat.HitSpec;
import lindefense.combat.Projectile;
import lindefense.effects.Barrage;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.BuffStat;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * Grok (spec 3.5 #12). Thermal micro-missiles (splash, EXPLOSIVE, built-in camo detection) and the burn (U1) are data-driven;
 * Densest Cluster targeting unlocks at U2 ({@link #allowsDensestCluster}).
 * <ul>
 *   <li>U1: a burn applied by a hit spreads {@code burnSpreads} time(s) (hop count; 1 = once) to enemies within
 *       {@code burnSpreadRadius} of the directly hit enemy. Spread burns never spread further.</li>
 *   <li>U2 Paradox Trap: every {@code volleyInterval} s a volley of {@code volleyMissiles} explosions ({@code volleyDamage}
 *       each, splash = the tower's splash) lands on the densest cluster among targetable enemies in range.</li>
 *   <li>U3 Chaos Mode: attacks cycle incendiary (keeps the burn), cluster (no burn; the impact releases
 *       {@code chaosSplit} fragments at the nearest enemies within {@code chaosSplitRange}) and heavy (no burn;
 *       x{@code chaosHeavyMoabMult} vs MOAB-class). Fragments cannot split again.</li>
 * </ul>
 */
public final class GrokBehavior extends TowerBehavior {
    private static final String TAG_CLUSTER = "cluster";
    private static final String TAG_FRAGMENT = "fragment";
    private static final String TAG_HEAVY = "heavy";

    @Override public boolean allowsDensestCluster(Tower t) { return t.getStats().has("allowDensest"); }

    @Override public void update(Game game, Tower t, double dt) {
        TowerStats s = t.getStats();
        if (s.has("volleyInterval") && t.periodic("volley", s.p("volleyInterval"), dt)) volley(game, t, s);
    }

    private void volley(Game game, Tower t, TowerStats s) {
        List<Enemy> inRange = new ArrayList<>();
        for (Enemy e : enemiesWithin(game, t.getPosition(), t.effectiveRange(game))) {
            if (e.isTargetableBy(t.canSeeCamo())) inRange.add(e);
        }
        Enemy centre = Clusters.densest(inRange, game.getEnemyManager().getEnemies());
        if (centre == null) return;
        Barrage b = new Barrage(t, new HitSpec(s.p("volleyDamage"), s.hit.type), s.pi("volleyMissiles"), s.splashRadius);
        b.aim = Barrage.Aim.POINT;
        b.point = centre.getPosition();
        game.getEffects().fire(b);
    }

    @Override public void modifyAttack(Game game, Tower t, AttackContext c) {
        TowerStats s = t.getStats();
        if (!s.has("chaosCycle")) return;
        int kind = (int) t.addCounter("chaos", 1) - 1;
        kind %= s.pi("chaosCycle");
        if (kind == 0) { c.tag = "incendiary"; return; }
        HitSpec h = c.editHit();
        h.dotDps = 0;
        h.dotDuration = 0;
        if (kind == 1) c.tag = TAG_CLUSTER;
        else { c.tag = TAG_HEAVY; h.multVsBoss *= s.p("chaosHeavyMoabMult"); }
    }

    @Override public void onHit(Game game, Tower t, HitContext c) {
        if (c.immune) return;
        TowerStats s = t.getStats();
        if (c.hit().dotDps > 0 && s.has("burnSpreadRadius") && c.target.isAlive()) spreadBurn(game, t, c, s);
        Projectile p = c.projectile;
        if (p != null && TAG_CLUSTER.equals(p.getShot().tag) && t.state().get("lastSplit") != p) {
            t.state().put("lastSplit", p);
            split(game, t, c, p, s);
        }
    }

    private void spreadBurn(Game game, Tower t, HitContext c, TowerStats s) {
        Set<Integer> seen = new HashSet<>();
        seen.add(c.target.getId());
        List<Enemy> frontier = new ArrayList<>();
        frontier.add(c.target);
        for (int hop = 0; hop < s.pi("burnSpreads"); hop++) {
            List<Enemy> next = new ArrayList<>();
            for (Enemy from : frontier) {
                for (Enemy e : enemiesWithin(game, from.getPosition(), s.p("burnSpreadRadius"))) {
                    if (!seen.add(e.getId()) || !e.isTargetableBy(t.canSeeCamo())) continue;
                    e.applyBurn(c.hit().dotDps, c.hit().dotDuration, t);
                    next.add(e);
                }
            }
            frontier = next;
        }
    }

    private void split(Game game, Tower t, HitContext c, Projectile p, TowerStats s) {
        Vec at = p.getPosition();
        // enemies the blast itself reached are not fragment targets (they were already hit)
        Set<Integer> blasted = new HashSet<>();
        for (Enemy e : game.getEnemyManager().getGrid().near(at.x, at.y, p.getShot().splash, new ArrayList<>())) {
            if (e.isAlive() && !e.isStored() && e.getPosition().dist(at) <= p.getShot().splash + e.getRadius()) blasted.add(e.getId());
        }
        blasted.add(c.target.getId());
        List<Enemy> near = new ArrayList<>();
        for (Enemy e : enemiesWithin(game, at, s.p("chaosSplitRange"))) {
            if (!blasted.contains(e.getId()) && e.isTargetableBy(t.canSeeCamo())) near.add(e);
        }
        if (near.isEmpty()) return;
        near.sort((a, b) -> Double.compare(a.getPosition().distSq(at), b.getPosition().distSq(at)));
        for (int i = 0; i < s.pi("chaosSplit"); i++) {
            Projectile.Shot f = Projectile.Shot.of(game, t, at, near.get(i % near.size()));
            f.hit = p.getHit();
            f.splash = 0;
            f.pierce = 1;
            f.tag = TAG_FRAGMENT;
            game.getCombat().fire(f).getHitIds().addAll(blasted);
        }
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        for (Tower o : game.getTowerManager().getTowers()) {
            if (o.getPosition().dist(t.getPosition()) <= a.p("radius")) {
                o.addTimedBuff(BuffStat.BOSS_DAMAGE_PCT, t.getType(), a.p("bossDamagePct"), a.p("duration"));
            }
        }
        return true;
    }
}
