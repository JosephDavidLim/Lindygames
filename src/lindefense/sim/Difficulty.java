package lindefense.sim;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lindefense.enemy.EnemyType;
import lindefense.enemy.Modifier;
import lindefense.wave.SpawnGroup;
import lindefense.wave.WaveData;
import lindefense.wave.WaveDef;

/**
 * "Difficulty Growth": four selectable modes named after Big-O classes. Every knob is a
 * RELATIVE multiplier (or addition) on top of the base game, so tower / enemy stat changes
 * do not invalidate the tiers. {@link #O1} is an exact no-op (the original game).
 *
 * <p>Per-wave growth: HP, enemy count and elite chance ramp with a normalised curve
 * {@link #growth(int)} in [0,1] shaped like the mode's name (flat, linear, quadratic,
 * capped exponential). A ramped knob is {@code base + (final - base) * growth(wave)}.
 * Tower damage, income and starting resources are flat per mode.</p>
 *
 * <p>Elites: a share of each non-boss spawn group is split off into an elite group that gets
 * extra Camo/Regrow/Fortified modifiers and may be promoted to a tougher enemy type. Everything
 * is deterministic from (game seed, wave, group index). Extra bosses are added as additional
 * spawn groups; the normal splitting and Palantir logic apply unchanged.</p>
 */
public enum Difficulty {
    O1("O1", "O(1)", "O(1)", "Constant (current)", "The original game, unchanged.",
        Curve.FLAT, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1),
    ON("ON", "O(N)", "O(N)", "Linear (Normal)", "Tougher enemies, more of them, weaker towers, leaner income.",
        Curve.LINEAR, 1.10, 1.50, 0.92, 0.80, 1.10, 1.40, 0, 0, 0, 0, 1, 1),
    ON2("ON2", "O(N²)", "O(N^2)", "Quadratic (Hard)", "Everything scales harder, and late waves bring elite enemies.",
        Curve.QUADRATIC, 1.25, 3.10, 0.84, 0.56, 1.25, 2.40, 0.03, 0.80, 2, 0.50, 1, 1),
    ONN("ONN", "O(Nᴺ)", "O(N^N)", "Super-exponential (Impossible)", "Enemy health climbs to 3x, weaker towers, half income (double starting cash), elites and extra bosses. Still beatable.",
        Curve.SUPEREXP, 1.30, 3.30, 0.75, 0.50, 1.30, 2.50, 0.05, 0.80, 2, 0.50, 2, 1);

    /** Shape of the per-wave growth curve. */
    public enum Curve { FLAT, LINEAR, QUADRATIC, SUPEREXP }

    /** One additional boss group added on a given wave. */
    public static final class ExtraBoss {
        public final int wave, count;
        public final EnemyType type;
        public final String mods;
        public final double startDelay, interval;
        ExtraBoss(int wave, EnemyType type, int count, String mods, double startDelay, double interval) {
            this.wave = wave; this.type = type; this.count = count; this.mods = mods; this.startDelay = startDelay; this.interval = interval;
        }
    }

    /** System property for the default mode: {@code -Dlindefense.difficulty=O1|ON|ON2|ONN}. */
    public static final String PROPERTY = "lindefense.difficulty";

    public final String id;
    public final String label;
    public final String asciiLabel;
    public final String subtitle;
    public final String description;
    public final Curve curve;
    public final double hpBase, hpFinal;
    /** Flat tower damage multiplier. */
    public final double towerDamage;
    /** Flat multiplier on kill rewards and the end-of-wave bonus. */
    public final double income;
    public final double countBase, countFinal;
    public final double eliteBase, eliteFinal;
    /** Extra modifiers given to an elite (when available). */
    public final int eliteMods;
    /** Chance an elite is also promoted to a tougher enemy type. */
    public final double promoteChance;
    public final double startCashMult, startLivesMult;
    /** Small extra on top of damage-compensating Palantir HP. */
    public final double palantirExtra;
    private final List<ExtraBoss> extraBosses = new ArrayList<>();

    private static Difficulty selected;
    private static final Map<EnemyType, EnemyType> PROMOTION = new EnumMap<>(EnemyType.class);
    static {
        PROMOTION.put(EnemyType.PRIMITIVE_VARIABLE, EnemyType.STATIC_ARRAY);
        PROMOTION.put(EnemyType.STATIC_ARRAY, EnemyType.DYNAMIC_ARRAY);
        PROMOTION.put(EnemyType.DYNAMIC_ARRAY, EnemyType.DOUBLY_LINKED_LIST);
        PROMOTION.put(EnemyType.SINGLY_LINKED_LIST, EnemyType.DOUBLY_LINKED_LIST);
        PROMOTION.put(EnemyType.BUBBLE_SORT, EnemyType.MERGE_SORT);
        PROMOTION.put(EnemyType.SELECTION_SORT, EnemyType.QUICK_SORT);
        PROMOTION.put(EnemyType.INSERTION_SORT, EnemyType.QUICK_SORT);
        PROMOTION.put(EnemyType.SHELL_SORT, EnemyType.QUICK_SORT);
        PROMOTION.put(EnemyType.MERGE_SORT, EnemyType.QUICK_SORT);
        PROMOTION.put(EnemyType.QUICK_SORT, EnemyType.HEAP_SORT);
        PROMOTION.put(EnemyType.HEAP_SORT, EnemyType.RADIX_SORT);
        PROMOTION.put(EnemyType.COUNTING_SORT, EnemyType.BUCKET_SORT);
        PROMOTION.put(EnemyType.RADIX_SORT, EnemyType.TIMSORT);
        PROMOTION.put(EnemyType.BUCKET_SORT, EnemyType.TIMSORT);
        // O(N^N) extra boss schedule: (wave, type, count, modifiers, start delay, interval)
        ONN.boss(25, EnemyType.BELLARMINE_COLLEGE_PREP, 1, null, 12, 1);
        ONN.boss(30, EnemyType.BELLARMINE_COLLEGE_PREP, 1, null, 14, 1);
        ONN.boss(35, EnemyType.BELLARMINE_COLLEGE_PREP, 1, null, 10, 1);
        ONN.boss(45, EnemyType.BELLARMINE_COLLEGE_PREP, 1, null, 10, 1);
        ONN.boss(50, EnemyType.BELLARMINE_COLLEGE_PREP, 1, null, 12, 1);
        ONN.boss(55, EnemyType.BELLARMINE_COLLEGE_PREP, 1, null, 14, 1);
        ONN.boss(65, EnemyType.BELLARMINE_COLLEGE_PREP, 1, null, 15, 1);
        ONN.boss(70, EnemyType.SW_RESOURCE_CONSULTANT, 1, null, 15, 1);
        ONN.boss(75, EnemyType.BELLARMINE_COLLEGE_PREP, 2, "F", 8, 4);
        ONN.boss(85, EnemyType.BELLARMINE_COLLEGE_PREP, 2, "F", 12, 4);
    }

    Difficulty(String id, String label, String ascii, String subtitle, String description, Curve curve,
               double hpBase, double hpFinal, double towerDamage, double income, double countBase, double countFinal,
               double eliteBase, double eliteFinal, int eliteMods, double promoteChance, double cash, double lives) {
        this.id = id; this.label = label; this.asciiLabel = ascii; this.subtitle = subtitle; this.description = description;
        this.curve = curve; this.hpBase = hpBase; this.hpFinal = hpFinal; this.towerDamage = towerDamage; this.income = income;
        this.countBase = countBase; this.countFinal = countFinal; this.eliteBase = eliteBase; this.eliteFinal = eliteFinal;
        this.eliteMods = eliteMods; this.promoteChance = promoteChance; this.startCashMult = cash; this.startLivesMult = lives;
        this.palantirExtra = 1 + 0.03 * ordinal();
    }

    private void boss(int wave, EnemyType t, int count, String mods, double delay, double interval) {
        extraBosses.add(new ExtraBoss(wave, t, count, mods, delay, interval));
    }

    /** True for the unmodified original game. */
    public boolean isBase() { return this == O1; }
    public List<ExtraBoss> getExtraBosses() { return extraBosses; }

    // ------------------------------------------------------------------ lookup / selection
    /** Parses O1 / ON / ON2 / ONN (also "O(1)", "O(N^2)" ...); null if unrecognised. */
    public static Difficulty parse(String s) {
        if (s == null) return null;
        String k = s.trim().toUpperCase().replace("(", "").replace(")", "").replace("^", "").replace("²", "2").replace("ᴺ", "N");
        for (Difficulty d : values()) if (d.id.equals(k)) return d;
        return null;
    }

    /** Default mode: the system property if valid, else {@link #O1}. */
    public static Difficulty defaultMode() {
        Difficulty d = parse(System.getProperty(PROPERTY));
        return d == null ? O1 : d;
    }

    /** The mode picked in the UI (initially {@link #defaultMode()}); used by the pre-game flow only. */
    public static Difficulty getSelected() { return selected == null ? defaultMode() : selected; }
    public static void setSelected(Difficulty d) { selected = d; }

    // ------------------------------------------------------------------ curves
    /** Normalised growth in [0,1] at the wave (1 at wave 100). */
    public double growth(int wave) {
        double x = Math.max(0, Math.min(1, wave / (double) WaveData.WAVE_COUNT));
        switch (curve) {
            case LINEAR: return x;
            case QUADRATIC: return x * x;
            case SUPEREXP: return (Math.exp(6 * x) - 1) / (Math.exp(6) - 1);
            default: return 0;
        }
    }

    /**
     * Palantir's HP multiplier: tower damage is already reduced, so the final boss only gets back-compensating HP
     * (times a small per-mode extra) and wave 100 stays a fair DPS check; difficulty comes from the gauntlet around it.
     */
    public double palantirHp() { return towerDamage * palantirExtra; }

    /** HP multiplier for a specific type (Palantir is special-cased, see {@link #palantirHp()}). */
    public double hpMultiplier(int wave, EnemyType type) {
        if (type == EnemyType.PALANTIR) return palantirHp();
        return hpMultiplier(wave);
    }

    private static double ramp(double base, double fin, double g) { return base + (fin - base) * g; }

    /** Enemy HP multiplier for enemies spawned on the wave. */
    public double hpMultiplier(int wave) { return ramp(hpBase, hpFinal, growth(wave)); }
    public double countMultiplier(int wave) { return ramp(countBase, countFinal, growth(wave)); }
    public double eliteChance(int wave) { return ramp(eliteBase, eliteFinal, growth(wave)); }
    public int startCash(int base) { return (int) Math.round(base * startCashMult); }
    public int startLives(int base) { return (int) Math.round(base * startLivesMult); }

    // ------------------------------------------------------------------ wave shaping
    /**
     * Returns the wave definition to actually spawn. For {@link #O1} this is the very same
     * object; otherwise counts, elites and extra bosses are applied deterministically from
     * (seed, wave).
     */
    public WaveDef shape(WaveDef base, long seed) {
        if (isBase()) return base;
        int wave = base.number;
        double cm = countMultiplier(wave), ec = eliteChance(wave);
        List<SpawnGroup> out = new ArrayList<>();
        int gi = 0;
        for (SpawnGroup g : base.groups) {
            Random r = new Random(seed * 1_000_003L + wave * 7919L + (gi++) * 104_729L + id.hashCode());
            if (g.type.isBoss()) { out.add(g); continue; }
            double exact = g.count * cm;
            int n = (int) Math.floor(exact);
            if (r.nextDouble() < exact - n) n++;
            n = Math.max(1, n);
            double interval = g.interval / (1 + 0.5 * (cm - 1));   // slight compression so extra spawns do not stretch the wave
            int plain = 0, elite = 0, promoted = 0;
            for (int i = 0; i < n; i++) {
                if (ec > 0 && r.nextDouble() < ec) {
                    if (r.nextDouble() < promoteChance && PROMOTION.containsKey(g.type)) promoted++; else elite++;
                } else plain++;
            }
            EnumSet<Modifier> em = eliteModifiers(g.modifiers, r);
            if (plain > 0) out.add(new SpawnGroup(g.type, plain, g.modifiers, interval, g.startDelay, g.route));
            if (elite > 0) out.add(new SpawnGroup(g.type, elite, em, interval * n / elite, g.startDelay + interval * 0.5, g.route));
            if (promoted > 0) out.add(new SpawnGroup(PROMOTION.get(g.type), promoted, em, interval * n / promoted, g.startDelay + interval * 0.25, g.route));
        }
        for (ExtraBoss b : extraBosses) {
            if (b.wave != wave) continue;
            EnumSet<Modifier> m = EnumSet.noneOf(Modifier.class);
            if (b.mods != null) {
                if (b.mods.indexOf('C') >= 0) m.add(Modifier.CAMO);
                if (b.mods.indexOf('R') >= 0) m.add(Modifier.REGROW);
                if (b.mods.indexOf('F') >= 0) m.add(Modifier.FORTIFIED);
            }
            out.add(new SpawnGroup(b.type, b.count, m, b.interval, b.startDelay, -1));
        }
        return new WaveDef(wave, base.note, out.toArray(new SpawnGroup[0]));
    }

    private EnumSet<Modifier> eliteModifiers(EnumSet<Modifier> have, Random r) {
        EnumSet<Modifier> m = EnumSet.copyOf(have);
        List<Modifier> missing = new ArrayList<>(EnumSet.complementOf(m));
        for (int i = 0; i < eliteMods && !missing.isEmpty(); i++) m.add(missing.remove(r.nextInt(missing.size())));
        return m;
    }
}
