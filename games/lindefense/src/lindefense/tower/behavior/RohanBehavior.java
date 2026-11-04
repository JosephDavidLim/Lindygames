package lindefense.tower.behavior;

import java.util.ArrayList;
import java.util.List;
import lindefense.combat.AttackContext;
import lindefense.combat.Combat;
import lindefense.combat.DamageType;
import lindefense.combat.HitContext;
import lindefense.combat.HitSpec;
import lindefense.combat.Projectile;
import lindefense.combat.VisualEvent;
import lindefense.enemy.Enemy;
import lindefense.enemy.StatusEffect;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * Rohan, Defensive Crowd-Control Specialist. Cryo Blast: an icy shell explodes on impact, damaging every enemy within
 * the splash radius, slowing each by {@code slowPct} for {@code slowDuration} and freezing each non-boss with
 * probability {@code freezeChance} for {@code freezeDuration} (rolled per enemy). Bosses are never frozen and receive
 * exactly {@code bossSlowPct} slow instead. Absolute Zero hits every enemy on the map: damage first, then a freeze
 * (non-bosses) or an exact 50% slow (bosses). Frozen enemies follow the game's existing FREEZE rule (cannot move and
 * take x2 damage while frozen); Palantir's phase-1 control immunity still applies.
 */
public final class RohanBehavior extends TowerBehavior {
    public static final int ICE_RGB = 0xbff6ff;

    @Override public void modifyAttack(Game game, Tower t, AttackContext c) {
        c.onImpact = (g, p, e) -> blast(g, p, e);
    }

    private static void blast(Game game, Projectile p, Enemy struck) {
        Projectile.Shot sh = p.getShot();
        Tower t = sh.source;
        TowerStats s = t.getStats();
        Vec at = struck.getPosition();
        Combat cb = game.getCombat();
        List<HitContext> hits = cb.areaHits(at, s.splashRadius, sh.hit, t, sh.damageMult, sh.extraDamage, null, null, sh.detection, sh.buffed, p);
        for (HitContext h : hits) {
            Enemy e = h.target;
            if (h.immune || !e.isAlive()) continue;
            if (e.isBoss()) {
                e.applyExactSlow(s.p("bossSlowPct"), s.p("slowDuration"));
            } else {
                e.applyStatus(StatusEffect.Kind.SLOW, s.p("slowPct"), s.p("slowDuration"));
                if (game.getRng().chance(s.p("freezeChance"))) e.applyStatus(StatusEffect.Kind.FREEZE, 1, s.p("freezeDuration"));
            }
        }
        cb.emit(VisualEvent.explosion(at, s.splashRadius, ICE_RGB));
        p.kill();
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        List<Enemy> snapshot = new ArrayList<>();
        for (Enemy e : game.getEnemyManager().getEnemies()) if (e.isAlive() && !e.isStored()) snapshot.add(e);
        if (snapshot.isEmpty()) return false;
        HitSpec h = new HitSpec(a.p("damage"), DamageType.ENERGY);
        Combat cb = game.getCombat();
        double mult = t.damageMultiplier(game);
        for (Enemy e : snapshot) {
            if (!e.isAlive() || e.isStored()) continue;
            HitContext c = new HitContext(e, h, t);
            c.mult = mult; c.tag = "absoluteZero";
            cb.applyHit(c);   // damage before the freeze, so the freeze's x2 does not apply to the ability's own hit
            if (!e.isAlive() || c.immune) continue;
            if (e.isBoss()) e.applyExactSlow(a.p("bossSlowPct"), a.p("bossSlowDuration"));
            else e.applyStatus(StatusEffect.Kind.FREEZE, 1, a.p("freezeDuration"));
        }
        return true;
    }
}
