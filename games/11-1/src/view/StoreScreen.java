package view;

import acm.graphics.GCanvas;

import entities.DefenderType;
import game.GameMap;
import game.Gear;
import game.Profile;

import java.awt.Color;

/**
 * The Black Market, where Credits buy gear. Better items appear as the player
 * climbs the Institute. Sunny Joe runs the counter and reacts to what you do.
 */
public class StoreScreen extends Screen {
    private static final double COL_W = 174;
    private static final double ROW_H = 96;
    private static final double GRID_Y = 76;

    private final int returnLevel;
    private final Profile profile;
    private String clerkLine = "Quiet. None of this was bought here, and neither were you.";

    /**
     * Creates the store.
     *
     * @param canvas the canvas to draw on
     * @param nav the navigator
     * @param returnLevel level whose loadout to return to, or 0 for the world map
     */
    public StoreScreen(GCanvas canvas, Navigator nav, int returnLevel) {
        super(canvas, nav);
        this.returnLevel = returnLevel;
        this.profile = nav.getProfile();
    }

    @Override
    public void show() {
        Music.play("title");
        redraw();
    }

    private void redraw() {
        canvas.removeAll();
        buttons.clear();
        fillMapBackground(GameMap.CAFETERIA);
        canvas.add(Ui.window("Black Market", 8, 8, Layout.W - 16, 472));
        canvas.add(Assets.defender(DefenderType.JOSEPH, 40), 16, 38);
        canvas.add(Ui.round(60, 42, 500, 28, 10, Ui.PAPER));
        canvas.add(Ui.text(clerkLine, "SansSerif-plain-13", Ui.INK, 70, 61));
        canvas.add(Ui.text("Credits: " + profile.getCredits(), "SansSerif-bold-15", Ui.MONEY, 572, 62));

        int best = Gear.bestTierAt(profile.getMaxUnlocked());
        int i = 0;
        for (Gear g : Gear.values()) {
            double x = 14 + g.getTier() * (COL_W + 1);
            double y = GRID_Y + g.getSlot().ordinal() * (ROW_H + 2);
            drawItem(g, x, y, g.getTier() <= best, i);
            i++;
        }
        addButton("back", "Back", 20, Layout.H - 56, 100, 38, Ui.PAPER, Ui.INK, 16);
        addButton("squad", "Squad", 130, Layout.H - 56, 110, 38, Ui.GOLD, Ui.INK, 16);
    }

    private void drawItem(Gear g, double x, double y, boolean open, int index) {
        Color rim = Assets.rarityColor(g.getTier());
        canvas.add(Ui.round(x, y, COL_W - 2, ROW_H, 10, rim));
        canvas.add(Ui.round(x + 2, y + 2, COL_W - 6, ROW_H - 4, 8, open ? Ui.PAPER : Ui.DISABLED));
        canvas.add(Assets.gear(g, 46), x + 6, y + 6);
        canvas.add(Ui.text(g.getDisplayName(), "SansSerif-bold-11", Ui.INK, x + 56, y + 19));
        canvas.add(Ui.text(g.describe() + "  " + g.getRarity(), "SansSerif-plain-10", Ui.INK, x + 56, y + 34));
        canvas.add(Ui.text("In bag: " + profile.countOf(g), "SansSerif-plain-10", Ui.INK, x + 56, y + 48));
        if (open) {
            boolean afford = profile.getCredits() >= g.getPrice();
            Button buy = addButton("buy" + index, "Buy " + g.getPrice(), x + 6, y + 58, 86, 30, Ui.GOLD, Ui.INK, 12);
            buy.setEnabled(afford, afford ? Ui.GOLD : Ui.DISABLED);
        } else {
            canvas.add(Ui.text("Unlocks later", "SansSerif-bold-11", Ui.MUTED, x + 8, y + 78));
        }
        boolean spare = profile.countOf(g) > 0;
        Button sell = addButton("sell" + index, "Sell " + g.getPrice() / 3, x + 95, y + 58, 70, 30, Ui.WINDOW, Ui.INK, 11);
        sell.setEnabled(spare, spare ? Ui.WINDOW : Ui.DISABLED);
    }

    @Override
    protected void onButton(String id) {
        if ("back".equals(id)) {
            if (returnLevel > 0) {
                nav.showLoadout(returnLevel);
            } else {
                nav.showLevelSelect();
            }
        } else if ("squad".equals(id)) {
            nav.showSquad(returnLevel);
        } else if (id.startsWith("sell")) {
            Gear g = Gear.values()[Integer.parseInt(id.substring(4))];
            if (profile.sell(g)) {
                Sfx.play("coin");
                clerkLine = "I'll take that " + g.getDisplayName() + ". I never saw you.";
                nav.saveProfile();
            }
            redraw();
        } else if (id.startsWith("buy")) {
            Gear g = Gear.values()[Integer.parseInt(id.substring(3))];
            if (profile.buy(g)) {
                Sfx.play("coin");
                clerkLine = "Your " + g.getDisplayName() + ". Equip it under Squad. Don't tell the Ledger.";
                nav.saveProfile();
            }
            redraw();
        }
    }
}
