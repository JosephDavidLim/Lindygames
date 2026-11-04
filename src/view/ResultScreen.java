package view;

import acm.graphics.GCanvas;

import entities.DefenderType;
import game.Gear;
import game.Hero;
import game.KeyItem;
import game.Level;
import game.Levels;
import game.Party;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Shown after a level ends: win or lose, what the party earned, who levelled
 * up, what joined the squad, and where to go next. EXP and loot are kept even
 * after a loss, so a tough level can be trained for.
 */
public class ResultScreen extends Screen {
    private final int level;
    private final boolean won;
    private final List<DefenderType> loadout;
    private final Party party;
    private Button squadButton;
    private Button autoButton;

    /**
     * Creates the result screen.
     *
     * @param canvas the canvas to draw on
     * @param nav the navigator
     * @param level the level that was played
     * @param won true if it was cleared
     * @param loadout the defenders used, for Retry
     * @param party what the party earned
     */
    public ResultScreen(GCanvas canvas, Navigator nav, int level, boolean won,
                        List<DefenderType> loadout, Party party) {
        super(canvas, nav);
        this.level = level;
        this.won = won;
        this.loadout = loadout;
        this.party = party;
    }

    @Override
    public void show() {
        Music.play("title");
        Level played = Levels.get(level);
        fillMapBackground(played.getMap());
        double cx = Layout.W / 2.0;
        boolean finale = won && level == Levels.count();

        if (finale) {
            canvas.add(Ui.centered("THE DEADLINE IS BROKEN", "Serif-bold-38", Ui.GOLD, cx, 56));
        } else if (won) {
            canvas.add(Ui.centered("Level " + level + " cleared!", "Serif-bold-40", Ui.GOLD, cx, 56));
        } else {
            canvas.add(Ui.centered("The semester won this round", "Serif-bold-36", Ui.PINK, cx, 56));
        }
        canvas.add(Ui.centered(won ? played.getName() + " is behind you."
                : "You keep your EXP and loot. The Ledger keeps the rest. Try again.",
                "SansSerif-bold-15", Ui.CHALK, cx, 82));

        canvas.add(Ui.window("Rewards", 110, 96, 500, 186));
        canvas.add(Ui.text("EXP  +" + party.getExpEarned(), "SansSerif-bold-15", Ui.MONEY, 130, 152));
        canvas.add(Ui.text("Credits  +" + party.getCreditsEarned(), "SansSerif-bold-15", Ui.MONEY, 270, 152));
        drawLevelUps();
        drawGear();
        if (!party.getQuestsDone().isEmpty()) {
            canvas.add(Ui.text("Request done: " + party.getQuestsDone().get(0).getTitle() + ". Collect it under Requests!",
                    "SansSerif-bold-12", Ui.GOOD, 130, 268));
        }

        drawUnlocks();

        double bx = cx - 335;
        if (finale) {
            addButton("ending", "The Door", bx, Layout.H - 64, 150, 46, Ui.GOLD, Ui.INK, 17);
        } else if (won && level < Levels.count()) {
            addButton("next", "Keep Going", bx, Layout.H - 64, 150, 46, Ui.GOLD, Ui.INK, 17);
        }
        boolean waiting = nav.getProfile().needsSquadVisit();
        if (nav.getProfile().hasUnspentAp()) {
            autoButton = addButton("autoall", "Auto-assign all", 462, 131, 138, 22, Ui.GOLD, Ui.INK, 12);
        }
        squadButton = addButton("squad", waiting ? "Squad (!)" : "Squad", bx + 165, Layout.H - 64, 170, 46,
                nav.getProfile().hasUnspentAp() ? Ui.GOLD : Ui.PAPER, Ui.INK, 16);
        addButton("retry", "Retry", bx + 350, Layout.H - 64, 150, 46, Ui.PAPER, Ui.INK, 17);
        addButton("levels", "Map", bx + 515, Layout.H - 64, 150, 46, Ui.PAPER, Ui.INK, 17);
    }

    private void drawLevelUps() {
        List<Hero> unique = new ArrayList<>();
        for (Hero h : party.getLevelUps()) {
            if (!unique.contains(h)) {
                unique.add(h);
            }
        }
        if (unique.isEmpty()) {
            canvas.add(Ui.text("No level-ups this time.", "SansSerif-plain-12", Ui.INK, 130, 184));
            return;
        }
        canvas.add(Ui.text("LEVEL UP!", "SansSerif-bold-13", Ui.PINK, 130, 184));
        for (int i = 0; i < unique.size(); i++) {
            Hero h = unique.get(i);
            double x = 206 + i * 44;
            canvas.add(Assets.defender(h.getType(), 38), x, 158);
            canvas.add(Ui.centered("Lv." + h.getLevel(), "SansSerif-bold-10", Ui.INK, x + 19, 206));
        }
    }

    private void drawGear() {
        List<Gear> found = party.getGearFound();
        canvas.add(Ui.text(found.isEmpty() ? "No items found." : "Items:", "SansSerif-bold-13", Ui.INK, 130, 238));
        for (int i = 0; i < found.size() && i < 9; i++) {
            canvas.add(Assets.gear(found.get(i), 36), 180 + i * 42, 216);
        }
    }

    private void drawUnlocks() {
        double cx = Layout.W / 2.0;
        List<DefenderType> unlocks = Levels.get(level).getUnlocks();
        if (won && !unlocks.isEmpty() && nav.firstClearJustHappened()) {
            canvas.add(Ui.centered("New classmate joined your squad:", "SansSerif-bold-16", Ui.CHALK, cx, 304));
            double total = unlocks.size() * 110;
            for (int i = 0; i < unlocks.size(); i++) {
                double x = cx - total / 2 + i * 110;
                canvas.add(Assets.defender(unlocks.get(i), 96), x + 7, 312);
                canvas.add(Ui.centered(unlocks.get(i).getDisplayName(), "SansSerif-bold-13", Ui.GOLD, x + 55, 424));
            }
        }
        KeyItem item = KeyItem.fromLevel(level);
        if (won && item != null && nav.firstClearJustHappened()) {
            canvas.add(Ui.centered("KEY ITEM: " + item.getDisplayName() + "!  " + item.getDescription(),
                    "SansSerif-bold-16", Ui.GOLD, cx, 462));
        }
    }

    @Override
    protected void onButton(String id) {
        if ("autoall".equals(id)) {
            nav.getProfile().autoAssignAll();
            nav.saveProfile();
            Sfx.play("select");
            autoButton.setText("Points spent!");
            autoButton.setEnabled(false, Ui.PAPER);
            squadButton.setText("Squad");
            squadButton.setFill(Ui.PAPER);
        } else if ("ending".equals(id)) {
            nav.showStory(true);
        } else if ("next".equals(id)) {
            nav.showExplore(level + 1);
        } else if ("retry".equals(id)) {
            nav.startLevel(level, loadout);
        } else if ("levels".equals(id)) {
            nav.showLevelSelect();
        } else if ("squad".equals(id)) {
            nav.showSquad(level);
        }
    }
}
