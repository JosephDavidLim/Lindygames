package lindefense.tower;

/** Immutable definition of one tower: identity, base cost, base stats, 4 upgrades (5 states) and ability. */
public class TowerDef {
    public final TowerType type;
    public final String subtitle;
    public final String description;
    public final int baseCost;
    public final TowerStats baseStats;
    public final UpgradeDef[] upgrades;
    public final AbilityDef ability;
    /** Model-lineup label for each of the five progression states (base + 4 upgrades). */
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

    /** Computes the cumulative stats for a tier (0 = base .. 4 = superclass). */
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
}
