package lindefense.tower.behavior;

import java.util.List;
import lindefense.combat.HitContext;
import lindefense.combat.HitSpec;
import lindefense.combat.VisualEvent;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyType;
import lindefense.map.Path;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * Cohere (spec 3.6 #17). Heavy artillery shells (splash), Formation Breaker (ARMOR CRACK + 50% mark, U2) and
 * Supreme Command (U3) are data-driven. Fragmentation (U1) and Command Override are implemented here.
 *
 * <p>Fragmentation is resolved as instant radial rays rather than projectile entities (so a heavy shell never
 * spawns eight more objects): each of the {@code shrapnelCount} fragments flies {@code shrapnelRange} px from the
 * impact and hits up to {@code shrapnelPierce} enemies along its line. The enemy the shell struck first is not
 * hit by the shrapnel (it already took the shell).</p>
 */
public final class CohereBehavior extends TowerBehavior {
    private static final String LAST_SHELL = "fragShell";

    @Override public void onHit(Game game, Tower t, HitContext c) {
        TowerStats s = t.getStats();
        if (c.projectile == null || !s.has("shrapnelCount")) return;
        // one burst per shell: the splash calls onHit once per victim, all with the same projectile
        if (t.state().get(LAST_SHELL) == c.projectile) return;
        t.state().put(LAST_SHELL, c.projectile);
        fragment(game, t, s, c.projectile.getPosition(), c.target);
    }

    private static void fragment(Game game, Tower t, TowerStats s, Vec impact, Enemy struck) {
        int n = s.pi("shrapnelCount"), pierce = Math.max(1, s.pi("shrapnelPierce"));
        double range = s.p("shrapnelRange"), width = s.p("shrapnelWidth");
        HitSpec h = new HitSpec(s.p("shrapnelDamage"), s.hit.type);
        double mult = t.damageMultiplier(game);
        boolean sees = t.canSeeCamo();
        double rot = game.getRng().range(0, Math.PI * 2 / n);
        int rgb = t.getType().secondaryRgb;
        List<Enemy> list = new java.util.ArrayList<>();
        Enemy[] done = new Enemy[pierce];
        for (int k = 0; k < n; k++) {
            double ang = rot + k * Math.PI * 2 / n;
            Vec end = new Vec(impact.x + Math.cos(ang) * range, impact.y + Math.sin(ang) * range);
            java.util.Arrays.fill(done, null);
            // only the enemies near this shard's line (grid box around the segment, widened by the beam width)
            game.getEnemyManager().getGrid().box(Math.min(impact.x, end.x) - width, Math.min(impact.y, end.y) - width,
                Math.max(impact.x, end.x) + width, Math.max(impact.y, end.y) + width, list);
            int hits = 0;
            for (int pass = 0; pass < pierce; pass++) {
                Enemy best = null;
                double bestD = Double.MAX_VALUE;
                for (int i = 0; i < list.size(); i++) {
                    Enemy e = list.get(i);
                    if (e == struck || !e.isTargetableBy(sees)) continue;
                    if (Path.pointSegmentDistance(e.getPosition(), impact, end) > e.getRadius() + width) continue;
                    boolean seen = false;
                    for (int j = 0; j < hits; j++) if (done[j] == e) { seen = true; break; }
                    if (seen) continue;
                    double d = e.getPosition().distSq(impact);
                    if (d < bestD) { bestD = d; best = e; }
                }
                if (best == null) break;
                done[hits++] = best;
                game.getCombat().applyHit(best, h, t, mult, 0);
            }
            game.getCombat().emit(VisualEvent.beam(impact, end, rgb));
        }
    }

    /** Command Override: the {@code count} nearest non-boss enemies (Bellarmine College Prep allowed) walk backwards for 8 s. */
    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        int count = (int) a.p("count");
        Enemy[] pick = new Enemy[count];
        double[] dist = new double[count];
        int found = 0;
        Vec tp = t.getPosition();
        List<Enemy> list = game.getEnemyManager().getEnemies();
        for (int i = 0; i < list.size(); i++) {
            Enemy e = list.get(i);
            if (!e.isAlive() || e.isStored()) continue;
            if (e.isBoss() && e.getType() != EnemyType.BELLARMINE_COLLEGE_PREP) continue;
            double d = e.getPosition().distSq(tp);
            if (found >= count && d >= dist[count - 1]) continue;
            if (found < count) found++;
            int at = found - 1;
            while (at > 0 && dist[at - 1] > d) { dist[at] = dist[at - 1]; pick[at] = pick[at - 1]; at--; }
            dist[at] = d; pick[at] = e;
        }
        if (found == 0) return false;
        for (int i = 0; i < found; i++) {
            pick[i].startReverse(a.p("duration"), a.p("contactDps"), t);
            game.getCombat().emit(VisualEvent.text(pick[i].getPosition(), "Override", t.getType().primaryRgb));
        }
        return true;
    }
}
