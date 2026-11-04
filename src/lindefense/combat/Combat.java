package lindefense.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lindefense.enemy.Boss;
import lindefense.enemy.Enemy;
import lindefense.enemy.StatusEffect;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AttackKind;
import lindefense.tower.Targeting;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;

/**
 * The combat engine: target acquisition, attack execution for every
 * {@link AttackKind}, projectile movement/collision and the single damage
 * formula used by every hit in the game (towers, abilities and map hazards).
 */
public class Combat {
    /** Maximum visual events kept per tick to keep the renderer responsive. */
    private static final int MAX_EVENTS_PER_TICK = 400;

    private final Game game;
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Projectile> newProjectiles = new ArrayList<>();

    public Combat(Game game) { this.game = game; }

    public List<Projectile> getProjectiles() { return projectiles; }

    // ------------------------------------------------------------------
    // Target acquisition
    // ------------------------------------------------------------------

    /** Returns up to {@code n} valid targets for the tower, ordered by its priority. */
    public List<Enemy> acquireTargets(Tower t, int n) {
        double range = t.effectiveRange(game);
        Vec tp = t.getPosition();
        boolean sees = t.canSeeCamo();
        List<Enemy> candidates = new ArrayList<>();
        for (Enemy e : game.getEnemyManager().getEnemies()) {
            if (!e.isTargetableBy(sees)) continue;
            if (tp.dist(e.getPosition()) > range + e.getRadius()) continue;
            if (!game.getMap().hasLineOfSight(tp, e.getPosition())) continue;
            candidates.add(e);
        }
        if (candidates.isEmpty()) return candidates;
        candidates.sort(comparatorFor(t.getTargeting(), tp));
        if (candidates.size() > n) return new ArrayList<>(candidates.subList(0, n));
        return candidates;
    }

    /** The ordering used by each targeting priority. */
    public static Comparator<Enemy> comparatorFor(Targeting targeting, Vec towerPos) {
        switch (targeting) {
            case LAST: return Comparator.comparingDouble(Enemy::getRemainingDistance).reversed();
            case STRONGEST: return Comparator.comparingDouble(Enemy::getHp).reversed().thenComparingDouble(Enemy::getRemainingDistance);
            case WEAKEST: return Comparator.comparingDouble(Enemy::getHp).thenComparingDouble(Enemy::getRemainingDistance);
            case CLOSEST: return Comparator.comparingDouble((Enemy e) -> e.getPosition().distSq(towerPos));
            case SMART: return Comparator.comparingInt((Enemy e) -> e.isCamo() ? 0 : 1)
                .thenComparingInt(e -> e.isBoss() ? 0 : 1)
                .thenComparingDouble(Enemy::getHp).reversed()
                .thenComparingDouble(Enemy::getRemainingDistance);
            case FIRST:
            default: return Comparator.comparingDouble(Enemy::getRemainingDistance);
        }
    }

    // ------------------------------------------------------------------
    // Attack execution
    // ------------------------------------------------------------------

    /** Performs one attack for the tower if a target exists. Returns true if it attacked. */
    public boolean attack(Tower t) {
        TowerStats s = t.getStats();
        switch (s.kind) {
            case PULSE: return pulse(t, s);
            case CHAIN: return chain(t, s);
            case INSTANT: return instant(t, s);
            case PROJECTILE: return shoot(t, s);
            default: return false;
        }
    }

    private boolean shoot(Tower t, TowerStats s) {
        List<Enemy> targets = acquireTargets(t, s.targets);
        if (targets.isEmpty()) return false;
        double mult = t.damageMultiplier(game);
        for (Enemy target : targets) {
            for (int i = 0; i < s.projectilesPerShot; i++) {
                Vec start = t.getPosition();
                if (s.projectilesPerShot > 1) {
                    double ang = (i - (s.projectilesPerShot - 1) / 2.0) * 0.35;
                    Vec d = target.getPosition().sub(start).normalized();
                    Vec perp = new Vec(-d.y, d.x);
                    start = start.add(perp.scale(ang * 20));
                }
                newProjectiles.add(new Projectile(t, s, start, target, mult, true, t.getType().primaryRgb));
            }
            t.setLastTarget(target);
        }
        t.onAttacked(game);
        return true;
    }

    private boolean instant(Tower t, TowerStats s) {
        List<Enemy> targets = acquireTargets(t, s.targets);
        if (targets.isEmpty()) return false;
        double mult = t.damageMultiplier(game);
        for (Enemy target : targets) {
            Vec tp = t.getPosition();
            if (s.pierce > 1) {
                // beam through everything on the line tower -> target, out to range
                Vec dir = target.getPosition().sub(tp).normalized();
                double range = t.effectiveRange(game);
                Vec end = tp.add(dir.scale(range));
                List<Enemy> onLine = new ArrayList<>();
                for (Enemy e : game.getEnemyManager().getEnemies()) {
                    if (!e.isTargetableBy(t.canSeeCamo())) continue;
                    if (lindefense.map.Path.pointSegmentDistance(e.getPosition(), tp, end) <= e.getRadius() + 4) onLine.add(e);
                }
                onLine.sort(Comparator.comparingDouble(e -> e.getPosition().distSq(tp)));
                int hits = 0;
                for (Enemy e : onLine) {
                    if (hits++ >= s.pierce) break;
                    int extra = t.registerStackHit(e.getId());
                    applyHit(e, s.hit, t, mult, extra * s.stackStep);
                }
                emit(VisualEvent.beam(tp, end, t.getType().primaryRgb));
            } else {
                int extra = t.registerStackHit(target.getId());
                applyHit(target, s.hit, t, mult, extra * s.stackStep);
                emit(VisualEvent.beam(tp, target.getPosition(), t.getType().primaryRgb));
            }
            t.setLastTarget(target);
        }
        t.onAttacked(game);
        return true;
    }

    private boolean chain(Tower t, TowerStats s) {
        List<Enemy> targets = acquireTargets(t, 1);
        if (targets.isEmpty()) return false;
        double mult = t.damageMultiplier(game);
        Enemy cur = targets.get(0);
        Set<Integer> hit = new HashSet<>();
        Vec from = t.getPosition();
        applyHit(cur, s.hit, t, mult, 0);
        emit(VisualEvent.chain(from, cur.getPosition(), t.getType().primaryRgb));
        hit.add(cur.getId());
        for (int hop = 0; hop < s.chainHops; hop++) {
            Enemy next = nearestEnemy(cur.getPosition(), s.chainRange, hit, t.canSeeCamo());
            if (next == null) break;
            emit(VisualEvent.chain(cur.getPosition(), next.getPosition(), t.getType().secondaryRgb));
            applyHit(next, s.hit, t, mult, 0);
            hit.add(next.getId());
            cur = next;
        }
        t.setLastTarget(targets.get(0));
        t.onAttacked(game);
        return true;
    }

    private boolean pulse(Tower t, TowerStats s) {
        double range = t.effectiveRange(game);
        Vec tp = t.getPosition();
        List<Enemy> inRange = new ArrayList<>();
        for (Enemy e : game.getEnemyManager().getEnemies()) {
            if (!e.isTargetableBy(t.canSeeCamo())) continue;
            if (tp.dist(e.getPosition()) <= range + e.getRadius()) inRange.add(e);
        }
        if (inRange.isEmpty()) return false;
        inRange.sort(comparatorFor(t.getTargeting(), tp));
        int idx = t.nextPulseIndex();
        boolean heavy = s.heavyEvery > 0 && idx % s.heavyEvery == 0;
        HitSpec h = s.hit;
        if (heavy || s.resonancePer > 0) {
            h = s.hit.copy();
            if (heavy) {
                h.stunDuration = Math.max(h.stunDuration, s.heavyStun);
                h.knockback = Math.max(h.knockback, s.heavyKnockback);
            }
            if (s.resonancePer > 0) h.damage += Math.min(s.resonanceCap, inRange.size() / s.resonancePer);
        }
        double mult = t.damageMultiplier(game);
        int n = 0;
        for (Enemy e : inRange) {
            if (n++ >= s.pierce) break;
            applyHit(e, h, t, mult, 0);
        }
        emit(VisualEvent.ability(tp, range, heavy ? t.getType().secondaryRgb : t.getType().primaryRgb));
        t.setLastTarget(inRange.get(0));
        t.onAttacked(game);
        return true;
    }

    /** Nearest targetable enemy to a point within radius, excluding ids. */
    public Enemy nearestEnemy(Vec p, double radius, Set<Integer> exclude, boolean seesCamo) {
        Enemy best = null;
        double bestD = radius * radius;
        for (Enemy e : game.getEnemyManager().getEnemies()) {
            if (!e.isTargetableBy(seesCamo)) continue;
            if (exclude != null && exclude.contains(e.getId())) continue;
            double d = e.getPosition().distSq(p);
            if (d <= bestD) { bestD = d; best = e; }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Projectiles
    // ------------------------------------------------------------------

    public void addProjectile(Projectile p) { newProjectiles.add(p); }

    public void updateProjectiles(double dt) {
        if (!newProjectiles.isEmpty()) { projectiles.addAll(newProjectiles); newProjectiles.clear(); }
        List<Enemy> enemies = game.getEnemyManager().getEnemies();
        for (int i = 0; i < projectiles.size(); i++) {
            Projectile p = projectiles.get(i);
            if (!p.move(dt)) continue;
            boolean sees = p.getSource().canSeeCamo();
            for (int j = 0; j < enemies.size() && p.isAlive(); j++) {
                Enemy e = enemies.get(j);
                if (!e.isTargetableBy(sees) || p.getHitIds().contains(e.getId())) continue;
                double r = e.getRadius() + p.getSize();
                if (e.getPosition().distSq(p.getPosition()) <= r * r) {
                    onProjectileHit(p, e);
                }
            }
        }
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            if (!projectiles.get(i).isAlive()) {
                int last = projectiles.size() - 1;
                projectiles.set(i, projectiles.get(last));
                projectiles.remove(last);
            }
        }
        if (!newProjectiles.isEmpty()) { projectiles.addAll(newProjectiles); newProjectiles.clear(); }
    }

    private void onProjectileHit(Projectile p, Enemy e) {
        TowerStats s = p.getStats();
        Tower t = p.getSource();
        p.getHitIds().add(e.getId());
        if (s.splashRadius > 0) {
            areaHit(p.getPosition(), s.splashRadius, s.hit, t, p.getDamageMult(), s.pierce, null, true);
            emit(VisualEvent.explosion(p.getPosition(), s.splashRadius, t.getType().primaryRgb));
            p.kill();
            return;
        }
        applyHit(e, s.hit, t, p.getDamageMult(), 0);
        emit(VisualEvent.hit(e.getPosition(), t.getType().primaryRgb));
        if (p.canClone()) {
            p.consumeClone();
            Set<Integer> exclude = new HashSet<>(p.getHitIds());
            for (int c = 1; c < s.clones; c++) {
                Enemy nt = nearestEnemy(e.getPosition(), s.bounceRange, exclude, t.canSeeCamo());
                if (nt == null) break;
                exclude.add(nt.getId());
                Projectile clone = new Projectile(t, s, e.getPosition(), nt, p.getDamageMult(), false, t.getType().secondaryRgb);
                clone.getHitIds().add(e.getId());
                newProjectiles.add(clone);
            }
        }
        if (p.getBouncesLeft() > 0) {
            Enemy next = nearestEnemy(e.getPosition(), s.bounceRange, p.getHitIds(), t.canSeeCamo());
            if (next != null) { p.consumeBounce(); p.retarget(next); return; }
        }
        if (p.consumePierce()) p.kill();
    }

    /** Hits every targetable enemy within radius (up to maxTargets, nearest first). */
    public int areaHit(Vec center, double radius, HitSpec hit, Tower source, double mult, int maxTargets,
                       Set<Integer> exclude, boolean requireDetection) {
        List<Enemy> inRange = new ArrayList<>();
        boolean sees = source == null || !requireDetection || source.canSeeCamo();
        for (Enemy e : game.getEnemyManager().getEnemies()) {
            if (!e.isTargetableBy(sees)) continue;
            if (exclude != null && exclude.contains(e.getId())) continue;
            if (e.getPosition().dist(center) <= radius + e.getRadius()) inRange.add(e);
        }
        inRange.sort(Comparator.comparingDouble(e -> e.getPosition().distSq(center)));
        int n = 0;
        for (Enemy e : inRange) {
            if (n >= maxTargets) break;
            applyHit(e, hit, source, mult, 0);
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------
    // The damage formula
    // ------------------------------------------------------------------

    /**
     * Applies a hit to an enemy and returns the HP actually removed.
     *
     * <pre>
     * raw     = (hit.damage + extra) * mult * (1 + situational bonuses)
     * raw    *= resistance(type)            (0 = immune; ignored if hit.ignoresResistance or TRUE)
     * raw    *= 1 - armour * (1 - armorPierce)
     * raw    *= 0.5 if boss weak-hit rule applies, 0.5 if Heap-shielded
     * raw    *= 1 + marked bonus
     * raw    += min(pctCap, pctMaxHp * maxHp)
     * </pre>
     * Control effects (slow, stun, knockback, armour break, mark, disrupt, DOT)
     * are then applied with boss-specific values; Palantir in phase 1 ignores
     * slows, stuns and knockback.
     */
    public double applyHit(Enemy e, HitSpec hit, Tower source, double mult, double extra) {
        if (e == null || !e.isAlive() || e.isStored()) return 0;
        double raw = (hit.damage + extra) * mult;
        double bonus = 1.0;
        if (e.isBoss()) bonus += hit.bonusVsBoss;
        if (e.isFortified()) bonus += hit.bonusVsFortified; else bonus += hit.bonusVsNonFortified;
        if (e.getType().isComplex()) bonus += hit.bonusVsComplex;
        if (e.getType().isFast()) bonus += hit.bonusVsFast;
        if (e.getType().hasChildren()) bonus += hit.bonusVsSplitters;
        raw *= Math.max(0.1, bonus);

        double resist = (hit.ignoresResistance || hit.type == DamageType.TRUE) ? 1.0 : e.resistanceMultiplier(hit.type);
        boolean immune = resist <= 0;
        double dmg = raw * resist;
        if (hit.type != DamageType.TRUE) {
            double armor = e.effectiveArmor() * (1 - hit.armorPierce);
            dmg *= (1 - Math.max(0, Math.min(0.95, armor)));
            int weak = e.getType().getWeakHitThreshold();
            if (weak > 0 && hit.damage < weak) dmg *= 0.5;
        }
        if (e.isShielded(game.getTick())) dmg *= 0.5;
        dmg *= 1 + e.getMarkedBonus();
        if (hit.pctMaxHp > 0) dmg += Math.min(hit.pctCap, hit.pctMaxHp * e.getMaxHp());

        double dealt = 0;
        if (!immune && dmg > 0) {
            dealt = e.takeDamage(dmg, game.getTime());
            e.setLastHitBy(source);
            if (source != null) source.addDamageDealt(dealt);
        } else if (immune && source != null && game.getRng().chance(0.05)) {
            emit(VisualEvent.text(e.getPosition(), "Immune", 0xaaaaaa));
        }

        // control effects
        boolean controlImmune = (e instanceof Boss) && ((Boss) e).isControlImmune();
        if (!controlImmune) {
            if (hit.slowFraction > 0) e.applyStatus(StatusEffect.Kind.SLOW, hit.slowFraction, hit.slowDuration);
            double stun = e.isBoss() ? hit.bossStunDuration : hit.stunDuration;
            if (stun > 0) e.applyStatus(StatusEffect.Kind.STUN, 1, stun);
            double kb = e.isBoss() ? hit.bossKnockback : hit.knockback;
            if (kb > 0 && !immune) {
                if (hit.uncappedKnockback) e.shiftDistance(-kb); else e.applyKnockback(kb, game.getTime());
            }
        }
        if (hit.armorBreak > 0) e.applyStatus(StatusEffect.Kind.ARMOR_BREAK, hit.armorBreak, hit.armorBreakDuration);
        if (hit.markBonus > 0) e.applyStatus(StatusEffect.Kind.MARKED, hit.markBonus, hit.markDuration);
        if (hit.disruptDuration > 0) e.applyStatus(StatusEffect.Kind.DISRUPTED, 1, hit.disruptDuration);
        if (hit.dotDps > 0) e.applyStatus(StatusEffect.Kind.DOT, hit.dotDps, hit.dotDuration);
        if (hit.stripsFortified && !e.isBoss() && e.isFortified()) {
            e.stripFortified();
            emit(VisualEvent.text(e.getPosition(), "Stripped!", 0xd9c6ff));
        }
        return dealt;
    }

    /** True damage from map hazards (short circuits, heat). */
    public double applyMapDamage(Enemy e, double amount) {
        if (e == null || !e.isAlive() || e.isStored()) return 0;
        double dealt = e.takeDamage(amount, game.getTime());
        e.setLastHitBy(null);
        emit(VisualEvent.hit(e.getPosition(), 0xffffff));
        return dealt;
    }

    private void emit(VisualEvent ev) {
        if (game.getEvents().size() < MAX_EVENTS_PER_TICK) game.getEvents().add(ev);
    }

    public void clear() { projectiles.clear(); newProjectiles.clear(); }
}
