package lindefense.tower;

/**
 * Data for a tower's active ability. The effect is identified by {@link Effect};
 * {@link AbilityManager} implements each one.
 */
public class AbilityDef {
    public enum Effect {
        /** Self buff: rateMult / dmgMult for duration. */
        SELF_BUFF,
        /** Damage + control applied to every enemy within the tower's range. */
        AREA_BLAST,
        /** Stun every enemy on the map (bosses for bossDuration). */
        GLOBAL_FREEZE,
        /** Cash grant + reveal all camo for duration. */
        DEEP_RESEARCH,
        /** Summon temporary towers near the caster. */
        SUMMON,
        /** Heavy single-target strike on the strongest enemy in range. */
        STRONGEST_STRIKE,
        /** Strip Fortified from non-boss enemies in range; bosses take damage. */
        QUANTIZE,
        /** Kimi: damage = stacks * magnitude in a small area around the current target. */
        CONTEXT_DUMP,
        /** Copilot: rate buff to every tower within range. */
        TEAM_RATE_BUFF,
        /** Brad: global damage buff + clears Predictive Surveillance. */
        COMMAND_OVERRIDE,
        /** Bellarmine+AI: reveal all camo, stun camo enemies. */
        REVEAL_ALL
    }

    public final String name;
    public final String description;
    public final double cooldown;
    public final Effect effect;
    public final double duration;
    public final double magnitude;
    public final double secondary;

    public AbilityDef(String name, String description, double cooldown, Effect effect,
                      double duration, double magnitude, double secondary) {
        this.name = name; this.description = description; this.cooldown = cooldown;
        this.effect = effect; this.duration = duration; this.magnitude = magnitude; this.secondary = secondary;
    }
}
