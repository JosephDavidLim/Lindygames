package lindefense.combat;

/**
 * Describes what one hit does to an enemy. Towers carry a HitSpec in their
 * stats; abilities and map hazards build ad-hoc ones. All fields are public and
 * mutable so upgrade lambdas can tweak them.
 */
public class HitSpec {
    public double damage = 1;
    public DamageType type = DamageType.ENERGY;
    /** Fraction of enemy armour ignored (1 = full armour piercing). */
    public double armorPierce = 0;
    /** Ignores type immunities / resistances (adaptive or kinetic penetration). */
    public boolean ignoresResistance = false;
    /** Additional damage multiplier against boss-class enemies (0.5 = +50%). */
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

    public double slowFraction = 0;
    public double slowDuration = 0;
    public double stunDuration = 0;
    /** Stun applied to bosses (usually shorter). */
    public double bossStunDuration = 0;
    /** Pixels pushed back along the track (positive = backwards). */
    public double knockback = 0;
    public double bossKnockback = 0;
    /** Abilities: ignore the per-second knockback budget. */
    public boolean uncappedKnockback = false;
    public double armorBreak = 0;
    public double armorBreakDuration = 0;
    public double markBonus = 0;
    public double markDuration = 0;
    public double disruptDuration = 0;
    public double dotDps = 0;
    public double dotDuration = 0;
    /** Qwen: removes Fortified from non-boss targets. */
    public boolean stripsFortified = false;

    public HitSpec() { }

    public HitSpec(double damage, DamageType type) { this.damage = damage; this.type = type; }

    public HitSpec copy() {
        HitSpec h = new HitSpec();
        h.damage = damage; h.type = type; h.armorPierce = armorPierce; h.ignoresResistance = ignoresResistance;
        h.bonusVsBoss = bonusVsBoss; h.bonusVsFortified = bonusVsFortified; h.bonusVsNonFortified = bonusVsNonFortified;
        h.bonusVsComplex = bonusVsComplex; h.bonusVsFast = bonusVsFast; h.bonusVsSplitters = bonusVsSplitters;
        h.pctMaxHp = pctMaxHp; h.pctCap = pctCap;
        h.slowFraction = slowFraction; h.slowDuration = slowDuration; h.stunDuration = stunDuration;
        h.bossStunDuration = bossStunDuration; h.knockback = knockback; h.bossKnockback = bossKnockback;
        h.uncappedKnockback = uncappedKnockback; h.armorBreak = armorBreak; h.armorBreakDuration = armorBreakDuration; h.markBonus = markBonus;
        h.markDuration = markDuration; h.disruptDuration = disruptDuration; h.dotDps = dotDps; h.dotDuration = dotDuration;
        h.stripsFortified = stripsFortified;
        return h;
    }
}
