package lindefense.tower.behavior;

import java.util.HashSet;
import java.util.Set;
import lindefense.combat.AttackContext;
import lindefense.combat.Combat;
import lindefense.combat.DamageType;
import lindefense.combat.HitContext;
import lindefense.combat.HitSpec;
import lindefense.combat.Projectile;
import lindefense.combat.VisualEvent;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * Brad Lindemann, Legendary Area-Damage Commander. Command Lightning: a large energy shell flies at the strongest
 * enemy in range; on impact it deals full damage to the enemy it struck and every enemy within the splash radius,
 * then lightning chains from that enemy to up to {@code chainHops} more enemies, each the nearest not-yet-chained
 * enemy within {@code chainRange} of the previous one, for {@code chainDamage}. Enemies caught in the explosion can
 * still be chained; the chain never hits the same enemy twice in one attack. Supreme Command (a self buff) doubles
 * the primary damage, multiplies the splash radius by 1.5 and raises the chain to 10 targets at 35 damage.
 */
public final class BradLindemannBehavior extends TowerBehavior {
    /** Self-buff key while Supreme Command is active. */
    public static final String SUPREME = "supremeCommand";
    /** Lightning colour. */
    public static final int LIGHTNING_RGB = 0xfff3a0;

    @Override public void modifyAttack(Game game, Tower t, AttackContext c) {
        if (isSupreme(t)) {
            c.splash = t.getStats().splashRadius * t.getAbility().p("splashMult");
            c.tag = "supreme";
        }
        c.onImpact = (g, p, e) -> impact(g, p, e);
    }

    public static boolean isSupreme(Tower t) { return t.hasSelfBuff(SUPREME); }

    /** Current splash radius (px), including Supreme Command. */
    public static double splashRadius(Tower t) {
        return t.getStats().splashRadius * (isSupreme(t) ? t.getAbility().p("splashMult") : 1);
    }

    private static void impact(Game game, Projectile p, Enemy struck) {
        Projectile.Shot sh = p.getShot();
        Tower t = sh.source;
        TowerStats s = t.getStats();
        AbilityDef a = t.getAbility();
        boolean sup = isSupreme(t);
        Combat cb = game.getCombat();
        Vec at = struck.getPosition();
        double radius = splashRadius(t);
        double mult = sh.damageMult * (sup ? a.p("damageMult") : 1);
        // 1. explosion: the struck enemy and everything in the radius take the full primary damage
        cb.areaHits(at, radius, sh.hit, t, mult, sh.extraDamage, null, null, sh.detection, sh.buffed, p);
        cb.emit(VisualEvent.explosion(at, radius, t.getType().primaryRgb));
        cb.emit(VisualEvent.ability(at, radius * 1.25, LIGHTNING_RGB));
        // 2. chain lightning from the struck enemy, nearest hops, never the same enemy twice
        int links = sup ? (int) a.p("chainTargets") : s.chainHops;
        HitSpec chain = new HitSpec(sup ? a.p("chainDamage") : s.p("chainDamage"), DamageType.ENERGY);
        Set<Integer> chained = new HashSet<>();
        chained.add(struck.getId());
        Vec cur = at;
        for (int i = 0; i < links; i++) {
            Enemy next = cb.nearestEnemy(cur, s.chainRange, chained, sh.detection);
            if (next == null) break;
            chained.add(next.getId());
            Vec np = next.getPosition();
            cb.emit(VisualEvent.chain(cur, np, LIGHTNING_RGB));
            HitContext h = new HitContext(next, chain, t);
            h.mult = sh.damageMult; h.fromAttack = sh.buffed; h.detection = sh.detection; h.projectile = p; h.tag = "chain";
            cb.applyHit(h);
            cur = np;
        }
        p.kill();
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        t.addSelfBuff(SUPREME, 1, 1, a.p("duration"));
        return true;
    }
}
