package view;

import acm.graphics.GCanvas;
import acm.graphics.GCompound;
import acm.graphics.GOval;

import game.GameMap;
import game.Level;
import game.Levels;

import java.awt.Color;
import java.util.List;

/**
 * The map of the Institute: six floors, each with three periods. Hovering shows details,
 * and a glowing portal marks where the player should go next.
 */
public class LevelSelectScreen extends Screen {
    private static final double CARD_W = 226;
    private static final double CARD_H = 216;
    private static final double GAP_X = 8;
    private static final double START_X = 9;
    private static final double START_Y = 48;

    private final GOval[] portal = new GOval[3];
    private Tooltip tooltip;
    private int frame;

    /**
     * Creates the Institute map.
     *
     * @param canvas the canvas to draw on
     * @param nav the navigator
     */
    public LevelSelectScreen(GCanvas canvas, Navigator nav) {
        super(canvas, nav);
    }

    @Override
    public void show() {
        Music.play("title");
        fillMapBackground(GameMap.ADMISSIONS);
        canvas.add(Ui.centered("Institute Map", "Serif-bold-32", Ui.GOLD, Layout.W / 2.0, 36));
        GameMap[] maps = GameMap.values();
        int max = nav.getMaxUnlocked();
        for (int i = 0; i < maps.length; i++) {
            drawCard(maps[i], START_X + (i % 3) * (CARD_W + GAP_X), START_Y + (i / 3) * (CARD_H + 4), max);
        }
        boolean waiting = nav.getProfile().needsSquadVisit();
        boolean ready = nav.getProfile().hasQuestReady();
        addButton("back", "Back", 12, Layout.H - 46, 80, 36, Ui.PAPER, Ui.INK, 15);
        addButton("squad", waiting ? "Squad (!)" : "Squad", 100, Layout.H - 46, 110, 36,
                waiting ? Ui.GOLD : Ui.PAPER, Ui.INK, 14);
        addButton("store", "Black Market", 218, Layout.H - 46, 130, 36, Ui.PAPER, Ui.INK, 14);
        addButton("quests", ready ? "Requests (!)" : "Requests", 356, Layout.H - 46, 110, 36,
                ready ? Ui.GOLD : Ui.PAPER, Ui.INK, 14);
        canvas.add(Ui.text("Credits: " + nav.getProfile().getCredits(), "SansSerif-bold-15", Ui.GOLD, 490, Layout.H - 22));
        tooltip = new Tooltip();
        canvas.add(tooltip);
    }

    private void drawCard(GameMap map, double x, double y, int max) {
        List<Level> levels = Levels.onMap(map);
        boolean open = levels.get(0).getNumber() <= max;
        boolean current = levels.get(0).getNumber() <= max && max <= levels.get(levels.size() - 1).getNumber();
        GCompound window = Ui.window(map.getDisplayName(), x, y, CARD_W, CARD_H);
        canvas.add(window);
        canvas.add(Assets.mapScene(map, CARD_W - 16, 104), x + 8, y + 34);
        if (!open) {
            canvas.add(Ui.rect(x + 8, y + 34, CARD_W - 16, 104, new Color(20, 22, 36, 170)));
            canvas.add(Ui.centered("Locked", "SansSerif-bold-16", Ui.CHALK, x + CARD_W / 2, y + 92));
        }
        if (current) {
            for (int i = 0; i < portal.length; i++) {
                portal[i] = new GOval(30, 70);
                portal[i].setFilled(true);
                portal[i].setColor(new Color(120, 210, 255, 90));
                canvas.add(portal[i], x + CARD_W - 50, y + 56);
            }
        }
        double bw = (CARD_W - 28) / 3;
        for (int i = 0; i < levels.size(); i++) {
            Level l = levels.get(i);
            boolean ok = l.getNumber() <= max;
            Color fill = ok ? (l.getNumber() == max ? Ui.GOLD : Ui.PAPER) : Ui.DISABLED;
            Button b = addButton("level" + l.getNumber(), String.valueOf(l.getNumber()), x + 8 + i * (bw + 2), y + 144,
                    bw, 40, fill, ok ? Ui.INK : Ui.MUTED, 20);
            b.setEnabled(ok, fill);
            if (l.getBoss() != null) {
                canvas.add(Ui.centered("BOSS", "SansSerif-bold-9", Ui.PINK, x + 8 + i * (bw + 2) + bw / 2, y + 196));
            }
        }
        canvas.add(Ui.text("Enemy Lv." + levels.get(0).getEnemyLevel() + "-" + levels.get(levels.size() - 1).getEnemyLevel(),
                "SansSerif-plain-10", Ui.INK, x + 10, y + 208));
    }

    @Override
    public void update() {
        frame++;
        for (int i = 0; i < portal.length; i++) {
            if (portal[i] != null) {
                double pulse = 1 + 0.18 * Math.sin(frame * 0.12 + i * 2);
                double cx = portal[i].getX() + portal[i].getWidth() / 2;
                double cy = portal[i].getY() + portal[i].getHeight() / 2;
                double w = (30 - i * 8) * pulse;
                double h = (70 - i * 14) * pulse;
                portal[i].setBounds(cx - w / 2, cy - h / 2, w, h);
            }
        }
    }

    @Override
    protected void onHover(double x, double y) {
        if (tooltip == null) {
            return;
        }
        tooltip.hide();
        for (Button b : buttons) {
            if (b.getId().startsWith("level") && b.contains(x, y)) {
                Level l = Levels.get(Integer.parseInt(b.getId().substring(5)));
                if (l.getNumber() > nav.getMaxUnlocked()) {
                    tooltip.show("Locked", x, y, "Clear the level before it.");
                } else {
                    tooltip.show(l.getName(), x, y, l.getMap().getRule(), "Enemy Lv." + l.getEnemyLevel()
                            + (l.getBoss() != null ? ", with a boss: " + l.getBoss().getDisplayName() : ""));
                }
                tooltip.sendToFront();
                return;
            }
        }
    }

    @Override
    protected void onButton(String id) {
        if ("back".equals(id)) {
            nav.showMenu();
        } else if ("squad".equals(id)) {
            nav.showSquad(0);
        } else if ("store".equals(id)) {
            nav.showStore(0);
        } else if ("quests".equals(id)) {
            nav.showQuests(0);
        } else if (id.startsWith("level")) {
            nav.showExplore(Integer.parseInt(id.substring(5)));
        }
    }
}
