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
        // Hero Store heroes
        MAP.put(TowerType.BRAD_LINDEMANN, new BradLindemannBehavior());
        MAP.put(TowerType.BRANDON, new BrandonBehavior());
        MAP.put(TowerType.ROHAN, new RohanBehavior());
        MAP.put(TowerType.RONIL, new RonilBehavior());
        MAP.put(TowerType.SHASHANK, new ShashankBehavior());
        MAP.put(TowerType.OWEN, new OwenBehavior());
        // Primary (B1)
        MAP.put(TowerType.GEMINI, new GeminiBehavior());
        MAP.put(TowerType.CLAUDE, new ClaudeBehavior());
        MAP.put(TowerType.GPT, new GptBehavior());
        MAP.put(TowerType.ZETHOS, new ZethosBehavior());
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
