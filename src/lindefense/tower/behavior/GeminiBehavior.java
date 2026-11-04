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
 * Gemini (spec 3.2 #4). Instant laser, ricochet (U1), camo detection and camo/regrow stripping (U2) and the
 * 8-enemy chain with x3 vs MOAB-class (U3) are all data-driven (INSTANT + chainHops + HitSpec flags).
 * Argon Singularity is implemented here: every enemy within the radius is pulled to the vortex centre and held
 * for the hold time, then takes the damage (bosses are not held but take the boss damage).
 */
public final class GeminiBehavior extends TowerBehavior {
    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        List<Enemy> caught = enemiesWithin(game, t.getPosition(), a.p("radius"));
        if (caught.isEmpty()) return false;
        // the vortex forms on the track at the caught enemy nearest to Gemini
        Enemy centre = caught.get(0);
        for (Enemy e : caught) if (e.getPosition().dist(t.getPosition()) < centre.getPosition().dist(t.getPosition())) centre = e;
        double hold = a.p("holdTime");
        int rgb = t.getType().primaryRgb;
        game.getCombat().emit(VisualEvent.ability(centre.getPosition(), a.p("radius"), rgb));
        final List<Enemy> victims = new ArrayList<>(caught);
        for (Enemy e : victims) {
            if (!e.isBoss()) e.hold(hold, centre.getDistance(), a.p("pullSpeed"));
            game.getCombat().emit(VisualEvent.beam(t.getPosition(), e.getPosition(), rgb));
        }
        final double damage = a.p("damage"), bossDamage = a.p("bossDamage");
        game.getEffects().schedule(hold, () -> {
            for (Enemy e : victims) {
                if (!e.isAlive() || e.isStored()) continue;
                HitSpec h = new HitSpec(e.isBoss() ? bossDamage : damage, DamageType.ENERGY);
                game.getCombat().applyHit(e, h, t, 1, 0);
                game.getCombat().emit(VisualEvent.explosion(e.getPosition(), 30, rgb));
            }
        });
        return true;
    }
}
