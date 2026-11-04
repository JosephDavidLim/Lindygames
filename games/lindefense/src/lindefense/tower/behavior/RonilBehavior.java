package lindefense.tower.behavior;

import java.util.ArrayList;
import java.util.List;
import lindefense.combat.DamageType;
import lindefense.combat.HitSpec;
import lindefense.combat.VisualEvent;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;

/**
 * Ronil, Explosive Artillery Specialist. Meteor Strike (the long-range, slow, full-damage splash projectile) is
 * data-driven. Meteor Shower launches {@code meteors} meteors evenly over {@code duration} seconds; each one picks a
 * random enemy location anywhere on the map that is outside the blast radius of every earlier impact of the same
 * shower (falling back to any enemy location only when no fresh spot exists), falls for {@code fallTime} seconds and
 * deals {@code damage} to every enemy within {@code splash}.
 */
public final class RonilBehavior extends TowerBehavior {
    /** One incoming meteor (read by the renderer). */
    public static final class Meteor {
        public final Vec at; public final double launched, fall, radius;
        Meteor(Vec at, double launched, double fall, double radius) { this.at = at; this.launched = launched; this.fall = fall; this.radius = radius; }
    }

    private static final String METEORS = "meteors";

    /** Meteors currently falling for this tower. */
    @SuppressWarnings("unchecked")
    public static List<Meteor> meteors(Tower t) {
        return (List<Meteor>) t.state().computeIfAbsent(METEORS, k -> new ArrayList<Meteor>());
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        if (candidates(game, t).isEmpty()) return false;
        int n = (int) a.p("meteors");
        double gap = a.p("duration") / n;
        List<Vec> used = new ArrayList<>();
        for (int i = 0; i < n; i++) game.getEffects().schedule(i * gap, () -> launch(game, t, used));
        return true;
    }

    private static List<Enemy> candidates(Game game, Tower t) {
        List<Enemy> out = new ArrayList<>();
        for (Enemy e : game.getEnemyManager().getEnemies()) {
            if (e.isAlive() && !e.isStored() && e.isTargetableBy(t.canSeeCamo())) out.add(e);
        }
        return out;
    }

    /** A random enemy position away from every earlier impact point, or any enemy position if none is fresh. */
    static Vec pickSpot(Game game, Tower t, List<Vec> used, double radius) {
        List<Enemy> all = candidates(game, t);
        if (all.isEmpty()) return null;
        List<Enemy> fresh = new ArrayList<>();
        for (Enemy e : all) {
            boolean ok = true;
            for (Vec u : used) if (e.getPosition().withinDist(u, radius)) { ok = false; break; }
            if (ok) fresh.add(e);
        }
        List<Enemy> pool = fresh.isEmpty() ? all : fresh;
        return pool.get(game.getRng().nextInt(pool.size())).getPosition();
    }

    private static void launch(Game game, Tower t, List<Vec> used) {
        if (!t.isAlive()) return;
        AbilityDef a = t.getAbility();
        double radius = a.p("splash");
        Vec at = pickSpot(game, t, used, radius);
        if (at == null) return;
        used.add(at);
        Meteor m = new Meteor(at, game.getTime(), a.p("fallTime"), radius);
        meteors(t).add(m);
        game.getEffects().schedule(m.fall, () -> {
            meteors(t).remove(m);
            if (!t.isAlive()) return;
            HitSpec h = new HitSpec(a.p("damage"), DamageType.EXPLOSIVE);
            game.getCombat().areaHit(at, radius, h, t, t.damageMultiplier(game), 0, 999, null, t.canSeeCamo(), false, null);
            game.getCombat().emit(VisualEvent.explosion(at, radius, t.getType().primaryRgb));
            game.getCombat().emit(VisualEvent.ability(at, radius * 1.3, 0xffd166));
        });
    }
}
