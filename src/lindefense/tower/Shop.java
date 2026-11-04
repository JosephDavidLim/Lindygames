package lindefense.tower;

import lindefense.sim.Game;

/** Pricing helpers: purchase price, next-upgrade price, sell refunds (70%) and shop availability rules. */
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

    /**
     * Position-independent reason the tower type cannot be bought right now, or null if it can: only one copy of
     * each hero may be placed ("Only one Brad Lindemann"), plus the cash check. The UI uses this to grey out shop cards.
     */
    public static String unavailableReason(Game game, TowerType type) {
        if (type.isHero() && game.getTowerManager().heroPlaced(type)) return "Only one " + type.displayName + " allowed";
        if (game.getCash() < priceOf(type)) return "Not enough cash";
        return null;
    }
}
