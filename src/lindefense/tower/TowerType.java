package lindefense.tower;

/**
 * Identifiers for all 20 playable towers (3 heroes + 17 standard), plus the
 * internal DALL-E Sprite summon. Display names are the AI model / company names
 * as required by the spec; descriptive nicknames are subtitles in the catalog.
 */
public enum TowerType {
    // Heroes
    BELLARMINE_STUDENT("Bellarmine Student", Faction.HERO, 0xd8d8d8, 0x1b3a6b),
    BELLARMINE_STUDENT_AI("Bellarmine Student + AI", Faction.HERO, 0x9fd3ff, 0x1b3a6b),
    BRAD_LINDEMANN("Brad Lindemann", Faction.HERO, 0xffd700, 0x111111),
    // Primary
    GEMINI("Gemini", Faction.PRIMARY, 0x4f7cff, 0x9b5cff),
    CLAUDE("Claude", Faction.PRIMARY, 0xe88a4a, 0xf3d9c4),
    GPT("GPT", Faction.PRIMARY, 0x10a37f, 0xffffff),
    // Military
    DEEPSEEK("DeepSeek", Faction.MILITARY, 0x1f3b8f, 0x4d7cff),
    COPILOT("Copilot", Faction.MILITARY, 0x2f7fe0, 0xb8dcff),
    // Magic
    DALLE("DALL·E", Faction.MAGIC, 0xffb347, 0x5bc0eb),
    SORA("Sora", Faction.MAGIC, 0x8fd3ff, 0xffffff),
    SUNO("Suno", Faction.MAGIC, 0xff5fa2, 0xffd1e6),
    // Support
    GROK("Grok", Faction.SUPPORT, 0x151515, 0xff7a00),
    PERPLEXITY("Perplexity", Faction.SUPPORT, 0x20b2aa, 0xe0fffa),
    // Experimental Titans
    QWEN("Qwen", Faction.TITAN, 0x7b3fe4, 0xd9c6ff),
    LLAMA("Llama", Faction.TITAN, 0xf2c94c, 0x8a5a2b),
    MISTRAL("Mistral", Faction.TITAN, 0x2bb3a3, 0xd8fff6),
    COHERE("Cohere", Faction.TITAN, 0xff7f6e, 0xffe3de),
    GLM("GLM", Faction.TITAN, 0x19e6ff, 0x0a2a3a),
    KIMI("Kimi", Faction.TITAN, 0x3d3fa8, 0xc6c7ff),
    MINIMAX("MiniMax", Faction.TITAN, 0xe03131, 0xffd6d6),
    // Internal summon (not in shop)
    DALLE_SPRITE("Sprite", Faction.MAGIC, 0xffe08a, 0x5bc0eb);

    public final String displayName;
    public final Faction faction;
    public final int primaryRgb;
    public final int secondaryRgb;

    TowerType(String displayName, Faction faction, int primaryRgb, int secondaryRgb) {
        this.displayName = displayName; this.faction = faction;
        this.primaryRgb = primaryRgb; this.secondaryRgb = secondaryRgb;
    }

    public boolean isHero() { return faction == Faction.HERO; }
    public boolean isPurchasable() { return this != DALLE_SPRITE; }

    /** The 20 purchasable towers in shop order. */
    public static TowerType[] purchasable() {
        TowerType[] all = values();
        TowerType[] out = new TowerType[all.length - 1];
        int i = 0;
        for (TowerType t : all) if (t.isPurchasable()) out[i++] = t;
        return out;
    }
}
