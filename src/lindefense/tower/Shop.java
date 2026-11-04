package lindefense.tower;

/** Pricing helpers: purchase price, next-upgrade price and sell refunds (70%). */
public final class Shop {
    public static final double SELL_FRACTION = 0.70;

    private Shop() { }

    public static int priceOf(TowerType type) { return TowerCatalog.get(type).baseCost; }

    public static int upgradeCost(Tower t) {
        UpgradeDef u = t.nextUpgrade();
        return u == null ? 0 : u.cost;
    }

    /** Refund for selling: 70% of base cost plus all purchased upgrades, rounded down. */
    public static int sellValue(Tower t) {
        return (int) Math.floor(t.getInvestedCost() * SELL_FRACTION);
    }
}
