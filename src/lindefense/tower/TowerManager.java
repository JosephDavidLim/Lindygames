package lindefense.tower;

import java.util.ArrayList;
import java.util.List;
import lindefense.combat.VisualEvent;
import lindefense.enemy.Enemy;
import lindefense.enemy.StatusEffect;
import lindefense.sim.Game;
import lindefense.sim.Vec;

/**
 * Owns placed towers: placement validation, purchase, upgrade, selling,
 * temporary summons, aura recomputation and the per-tick attack loop.
 */
public class TowerManager {
    private static final int AURA_REFRESH_TICKS = 6;

    private final Game game;
    private final List<Tower> towers = new ArrayList<>();
    private final List<Tower> pending = new ArrayList<>();
    private int tickCounter;

    public TowerManager(Game game) { this.game = game; }

    public List<Tower> getTowers() { return towers; }

    /** Permanent (player-owned) towers only. */
    public List<Tower> getPlayerTowers() {
        List<Tower> l = new ArrayList<>();
        for (Tower t : towers) if (!t.isTemporary()) l.add(t);
        return l;
    }

    public void clear() { towers.clear(); pending.clear(); }

    // ------------------------------------------------------------------
    // Placement
    // ------------------------------------------------------------------

    /** Returns null if a tower of this type can be placed at p, otherwise the reason it cannot. */
    public String placementProblem(TowerType type, Vec p) {
        TowerDef def = TowerCatalog.get(type);
        if (!game.getMap().isBuildable(p, def.radius)) return "Cannot build here";
        for (Tower t : towers) {
            if (t.isTemporary()) continue;
            if (t.getPosition().dist(p) < t.getRadius() + def.radius) return "Overlaps another tower";
        }
        if (game.getCash() < def.baseCost) return "Not enough cash";
        return null;
    }

    public boolean canPlace(TowerType type, Vec p) { return placementProblem(type, p) == null; }

    /** Buys and places a tower. Returns the tower, or null if placement is invalid. */
    public Tower place(TowerType type, Vec p) {
        if (!canPlace(type, p)) return null;
        TowerDef def = TowerCatalog.get(type);
        if (!game.spendCash(def.baseCost)) return null;
        Tower t = new Tower(def, p);
        towers.add(t);
        game.getEvents().add(VisualEvent.ability(p, def.radius * 2, type.primaryRgb));
        recomputeAuras();
        return t;
    }

    /** Adds a temporary tower (summon / clone). */
    public Tower addTemporary(TowerType type, int tier, Vec p, double lifetime, Tower owner) {
        Tower t = new Tower(TowerCatalog.get(type), p, tier, true, lifetime, owner);
        pending.add(t);
        game.getEvents().add(VisualEvent.ability(p, 30, type.secondaryRgb));
        return t;
    }

    public boolean canUpgrade(Tower t) {
        return t != null && t.isAlive() && !t.isTemporary() && t.canUpgrade() && game.getCash() >= t.nextUpgrade().cost;
    }

    /** Purchases the next upgrade. Returns false if unaffordable or maxed. */
    public boolean upgrade(Tower t) {
        if (!canUpgrade(t)) return false;
        int cost = t.nextUpgrade().cost;
        if (!game.spendCash(cost)) return false;
        t.applyUpgrade();
        t.clearSurveilled();
        game.getEvents().add(VisualEvent.ability(t.getPosition(), 50, t.getType().secondaryRgb));
        recomputeAuras();
        return true;
    }

    /** Sells the tower for 70% of everything spent on it. Returns the refund. */
    public int sell(Tower t) {
        if (t == null || !t.isAlive() || t.isTemporary()) return 0;
        int refund = Shop.sellValue(t);
        t.markRemoved();
        towers.remove(t);
        game.addCash(refund, t.getPosition());
        game.getEvents().add(VisualEvent.text(t.getPosition(), "+$" + refund, 0xffd166));
        recomputeAuras();
        return refund;
    }

    /** Tower whose footprint contains p, or null. */
    public Tower towerAt(Vec p) {
        Tower best = null;
        double bestD = Double.MAX_VALUE;
        for (Tower t : towers) {
            if (t.isTemporary()) continue;
            double d = t.getPosition().dist(p);
            if (d <= t.getRadius() + 2 && d < bestD) { bestD = d; best = t; }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Auras
    // ------------------------------------------------------------------

    /** Recomputes every aura-granted bonus from scratch. */
    public void recomputeAuras() {
        for (Tower t : towers) t.resetAuraBonuses();
        for (Tower src : towers) {
            TowerStats s = src.getStats();
            if (s.auraRange <= 0) continue;
            for (Tower dst : towers) {
                if (dst == src) continue;
                if (dst.getPosition().dist(src.getPosition()) > s.auraRange) continue;
                double rate = s.auraRate;
                if (dst.getType().faction == Faction.MILITARY && s.auraRateMilitary > rate) rate = s.auraRateMilitary;
                if (rate > 0) dst.addAuraRate(rate);
                if (s.auraDamage > 0) dst.addAuraDamage(s.auraDamage);
                if (s.auraRangeBonus > 0) dst.addAuraRange(s.auraRangeBonus);
                if (s.auraDetection) dst.grantDetection();
                if (s.auraSurveillanceImmunity) dst.grantSurveillanceImmunity();
            }
        }
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public void update(double dt) {
        if (!pending.isEmpty()) { towers.addAll(pending); pending.clear(); }
        if (++tickCounter % AURA_REFRESH_TICKS == 0) recomputeAuras();
        for (int i = 0; i < towers.size(); i++) {
            Tower t = towers.get(i);
            boolean wantsAttack = t.tickTimers(game, dt);
            if (!t.isAlive()) continue;
            TowerStats s = t.getStats();
            if (s.auraEnemySlow > 0) {
                double r = t.effectiveRange(game);
                for (Enemy e : game.getEnemyManager().getEnemies()) {
                    if (e.getPosition().dist(t.getPosition()) <= r + e.getRadius()) {
                        e.applyStatus(StatusEffect.Kind.SLOW, s.auraEnemySlow, 0.2);
                    }
                }
            }
            if (t.tickSummon(dt) && !t.isSurveilled()) {
                summonSprites(t, s.summonCount, s.summonLifetime);
            }
            if (wantsAttack) {
                if (!game.getCombat().attack(t)) t.idle();
            }
        }
        for (int i = towers.size() - 1; i >= 0; i--) {
            if (!towers.get(i).isAlive()) towers.remove(i);
        }
        if (!pending.isEmpty()) { towers.addAll(pending); pending.clear(); }
    }

    /** Summons DALL-E sprites around the owner at random offsets inside its range. */
    public void summonSprites(Tower owner, int count, double lifetime) {
        for (int i = 0; i < count; i++) {
            double ang = game.getRng().range(0, Math.PI * 2);
            double dist = game.getRng().range(30, Math.max(40, owner.getStats().range * 0.6));
            Vec p = owner.getPosition().add(new Vec(Math.cos(ang) * dist, Math.sin(ang) * dist));
            p = new Vec(Math.max(10, Math.min(lindefense.map.GameMap.WIDTH - 10, p.x)),
                        Math.max(10, Math.min(lindefense.map.GameMap.HEIGHT - 10, p.y)));
            addTemporary(TowerType.DALLE_SPRITE, 0, p, lifetime, owner);
        }
    }
}
