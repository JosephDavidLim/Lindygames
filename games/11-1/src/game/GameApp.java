package game;

import acm.graphics.GCanvas;
import acm.program.GraphicsProgram;

import entities.DefenderType;
import view.ExploreScreen;
import view.LevelSelectScreen;
import view.Layout;
import view.LoadoutScreen;
import view.MenuScreen;
import view.Navigator;
import view.PlayScreen;
import view.QuestScreen;
import view.ResultScreen;
import view.ScaledCanvas;
import view.SquadScreen;
import view.StoreScreen;
import view.StoryScreen;
import view.Screen;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Entry point for 11/1. Owns the window, the game loop and the
 * current screen. Mouse presses arrive on the AWT thread, so they are queued
 * and handled by the loop thread to avoid concurrent modification.
 */
public class GameApp extends GraphicsProgram implements Navigator {
    private static final long serialVersionUID = 1L;
    private static final long FRAME_MS = 33;
    private static final File SAVE = new File("progress.txt");

    private final ConcurrentLinkedQueue<double[]> clicks = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<int[]> keys = new ConcurrentLinkedQueue<>();
    private volatile double hoverX = -1;
    private volatile double hoverY = -1;
    private Screen current;
    private Profile profile;
    private boolean newSlot;
    private boolean firstClear;

    /**
     * Uses a canvas that scales the game to fit the window.
     *
     * @return the canvas
     */
    @Override
    protected GCanvas createGCanvas() {
        return new ScaledCanvas();
    }

    /**
     * Sets up the window and mouse input.
     */
    @Override
    public void init() {
        setSize(Layout.W, Layout.H);
        setTitle("11/1");
        addMouseListeners();
        addKeyListeners();
        profile = Profile.load(SAVE);
    }

    /**
     * Runs the game loop: handle queued clicks, update the screen, wait for the next frame.
     */
    @Override
    public void run() {
        int extraW = Layout.W - getWidth();
        int extraH = Layout.H - getHeight();
        if (extraW != 0 || extraH != 0) {
            setSize(Layout.W + extraW, Layout.H + extraH);
        }
        showMenu();
        while (true) {
            long start = System.currentTimeMillis();
            double[] c;
            while ((c = clicks.poll()) != null) {
                current.click(c[0], c[1]);
            }
            int[] k;
            while ((k = keys.poll()) != null) {
                if (k[1] == 1) {
                    current.keyDown(k[0]);
                } else {
                    current.keyUp(k[0]);
                }
            }
            current.hover(hoverX, hoverY);
            current.update();
            pause(Math.max(1, FRAME_MS - (System.currentTimeMillis() - start)));
        }
    }

    /**
     * Queues a mouse press for the game loop.
     *
     * @param e the mouse event
     */
    @Override
    public void mousePressed(MouseEvent e) {
        ScaledCanvas c = (ScaledCanvas) getGCanvas();
        clicks.add(new double[] {c.toGameX(e.getX()), c.toGameY(e.getY())});
        getGCanvas().requestFocusInWindow();
    }

    /**
     * Remembers where the mouse is for the game loop.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        ScaledCanvas c = (ScaledCanvas) getGCanvas();
        hoverX = c.toGameX(e.getX());
        hoverY = c.toGameY(e.getY());
    }

    /**
     * Treats dragging like moving, so tooltips keep working with a button held.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    /**
     * Queues a key press for the game loop.
     *
     * @param e the key event
     */
    @Override
    public void keyPressed(KeyEvent e) {
        keys.add(new int[] {e.getKeyCode(), 1});
    }

    /**
     * Queues a key release for the game loop.
     *
     * @param e the key event
     */
    @Override
    public void keyReleased(KeyEvent e) {
        keys.add(new int[] {e.getKeyCode(), 0});
    }

    private void go(Screen next) {
        if (current != null) {
            current.hide();
        }
        current = next;
        current.show();
        getGCanvas().requestFocusInWindow();
    }

    @Override
    public void showMenu() {
        go(new MenuScreen(getGCanvas(), this));
    }

    @Override
    public void showLevelSelect() {
        go(new LevelSelectScreen(getGCanvas(), this));
    }

    @Override
    public void showLoadout(int level) {
        go(new LoadoutScreen(getGCanvas(), this, level));
    }

    @Override
    public void startLevel(int level, List<DefenderType> loadout) {
        go(new PlayScreen(getGCanvas(), this, level, loadout));
    }

    @Override
    public void showResult(int level, boolean won, List<DefenderType> loadout, Party party) {
        saveProfile();
        go(new ResultScreen(getGCanvas(), this, level, won, loadout, party));
        newSlot = false;
        firstClear = false;
    }

    @Override
    public void showStory(boolean ending) {
        go(new StoryScreen(getGCanvas(), this, ending));
    }

    @Override
    public void showExplore(int level) {
        go(new ExploreScreen(getGCanvas(), this, level));
    }

    @Override
    public void showQuests(int returnLevel) {
        go(new QuestScreen(getGCanvas(), this, returnLevel));
    }

    @Override
    public void showSquad(int returnLevel) {
        go(new SquadScreen(getGCanvas(), this, returnLevel));
    }

    @Override
    public void showStore(int returnLevel) {
        go(new StoreScreen(getGCanvas(), this, returnLevel));
    }

    @Override
    public Profile getProfile() {
        return profile;
    }

    @Override
    public void saveProfile() {
        profile.save(SAVE);
    }

    @Override
    public int getMaxUnlocked() {
        return profile.getMaxUnlocked();
    }

    @Override
    public int getSlots() {
        return Levels.slotsAfterClearing(profile.getMaxUnlocked() - 1);
    }

    @Override
    public boolean firstClearJustHappened() {
        return firstClear;
    }

    @Override
    public boolean slotJustUnlocked() {
        return newSlot;
    }

    @Override
    public void levelCleared(int level) {
        int before = getSlots();
        firstClear = profile.levelCleared(level);
        newSlot = getSlots() > before;
        saveProfile();
    }

    /**
     * Launches the game.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        new GameApp().start(args);
    }
}
