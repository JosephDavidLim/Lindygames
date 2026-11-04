package lindefense.ui.sprites.towers;

import lindefense.tower.TowerType;

/** Maps each {@link TowerType} to its {@link TowerArt} class. */
public final class TowerArtFactory {
    private TowerArtFactory() { }

    /**
     * Builds the body art for a tower.
     *
     * @param type tower type (null falls back to a generic bot)
     * @param tier progression tier, clamped to 0-4
     * @param r    footprint radius in logical units
     */
    public static TowerArt create(TowerType type, int tier, double r) {
        if (type == null) return new GenericArt(TowerType.GPT, tier, r);
        switch (type) {
            case CLAUDE: return new ClaudeArt(type, tier, r);
            case GPT: return new GptArt(type, tier, r);
            case GEMINI: return new GeminiArt(type, tier, r);
            case DEEPSEEK: return new DeepSeekArt(type, tier, r);
            case COPILOT: return new CopilotArt(type, tier, r);
            case DALLE: return new DalleArt(type, tier, r);
            case SORA: return new SoraArt(type, tier, r);
            case SUNO: return new SunoArt(type, tier, r);
            case GROK: return new GrokArt(type, tier, r);
            case PERPLEXITY: return new PerplexityArt(type, tier, r);
            case QWEN: return new QwenArt(type, tier, r);
            case LLAMA: return new LlamaArt(type, tier, r);
            case MISTRAL: return new MistralArt(type, tier, r);
            case COHERE: return new CohereArt(type, tier, r);
            case GLM: return new GlmArt(type, tier, r);
            case KIMI: return new KimiArt(type, tier, r);
            case MINIMAX: return new MiniMaxArt(type, tier, r);
            case BELLARMINE_STUDENT: return new StudentArt(type, tier, r);
            case BELLARMINE_STUDENT_AI: return new StudentAiArt(type, tier, r);
            case BRAD_LINDEMANN: return new BradArt(type, tier, r);
            case DALLE_SPRITE: return new SpriteArt(type, tier, r);
            default: return new GenericArt(type, tier, r);
        }
    }
}
