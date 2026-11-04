package lindefense.tower.behavior;

import java.util.HashMap;
import java.util.Map;
import lindefense.combat.Combat;
import lindefense.combat.DamageType;
import lindefense.combat.HitSpec;
import lindefense.combat.VisualEvent;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyGrid;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * GPT (spec 3.2 #6). Plasma orbs, splash (U1), burn (U2) and Adaptive Plasma's x2 / +2 / +2 / knockback (U3)
 * are data-driven via HitSpec. The two orbiting moons (U1) and the Astra Beam ability live here.
 */
public final class GptBehavior extends TowerBehavior {
    private static final String ANGLE = "moonAngle", BEAM_LEFT = "beamLeft", BEAM_ACC = "beamAcc", BEAM_TARGET = "beamTarget", MOON_CD = "moonCd";

    /** World position of moon {@code i} (for renderers and tests); null when the tower has no moons yet. */
    public static Vec moonPosition(Tower t, int i) {
        TowerStats s = t.getStats();
        if (!s.has("moonCount")) return null;
        double ang = t.counter(ANGLE) + Math.PI * 2 * i / s.p("moonCount");
        return t.getPosition().add(new Vec(Math.cos(ang), Math.sin(ang)).scale(s.p("moonRadius")));
    }

    /** True while the Astra Beam is firing. */
    public static boolean beamActive(Tower t) { return t.counter(BEAM_LEFT) > 0; }

    @Override public void update(Game game, Tower t, double dt) {
        updateMoons(game, t, dt);
        updateBeam(game, t, dt);
    }

    @SuppressWarnings("unchecked")
    private void updateMoons(Game game, Tower t, double dt) {
        TowerStats s = t.getStats();
        if (!s.has("moonCount")) return;
        t.addCounter(ANGLE, s.p("moonOrbitSpeed") * dt);
        Map<Long, Double> cd = (Map<Long, Double>) t.state().computeIfAbsent(MOON_CD, k -> new HashMap<Long, Double>());
        double now = game.getTime();
        HitSpec h = new HitSpec(s.p("moonDamage"), DamageType.ENERGY);
        int moons = s.pi("moonCount");
        for (int i = 0; i < moons; i++) {
            Vec m = moonPosition(t, i);
            EnemyGrid.Scan scan = game.getEnemyManager().getGrid().scan(m.x, m.y, s.p("moonSize"));
            try {
            for (Enemy e = scan.next(); e != null; e = scan.next()) {
                if (!e.isAlive() || e.isStored() || !e.isTargetableBy(t.canSeeCamo())) continue;
                if (e.getPosition().dist(m) > s.p("moonSize") + e.getRadius()) continue;
                long key = (long) i * 1_000_000L + e.getId();
                Double ready = cd.get(key);
                if (ready != null && ready > now) continue;
                cd.put(key, now + s.p("moonInterval"));
                game.getCombat().applyHit(e, h, t, 1, 0);
                game.getCombat().emit(VisualEvent.hit(m, t.getType().secondaryRgb));
            }
            } finally { scan.close(); }
        }
        cd.values().removeIf(r -> r <= now);
    }

    private void updateBeam(Game game, Tower t, double dt) {
        if (!beamActive(t)) return;
        AbilityDef a = t.getAbility();
        double step = Math.min(dt, t.counter(BEAM_LEFT));
        t.addCounter(BEAM_LEFT, -step);
        double acc = t.addCounter(BEAM_ACC, step);
        double tick = a.p("tick");
        while (acc >= tick - 1e-9) {
            acc -= tick;
            Enemy e = (Enemy) t.state().get(BEAM_TARGET);
            if (e == null || !e.isAlive() || e.isStored()) e = strongest(game, t);
            t.state().put(BEAM_TARGET, e);
            if (e == null) break;
            HitSpec h = new HitSpec(a.p("dps") * tick, DamageType.ENERGY);
            h.multVsBoss = a.p("bossMult");
            h.multVsFortified = Combat.CRACKED_FORTIFIED_MULT; // ignores Fortified: its extra HP does not count
            game.getCombat().applyHit(e, h, t, 1, 0);
            game.getCombat().emit(VisualEvent.beam(t.getPosition(), e.getPosition(), t.getType().primaryRgb));
        }
        t.setCounter(BEAM_ACC, acc);
        if (t.counter(BEAM_LEFT) <= 0) t.state().remove(BEAM_TARGET);
    }

    /** Strongest visible enemy anywhere on the track: most HP, ties to the one closest to the exit. */
    private static Enemy strongest(Game game, Tower t) {
        Enemy best = null;
        for (Enemy e : game.getEnemyManager().getEnemies()) {
            if (!e.isTargetableBy(t.canSeeCamo())) continue;
            if (best == null || e.getHp() > best.getHp()
                || (e.getHp() == best.getHp() && e.getRemainingDistance() < best.getRemainingDistance())) best = e;
        }
        return best;
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        Enemy e = strongest(game, t);
        if (e == null) return false;
        t.setCounter(BEAM_LEFT, a.p("duration"));
        t.setCounter(BEAM_ACC, 0);
        t.state().put(BEAM_TARGET, e);
        game.getCombat().emit(VisualEvent.ability(t.getPosition(), 40, t.getType().primaryRgb));
        return true;
    }
}
