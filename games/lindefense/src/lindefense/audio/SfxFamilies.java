package lindefense.audio;

import java.util.HashMap;
import java.util.Map;
import lindefense.combat.VisualEvent;
import lindefense.tower.AttackKind;
import lindefense.tower.TowerCatalog;
import lindefense.tower.TowerDef;
import lindefense.tower.TowerStats;
import lindefense.tower.TowerType;

/**
 * Maps simulation {@link VisualEvent}s to sound families. A tower's attack events carry its colour, so the
 * colour identifies the tower, and the tower's damage type / attack kind picks the family:
 * energy beams = laser, projectiles = shot, splash = explosion, magic = magic, physical beams = kinetic,
 * true damage = gravity, pulses = sonic, chains = chain lightning.
 */
final class SfxFamilies {
    private static final Map<Integer, Sfx> BY_RGB = new HashMap<>();

    static {
        // secondary colours first so primary colours win collisions
        for (TowerType t : TowerType.values()) put(t, t.secondaryRgb);
        for (TowerType t : TowerType.values()) put(t, t.primaryRgb);
    }

    private SfxFamilies() { }

    private static void put(TowerType t, int rgb) {
        TowerDef d = TowerCatalog.get(t);
        if (d == null) return;
        BY_RGB.put(rgb, familyOf(d.baseStats));
    }

    /** The sound family for a tower with the given stats. */
    static Sfx familyOf(TowerStats s) {
        if (s.kind == AttackKind.CHAIN) return Sfx.ATK_CHAIN;
        if (s.kind == AttackKind.PULSE) return Sfx.ATK_SONIC;
        if (s.splashRadius > 0) return Sfx.ATK_EXPLOSION;
        switch (s.hit.type) {
            case EXPLOSIVE: return Sfx.ATK_EXPLOSION;
            case MAGIC: return Sfx.ATK_MAGIC;
            case SONIC: return Sfx.ATK_SONIC;
            case GRAVITY: case TRUE: return Sfx.ATK_GRAVITY;
            case SHARP: case BLUNT: case KINETIC: return s.kind == AttackKind.PROJECTILE ? Sfx.ATK_PROJECTILE : Sfx.ATK_KINETIC;
            case ENERGY: default: return s.kind == AttackKind.PROJECTILE ? Sfx.ATK_PROJECTILE : Sfx.ATK_LASER;
        }
    }

    /** The effect for a simulation event, or null for none. */
    static Sfx forEvent(VisualEvent ev) {
        switch (ev.kind) {
            case BEAM: return BY_RGB.getOrDefault(ev.rgb, Sfx.ATK_LASER);
            case CHAIN: return Sfx.ATK_CHAIN;
            case HIT: return ev.rgb == 0xffffff ? Sfx.ENEMY_HIT : BY_RGB.getOrDefault(ev.rgb, Sfx.ENEMY_HIT);
            case EXPLOSION: return Sfx.ATK_EXPLOSION;
            case DEATH: return Sfx.ENEMY_POP;
            case SPLIT: return Sfx.ENEMY_SPLIT;
            case ABILITY: return BY_RGB.get(ev.rgb) == Sfx.ATK_SONIC ? Sfx.ATK_SONIC : Sfx.ABILITY;
            case PHASE: return Sfx.BOSS_ALARM;
            default: return null;
        }
    }
}
