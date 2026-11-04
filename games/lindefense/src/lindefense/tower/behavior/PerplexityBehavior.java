package lindefense.tower.behavior;

import java.util.List;
import lindefense.combat.HitContext;
import lindefense.combat.HitSpec;
import lindefense.combat.VisualEvent;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * Perplexity (spec 3.5 #13). Fully data-driven except two small pieces implemented here: the radar pulse (the
 * CUSTOM attack: pulseDamage to every Camo enemy in range) and Deep Research Scrape. Detection aura, map-wide camo
 * reveal (U1), the +20% mark / +15% range / Surveillance immunity (U2) and the round income ($40 -> $250, via the
 * default {@link #onRoundEnd}) come from the stats.
 */
public final class PerplexityBehavior extends TowerBehavior {
    @Override public boolean attack(Game game, Tower t) {
        TowerStats s = t.getStats();
        double range = t.effectiveRange(game);
        List<Enemy> camo = new java.util.ArrayList<>();
        for (Enemy e : enemiesWithin(game, t.getPosition(), range)) if (e.isCamo()) camo.add(e);
        if (camo.isEmpty()) return false;
        HitSpec h = new HitSpec(s.p("pulseDamage"), s.hit.type);
        for (Enemy e : camo) {
            HitContext c = new HitContext(e, h, t);
            c.fromAttack = true;
            game.getCombat().applyHit(c);
        }
        game.getCombat().emit(VisualEvent.ability(t.getPosition(), range, t.getType().primaryRgb));
        t.onAttacked(game);
        return true;
    }

    @Override public boolean activateAbility(Game game, Tower t, Vec target) {
        AbilityDef a = t.getAbility();
        double cash = Math.min(a.p("maxCash"), Math.floor(game.getRoundPopValue() * a.p("popValuePct")));
        if (cash > 0) game.addCash(cash, t.getPosition());
        game.getCombat().emit(VisualEvent.text(t.getPosition(), "+$" + (int) cash, 0xffd166));
        return true;
    }
}
