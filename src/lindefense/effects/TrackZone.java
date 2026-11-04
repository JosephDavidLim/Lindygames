package lindefense.effects;

import lindefense.combat.Combat;
import lindefense.combat.HitContext;
import lindefense.combat.HitSpec;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyGrid;
import lindefense.enemy.StatusEffect;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Tower;

/**
 * A circular area on the track that acts on every enemy inside it: periodic tick damage, a slow or a speed cap,
 * armour crack, and a duration. Used by DALL-E paint zones and Outpainting, Sora loop zones, Mistral tornado,
 * vortex and Stratosphere Tempest.
 *
 * <p>An enemy is "inside" when its centre is within {@link #radius} px of {@link #center}. Slows are refreshed
 * every tick for {@link #REFRESH} seconds so they end when the enemy leaves; they obey the global slow rules
 * (strongest wins, bosses receive 1/3). All parameters are public so a behaviour can configure a zone fluently:
 * <pre>
 * TrackZone z = new TrackZone(point, 35, owner);
 * z.duration = 4; z.tickInterval = 0.5; z.tickHit = hit;
 * game.getEffects().addZone(z);
 * </pre>
 */
public final class TrackZone {
    /** Seconds a zone-applied slow / crack lingers after being refreshed. */
    public static final double REFRESH = 0.25;

    public final Vec center;
    public final double radius;
    /** The tower credited with the zone's damage; also the tower whose removal ends the zone if {@link #bindToOwner}. */
    public final Tower owner;
    /** Seconds remaining; a value &le; 0 at creation with {@link #permanent} means it lasts as long as its owner. */
    public double duration = 1;
    public boolean permanent;
    public boolean bindToOwner = true;
    /** Seconds between damage ticks (0 = no tick damage). */
    public double tickInterval;
    /** Hit applied to each enemy every tick (null = none). Ability-style: no attack buffs. */
    public HitSpec tickHit;
    /** Slow fraction applied to enemies inside (0 = none). */
    public double slow;
    /**
     * Speed cap as a fraction of normal speed: 0.25 means enemies inside move at 25% speed (implemented as a 0.75
     * slow). 0 = no cap. The stronger of {@link #slow} and the cap's slow applies.
     */
    public double speedCap;
    /** Apply ARMOR CRACK to enemies inside (non-bosses only unless {@link #crackBosses}). */
    public boolean armorCrack;
    public boolean crackBosses;
    /** Whether bosses are damaged / slowed at all. */
    public boolean affectsBosses = true;
    /** Only bosses are affected (Tempest hits airships). */
    public boolean bossesOnly;
    /** Source detection for the EXPLOSIVE-vs-camo rule. */
    public boolean detection = true;
    public int rgb = 0xffffff;
    /** Free-form label for the renderer / tests ("paint", "loop", "tornado", "vortex", "outpaint", "tempest"). */
    public String kind = "zone";

    private double tickTimer;
    private boolean dead;

    public TrackZone(Vec center, double radius, Tower owner) {
        this.center = center; this.radius = radius; this.owner = owner;
    }

    public boolean contains(Enemy e) { return e.getPosition().withinDist(center, radius); }
    public boolean isAlive() { return !dead; }
    public void kill() { dead = true; }
    public double getRemaining() { return duration; }

    void update(Game game, double dt) {
        if (dead) return;
        if (bindToOwner && owner != null && !owner.isAlive()) { dead = true; return; }
        if (!permanent) {
            duration -= dt;
            if (duration <= 0) { dead = true; return; }
        }
        boolean tick = false;
        if (tickInterval > 0 && tickHit != null) {
            tickTimer += dt;
            if (tickTimer >= tickInterval) { tickTimer -= tickInterval; tick = true; }
        }
        double effSlow = Math.max(slow, speedCap > 0 ? 1 - speedCap : 0);
        Combat combat = game.getCombat();
        // only the grid cells under the zone; a tick hit may knock enemies around, so use a self-refreshing scan
        EnemyGrid.Scan scan = game.getEnemyManager().getGrid().scan(center.x, center.y, radius);
        try {
            for (Enemy e = scan.next(); e != null; e = scan.next()) {
                if (!e.isAlive() || e.isStored() || !contains(e)) continue;
                if (e.isBoss() && !affectsBosses) continue;
                if (bossesOnly && !e.isBoss()) continue;
                if (effSlow > 0) e.applyStatus(StatusEffect.Kind.SLOW, effSlow, REFRESH);
                if (armorCrack && (!e.isBoss() || crackBosses)) e.applyStatus(StatusEffect.Kind.ARMOR_CRACK, 1, REFRESH);
                if (tick) {
                    HitContext h = new HitContext(e, tickHit, owner);
                    h.detection = detection;
                    h.mult = game.getGlobalDamageMultiplier();
                    combat.applyHit(h);
                }
            }
        } finally {
            scan.close();
        }
    }
}
