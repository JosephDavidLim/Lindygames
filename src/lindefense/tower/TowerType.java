package lindefense.tower;

/**
 * Identifiers for every playable unit: 6 Hero Store heroes and 18 standard towers (including Zethos, a Primary
 * tower), plus the internal DALL-E Sprite summon. Heroes are unlocked in the Hero Store and are never sold in the
 * regular tower shop. Display names are shown in the UI; descriptive nicknames are subtitles in the catalog.
 */
public enum TowerType {
    // Heroes (Hero Store only)
    BRAD_LINDEMANN("Brad Lindemann", Faction.HERO, 0xffd700, 0x111111),
    BRANDON("Brandon", Faction.HERO, 0x9dff3a, 0x16301a),
    ROHAN("Rohan", Faction.HERO, 0xbff6ff, 0x2563a8),
    RONIL("Ronil", Faction.HERO, 0xff5a1f, 0x3a1206),
    SHASHANK("Shashank", Faction.HERO, 0x46e08a, 0x0e2b1d),
    OWEN("Owen", Faction.HERO, 0xffa13d, 0x2b2f3a),
    // Primary
    GEMINI("Gemini", Faction.PRIMARY, 0x4f7cff, 0x9b5cff),
    CLAUDE("Claude", Faction.PRIMARY, 0xe88a4a, 0xf3d9c4),
    GPT("GPT", Faction.PRIMARY, 0x10a37f, 0xffffff),
    ZETHOS("Zethos", Faction.PRIMARY, 0x9a6bff, 0x22e6ff),
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

    /** The six Hero Store heroes in store order. */
    public static TowerType[] heroes() {
        java.util.List<TowerType> l = new java.util.ArrayList<>();
        for (TowerType t : values()) if (t.isHero()) l.add(t);
        return l.toArray(new TowerType[0]);
    }

    /** The regular tower shop's towers (everything purchasable except heroes) in shop order. */
    public static TowerType[] shopTowers() {
        java.util.List<TowerType> l = new java.util.ArrayList<>();
        for (TowerType t : values()) if (t.isPurchasable() && !t.isHero()) l.add(t);
        return l.toArray(new TowerType[0]);
    }

    /** Every placeable unit (heroes + shop towers) in catalog order. */
    public static TowerType[] purchasable() {
        TowerType[] all = values();
        TowerType[] out = new TowerType[all.length - 1];
        int i = 0;
        for (TowerType t : all) if (t.isPurchasable()) out[i++] = t;
        return out;
    }
}
