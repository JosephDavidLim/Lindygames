package lindefense.tower;

import java.util.function.Consumer;

/** One purchasable upgrade: a name, cost, description and a mutation of the stats. */
public class UpgradeDef {
    public final String name;
    public final int cost;
    public final String description;
    public final Consumer<TowerStats> apply;

    public UpgradeDef(String name, int cost, String description, Consumer<TowerStats> apply) {
        this.name = name; this.cost = cost; this.description = description; this.apply = apply;
    }
}
