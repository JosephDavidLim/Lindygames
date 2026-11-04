package lindefense.tower;

/** Immutable definition of one tower: identity, base cost, base stats, exactly 3 upgrades (4 states) and an ability. */
public class TowerDef {
    public final TowerType type;
    /** The spec's nickname, shown under the family name ("Prism Blaster"). */
    public final String subtitle;
    public final String description;
    public final int baseCost;
    public final TowerStats baseStats;
    public final UpgradeDef[] upgrades;
    public final AbilityDef ability;
    /** Label for each of the four progression states (base + 3 upgrades): model names or hero tier names. */
    public final String[] tierLabels;
    /** Placement footprint radius. */
    public final double radius;

    public TowerDef(TowerType type, String subtitle, String description, int baseCost, double radius,
                    TowerStats baseStats, UpgradeDef[] upgrades, AbilityDef ability, String[] tierLabels) {
        if (upgrades.length != TowerCatalog.UPGRADES) throw new IllegalArgumentException(type + " must have exactly " + TowerCatalog.UPGRADES + " upgrades");
        if (tierLabels.length != TowerCatalog.UPGRADES + 1) throw new IllegalArgumentException(type + " must have " + (TowerCatalog.UPGRADES + 1) + " tier labels");
        this.type = type; this.subtitle = subtitle; this.description = description; this.baseCost = baseCost;
        this.radius = radius; this.baseStats = baseStats; this.upgrades = upgrades; this.ability = ability;
        this.tierLabels = tierLabels;
    }

    /** Computes the cumulative stats for a tier (0 = base .. 3 = fully upgraded). */
    public TowerStats statsForTier(int tier) {
        TowerStats s = baseStats.copy();
        for (int i = 0; i < tier && i < upgrades.length; i++) upgrades[i].apply.accept(s);
        return s;
    }

    /** Total cost of base purchase plus the first {@code tier} upgrades. */
    public int totalCostForTier(int tier) {
        int c = baseCost;
        for (int i = 0; i < tier && i < upgrades.length; i++) c += upgrades[i].cost;
        return c;
    }

    /** Total cost of the fully upgraded tower (the spec's "Total"). */
    public int totalCost() { return totalCostForTier(upgrades.length); }
}
