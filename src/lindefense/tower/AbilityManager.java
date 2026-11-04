package lindefense.tower;

import java.util.List;
import lindefense.combat.DamageType;
import lindefense.combat.HitSpec;
import lindefense.combat.VisualEvent;
import lindefense.enemy.Boss;
import lindefense.enemy.Enemy;
import lindefense.enemy.StatusEffect;
import lindefense.sim.Game;
import lindefense.sim.Vec;

/**
 * Executes tower active abilities and enforces cooldowns. Each
 * {@link AbilityDef.Effect} is implemented here using the shared combat engine.
 */
public class AbilityManager {
    private final Game game;

    public AbilityManager(Game game) { this.game = game; }

    public boolean canActivate(Tower t) {
        return t != null && t.isAlive() && !t.isTemporary() && t.getAbility() != null && t.isAbilityReady();
    }

    /** Activates the tower's ability if ready. Returns true on success. */
    public boolean activate(Tower t) {
        if (!canActivate(t)) return false;
        AbilityDef a = t.getAbility();
        Vec p = t.getPosition();
        double range = t.effectiveRange(game);
        int rgb = t.getType().primaryRgb;
        switch (a.effect) {
            case SELF_BUFF:
                t.addBuff(a.magnitude, a.secondary, 1.0, a.duration);
                break;
            case AREA_BLAST: {
                HitSpec h = new HitSpec(a.magnitude, DamageType.TRUE);
                h.uncappedKnockback = true;
                switch (t.getType()) {
                    case GEMINI: h.stunDuration = a.duration; h.bossStunDuration = a.duration * 0.5; h.type = DamageType.ENERGY; h.ignoresResistance = true; break;
                    case CLAUDE: h.stunDuration = a.duration; h.slowFraction = a.secondary; h.slowDuration = a.duration; break;
                    case SUNO: h.stunDuration = a.duration; h.bossStunDuration = a.duration * 0.5; h.type = DamageType.MAGIC; h.ignoresResistance = true; break;
                    case GROK: h.disruptDuration = a.duration; h.knockback = a.secondary; h.bossKnockback = 12; h.slowFraction = 0.3; h.slowDuration = a.duration; break;
                    case LLAMA: h.type = DamageType.PHYSICAL; h.ignoresResistance = true; h.knockback = a.secondary; break;
                    case MISTRAL: h.knockback = a.secondary; h.slowFraction = 0.5; h.slowDuration = a.duration; h.bossKnockback = 10; break;
                    case GLM: h.knockback = a.secondary; h.stunDuration = a.duration; h.bossKnockback = 15; h.bossStunDuration = 0.5; break;
                    default: break;
                }
                game.getCombat().areaHit(p, range, h, t, 1.0, 999, null, false);
                break;
            }
            case GLOBAL_FREEZE:
                for (Enemy e : game.getEnemyManager().getEnemies()) {
                    boolean immune = e instanceof Boss && ((Boss) e).isControlImmune();
                    if (!immune) e.applyStatus(StatusEffect.Kind.STUN, 1, e.isBoss() ? a.magnitude : a.duration);
                }
                game.getEvents().add(VisualEvent.banner("FREEZE FRAME", rgb, 1.5));
                break;
            case DEEP_RESEARCH: {
                double cash = a.magnitude + a.secondary * game.getWave();
                game.addCash(cash, p);
                for (Enemy e : game.getEnemyManager().getEnemies()) e.applyStatus(StatusEffect.Kind.REVEALED, 1, a.duration);
                break;
            }
            case REVEAL_ALL:
                for (Enemy e : game.getEnemyManager().getEnemies()) {
                    e.applyStatus(StatusEffect.Kind.REVEALED, 1, a.duration);
                    boolean immune = e instanceof Boss && ((Boss) e).isControlImmune();
                    if (e.isCamo() && !immune) e.applyStatus(StatusEffect.Kind.STUN, 1, a.magnitude);
                }
                break;
            case SUMMON:
                if (t.getType() == TowerType.MINIMAX) {
                    Vec q = p.add(new Vec(0, -(t.getRadius() * 2 + 6)));
                    game.getTowerManager().addTemporary(TowerType.MINIMAX, t.getTier(), q, a.duration, t);
                } else {
                    game.getTowerManager().summonSprites(t, (int) a.magnitude, a.duration);
                }
                break;
            case STRONGEST_STRIKE: {
                Enemy best = null;
                for (Enemy e : game.getCombat().acquireTargets(t, 999)) if (best == null || e.getHp() > best.getHp()) best = e;
                if (best == null) return false;
                HitSpec h = new HitSpec(a.magnitude, DamageType.TRUE);
                h.stunDuration = a.duration; h.bossStunDuration = a.secondary;
                game.getCombat().applyHit(best, h, t, 1.0, 0);
                game.getEvents().add(VisualEvent.beam(p, best.getPosition(), rgb));
                game.getEvents().add(VisualEvent.explosion(best.getPosition(), 30, rgb));
                break;
            }
            case QUANTIZE: {
                HitSpec bossHit = new HitSpec(a.magnitude, DamageType.TRUE);
                for (Enemy e : game.getCombat().acquireTargets(t, 999)) {
                    if (e.isBoss()) game.getCombat().applyHit(e, bossHit, t, 1.0, 0);
                    else if (e.stripFortified()) game.getEvents().add(VisualEvent.text(e.getPosition(), "Quantized", 0xd9c6ff));
                }
                break;
            }
            case CONTEXT_DUMP: {
                int stacks = t.getStacks();
                Enemy target = t.getLastTarget();
                if (target == null || !target.isAlive()) {
                    List<Enemy> ts = game.getCombat().acquireTargets(t, 1);
                    if (ts.isEmpty()) return false;
                    target = ts.get(0);
                }
                double dmg = a.magnitude * Math.max(1, stacks);
                HitSpec h = new HitSpec(dmg, DamageType.TRUE);
                game.getCombat().areaHit(target.getPosition(), a.secondary, h, t, 1.0, 999, null, false);
                game.getEvents().add(VisualEvent.explosion(target.getPosition(), a.secondary, rgb));
                t.resetStacks();
                break;
            }
            case TEAM_RATE_BUFF:
                for (Tower o : game.getTowerManager().getTowers()) {
                    if (o.getPosition().dist(p) <= range) o.addBuff(a.magnitude, 1.0, 1.0, a.duration);
                }
                break;
            case COMMAND_OVERRIDE:
                for (Tower o : game.getTowerManager().getTowers()) {
                    o.addBuff(1.0, a.magnitude, 1.0, a.duration);
                    o.clearSurveilled();
                }
                game.getEvents().add(VisualEvent.banner("COMMAND OVERRIDE", rgb, 1.5));
                break;
            default:
                return false;
        }
        t.startAbilityCooldown();
        game.getEvents().add(VisualEvent.ability(p, Math.max(40, range), rgb));
        game.getEvents().add(VisualEvent.text(p, a.name + "!", rgb));
        return true;
    }
}
