package view;

import acm.graphics.GCanvas;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * One full-window screen. The program shows one screen at a time, forwards
 * clicks to it, and calls {@link #update()} every game tick.
 */
public abstract class Screen {
    /** The canvas this screen draws on. */
    protected final GCanvas canvas;
    /** Used to switch to another screen. */
    protected final Navigator nav;
    /** Buttons on this screen. */
    protected final List<Button> buttons = new ArrayList<>();

    /**
     * Creates a screen.
     *
     * @param canvas the canvas to draw on
     * @param nav the navigator for switching screens
     */
    protected Screen(GCanvas canvas, Navigator nav) {
        this.canvas = canvas;
        this.nav = nav;
    }

    /**
     * Draws the screen's starting contents onto the canvas.
     */
    public abstract void show();

    /**
     * Removes everything this screen drew.
     */
    public void hide() {
        canvas.removeAll();
        buttons.clear();
    }

    /**
     * Runs one game tick. Does nothing by default.
     */
    public void update() {
    }

    /**
     * Handles a mouse press. Buttons get first chance, then {@link #onClick}.
     *
     * @param x click x in pixels
     * @param y click y in pixels
     */
    public void click(double x, double y) {
        for (Button b : buttons) {
            if (b.isEnabled() && b.contains(x, y)) {
                onButton(b.getId());
                return;
            }
        }
        onClick(x, y);
    }

    /**
     * Handles a key going down.
     *
     * @param code the key code from {@code java.awt.event.KeyEvent}
     */
    public void keyDown(int code) {
    }

    /**
     * Handles a key coming up.
     *
     * @param code the key code from {@code java.awt.event.KeyEvent}
     */
    public void keyUp(int code) {
    }

    /**
     * Handles the mouse moving: lights the button under it, then lets the
     * screen react.
     *
     * @param x x in pixels
     * @param y y in pixels
     */
    public void hover(double x, double y) {
        for (Button b : buttons) {
            b.setHover(b.contains(x, y));
        }
        onHover(x, y);
    }

    /**
     * Lets a screen react to where the mouse is.
     *
     * @param x x in pixels
     * @param y y in pixels
     */
    protected void onHover(double x, double y) {
    }

    /**
     * Draws part of a floor's corridor, darkened, as a screen background.
     *
     * @param map the map to show
     */
    protected void fillMapBackground(game.GameMap map) {
        canvas.add(Assets.corridor(map, 1440, Layout.H), 0, 0);
        canvas.add(Ui.rect(0, 0, Layout.W, Layout.H, new Color(6, 6, 20, 120)));
    }

    /**
     * Called when a button is clicked.
     *
     * @param id the button id
     */
    protected void onButton(String id) {
    }

    /**
     * Called when a click lands outside every button.
     *
     * @param x click x in pixels
     * @param y click y in pixels
     */
    protected void onClick(double x, double y) {
    }

    /**
     * Creates a button, adds it to the canvas and registers it for clicks.
     *
     * @param id button id
     * @param text button text
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     * @param fill background color
     * @param textColor text color
     * @param fontSize label font size
     * @return the button
     */
    protected Button addButton(String id, String text, double x, double y, double w, double h,
                               Color fill, Color textColor, int fontSize) {
        Button b = new Button(id, text, x, y, w, h, fill, textColor, fontSize);
        canvas.add(b.getShape(), x, y);
        buttons.add(b);
        return b;
    }

    /**
     * Fills the whole window with one color.
     *
     * @param c the color
     */
    protected void fillBackground(Color c) {
        canvas.add(Ui.rect(0, 0, Layout.W, Layout.H, c));
    }
}
