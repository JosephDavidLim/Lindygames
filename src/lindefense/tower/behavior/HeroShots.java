package lindefense.tower.behavior;

import lindefense.combat.Projectile;

/** Shared projectile impact handlers for the Hero Store heroes. */
final class HeroShots {
    private HeroShots() { }

    /**
     * A bolt that homes on its target until the first hit, then keeps flying in a straight line, damaging each enemy
     * it passes through once until its pierce is spent (Brandon's Rapid Volley, Shashank's Data Drain).
     */
    static final Projectile.ImpactHandler STRAIGHT_PIERCE = (game, p, e) -> {
        game.getCombat().projectileHit(p, e, 1);
        if (p.consumePierce()) p.kill(); else p.retarget(null);
    };
}
