package lindefense.combat;

/** Damage categories. Enemy resistances and immunities are keyed by these. */
public enum DamageType {
    /** Sharp / kinetic attacks (Llama, Bellarmine Student, Copilot snippets). */
    PHYSICAL("Physical"),
    /** Beams, bolts and electricity (GPT, Gemini, DeepSeek, Cohere, Kimi, ...). */
    ENERGY("Energy"),
    /** Artillery and blasts (Brad Lindemann). */
    EXPLOSIVE("Explosive"),
    /** Creative / arcane effects (DALL-E, Sora, Suno, Mistral, GLM, MiniMax). */
    MAGIC("Magic"),
    /** Ignores all resistances, immunities and armor (map hazards, true-damage abilities). */
    TRUE("True");

    public final String label;
    DamageType(String label) { this.label = label; }
}
