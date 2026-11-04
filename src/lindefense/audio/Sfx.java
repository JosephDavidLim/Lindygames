package lindefense.audio;

/**
 * Every one-shot sound effect in the game. Each entry names its WAV file under {@code assets/sfx/},
 * the minimum gap between two plays (the per-sound throttle that keeps huge fights smooth), a base gain,
 * a priority used when the voice pool is full, and whether a little random pitch/volume variation is applied.
 */
public enum Sfx {
    UI_CLICK("ui_click", 30, 0.70f, 3, false),
    UI_HOVER("ui_hover", 70, 0.50f, 1, false),
    UI_SELECT("ui_select", 60, 0.70f, 3, false),
    TOWER_PLACE("tower_place", 60, 0.75f, 4, false),
    TOWER_UPGRADE("tower_upgrade", 80, 0.75f, 4, false),
    TOWER_SELL("tower_sell", 80, 0.75f, 4, false),
    INVALID("invalid", 120, 0.75f, 4, false),

    ATK_LASER("atk_laser", 66, 0.35f, 1, true),
    ATK_PROJECTILE("atk_projectile", 66, 0.40f, 1, true),
    ATK_EXPLOSION("atk_explosion", 66, 0.50f, 2, true),
    ATK_SONIC("atk_sonic", 90, 0.35f, 1, true),
    ATK_MAGIC("atk_magic", 66, 0.35f, 1, true),
    ATK_KINETIC("atk_kinetic", 66, 0.45f, 1, true),
    ATK_GRAVITY("atk_gravity", 80, 0.35f, 1, true),
    ATK_CHAIN("atk_chain", 66, 0.35f, 1, true),

    ENEMY_POP("enemy_pop", 50, 0.40f, 1, true),
    ENEMY_HIT("enemy_hit", 60, 0.25f, 0, true),
    ENEMY_SPLIT("enemy_split", 80, 0.40f, 1, true),
    LIFE_LOST("life_lost", 100, 0.90f, 6, false),

    WAVE_START("wave_start", 300, 0.80f, 5, false),
    WAVE_COMPLETE("wave_complete", 300, 0.80f, 5, false),
    BOSS_ALARM("boss_alarm", 1500, 0.80f, 7, false),
    BOSS_DEATH("boss_death", 500, 1.00f, 7, false),
    PALANTIR_WARNING("palantir_warning", 1500, 0.90f, 7, false),
    ABILITY("ability", 120, 0.60f, 3, true),
    CASH("cash", 150, 0.60f, 3, false),
    VICTORY("victory", 1000, 0.90f, 8, false),
    DEFEAT("defeat", 1000, 0.90f, 8, false);

    /** File name without extension, inside {@code assets/sfx/}. */
    public final String file;
    /** Minimum milliseconds between two accepted plays of this sound. */
    public final int minGapMs;
    /** Base linear gain (0..1). */
    public final float gain;
    /** Sounds with a higher priority may steal a voice from lower ones when all voices are busy. */
    public final int priority;
    /** Apply a small random pitch and volume variation per play. */
    public final boolean vary;

    Sfx(String file, int minGapMs, float gain, int priority, boolean vary) {
        this.file = file; this.minGapMs = minGapMs; this.gain = gain; this.priority = priority; this.vary = vary;
    }
}
