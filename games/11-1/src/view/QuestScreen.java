package view;

import acm.graphics.GCanvas;

import game.GameMap;
import game.Profile;
import game.Quest;
import game.QuestLog;

import java.awt.Color;
import java.util.List;

/**
 * The request board: classmates ask for favors, and the player accepts them
 * and collects rewards.
 */
public class QuestScreen extends Screen {
    private static final int PER_PAGE = 5;
    private static final double ROW_H = 84;
    private static final double ROW_Y = 46;

    private final int returnLevel;
    private final Profile profile;
    private int page;

    /**
     * Creates the quest board.
     *
     * @param canvas the canvas to draw on
     * @param nav the navigator
     * @param returnLevel level whose loadout to return to, or 0 for the map
     */
    public QuestScreen(GCanvas canvas, Navigator nav, int returnLevel) {
        super(canvas, nav);
        this.returnLevel = returnLevel;
        this.profile = nav.getProfile();
    }

    @Override
    public void show() {
        Music.play("title");
        draw();
    }

    private void draw() {
        canvas.removeAll();
        buttons.clear();
        fillMapBackground(GameMap.HALLWAY);
        canvas.add(Ui.window("Requests", 8, 8, Layout.W - 16, 484));
        List<Quest> quests = profile.visibleQuests();
        int pages = Math.max(1, (quests.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        if (quests.isEmpty()) {
            canvas.add(Ui.centered("Nobody has asked you for anything yet. Keep going.", "SansSerif-bold-16", Ui.INK,
                    Layout.W / 2.0, 240));
        }
        for (int i = page * PER_PAGE; i < quests.size() && i < (page + 1) * PER_PAGE; i++) {
            drawQuest(quests.get(i), ROW_Y + (i - page * PER_PAGE) * ROW_H);
        }
        addButton("back", "Back", 20, Layout.H - 50, 100, 36, Ui.PAPER, Ui.INK, 15);
        if (pages > 1) {
            Button prev = addButton("prev", "Prev", 440, Layout.H - 50, 80, 36, Ui.PAPER, Ui.INK, 14);
            prev.setEnabled(page > 0, page > 0 ? Ui.PAPER : Ui.DISABLED);
            Button next = addButton("next", "Next", 610, Layout.H - 50, 80, 36, Ui.PAPER, Ui.INK, 14);
            next.setEnabled(page < pages - 1, page < pages - 1 ? Ui.PAPER : Ui.DISABLED);
            canvas.add(Ui.centered((page + 1) + " / " + pages, "SansSerif-bold-14", Ui.CHALK, 565, Layout.H - 27));
        }
    }

    private void drawQuest(Quest q, double y) {
        QuestLog log = profile.getQuests();
        QuestLog.State state = log.stateOf(q);
        canvas.add(Ui.round(16, y, Layout.W - 32, ROW_H - 6, 10, Ui.PAPER));
        canvas.add(Assets.defender(q.getNpc(), 68), 22, y + 4);
        canvas.add(Ui.text(q.getTitle() + "  (" + q.getNpc().getDisplayName() + ")", "SansSerif-bold-14", Ui.INK, 96, y + 19));
        canvas.add(Ui.text("\"" + q.getLine() + "\"", "SansSerif-plain-11", Ui.MUTED, 96, y + 35));
        canvas.add(Ui.text(q.describeGoal(), "SansSerif-bold-12", Ui.PINK, 96, y + 52));
        canvas.add(Ui.text("Reward: " + q.describeReward(), "SansSerif-plain-11", Ui.MONEY, 96, y + 69));
        if (state == QuestLog.State.NEW) {
            addButton("accept" + q.ordinal(), "Accept", 574, y + 22, 118, 34, Ui.GOLD, Ui.INK, 14);
        } else if (state == QuestLog.State.READY) {
            addButton("claim" + q.ordinal(), "Collect!", 574, y + 22, 118, 34, Ui.GOOD, Ui.INK, 14);
        } else {
            canvas.add(Ui.bar(574, y + 30, 118, 18, (double) log.progressOf(q) / q.getCount(), new Color(190, 110, 255),
                    log.progressOf(q) + " / " + q.getCount()));
        }
    }

    @Override
    protected void onButton(String id) {
        if ("back".equals(id)) {
            nav.saveProfile();
            if (returnLevel > 0) {
                nav.showLoadout(returnLevel);
            } else {
                nav.showLevelSelect();
            }
            return;
        } else if ("prev".equals(id)) {
            page--;
        } else if ("next".equals(id)) {
            page++;
        } else if (id.startsWith("accept")) {
            profile.getQuests().accept(Quest.values()[Integer.parseInt(id.substring(6))]);
            Sfx.play("select");
        } else if (id.startsWith("claim")) {
            profile.claim(Quest.values()[Integer.parseInt(id.substring(5))]);
            Sfx.play("levelup");
        }
        nav.saveProfile();
        draw();
    }
}
