package view;

import acm.graphics.GCanvas;
import acm.graphics.GLine;
import acm.graphics.GRect;

import game.Levels;
import game.Hero;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The title screen: an empty wet hallway at night, rain on the camera, and a
 * small menu. The ceiling lights fail now and then.
 */
public class MenuScreen extends Screen {
    private static final double PANEL_X = 232;
    private static final double PANEL_Y = 300;
    private static final double PANEL_W = 256;
    private static final double PANEL_H = 226;
    private static final int RAIN = 60;

    private final List<GLine> rain = new ArrayList<>();
    private final Random rng = new Random();
    private GRect flicker;
    private int frame;

    /**
     * Creates the title screen.
     *
     * @param canvas the canvas to draw on
     * @param nav the navigator
     */
    public MenuScreen(GCanvas canvas, Navigator nav) {
        super(canvas, nav);
    }

    @Override
    public void show() {
        Music.play("title");
        canvas.add(Assets.titleScene(Layout.W, Layout.H), 0, 0);
        for (int i = 0; i < RAIN; i++) {
            GLine drop = new GLine(0, 0, -1, 11);
            drop.setColor(new Color(170, 210, 255, 70));
            drop.setLocation(rng.nextInt(Layout.W), rng.nextInt(Layout.H));
            rain.add(drop);
            canvas.add(drop);
        }
        flicker = Ui.rect(0, 0, Layout.W, Layout.H, new Color(0, 0, 8, 0));
        canvas.add(flicker);
        drawPanel();
    }

    private void drawPanel() {
        canvas.add(Ui.round(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 12, new Color(8, 8, 24, 225)));
        GRect border = new GRect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
        border.setColor(Ui.GOLD);
        canvas.add(border);
        Hero h = nav.getProfile().hero(nav.getProfile().getMain());
        canvas.add(Ui.centered("ATTEMPT " + nav.getProfile().getAttempt(), "SansSerif-bold-15", new Color(255, 80, 170),
                PANEL_X + PANEL_W / 2, PANEL_Y + 22));
        canvas.add(Ui.centered(h.getType().getDisplayName() + "  Lv." + h.getLevel() + " " + h.getTitle(),
                "SansSerif-bold-12", Ui.CHALK, PANEL_X + PANEL_W / 2, PANEL_Y + 40));
        int next = nav.getMaxUnlocked();
        canvas.add(Ui.centered("Next: " + Levels.get(next).getMap().getDisplayName() + ", " + Levels.get(next).getName(),
                "SansSerif-plain-11", Ui.MUTED, PANEL_X + PANEL_W / 2, PANEL_Y + 56));
        addButton("play", "ENTER THE INSTITUTE", PANEL_X + 10, PANEL_Y + 64, PANEL_W - 20, 44, Ui.GOLD, Ui.INK, 16);
        double w = (PANEL_W - 26) / 2;
        boolean waiting = nav.getProfile().needsSquadVisit();
        boolean ready = nav.getProfile().hasQuestReady();
        addButton("squad", waiting ? "Squad (!)" : "Squad", PANEL_X + 10, PANEL_Y + 114, w, 32, Ui.PAPER, Ui.INK, 14);
        addButton("map", "Institute Map", PANEL_X + 16 + w, PANEL_Y + 114, w, 32, Ui.PAPER, Ui.INK, 13);
        addButton("store", "Black Market", PANEL_X + 10, PANEL_Y + 152, w, 32, Ui.PAPER, Ui.INK, 13);
        addButton("quests", ready ? "Requests (!)" : "Requests", PANEL_X + 16 + w, PANEL_Y + 152, w, 32, Ui.PAPER, Ui.INK, 14);
        addButton("exit", "Exit", PANEL_X + 10, PANEL_Y + 190, PANEL_W - 20, 28, Ui.PAPER, Ui.INK, 13);
    }

    @Override
    public void update() {
        frame++;
        for (GLine drop : rain) {
            double y = drop.getY() + 8;
            drop.setLocation(y > Layout.H ? rng.nextInt(Layout.W) : drop.getX() - 1, y > Layout.H ? -12 : y);
        }
        int alpha = frame % 263 < 5 || frame % 401 < 3 ? 90 : 0;
        flicker.setFillColor(new Color(0, 0, 8, alpha));
        flicker.setColor(flicker.getFillColor());
    }

    @Override
    protected void onButton(String id) {
        if ("play".equals(id)) {
            if (nav.getProfile().isIntroSeen()) {
                nav.showExplore(nav.getMaxUnlocked());
            } else {
                nav.showStory(false);
            }
        } else if ("map".equals(id)) {
            nav.showLevelSelect();
        } else if ("quests".equals(id)) {
            nav.showQuests(0);
        } else if ("squad".equals(id)) {
            nav.showSquad(0);
        } else if ("store".equals(id)) {
            nav.showStore(0);
        } else if ("exit".equals(id)) {
            nav.saveProfile();
            System.exit(0);
        }
    }
}
