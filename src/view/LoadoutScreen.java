package view;

import acm.graphics.GCanvas;
import acm.graphics.GLabel;
import acm.graphics.GRoundRect;

import entities.DefenderType;
import game.Levels;
import game.Shop;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Lets the player pick up to six unlocked defenders before a level.
 */
public class LoadoutScreen extends Screen {
    private static final int PER_ROW = 6;
    private static final double TILE_W = 104;
    private static final double TILE_H = 106;
    private static final double GAP = 10;
    private static final double TOP_Y = 66;

    private final int level;
    private final List<DefenderType> owned;
    private final List<DefenderType> chosen = new ArrayList<>();
    private final List<GRoundRect> tileBgs = new ArrayList<>();
    private final int slots;
    private GLabel countLabel;
    private Button startButton;

    /**
     * Creates the loadout screen.
     *
     * @param canvas the canvas to draw on
     * @param nav the navigator
     * @param level the level about to be played
     */
    public LoadoutScreen(GCanvas canvas, Navigator nav, int level) {
        super(canvas, nav);
        this.level = level;
        this.owned = Levels.unlockedBefore(Math.max(level, nav.getMaxUnlocked()));
        this.slots = nav.getSlots();
        for (int i = 0; i < owned.size() && i < slots; i++) {
            chosen.add(owned.get(i));
        }
    }

    @Override
    public void show() {
        Music.play("title");
        fillMapBackground(Levels.get(level).getMap());
        canvas.add(Ui.centered(Levels.get(level).getName(),
                "Serif-bold-28", Ui.CHALK, Layout.W / 2.0, 40));
        countLabel = Ui.centered("", "SansSerif-bold-14", Ui.GOLD, Layout.W / 2.0, 58);
        canvas.add(countLabel);

        double startX = (Layout.W - (PER_ROW * TILE_W + (PER_ROW - 1) * GAP)) / 2;
        for (int i = 0; i < owned.size(); i++) {
            DefenderType t = owned.get(i);
            double x = startX + (i % PER_ROW) * (TILE_W + GAP);
            double y = TOP_Y + 8 + (i / PER_ROW) * (TILE_H + 8);
            GRoundRect bg = Ui.round(x, y, TILE_W, TILE_H, 12, Ui.PAPER);
            tileBgs.add(bg);
            canvas.add(bg);
            canvas.add(Assets.defender(t, 54), x + (TILE_W - 54) / 2, y + 4);
            canvas.add(Ui.text("Lv." + nav.getProfile().hero(t).getLevel(), "SansSerif-bold-11", Ui.GOLD, x + 6, y + 15));
            canvas.add(Ui.centered(t.getDisplayName(), "SansSerif-bold-12", Ui.INK, x + TILE_W / 2, y + 70));
            canvas.add(Ui.centered(t.describe(), "SansSerif-plain-9", Ui.INK, x + TILE_W / 2, y + 84));
            canvas.add(Ui.centered(t.getCost() + " FP", "SansSerif-bold-11", Ui.MONEY,
                    x + TILE_W / 2, y + 99));
        }
        startButton = addButton("start", "START", Layout.W - 190, Layout.H - 58, 160, 42, Ui.GOLD, Ui.INK, 20);
        addButton("back", "Back", 20, Layout.H - 54, 100, 36, Ui.PAPER, Ui.INK, 16);
        addButton("squad", nav.getProfile().needsSquadVisit() ? "Squad (!)" : "Squad", 130, Layout.H - 54, 150, 36,
                nav.getProfile().needsSquadVisit() ? Ui.GOLD : Ui.PAPER, Ui.INK, 14);
        addButton("store", "Black Market", 290, Layout.H - 54, 130, 36, Ui.PAPER, Ui.INK, 14);
        canvas.add(Ui.centered(Levels.get(level).getMap().getRule() + "  Enemies are Lv." + Levels.get(level).getEnemyLevel() + ".",
                "SansSerif-bold-13", Ui.CHALK, Layout.W / 2.0, Layout.H - 72));
        refresh();
    }

    @Override
    protected void onClick(double x, double y) {
        double startX = (Layout.W - (PER_ROW * TILE_W + (PER_ROW - 1) * GAP)) / 2;
        for (int i = 0; i < owned.size(); i++) {
            double tx = startX + (i % PER_ROW) * (TILE_W + GAP);
            double ty = TOP_Y + 8 + (i / PER_ROW) * (TILE_H + 8);
            if (x >= tx && x <= tx + TILE_W && y >= ty && y <= ty + TILE_H) {
                toggle(owned.get(i));
                return;
            }
        }
    }

    @Override
    protected void onButton(String id) {
        if ("back".equals(id)) {
            nav.showExplore(level);
        } else if ("squad".equals(id)) {
            nav.showSquad(level);
        } else if ("store".equals(id)) {
            nav.showStore(level);
        } else if ("start".equals(id)) {
            nav.startLevel(level, new ArrayList<>(chosen));
        }
    }

    private void toggle(DefenderType t) {
        if (chosen.contains(t)) {
            chosen.remove(t);
        } else if (chosen.size() < slots) {
            chosen.add(t);
        }
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < owned.size(); i++) {
            boolean on = chosen.contains(owned.get(i));
            GRoundRect bg = tileBgs.get(i);
            bg.setFillColor(on ? Ui.SELECTED : Ui.PAPER);
            bg.setColor(on ? Ui.SELECTED : Ui.PAPER);
        }
        Ui.recenter(countLabel, "Choose up to " + slots + " classmates   (" + chosen.size()
                + "/" + slots + ")", Layout.W / 2.0);
        startButton.setEnabled(!chosen.isEmpty(), Ui.GOLD);
    }
}
