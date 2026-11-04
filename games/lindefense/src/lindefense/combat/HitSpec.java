package lindefense.combat;

/**
 * Describes what one hit does to an enemy. Towers carry a HitSpec in their stats; abilities, summons and map
 * hazards build ad-hoc ones. All fields are public and mutable so upgrade lambdas can tweak them.
 * <p>The damage formula that consumes these fields lives in {@link Combat#applyHit(HitContext)}.
 * Defaults are neutral (multipliers 1, everything else 0).</p>
 */
public class HitSpec {
    public double damage = 1;
    public DamageType type = DamageType.ENERGY;
    /** Fraction of enemy armour ignored (1 = full armour piercing). */
    public double armorPierce = 0;
    /**
     * "Can damage Lead": Sharp hits of this spec ignore Lead-type immunity (Claude U3). Other damage types are
     * unaffected (they never needed it).
     */
    public boolean ignoresResistance = false;

    // ---- situational damage (spec): multipliers default 1, flats default 0 ----
    /** Multiplier vs MOAB-class (any boss), e.g. 3 = "x3 vs MOAB-class". */
    public double multVsBoss = 1;
    /** Flat extra damage vs MOAB-class, e.g. Brad "+2 dmg vs MOAB-class". */
    public double flatVsBoss = 0;
    public double multVsFortified = 1;
    public double flatVsFortified = 0;
    public double flatVsLead = 0;
    /** Multiplier vs Lead-type (study-buddy drones x3). */
    public double multVsLead = 1;
    public double flatVsCeramic = 0;
    /** Extra multiplier vs Ceramic-type, applied on top of the damage-type rule (x2 for BLUNT/KINETIC/SONIC). */
    public double multVsCeramic = 1;
    /** DeepSeek U3: multiplier vs all Sorting Algorithm enemies and Linked Lists. */
    public double multVsSortingAndLists = 1;
    /** Qwen U3: multiplier vs enemies whose base layer count (EnemyType.baseHp) is at least 5. */
    public double multVsHighHp = 1;

    // ---- legacy additive percent bonuses (0.5 = +50%); still honoured ----
    public double bonusVsBoss = 0;
    public double bonusVsFortified = 0;
    public double bonusVsNonFortified = 0;
    /** Against "complex" enemies (base HP >= 6 or boss). */
    public double bonusVsComplex = 0;
    /** Against fast enemies (base speed >= 1.8x). */
    public double bonusVsFast = 0;
    /** Against enemies that release children when destroyed. */
    public double bonusVsSplitters = 0;
    /** Extra damage equal to this fraction of the target's max HP (capped by {@link #pctCap}). */
    public double pctMaxHp = 0;
    public double pctCap = 60;

    // ---- control effects ----
    public double slowFraction = 0;
    public double slowDuration = 0;
    /** Stun applied to non-bosses. */
    public double stunDuration = 0;
    /** Stun applied to bosses (0 = bosses immune, the spec default). */
    public double bossStunDuration = 0;
    /** Pixels pushed back along the track (positive = backwards). Bosses immune unless {@link #bossKnockback}. */
    public double knockback = 0;
    public double bossKnockback = 0;
    /** Extra knockback only vs fast enemies (Linked Lists or >= 2.0x speed): Mistral. */
    public double knockbackVsFast = 0;
    /** Extra knockback only vs Lead-type enemies (GPT U3 "SONIC-like"); SONIC damage already does 20 px. */
    public double knockbackVsLead = 0;
    /** Abilities: ignore the per-second knockback budget. */
    public boolean uncappedKnockback = false;
    /** ARMOR CRACK: any value &gt; 0 applies it for {@link #armorBreakDuration} s (magnitude kept for UI text). */
    public double armorBreak = 0;
    public double armorBreakDuration = 0;
    /** Percent mark (+X% damage taken) for {@link #markDuration} s. */
    public double markBonus = 0;
    /** Flat mark (+N damage taken per hit) for {@link #markDuration} s. */
    public double markFlat = 0;
    public double markDuration = 0;
    public double disruptDuration = 0;
    /** Burn: damage per second for {@link #dotDuration} s; refreshes, never stacks. */
    public double dotDps = 0;
    public double dotDuration = 0;
    /** Qwen: removes Fortified from non-boss targets. */
    public boolean stripsFortified = false;
    /** Gemini U2: permanently removes Camo and Regrow from non-boss targets. */
    public boolean stripsCamoRegrow = false;

    public HitSpec() { }

    public HitSpec(double damage, DamageType type) { this.damage = damage; this.type = type; }

    public HitSpec copy() {
        HitSpec h = new HitSpec();
        h.damage = damage; h.type = type; h.armorPierce = armorPierce; h.ignoresResistance = ignoresResistance;
        h.multVsBoss = multVsBoss; h.flatVsBoss = flatVsBoss; h.multVsFortified = multVsFortified;
        h.flatVsFortified = flatVsFortified; h.flatVsLead = flatVsLead; h.multVsLead = multVsLead; h.flatVsCeramic = flatVsCeramic;
        h.multVsCeramic = multVsCeramic; h.multVsSortingAndLists = multVsSortingAndLists; h.multVsHighHp = multVsHighHp;
        h.bonusVsBoss = bonusVsBoss; h.bonusVsFortified = bonusVsFortified; h.bonusVsNonFortified = bonusVsNonFortified;
        h.bonusVsComplex = bonusVsComplex; h.bonusVsFast = bonusVsFast; h.bonusVsSplitters = bonusVsSplitters;
        h.pctMaxHp = pctMaxHp; h.pctCap = pctCap;
        h.slowFraction = slowFraction; h.slowDuration = slowDuration; h.stunDuration = stunDuration;
        h.bossStunDuration = bossStunDuration; h.knockback = knockback; h.bossKnockback = bossKnockback;
        h.knockbackVsFast = knockbackVsFast; h.knockbackVsLead = knockbackVsLead;
        h.uncappedKnockback = uncappedKnockback; h.armorBreak = armorBreak; h.armorBreakDuration = armorBreakDuration;
        h.markBonus = markBonus; h.markFlat = markFlat; h.markDuration = markDuration;
        h.disruptDuration = disruptDuration; h.dotDps = dotDps; h.dotDuration = dotDuration;
        h.stripsFortified = stripsFortified; h.stripsCamoRegrow = stripsCamoRegrow;
        return h;
    }
}
