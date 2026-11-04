package lindefense.tower.behavior;

import java.util.EnumMap;
import java.util.Map;
import lindefense.tower.TowerType;

/** Registry mapping every {@link TowerType} to its (stateless, shared) {@link TowerBehavior}. */
public final class Behaviors {
    private static final Map<TowerType, TowerBehavior> MAP = new EnumMap<>(TowerType.class);

    private Behaviors() { }

    public static TowerBehavior of(TowerType type) {
        TowerBehavior b = MAP.get(type);
        return b == null ? GENERIC : b;
    }

    private static final TowerBehavior GENERIC = new GenericBehavior();

    static {
        // Heroes (B1)
        MAP.put(TowerType.BELLARMINE_STUDENT, new BellarmineStudentBehavior());
        MAP.put(TowerType.BELLARMINE_STUDENT_AI, new BellarmineStudentAiBehavior());
        MAP.put(TowerType.BRAD_LINDEMANN, new BradLindemannBehavior());
        // Primary (B1)
        MAP.put(TowerType.GEMINI, new GeminiBehavior());
        MAP.put(TowerType.CLAUDE, new ClaudeBehavior());
        MAP.put(TowerType.GPT, new GptBehavior());
        // Military (B1)
        MAP.put(TowerType.DEEPSEEK, new DeepSeekBehavior());
        MAP.put(TowerType.COPILOT, new CopilotBehavior());
        // Magic (B2)
        MAP.put(TowerType.DALLE, new DalleBehavior());
        MAP.put(TowerType.SORA, new SoraBehavior());
        MAP.put(TowerType.SUNO, new SunoBehavior());
        // Support (B2)
        MAP.put(TowerType.GROK, new GrokBehavior());
        MAP.put(TowerType.PERPLEXITY, new PerplexityBehavior());
        // Titans (B3)
        MAP.put(TowerType.QWEN, new QwenBehavior());
        MAP.put(TowerType.LLAMA, new LlamaBehavior());
        MAP.put(TowerType.MISTRAL, new MistralBehavior());
        MAP.put(TowerType.COHERE, new CohereBehavior());
        MAP.put(TowerType.GLM, new GlmBehavior());
        MAP.put(TowerType.KIMI, new KimiBehavior());
        MAP.put(TowerType.MINIMAX, new MiniMaxBehavior());
    }
}
