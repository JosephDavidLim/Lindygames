package lindefense.tower.behavior;

import lindefense.combat.AttackContext;
import lindefense.combat.Combat;
import lindefense.combat.Projectile;
import lindefense.combat.VisualEvent;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * Zethos, the Quantum Architect (Primary tower, four upgrades). Each quantum projectile:
 * <ol>
 *   <li>homes on its target and hits it at full damage;</li>
 *   <li>at every impact (Tier 2+) explodes for {@code splashPct} of that impact's damage within the splash radius;</li>
 *   <li>(Tier 3+) bounces to up to {@code bounces} additional enemies, each the nearest eligible enemy within
 *       {@code bounceRange}, dealing {@code bouncePct} of the damage (75% at Tier 3, 100% at Tier 4); bounce hits do
 *       not use up pierce;</li>
 *   <li>then flies straight on, hitting further enemies at full damage until {@code pierce} direct hits are spent, so
 *       one projectile can hit up to {@code pierce + bounces} enemies directly.</li>
 * </ol>
 * Every enemy damaged by a projectile, directly or by its splash, is recorded in the projectile's hit set and is
 * never damaged again by that projectile (it is skipped by later collisions, splashes and bounce choices).
 * Reality Collapse (self buff): x2 damage, x2 attack speed (interval halved), +{@code rangeBonus} range, 8 bounces and
 * an explosion on every impact (tiers without one use the {@code fallback*} values).
 */
public final class ZethosBehavior extends TowerBehavior {
    /** Self-buff key while Reality Collapse is active. */
    public static final String COLLAPSE = "realityCollapse";

    public static boolean isCollapsing(Tower t) { return t.hasSelfBuff(COLLAPSE); }

    @Override public double flatRangeBonus(Tower t) {
        return isCollapsing(t) ? t.getAbility().p("rangeBonus") : 0;
    }

    @Override public void modifyAttack(Game game, Tower t, AttackContext c) {
        TowerStats s = t.getStats();
        AbilityDef a = t.getAbility();
        double splash = s.splashRadius, splashPct = s.p("splashPct", 0), bouncePct = s.p("bouncePct", 0);
        int bounces = s.bounces;
        if (isCollapsing(t)) {
            c.damageMult *= a.p("damageMult");
            bounces = (int) a.p("bounces");
            if (splash <= 0 || splashPct <= 0) { splash = a.p("fallbackSplash"); splashPct = a.p("fallbackSplashPct"); }
            if (bouncePct <= 0) bouncePct = a.p("fallbackBouncePct");
            c.tag = "collapse";
        }
        c.bounces = bounces;
        c.splash = splash;
        c.onImpact = new QuantumImpact(splash, splashPct, bouncePct);
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        t.addSelfBuff(COLLAPSE, a.p("rateMult"), 1, a.p("duration"));
        return true;
    }

    /** The per-shot collision logic, with the splash / bounce numbers frozen when the shot was fired. */
    static final class QuantumImpact implements Projectile.ImpactHandler {
        final double splash, splashPct, bouncePct;
        QuantumImpact(double splash, double splashPct, double bouncePct) { this.splash = splash; this.splashPct = splashPct; this.bouncePct = bouncePct; }

        @Override public void impact(Game game, Projectile p, Enemy e) {
            Combat cb = game.getCombat();
            Projectile.Shot sh = p.getShot();
            boolean bounceHit = p.handlerMark == 1;
            p.handlerMark = 0;
            double scale = bounceHit ? bouncePct : 1;
            Vec at = e.getPosition();
            cb.projectileHit(p, e, scale);
            if (splash > 0 && splashPct > 0) {
                // exclude AND record with the projectile's own hit set: nobody is damaged twice by one projectile
                cb.areaHits(at, splash, sh.hit, sh.source, sh.damageMult * scale * splashPct, sh.extraDamage,
                    p.getHitIds(), p.getHitIds(), sh.detection, sh.buffed, p);
                cb.emit(VisualEvent.explosion(at, splash, sh.source.getType().secondaryRgb));
            }
            boolean pierceSpent = bounceHit ? p.getPierceLeft() <= 0 : p.consumePierce();
            if (p.getBouncesLeft() > 0) {
                Enemy next = cb.nearestEnemy(at, sh.bounceRange, p.getHitIds(), sh.detection);
                if (next != null) {
                    p.consumeBounce();
                    p.resetTravel();
                    p.retarget(next);
                    p.handlerMark = 1;
                    cb.emit(VisualEvent.chain(at, next.getPosition(), sh.source.getType().primaryRgb));
                    return;
                }
            }
            if (!pierceSpent) { p.retarget(null); return; }   // pierce left: fly straight on
            p.kill();
        }
    }
}
