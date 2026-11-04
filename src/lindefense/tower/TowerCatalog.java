package lindefense.tower;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import lindefense.combat.DamageType;
import lindefense.tower.AbilityDef.Effect;

/**
 * Central tower data table: every tower's identity, base cost, base stats,
 * exactly four upgrades and one active ability. Tune values here; game logic
 * never hard-codes per-tower numbers.
 *
 * <p>Progression: base form + 4 purchasable upgrades = 5 states (tier 0-4).
 * This deliberately deviates from the master spec's "exactly 3 upgrades"
 * (section 4) at the user's request. Upgrade names are the real model lineups;
 * the fourth upgrade is an expensive "superclass" form that changes how the
 * tower plays. The displayed tower name stays the family name.</p>
 *
 * <p>Conventions: range in world px (field is 1200x900), rate in attacks per
 * second, speeds in px/s. Upgrade costs follow roughly 60% / 100% / 170% / 450%
 * of the base price; heroes are pricier and Brad Lindemann is by far the most
 * expensive unit in the game.</p>
 */
public final class TowerCatalog {
    public static final int UPGRADES = 4;
    private static final Map<TowerType, TowerDef> DEFS = new EnumMap<>(TowerType.class);

    private TowerCatalog() { }

    public static TowerDef get(TowerType type) { return DEFS.get(type); }
    public static Map<TowerType, TowerDef> all() { return Collections.unmodifiableMap(DEFS); }

    private static TowerStats stats(AttackKind kind, double dmg, DamageType dt, double range, double rate, int pierce) {
        TowerStats s = new TowerStats();
        s.kind = kind; s.hit.damage = dmg; s.hit.type = dt; s.range = range; s.rate = rate; s.pierce = pierce;
        return s;
    }

    /** An upgrade effect; the model name is supplied by the lineup in {@link #def}. */
    private static final class Up {
        final int cost; final String mechanic; final String desc; final Consumer<TowerStats> f;
        Up(int cost, String mechanic, String desc, Consumer<TowerStats> f) { this.cost = cost; this.mechanic = mechanic; this.desc = desc; this.f = f; }
    }

    private static Up up(int cost, String mechanic, String desc, Consumer<TowerStats> f) { return new Up(cost, mechanic, desc, f); }

    private static AbilityDef ab(String name, String desc, double cd, Effect e, double dur, double mag, double sec) {
        return new AbilityDef(name, desc, cd, e, dur, mag, sec);
    }

    /**
     * @param lineup five model names: base, u1, u2, u3, superclass u4
     */
    private static void def(TowerType type, String subtitle, String desc, int cost, TowerStats base, String[] lineup,
                            Up u1, Up u2, Up u3, Up u4, AbilityDef ability) {
        if (lineup.length != UPGRADES + 1) throw new IllegalArgumentException(type + " lineup must have 5 names");
        Up[] ups = { u1, u2, u3, u4 };
        UpgradeDef[] defs = new UpgradeDef[UPGRADES];
        for (int i = 0; i < UPGRADES; i++) {
            String d = (i == UPGRADES - 1 ? "SUPERCLASS - " : "") + ups[i].mechanic + ": " + ups[i].desc;
            defs[i] = new UpgradeDef(lineup[i + 1], ups[i].cost, d, ups[i].f);
        }
        DEFS.put(type, new TowerDef(type, subtitle, desc, cost, 20, base, defs, ability, lineup));
    }

    private static String[] lineup(String... names) { return names; }

    static {
        // =============================== HEROES ===============================
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 3, DamageType.PHYSICAL, 150, 1.0, 2);
            b.projectileSpeed = 380;
            def(TowerType.BELLARMINE_STUDENT, "Textbook Thrower",
                "Hero. Hurls heavy textbooks for solid physical damage. A cheap, reliable kinetic alternative to energy towers.",
                500, b, lineup("Freshman", "Sophomore", "Junior", "Senior", "Valedictorian"),
                up(300, "Caffeine Rush", "Attack speed 1.8/s, damage 4.", s -> { s.rate = 1.8; s.hit.damage = 4; }),
                up(500, "Varsity Arm", "Damage 6, pierce 4, knockback, kinetic hits penetrate Physical immunity.",
                    s -> { s.hit.damage = 6; s.pierce = 4; s.hit.knockback = 12; s.hit.ignoresResistance = true; }),
                up(850, "Senior Project", "Damage 10, two books per throw, +100% vs bosses, range 180.",
                    s -> { s.hit.damage = 10; s.projectilesPerShot = 2; s.hit.bonusVsBoss = 1.0; s.range = 180; }),
                up(2000, "Commencement Barrage", "Damage 18, three books per throw at 2.5/s, pierce 6, knockback 20, +200% vs bosses, books burst in a 40 px splash, range 210.",
                    s -> { s.hit.damage = 18; s.projectilesPerShot = 3; s.rate = 2.5; s.pierce = 6; s.hit.knockback = 20; s.hit.bonusVsBoss = 2.0; s.splashRadius = 40; s.range = 210; }),
                ab("All-Nighter", "Triples attack speed for 10 s.", 60, Effect.SELF_BUFF, 10, 3.0, 1.0));
        }
        {
            TowerStats b = stats(AttackKind.INSTANT, 2, DamageType.ENERGY, 190, 1.5, 1);
            b.seesCamo = true;
            def(TowerType.BELLARMINE_STUDENT_AI, "Autonomous Scholar",
                "Hero. A student armed with an AI agent. Sees camo, auto-prioritises camo and bosses, and shares its detection.",
                700, b, lineup("Prompt Engineer", "Pair Programmer", "Agent Orchestrator", "AGI Co-Author", "Human-AI Singularity"),
                up(400, "Shared Detection", "Towers within range gain camo detection. Damage 3.",
                    s -> { s.auraDetection = true; s.auraRange = 190; s.hit.damage = 3; }),
                up(700, "Autonomous Agent", "Fires at 2 targets at once, damage 4, 2/s, hits mark enemies (+15% damage taken).",
                    s -> { s.targets = 2; s.hit.damage = 4; s.rate = 2.0; s.hit.markBonus = 0.15; s.hit.markDuration = 3; }),
                up(1200, "AGI Thesis", "3 targets, damage 7, full armour piercing. Towers in aura are immune to Predictive Surveillance.",
                    s -> { s.targets = 3; s.hit.damage = 7; s.hit.armorPierce = 1.0; s.auraSurveillanceImmunity = true; s.auraRange = 220; }),
                up(2800, "Recursive Self-Improvement", "5 targets, damage 12, 2.5/s, mark +30%. Aura 280 px: camo detection, Surveillance immunity, +15% attack speed and +10% range for every tower inside.",
                    s -> { s.targets = 5; s.hit.damage = 12; s.rate = 2.5; s.hit.markBonus = 0.30; s.auraRange = 280; s.auraRate = 0.15; s.auraRangeBonus = 0.10; s.range = 230; }),
                ab("Reveal All", "Reveals every camo enemy for 12 s and stuns camo enemies for 1.5 s.", 50, Effect.REVEAL_ALL, 12, 1.5, 0));
        }
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 12, DamageType.EXPLOSIVE, 250, 0.5, 12);
            b.projectileSpeed = 300; b.splashRadius = 70; b.hit.armorPierce = 0.5; b.hit.bonusVsBoss = 0.5;
            b.auraRange = 220; b.auraDamage = 0.15;
            def(TowerType.BRAD_LINDEMANN, "The Commander",
                "Hero. Commander support, battlefield control and heavy artillery. Every tower in his aura hits harder.",
                2500, b, lineup("Lieutenant", "Captain", "Colonel", "General", "Supreme Commander"),
                up(1500, "Field Commander", "Aura +25% damage and +10% attack speed. Shells deal 20 damage with 90 px splash.",
                    s -> { s.auraDamage = 0.25; s.auraRate = 0.10; s.hit.damage = 20; s.splashRadius = 90; }),
                up(2500, "Siege Battery", "Two shells per salvo, 30 damage, 110 px splash, full armour piercing, 0.5 s stun on non-bosses. Aura 280.",
                    s -> { s.projectilesPerShot = 2; s.hit.damage = 30; s.splashRadius = 110; s.hit.armorPierce = 1.0; s.hit.stunDuration = 0.5; s.auraRange = 280; }),
                up(4500, "Theatre Command", "60 damage, 130 px splash, 0.7/s, +150% vs bosses. Aura +40% damage, +20% speed, camo detection, Surveillance immunity.",
                    s -> { s.hit.damage = 60; s.splashRadius = 130; s.rate = 0.7; s.hit.bonusVsBoss = 1.5; s.auraDamage = 0.40; s.auraRate = 0.20; s.auraDetection = true; s.auraSurveillanceImmunity = true; s.seesCamo = true; }),
                up(9000, "Total War Doctrine", "Three 120-damage shells per salvo at 0.9/s with 160 px splash, +250% vs bosses, 1 s stun (bosses 0.3 s). Aura 340 px: +60% damage, +35% attack speed, +15% range, detection and Surveillance immunity.",
                    s -> { s.projectilesPerShot = 3; s.hit.damage = 120; s.rate = 0.9; s.splashRadius = 160; s.hit.bonusVsBoss = 2.5; s.hit.stunDuration = 1.0; s.hit.bossStunDuration = 0.3; s.auraRange = 340; s.auraDamage = 0.60; s.auraRate = 0.35; s.auraRangeBonus = 0.15; s.range = 300; }),
                ab("Command Override", "All towers +50% damage for 8 s; clears Predictive Surveillance everywhere.", 60, Effect.COMMAND_OVERRIDE, 8, 1.5, 0));
        }

        // =============================== PRIMARY ===============================
        {
            TowerStats b = stats(AttackKind.CHAIN, 1, DamageType.ENERGY, 175, 1.2, 1);
            b.chainHops = 3; b.chainRange = 90;
            def(TowerType.GEMINI, "Prism Chain",
                "Arcs of light that chain between enemies. Excels against large groups and splitters.",
                220, b, lineup("Gemini 3.7 Flash", "Gemini 3.8 Flash", "Gemini 3.1 Pro", "Gemini 4 Argon", "Gemini Ultra"),
                up(130, "Multimodal Arcs", "Chains to 5 enemies, chain reach 115 px.", s -> { s.chainHops = 5; s.chainRange = 115; }),
                up(220, "Flash Overcharge", "Damage 2, attack speed 1.8/s.", s -> { s.hit.damage = 2; s.rate = 1.8; }),
                up(370, "Ultra Prism", "Chains to 9 enemies, damage 3, range 210, +100% damage vs splitting enemies.",
                    s -> { s.chainHops = 9; s.hit.damage = 3; s.range = 210; s.hit.bonusVsSplitters = 1.0; }),
                up(1000, "Prismatic Storm", "Chains to 16 enemies within 160 px, damage 6, 2.2/s, range 260; every arc slows 20% for 1 s.",
                    s -> { s.chainHops = 16; s.chainRange = 160; s.hit.damage = 6; s.rate = 2.2; s.range = 260; s.hit.slowFraction = 0.2; s.hit.slowDuration = 1; }),
                ab("Thunder Prism", "6 damage to every enemy in range and a 1 s stun.", 50, Effect.AREA_BLAST, 1.0, 6, 0));
        }
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 1, DamageType.ENERGY, 150, 1.0, 6);
            b.projectileSpeed = 450;
            def(TowerType.CLAUDE, "Constitutional Lance",
                "Fires long piercing thought-beams through whole lines of enemies. Later slows what it hits.",
                160, b, lineup("Claude Haiku 5.5", "Claude Sonnet 5.5", "Claude Fable 5.1", "Claude Opus 5.5", "Claude Mythos"),
                up(100, "Extended Thinking", "Pierce 12, range 185.", s -> { s.pierce = 12; s.range = 185; }),
                up(160, "Constitutional Slow", "Hits slow enemies by 30% for 2 s.", s -> { s.hit.slowFraction = 0.30; s.hit.slowDuration = 2; }),
                up(270, "Opus Beam", "Instant beam through everything on its line: damage 3, 1.5/s, 40% slow for 2.5 s.",
                    s -> { s.kind = AttackKind.INSTANT; s.hit.damage = 3; s.rate = 1.5; s.pierce = 16; s.hit.slowFraction = 0.40; s.hit.slowDuration = 2.5; }),
                up(720, "Mythos Lattice", "Two simultaneous beams at 2/s, damage 6, pierce 30, 50% slow for 3 s and a 0.3 s stun on non-bosses; range 220.",
                    s -> { s.targets = 2; s.rate = 2.0; s.hit.damage = 6; s.pierce = 30; s.hit.slowFraction = 0.5; s.hit.slowDuration = 3; s.hit.stunDuration = 0.3; s.range = 220; }),
                ab("Refusal Field", "Stuns all non-boss enemies in range for 2.5 s; bosses are slowed 50%.", 45, Effect.AREA_BLAST, 2.5, 0, 0.5));
        }
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 1, DamageType.ENERGY, 165, 1.5, 2);
            b.projectileSpeed = 420;
            def(TowerType.GPT, "Adaptive Generalist",
                "Reliable token bolts. Adapts to any enemy type, making it a safe all-rounder.",
                180, b, lineup("GPT-5.6 Terra", "GPT-6 Luna", "GPT-6.1 Sol", "GPT-6 Astra", "GPT Cosmos"),
                up(110, "Longer Context", "Range 210, pierce 3.", s -> { s.range = 210; s.pierce = 3; }),
                up(180, "Adaptive Tokens", "Damage 2. Attacks adapt: ignore every immunity and resistance.",
                    s -> { s.hit.damage = 2; s.hit.ignoresResistance = true; }),
                up(300, "Reasoning Model", "Damage 4, 2.5/s, pierce 5, bolts burst in a 45 px splash.",
                    s -> { s.hit.damage = 4; s.rate = 2.5; s.pierce = 5; s.splashRadius = 45; }),
                up(800, "Cosmic Inference", "Fires at 3 targets at once, damage 8, 3/s, 70 px splash hitting up to 8, +50% vs bosses, range 240.",
                    s -> { s.targets = 3; s.hit.damage = 8; s.rate = 3.0; s.splashRadius = 70; s.pierce = 8; s.hit.bonusVsBoss = 0.5; s.range = 240; }),
                ab("Function Call", "Triples attack speed for 6 s.", 40, Effect.SELF_BUFF, 6, 3.0, 1.0));
        }

        // =============================== MILITARY ===============================
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 5, DamageType.ENERGY, 220, 0.6, 1);
            b.projectileSpeed = 700; b.hit.bonusVsComplex = 0.5;
            def(TowerType.DEEPSEEK, "Deep Strike Railgun",
                "Slow, devastating railgun shots. +damage against complex data structures and bosses.",
                250, b, lineup("DeepSeek V4 Flash", "DeepSeek V4.1 Flash", "DeepSeek R1", "DeepSeek V4 Pro", "DeepSeek Abyss"),
                up(150, "R1 Reasoning", "Damage 8, +100% vs complex enemies and bosses.", s -> { s.hit.damage = 8; s.hit.bonusVsComplex = 1.0; }),
                up(250, "Armor Exploit", "Full armour piercing; hits break 15% armour for 4 s.",
                    s -> { s.hit.armorPierce = 1.0; s.hit.armorBreak = 0.15; s.hit.armorBreakDuration = 4; }),
                up(430, "Mixture of Experts", "Fires at the 3 strongest targets at once, damage 12, 0.8/s.",
                    s -> { s.targets = 3; s.hit.damage = 12; s.rate = 0.8; }),
                up(1150, "Abyssal Lance", "5 targets, damage 24 at 1/s, +150% vs complex enemies and bosses, armour break 30% for 5 s, range 280.",
                    s -> { s.targets = 5; s.hit.damage = 24; s.rate = 1.0; s.hit.bonusVsComplex = 1.5; s.hit.armorBreak = 0.30; s.hit.armorBreakDuration = 5; s.range = 280; }),
                ab("Deep Dive", "Quadruples damage for 8 s.", 60, Effect.SELF_BUFF, 8, 1.0, 4.0));
        }
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 1, DamageType.PHYSICAL, 150, 2.0, 2);
            b.projectileSpeed = 420; b.auraRange = 150; b.auraRate = 0.15; b.auraRateMilitary = 0.30;
            def(TowerType.COPILOT, "Pair Programmer",
                "Rapid code snippets plus an aura that speeds up nearby towers (Military towers twice as much).",
                200, b, lineup("Copilot", "Copilot Pro", "Microsoft 365 Copilot", "GitHub Copilot Enterprise", "Copilot Autopilot"),
                up(120, "Autocomplete", "Aura +25% speed (Military +50%), range 185.", s -> { s.auraRate = 0.25; s.auraRateMilitary = 0.50; s.range = 185; s.auraRange = 185; }),
                up(200, "Workspace Firewall", "Towers in aura are immune to Predictive Surveillance and gain +10% damage.",
                    s -> { s.auraSurveillanceImmunity = true; s.auraDamage = 0.10; }),
                up(340, "Agent Mode", "3 snippets per volley, damage 3, full armour piercing. Aura +35% speed (Military +70%).",
                    s -> { s.projectilesPerShot = 3; s.hit.damage = 3; s.hit.armorPierce = 1.0; s.auraRate = 0.35; s.auraRateMilitary = 0.70; }),
                up(900, "Autonomous Fleet", "5 snippets per volley, damage 5, 3/s, kinetic hits penetrate Physical immunity. Aura 240 px: +50% speed (Military +100%), +20% damage and camo detection.",
                    s -> { s.projectilesPerShot = 5; s.hit.damage = 5; s.rate = 3.0; s.hit.ignoresResistance = true; s.auraRange = 240; s.range = 240; s.auraRate = 0.50; s.auraRateMilitary = 1.0; s.auraDamage = 0.20; s.auraDetection = true; s.seesCamo = true; }),
                ab("Code Review", "Every tower in range attacks twice as fast for 5 s.", 50, Effect.TEAM_RATE_BUFF, 5, 2.0, 0));
        }

        // =============================== MAGIC ===============================
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 1, DamageType.MAGIC, 150, 0.9, 8);
            b.projectileSpeed = 300; b.splashRadius = 45;
            def(TowerType.DALLE, "Image Generator",
                "Paints bursts of colour that splash over groups and later summons short-lived Sprites that fight alongside it.",
                210, b, lineup("DALL·E", "DALL·E 2", "DALL·E 3", "GPT Image", "DALL·E Renaissance"),
                up(125, "Inpainting", "Splash 65 px, damage 2.", s -> { s.splashRadius = 65; s.hit.damage = 2; }),
                up(210, "Sprite Summons", "Every 12 s summons a Sprite (1 dmg, 2/s) that fights for 10 s.",
                    s -> { s.summonInterval = 12; s.summonCount = 1; s.summonLifetime = 10; }),
                up(360, "Masterpiece", "Damage 4, splash 90 px, two Sprites at a time that last 15 s.",
                    s -> { s.hit.damage = 4; s.splashRadius = 90; s.summonCount = 2; s.summonLifetime = 15; }),
                up(950, "Living Gallery", "Damage 8 at 1.2/s, 120 px splash hitting up to 16, range 190; summons 4 Sprites every 8 s that last 20 s.",
                    s -> { s.hit.damage = 8; s.rate = 1.2; s.splashRadius = 120; s.pierce = 16; s.range = 190; s.summonCount = 4; s.summonInterval = 8; s.summonLifetime = 20; }),
                ab("Generate", "Instantly summons 3 Sprites for 12 s.", 45, Effect.SUMMON, 12, 3, 0));
        }
        {
            TowerStats b = stats(AttackKind.PULSE, 1, DamageType.MAGIC, 140, 1.0, 12);
            b.hit.slowFraction = 0.25; b.hit.slowDuration = 1.5;
            def(TowerType.SORA, "Time Weaver",
                "Pulses that slow time for everything nearby. The premier slowing tower.",
                280, b, lineup("Sora", "Sora Turbo", "Sora 2", "Sora 2 Pro", "Sora Chronos"),
                up(170, "Slow Motion", "Slow 40%, range 165.", s -> { s.hit.slowFraction = 0.40; s.range = 165; }),
                up(280, "Frame Drop", "Every 4th pulse stuns non-bosses in range for 0.6 s.", s -> { s.heavyEvery = 4; s.heavyStun = 0.6; }),
                up(480, "Temporal Rewind", "Damage 3, a pulse every 0.7 s, 50% slow, each pulse rewinds enemies 10 px (bosses 3 px).",
                    s -> { s.hit.damage = 3; s.rate = 1 / 0.7; s.hit.slowFraction = 0.50; s.hit.knockback = 10; s.hit.bossKnockback = 3; s.pierce = 30; }),
                up(1250, "Chronos Field", "A pulse every 0.5 s, damage 5, 60% slow, rewinds 20 px (bosses 6 px), every 3rd pulse stuns 0.8 s; range 200, hits up to 60.",
                    s -> { s.rate = 2.0; s.hit.damage = 5; s.hit.slowFraction = 0.60; s.hit.knockback = 20; s.hit.bossKnockback = 6; s.heavyEvery = 3; s.heavyStun = 0.8; s.range = 200; s.pierce = 60; }),
                ab("Freeze Frame", "Stuns every enemy on the map for 2 s (bosses 1 s).", 75, Effect.GLOBAL_FREEZE, 2, 1, 0));
        }
        {
            TowerStats b = stats(AttackKind.PULSE, 1, DamageType.MAGIC, 125, 1.25, 50);
            def(TowerType.SUNO, "Sonic Composer",
                "Sound waves that hit everything in range at once. Louder in a crowd.",
                260, b, lineup("Suno v5", "Suno v6-Mini", "Suno v6", "Suno v6-Wild", "Suno Symphony"),
                up(155, "Bass Boost", "Range 155, damage 2.", s -> { s.range = 155; s.hit.damage = 2; }),
                up(260, "Resonance", "+1 damage per 5 enemies in range (max +4).", s -> { s.resonancePer = 5; s.resonanceCap = 4; }),
                up(440, "Full Album", "Pulse every 0.5 s, damage 3, each pulse pushes non-bosses back 6 px.",
                    s -> { s.rate = 2.0; s.hit.damage = 3; s.hit.knockback = 6; }),
                up(1150, "Grand Symphony", "3 pulses per second, damage 5, +1 per 3 enemies (max +8), pushes 10 px, +50% vs splitters, range 200, hits up to 100.",
                    s -> { s.rate = 3.0; s.hit.damage = 5; s.resonancePer = 3; s.resonanceCap = 8; s.hit.knockback = 10; s.hit.bonusVsSplitters = 0.5; s.range = 200; s.pierce = 100; }),
                ab("Drop the Beat", "20 damage to every enemy in range and a 1.5 s stun.", 50, Effect.AREA_BLAST, 1.5, 20, 0));
        }

        // =============================== SUPPORT ===============================
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 1, DamageType.ENERGY, 165, 1.3, 1);
            b.projectileSpeed = 400; b.hit.disruptDuration = 3; b.hit.slowFraction = 0.2; b.hit.slowDuration = 1;
            def(TowerType.GROK, "Disruptor",
                "Snarky bolts that disable enemy special mechanics (swaps, jumps, shields, storing) for a few seconds.",
                170, b, lineup("Grok 4.5", "Grok 4.6", "Grok 4.20 Reasoning", "Grok 4.7", "Grok Heavy"),
                up(100, "Meme Volley", "3 bolts per volley, pierce 2.", s -> { s.projectilesPerShot = 3; s.pierce = 2; }),
                up(170, "Chaos Field", "Bolts burst in a 50 px disruption splash (4 s disrupt, 30% slow 2 s), damage 2.",
                    s -> { s.splashRadius = 50; s.pierce = 8; s.hit.disruptDuration = 4; s.hit.slowFraction = 0.3; s.hit.slowDuration = 2; s.hit.damage = 2; }),
                up(290, "Unhinged Mode", "Damage 3, 2/s, splash 70 px, disrupted enemies take +25% damage from everything.",
                    s -> { s.hit.damage = 3; s.rate = 2.0; s.splashRadius = 70; s.hit.markBonus = 0.25; s.hit.markDuration = 4; }),
                up(760, "Maximum Chaos", "5 bolts per volley, damage 6, 2.5/s, 100 px splash, 6 s disrupt, 40% slow for 3 s, marked +40% for 5 s; range 200.",
                    s -> { s.projectilesPerShot = 5; s.hit.damage = 6; s.rate = 2.5; s.splashRadius = 100; s.hit.disruptDuration = 6; s.hit.slowFraction = 0.4; s.hit.slowDuration = 3; s.hit.markBonus = 0.40; s.hit.markDuration = 5; s.range = 200; s.pierce = 12; }),
                ab("System Prompt Leak", "Disrupts everything in range for 4 s and knocks it back 50 px (bosses 12 px).", 45, Effect.AREA_BLAST, 4, 0, 50));
        }
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 1, DamageType.ENERGY, 185, 1.0, 1);
            b.projectileSpeed = 450; b.seesCamo = true; b.auraDetection = true; b.auraRange = 185; b.incomePerWave = 15;
            def(TowerType.PERPLEXITY, "Research Engine",
                "Sees camo and shares detection with towers in range. Earns cash at the end of every wave.",
                250, b, lineup("Sonar", "Sonar Pro", "Sonar Reasoning Pro", "Sonar Deep Research", "Perplexity Oracle"),
                up(150, "Cited Sources", "Aura 225 px, hits mark enemies (+15% damage taken, 3 s), income $30/wave.",
                    s -> { s.auraRange = 225; s.range = 225; s.hit.markBonus = 0.15; s.hit.markDuration = 3; s.incomePerWave = 30; }),
                up(250, "Pro Search", "Damage 2, mark +25%, towers in aura gain +10% range, income $60/wave.",
                    s -> { s.hit.damage = 2; s.hit.markBonus = 0.25; s.auraRangeBonus = 0.10; s.incomePerWave = 60; }),
                up(430, "Encrypted Index", "Towers in aura are immune to Predictive Surveillance. Damage 3, 1.5/s, mark +35%, income $120/wave.",
                    s -> { s.auraSurveillanceImmunity = true; s.hit.damage = 3; s.rate = 1.5; s.hit.markBonus = 0.35; s.incomePerWave = 120; }),
                up(1150, "Omniscient Index", "Income $250/wave. Aura 300 px: detection, Surveillance immunity, +20% range, +15% damage. Darts pierce 3, damage 5 at 2/s, mark +50%.",
                    s -> { s.incomePerWave = 250; s.auraRange = 300; s.range = 300; s.auraRangeBonus = 0.20; s.auraDamage = 0.15; s.pierce = 3; s.hit.damage = 5; s.rate = 2.0; s.hit.markBonus = 0.50; }),
                ab("Deep Research", "Grants $100 + $5 per wave and reveals all camo enemies for 10 s.", 60, Effect.DEEP_RESEARCH, 10, 100, 5));
        }

        // =============================== TITANS ===============================
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 1, DamageType.ENERGY, 140, 1.6, 3);
            b.projectileSpeed = 400; b.hit.bonusVsFortified = 1.0; b.hit.bonusVsComplex = 0.5;
            def(TowerType.QWEN, "Layer Stripper",
                "Cheap and tireless. Shears layers off thick enemies and punishes Fortified targets.",
                140, b, lineup("Qwen3.8 Flash", "Qwen3.6 Max", "Qwen3.7 Plus", "Qwen3.8 Max", "Qwen Omni"),
                up(85, "Dense Layers", "Damage 2, pierce 4.", s -> { s.hit.damage = 2; s.pierce = 4; }),
                up(140, "Fortress Breaker", "+200% damage vs Fortified, full armour piercing.", s -> { s.hit.bonusVsFortified = 2.0; s.hit.armorPierce = 1.0; }),
                up(240, "Qwen-Max", "Damage 3, 2.2/s, hits strip Fortified from non-boss enemies.", s -> { s.hit.damage = 3; s.rate = 2.2; s.hit.stripsFortified = true; }),
                up(650, "Omni Shear", "Two targets, damage 6 at 3/s, pierce 8, +300% vs Fortified, +100% vs complex enemies, range 180.",
                    s -> { s.targets = 2; s.hit.damage = 6; s.rate = 3.0; s.pierce = 8; s.hit.bonusVsFortified = 3.0; s.hit.bonusVsComplex = 1.0; s.range = 180; }),
                ab("Quantize", "Strips Fortified from every non-boss in range; bosses take 60 true damage.", 60, Effect.QUANTIZE, 0, 60, 0));
        }
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 2, DamageType.PHYSICAL, 150, 1.2, 1);
            b.projectileSpeed = 330; b.hit.knockback = 8;
            def(TowerType.LLAMA, "Open Herd",
                "Kinetic spit that shoves enemies back down the track. Physical damage with real stopping power.",
                190, b, lineup("Llama 4 Scout", "Llama 4 Maverick", "Muse Glimmer", "Muse Wonder", "Llama Behemoth"),
                up(115, "Herd Charge", "Damage 3, knockback 15 px, pierce 2.", s -> { s.hit.damage = 3; s.hit.knockback = 15; s.pierce = 2; }),
                up(190, "Kinetic Penetrator", "Damage 4; kinetic hits penetrate Physical immunity (Heap Sort, Lockheed).",
                    s -> { s.hit.damage = 4; s.hit.ignoresResistance = true; }),
                up(320, "Llama Stampede", "3 projectiles per shot, damage 5, knockback 20, 1.5/s, +50% vs bosses.",
                    s -> { s.projectilesPerShot = 3; s.hit.damage = 5; s.hit.knockback = 20; s.rate = 1.5; s.hit.bonusVsBoss = 0.5; }),
                up(850, "Behemoth Charge", "5 projectiles per shot, damage 10 at 2/s, pierce 4, knockback 30 (bosses 4), +100% vs bosses, range 190.",
                    s -> { s.projectilesPerShot = 5; s.hit.damage = 10; s.rate = 2.0; s.pierce = 4; s.hit.knockback = 30; s.hit.bossKnockback = 4; s.hit.bonusVsBoss = 1.0; s.range = 190; }),
                ab("Stampede", "10 physical damage to everything in range; non-bosses are shoved back 90 px.", 45, Effect.AREA_BLAST, 0, 10, 90));
        }
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 1, DamageType.MAGIC, 165, 1.4, 5);
            b.projectileSpeed = 500; b.hit.slowFraction = 0.2; b.hit.slowDuration = 1.5; b.hit.bonusVsFast = 1.0;
            def(TowerType.MISTRAL, "Gale Caster",
                "Wind blades that slice through lines of enemies, slow them, and deal double damage to fast movers.",
                200, b, lineup("Ministral 3 8B", "Ministral 3 14B", "Mistral Small 4", "Mistral Large 4", "Mistral Tempest"),
                up(120, "Tailwind Cutter", "Pierce 8, slow 30%.", s -> { s.pierce = 8; s.hit.slowFraction = 0.30; }),
                up(200, "Headwind", "Hits push enemies back 12 px (bosses 2 px), slow 40%.", s -> { s.hit.knockback = 12; s.hit.bossKnockback = 2; s.hit.slowFraction = 0.40; }),
                up(340, "Mistral Large", "Damage 3, 2.2/s, pierce 12. Wind Wall: enemies inside range are passively slowed 20%.",
                    s -> { s.hit.damage = 3; s.rate = 2.2; s.pierce = 12; s.auraEnemySlow = 0.20; }),
                up(900, "Tempest Core", "Damage 6 at 3/s, pierce 20, 50% slow, knockback 20 (bosses 4), +150% vs fast enemies; Wind Wall slows 35% in a 220 px range.",
                    s -> { s.hit.damage = 6; s.rate = 3.0; s.pierce = 20; s.hit.slowFraction = 0.5; s.hit.knockback = 20; s.hit.bossKnockback = 4; s.hit.bonusVsFast = 1.5; s.auraEnemySlow = 0.35; s.range = 220; }),
                ab("Cyclone", "Blows all non-bosses in range back 120 px and slows everything 50% for 4 s.", 50, Effect.AREA_BLAST, 4, 0, 120));
        }
        {
            TowerStats b = stats(AttackKind.INSTANT, 3, DamageType.ENERGY, 185, 0.8, 1);
            b.hit.armorPierce = 0.5; b.hit.bonusVsBoss = 0.5;
            def(TowerType.COHERE, "Command Breaker",
                "Single-target rerank beams built for armoured, powerful enemies.",
                240, b, lineup("Command R7B", "Command R+", "Command A", "Command A+", "Command Apex"),
                up(145, "Rerank", "Damage 5; hits break 20% armour for 5 s (helps every tower).", s -> { s.hit.damage = 5; s.hit.armorBreak = 0.20; s.hit.armorBreakDuration = 5; }),
                up(240, "Command R+", "Damage 8, full armour piercing, +100% vs bosses.", s -> { s.hit.damage = 8; s.hit.armorPierce = 1.0; s.hit.bonusVsBoss = 1.0; }),
                up(410, "Enterprise Grade", "Damage 14, 1/s, plus 2% of target max HP (cap 60) per hit.", s -> { s.hit.damage = 14; s.rate = 1.0; s.hit.pctMaxHp = 0.02; s.hit.pctCap = 60; }),
                up(1100, "Apex Directive", "Two beams at 1.5/s, damage 30, +200% vs bosses, 4% of max HP per hit (cap 150), armour break 30% for 6 s, range 220.",
                    s -> { s.targets = 2; s.rate = 1.5; s.hit.damage = 30; s.hit.bonusVsBoss = 2.0; s.hit.pctMaxHp = 0.04; s.hit.pctCap = 150; s.hit.armorBreak = 0.30; s.hit.armorBreakDuration = 6; s.range = 220; }),
                ab("Embed Lock", "150 true damage to the strongest enemy in range and a 2 s stun (bosses 1 s).", 55, Effect.STRONGEST_STRIKE, 2, 150, 1));
        }
        {
            TowerStats b = stats(AttackKind.PULSE, 1, DamageType.MAGIC, 150, 1 / 1.2, 40);
            b.hit.knockback = 6; b.hit.bossKnockback = 1; b.hit.slowFraction = 0.15; b.hit.slowDuration = 1;
            def(TowerType.GLM, "Gravity Grid",
                "A gravity well that drags everything nearby backwards and slows it. Crowd control on a grid.",
                300, b, lineup("GLM 5.1", "GLM 5.2", "GLM 5.3 Flash", "GLM 5.3", "GLM Singularity"),
                up(180, "Grid Lock", "Range 175, damage 2, slow 25%.", s -> { s.range = 175; s.hit.damage = 2; s.hit.slowFraction = 0.25; }),
                up(300, "Singularity Well", "Every 5th pulse stuns non-bosses 1 s and drags them 30 px.", s -> { s.heavyEvery = 5; s.heavyStun = 1.0; s.heavyKnockback = 30; }),
                up(510, "GLM-Ultra", "Pulse every 0.8 s, damage 4, drag 12 px, range 210.", s -> { s.rate = 1.25; s.hit.damage = 4; s.hit.knockback = 12; s.range = 210; }),
                up(1350, "Collapsing Star", "Pulse every 0.5 s, damage 7, drag 20 px (bosses 3), 40% slow, every 4th pulse stuns 1.5 s and drags 60 px; range 250, hits up to 60.",
                    s -> { s.rate = 2.0; s.hit.damage = 7; s.hit.knockback = 20; s.hit.bossKnockback = 3; s.hit.slowFraction = 0.4; s.heavyEvery = 4; s.heavyStun = 1.5; s.heavyKnockback = 60; s.range = 250; s.pierce = 60; }),
                ab("Event Horizon", "Drags all non-bosses in range back 150 px and stuns them 2 s.", 60, Effect.AREA_BLAST, 2, 0, 150));
        }
        {
            TowerStats b = stats(AttackKind.INSTANT, 2, DamageType.ENERGY, 175, 1.0, 1);
            b.stackCap = 6; b.stackStep = 1; b.hit.bonusVsNonFortified = 1.0; b.hit.bonusVsFortified = -0.5;
            def(TowerType.KIMI, "Long-Context Scaler",
                "Each consecutive hit on the same target hits harder. Double damage to non-Fortified enemies, weak against Fortified.",
                320, b, lineup("Kimi K2.5", "Kimi K2.6", "Kimi K3", "Kimi K3.1 Pro", "Kimi Moonshot"),
                up(190, "128K Context", "Stack cap 10, range 200.", s -> { s.stackCap = 10; s.range = 200; }),
                up(320, "Moonshot", "Base damage 3, 1.5/s, stacks linger 3 s after switching targets.", s -> { s.hit.damage = 3; s.rate = 1.5; s.stackLinger = 3; }),
                up(540, "Kimi K2", "Base damage 5, stack cap 20, +2 per stack, beam pierces one extra enemy.", s -> { s.hit.damage = 5; s.stackCap = 20; s.stackStep = 2; s.pierce = 2; }),
                up(1450, "Infinite Context", "Base damage 8 at 2/s, +3 per stack up to 40 stacks that linger 5 s, beam pierces 3, +150% vs non-Fortified and no Fortified penalty; range 230.",
                    s -> { s.hit.damage = 8; s.rate = 2.0; s.stackStep = 3; s.stackCap = 40; s.stackLinger = 5; s.pierce = 3; s.hit.bonusVsNonFortified = 1.5; s.hit.bonusVsFortified = 0; s.range = 230; }),
                ab("Context Dump", "Deals 15 x current stacks true damage around the current target, then resets stacks.", 50, Effect.CONTEXT_DUMP, 0, 15, 70));
        }
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 1, DamageType.MAGIC, 150, 1.1, 1);
            b.projectileSpeed = 360; b.bounces = 4; b.bounceRange = 100;
            def(TowerType.MINIMAX, "Clone Bouncer",
                "Orbs that ricochet between enemies and later clone themselves on impact. Great against big groups.",
                230, b, lineup("MiniMax M2.5", "MiniMax M2.7", "MiniMax M3", "MiniMax M3.1 Flash", "MiniMax Hydra"),
                up(140, "Hailuo Bounce", "7 bounces, bounce reach 125 px.", s -> { s.bounces = 7; s.bounceRange = 125; }),
                up(230, "Clone Protocol", "Orbs split into 2 on first impact, damage 2.", s -> { s.clones = 2; s.hit.damage = 2; }),
                up(390, "MiniMax-M1", "Damage 3, 1.8/s, 10 bounces, 3 clones.", s -> { s.hit.damage = 3; s.rate = 1.8; s.bounces = 10; s.clones = 3; }),
                up(1050, "Hydra Swarm", "Damage 6 at 2.5/s, 16 bounces within 160 px, 5 clones per impact, orbs pierce 2; range 190.",
                    s -> { s.hit.damage = 6; s.rate = 2.5; s.bounces = 16; s.bounceRange = 160; s.clones = 5; s.pierce = 2; s.range = 190; }),
                ab("Mass Clone", "Summons a temporary clone of this MiniMax (same tier) for 12 s.", 50, Effect.SUMMON, 12, 1, 1));
        }

        // Internal summon: DALL-E Sprite
        {
            TowerStats b = stats(AttackKind.PROJECTILE, 1, DamageType.MAGIC, 110, 2.0, 1);
            b.projectileSpeed = 380;
            UpgradeDef[] none = new UpgradeDef[UPGRADES];
            for (int i = 0; i < UPGRADES; i++) none[i] = new UpgradeDef("-", 0, "", s -> { });
            DEFS.put(TowerType.DALLE_SPRITE, new TowerDef(TowerType.DALLE_SPRITE, "Generated Sprite",
                "A short-lived painted familiar.", 0, 10, b, none, null,
                new String[] { "Sprite", "Sprite", "Sprite", "Sprite", "Sprite" }));
        }
    }
}
