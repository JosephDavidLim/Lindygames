package lindefense.tower;

import lindefense.sim.Game;

/**
 * Pricing helpers: purchase price, next-upgrade price, sell refunds (70%) and shop availability rules. Heroes are
 * free to deploy once unlocked in the Hero Store, so dismissing one refunds nothing.
 */
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

    /** Hero Store price that permanently unlocks a hero (0 for shop towers). */
    public static int unlockPriceOf(TowerType type) { return TowerCatalog.get(type).unlockCost; }

    /**
     * Position-independent reason the tower type cannot be placed right now, or null if it can: heroes must be
     * unlocked and are deployed once per match; everything else needs the cash. The UI uses this to grey out cards.
     */
    public static String unavailableReason(Game game, TowerType type) {
        String hero = game.getTowerManager().heroProblem(type);
        if (hero != null) return hero;
        if (game.getCash() < priceOf(type)) return "Not enough cash";
        return null;
    }
}
