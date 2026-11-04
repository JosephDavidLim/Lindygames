package view;

import acm.graphics.GCanvas;

import entities.DefenderType;
import entities.EnemyType;
import game.GameMap;
import game.Story;

import java.awt.Color;

/**
 * The opening and the ending of the story: a scene with a caption on each page.
 */
public class StoryScreen extends Screen {
    private final boolean ending;
    private final String[][] pages;
    private int page;

    /**
     * Creates the story.
     *
     * @param canvas the canvas to draw on
     * @param nav the navigator
     * @param ending true for the ending, false for the opening
     */
    public StoryScreen(GCanvas canvas, Navigator nav, boolean ending) {
        super(canvas, nav);
        this.ending = ending;
        this.pages = ending ? Story.ENDING : Story.OPENING;
    }

    @Override
    public void show() {
        Music.play("title");
        draw();
    }

    private void draw() {
        canvas.removeAll();
        buttons.clear();
        String[] p = pages[page];
        canvas.add(Assets.corridor(GameMap.valueOf(p[0]), 1440, Layout.H), -page * 120, 0);
        boolean last = page == pages.length - 1;
        if (!ending) {
            DefenderType[] squad = {DefenderType.LUIS, DefenderType.AARAV, DefenderType.IMRAAN};
            canvas.add(Assets.defender(squad[Math.min(page, 2)], 120), 80, 300);
            EnemyType[] foes = {EnemyType.HOMEWORK_SHEET, EnemyType.REPORT_CARD, EnemyType.VICE_PRINCIPAL, EnemyType.THE_DEADLINE};
            canvas.add(Assets.enemy(foes[page], 170), 500, 240);
        } else if (page < 2) {
            canvas.add(Assets.defender(nav.getProfile().getMain(), 120), 300, 300);
        } else if (last) {
            canvas.add(Assets.enemy(EnemyType.THE_SUBSTITUTE, 170), 500, 240);
            canvas.add(Assets.defender(nav.getProfile().getMain(), 76), 120, 320);
        }
        canvas.add(Ui.round(24, 402, Layout.W - 48, 104, 14, new Color(6, 6, 20, 232)));
        canvas.add(Ui.centered(p[1], "Serif-bold-19", Ui.GOLD, Layout.W / 2.0, 440));
        canvas.add(Ui.centered(p[2], "SansSerif-plain-14", Ui.CHALK, Layout.W / 2.0, 470));
        canvas.add(Ui.centered((page + 1) + " / " + pages.length, "SansSerif-plain-11", Ui.MUTED, Layout.W / 2.0, 495));
        String label = !last ? "Next" : ending ? "Attempt " + (nav.getProfile().getAttempt() + 1) : "First period";
        addButton("next", label, Layout.W - 210, Layout.H - 44, 186, 34, Ui.GOLD, Ui.INK, 15);
        if (!ending && !last) {
            addButton("skip", "Skip", 24, Layout.H - 44, 90, 34, Ui.PAPER, Ui.INK, 15);
        }
    }

    @Override
    protected void onButton(String id) {
        if ("skip".equals(id) || ("next".equals(id) && page == pages.length - 1)) {
            finish();
        } else {
            page++;
            draw();
        }
    }

    private void finish() {
        if (ending) {
            nav.getProfile().nextAttempt();
            nav.saveProfile();
            nav.showMenu();
        } else {
            nav.getProfile().markIntroSeen();
            nav.saveProfile();
            nav.showExplore(nav.getMaxUnlocked());
        }
    }
}
