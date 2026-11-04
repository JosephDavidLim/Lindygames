package view;

import acm.graphics.GCanvas;
import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GLine;
import acm.graphics.GObject;
import acm.graphics.GRect;

import entities.DefenderType;
import game.GameMap;
import game.Level;
import game.Levels;
import game.Profile;
import game.Story;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A corridor of the Institute the player walks through before each fight.
 * People, terminals, lockers and notes tell the story to whoever stops to look.
 * The door at the far end leads to the fight.
 */
public class ExploreScreen extends Screen {
    private static final int WORLD_W = 1440;
    private static final int HERO_SIZE = 108;
    private static final double FEET_Y = 498;
    private static final double WALK = 5.2;
    private static final double START_X = 110;
    private static final double FIRST_STOP = 400;
    private static final double STOP_GAP = 300;
    private static final double DOOR_X = 1340;
    private static final double REACH = 66;
    private static final int RAIN = 44;

    private final int level;
    private final Level room;
    private final Profile profile;
    private final List<Story.Stop> stops;
    private final List<GObject> stopViews = new ArrayList<>();
    private final boolean[] seen;
    private final List<GLine> rain = new ArrayList<>();
    private final Random rng = new Random();

    private GObject scene;
    private GObject door;
    private GObject hero;
    private GCompound prompt;
    private GLabel promptText;
    private GCompound dialog;
    private GRect flicker;
    private double heroX = START_X;
    private double walkTarget = -1;
    private boolean left;
    private boolean right;
    private int openStop = -1;
    private int openPage;
    private int frame;

    /**
     * Creates the corridor before a level.
     *
     * @param canvas the canvas to draw on
     * @param nav the navigator
     * @param level the level at the end of this corridor
     */
    public ExploreScreen(GCanvas canvas, Navigator nav, int level) {
        super(canvas, nav);
        this.level = level;
        this.room = Levels.get(level);
        this.profile = nav.getProfile();
        this.stops = Story.forLevel(level);
        this.seen = new boolean[stops.size()];
    }

    @Override
    public void show() {
        GameMap map = room.getMap();
        Music.play(map.getMusic());
        scene = Assets.corridor(map, WORLD_W, Layout.H);
        canvas.add(scene, 0, 0);
        for (int i = 0; i < stops.size(); i++) {
            GObject v = makeStop(stops.get(i));
            stopViews.add(v);
            canvas.add(v);
        }
        door = makeDoor();
        canvas.add(door);
        hero = Assets.defender(profile.getMain(), HERO_SIZE);
        canvas.add(hero);
        for (int i = 0; i < RAIN; i++) {
            GLine drop = new GLine(0, 0, -1, 9);
            drop.setColor(new Color(150, 200, 255, 60));
            drop.setLocation(rng.nextInt(Layout.W), rng.nextInt(Layout.H));
            rain.add(drop);
            canvas.add(drop);
        }
        flicker = Ui.rect(0, 0, Layout.W, Layout.H, new Color(0, 0, 10, 0));
        canvas.add(flicker);
        drawHeader(map);
        prompt = new GCompound();
        prompt.add(Ui.round(0, 0, 170, 26, 8, new Color(8, 8, 24, 225)));
        promptText = Ui.centered("", "SansSerif-bold-12", Ui.GOLD, 85, 18);
        prompt.add(promptText);
        prompt.setVisible(false);
        canvas.add(prompt);
        place();
    }

    private void drawHeader(GameMap map) {
        canvas.add(Ui.round(8, 8, 390, 46, 10, new Color(8, 8, 24, 215)));
        canvas.add(Ui.text(map.getDisplayName() + ": " + room.getName(), "SansSerif-bold-15", Ui.GOLD, 18, 28));
        canvas.add(Ui.text("Attempt " + profile.getAttempt() + "    Arrows walk.  Up looks.  Find what you can.",
                "SansSerif-plain-11", Ui.CHALK, 18, 45));
        addButton("map", "Map", Layout.W - 76, 10, 66, 30, Ui.PAPER, Ui.INK, 13);
    }

    private GObject makeStop(Story.Stop stop) {
        DefenderType who = stop.getPerson();
        if (who != null && who != profile.getMain()) {
            return Assets.defender(who, 104);
        }
        GCompound c = new GCompound();
        Color neon = new Color(70, 230, 255);
        switch (stop.getKind()) {
            case LOCKER:
                c.add(Ui.round(0, 0, 66, 140, 3, new Color(30, 36, 86)));
                c.add(frame(0, 0, 66, 140, new Color(255, 70, 130)));
                c.add(Ui.rect(50, 60, 6, 18, new Color(255, 200, 90)));
                break;
            case PAPER:
                c.add(Ui.rect(0, 30, 60, 78, new Color(238, 232, 214)));
                for (int i = 0; i < 5; i++) {
                    c.add(Ui.rect(7, 42 + i * 12, 46 - i * 5, 3, new Color(60, 60, 90)));
                }
                c.add(frame(0, 30, 60, 78, new Color(255, 210, 110)));
                break;
            default:
                c.add(Ui.round(0, 0, 76, 118, 6, new Color(10, 12, 34)));
                c.add(Ui.rect(7, 8, 62, 52, new Color(18, 60, 96)));
                c.add(frame(0, 0, 76, 118, neon));
                for (int i = 0; i < 4; i++) {
                    c.add(Ui.rect(12, 15 + i * 10, 50 - i * 8, 3, neon));
                }
                break;
        }
        return c;
    }

    private static GRect frame(double x, double y, double w, double h, Color color) {
        GRect r = new GRect(x, y, w, h);
        r.setColor(color);
        return r;
    }

    private GObject makeDoor() {
        GCompound c = new GCompound();
        Color neon = room.getBoss() != null ? new Color(255, 60, 90) : new Color(70, 230, 255);
        c.add(Ui.rect(0, 0, 96, 190, new Color(8, 8, 26)));
        c.add(frame(0, 0, 96, 190, neon));
        c.add(frame(8, 8, 80, 174, neon));
        String text = room.getBoss() != null ? room.getBoss().getDisplayName().replace("The ", "").toUpperCase() : "PERIOD " + level;
        GLabel sign = Ui.centered(text, "SansSerif-bold-11", neon, 48, -8);
        c.add(sign);
        return c;
    }

    private double stopX(int i) {
        return FIRST_STOP + i * STOP_GAP;
    }

    private void place() {
        double camera = Math.max(0, Math.min(WORLD_W - Layout.W, heroX - Layout.W * 0.42));
        scene.setLocation(-camera, 0);
        for (int i = 0; i < stopViews.size(); i++) {
            GObject v = stopViews.get(i);
            v.setLocation(stopX(i) - camera - v.getWidth() / 2, FEET_Y - v.getHeight());
        }
        door.setLocation(DOOR_X - camera - 48, FEET_Y - 190);
        double bob = (left || right || walkTarget >= 0) ? -Math.abs(Math.sin(frame * 0.32)) * 6 : Math.sin(frame * 0.06) * 1.5;
        hero.setLocation(heroX - camera - HERO_SIZE / 2.0, FEET_Y - HERO_SIZE + bob + 6);

        int near = nearestStop();
        boolean atDoor = Math.abs(heroX - DOOR_X) < REACH + 14;
        prompt.setVisible(openStop < 0 && (near >= 0 || atDoor));
        if (prompt.isVisible()) {
            String text = atDoor ? "UP: go through the door" : "UP: " + (stops.get(near).getPerson() != null ? "talk" : "look");
            Ui.recenter(promptText, text, 85);
            prompt.setLocation(Math.max(4, Math.min(Layout.W - 174, heroX - camera - 85)), FEET_Y - HERO_SIZE - 34);
            prompt.sendToFront();
        }
    }

    private int nearestStop() {
        for (int i = 0; i < stops.size(); i++) {
            if (Math.abs(heroX - stopX(i)) < REACH) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void update() {
        frame++;
        for (GLine drop : rain) {
            double y = drop.getY() + 7;
            drop.setLocation(y > Layout.H ? rng.nextInt(Layout.W) : drop.getX() - 1, y > Layout.H ? -10 : y);
        }
        int alpha = (frame % 211 < 4 || frame % 379 < 3) ? 70 : 0;
        flicker.setFillColor(new Color(0, 0, 10, alpha));
        flicker.setColor(flicker.getFillColor());
        if (openStop < 0) {
            int move = (right ? 1 : 0) - (left ? 1 : 0);
            if (move != 0) {
                walkTarget = -1;
            } else if (walkTarget >= 0) {
                move = Math.abs(walkTarget - heroX) < WALK ? 0 : walkTarget > heroX ? 1 : -1;
                if (move == 0) {
                    walkTarget = -1;
                }
            }
            heroX = Math.max(50, Math.min(WORLD_W - 60, heroX + move * WALK));
        }
        place();
    }

    private void interact() {
        if (openStop >= 0) {
            advance();
        } else if (Math.abs(heroX - DOOR_X) < REACH + 14) {
            nav.showLoadout(level);
        } else {
            int near = nearestStop();
            if (near >= 0) {
                openStop = near;
                openPage = 0;
                seen[near] = true;
                Sfx.play("select");
                drawDialog();
            }
        }
    }

    private void advance() {
        openPage++;
        if (openPage >= stops.get(openStop).getPages().size()) {
            openStop = -1;
            closeDialog();
        } else {
            Sfx.play("select");
            drawDialog();
        }
    }

    private void closeDialog() {
        if (dialog != null) {
            canvas.remove(dialog);
            dialog = null;
        }
    }

    private void drawDialog() {
        closeDialog();
        Story.Stop stop = stops.get(openStop);
        boolean recording = stop.getPerson() != null && stop.getPerson() == profile.getMain();
        String speaker = recording ? "A recording, in your own voice" : stop.getSpeaker();
        dialog = new GCompound();
        dialog.add(Ui.round(0, 0, Layout.W - 40, 138, 14, new Color(8, 8, 24, 235)));
        GRect border = new GRect(0, 0, Layout.W - 40, 138);
        border.setColor(Ui.GOLD);
        dialog.add(border);
        dialog.add(Ui.text(speaker, "SansSerif-bold-16", Ui.GOLD, 18, 28));
        String text = Story.fill(stop.getPages().get(openPage), profile.getAttempt());
        double y = 56;
        for (String line : Ui.wrap(text, 72)) {
            dialog.add(Ui.text(line, "SansSerif-plain-15", Ui.CHALK, 18, y));
            y += 22;
        }
        dialog.add(Ui.text((openPage + 1) + " / " + stop.getPages().size() + "   UP or click to continue",
                "SansSerif-plain-11", Ui.MUTED, 18, 128));
        canvas.add(dialog, 20, 34);
    }

    @Override
    public void keyDown(int code) {
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
            left = true;
        } else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
            right = true;
        } else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_E) {
            interact();
        }
    }

    @Override
    public void keyUp(int code) {
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
            left = false;
        } else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
            right = false;
        }
    }

    @Override
    protected void onClick(double x, double y) {
        if (openStop >= 0) {
            advance();
            return;
        }
        double camera = Math.max(0, Math.min(WORLD_W - Layout.W, heroX - Layout.W * 0.42));
        double target = x + camera;
        if (Math.abs(target - heroX) < REACH) {
            interact();
        } else {
            walkTarget = target;
        }
    }

    @Override
    protected void onButton(String id) {
        if ("map".equals(id)) {
            nav.showLevelSelect();
        }
    }

    /**
     * Tells whether a stop has been looked at, for tests.
     *
     * @param index stop index
     * @return true once it has been opened
     */
    public boolean hasSeen(int index) {
        return seen[index];
    }

    /**
     * Sets the hero's position, for tests.
     *
     * @param x position along the corridor in pixels
     */
    public void setHeroX(double x) {
        heroX = x;
    }

    /**
     * Gets where a stop stands, for tests.
     *
     * @param index stop index
     * @return position along the corridor in pixels
     */
    public double getStopX(int index) {
        return stopX(index);
    }
}
