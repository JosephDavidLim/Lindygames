package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GObject;
import acm.graphics.GOval;
import acm.graphics.GRect;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Targeting;
import lindefense.tower.Tower;
import lindefense.tower.TowerType;

/**
 * The in-game screen: composes the world view, HUD and side panel, routes
 * mouse/keyboard input (in logical coordinates) to game actions, and shows the
 * pause / victory / defeat overlays.
 */
public final class GameScreen extends GCompound {
    private static final long serialVersionUID = 1L;

    private final LindefenseProgram app;
    private final Game game;
    private final WorldView world;
    private final HudPanel hud;
    private final SidePanel side;
    private final GCompound overlay = new GCompound();
    private final List<UIButton> overlayButtons = new ArrayList<>();
    private final List<GObject> fireworks = new ArrayList<>();

    private TowerType placing;
    private Tower selectedTower;
    private Enemy selectedEnemy;
    private Vec mouseWorld;
    private double mouseLx, mouseLy;
    private Game.State shownState = Game.State.PLAYING;
    private boolean pauseShown;
    private double fxTimer;

    public GameScreen(LindefenseProgram app, Game game) {
        this.app = app;
        this.game = game;
        add(Ui.rect(0, 0, Ui.W, Ui.H, Ui.BG, Ui.BG));
        world = new WorldView(this);
        hud = new HudPanel(this);
        side = new SidePanel(this);
        add(world);
        add(hud);
        add(side);
        add(overlay);
    }

    public Game getGame() { return game; }
    public TowerType getPlacing() { return placing; }
    public Tower getSelectedTower() { return selectedTower; }
    public Enemy getSelectedEnemy() { return selectedEnemy; }
    public Vec getMouseWorld() { return mouseWorld; }
    /** Last mouse position in logical coordinates {x, y}. */
    public double[] getMouseLogical() { return new double[] { mouseLx, mouseLy }; }
    public SidePanel getSidePanel() { return side; }
    /** B key / ALL button: switch the shop between one faction and all towers. */
    public void toggleShopAll() { side.toggleAll(); }

    // ------------------------------------------------------------------ actions
    public void beginPlacing(TowerType t) {
        placing = t;
        if (t != null) { selectedTower = null; selectedEnemy = null; }
        side.requestRebuild();
    }

    public void selectTower(Tower t) {
        selectedTower = t;
        if (t != null) { placing = null; selectedEnemy = null; }
        side.requestRebuild();
    }

    public void selectEnemy(Enemy e) {
        selectedEnemy = e;
        if (e != null) { placing = null; selectedTower = null; }
        side.requestRebuild();
    }

    public void startWave() { game.startNextWave(); }
    public void setSpeed(int s) { game.setSpeed(s); game.setPaused(false); }
    public void togglePause() { game.togglePause(); }
    public void toggleAuto() { game.getWaveManager().setAutoStart(!game.getWaveManager().isAutoStart()); }
    public void backToMenu() { app.showMenu(); }

    public void upgradeSelected() {
        if (selectedTower != null) { game.upgradeTower(selectedTower); side.requestRebuild(); }
    }

    public void sellSelected() {
        if (selectedTower != null) { game.sellTower(selectedTower); selectedTower = null; side.requestRebuild(); }
    }

    public void abilitySelected() {
        if (selectedTower != null) { game.activateAbility(selectedTower); side.requestRebuild(); }
    }

    public void cycleTargeting(int dir) {
        if (selectedTower == null) return;
        Targeting t = selectedTower.getTargeting();
        selectedTower.setTargeting(dir > 0 ? t.next() : t.prev());
        side.requestRebuild();
    }

    public void flashBanner(String text, Color col, double seconds) { hud.flashBanner(text, col, seconds); }

    // ------------------------------------------------------------------ input
    private List<UIButton> allButtons() {
        List<UIButton> all = new ArrayList<>(overlayButtons);
        all.addAll(hud.getButtons());
        all.addAll(side.getButtons());
        return all;
    }

    public void mousePressed(double lx, double ly, boolean rightButton) {
        mouseMoved(lx, ly);
        if (!overlayButtons.isEmpty()) {
            for (UIButton b : overlayButtons) if (b.hit(lx, ly)) { b.click(); return; }
            if (game.getState() != Game.State.PLAYING) return;
        }
        if (rightButton) {
            if (placing != null) beginPlacing(null);
            else { selectTower(null); selectEnemy(null); }
            return;
        }
        for (UIButton b : allButtons()) {
            if (b.hit(lx, ly)) { if (b.click()) return; }
        }
        if (lx >= Ui.PANEL_X) return;
        Vec w = toWorld(lx, ly);
        if (w == null) return;
        if (placing != null) {
            Tower t = game.placeTower(placing, w);
            if (t != null) {
                // keep placing the same type if affordable, otherwise select the new tower
                if (game.getCash() < t.getDef().baseCost) { placing = null; selectTower(t); }
                side.requestRebuild();
            }
            return;
        }
        Tower t = game.getTowerManager().towerAt(w);
        if (t != null) { selectTower(t); return; }
        Enemy e = world.pickEnemy(w);
        if (e != null) { selectEnemy(e); return; }
        selectTower(null);
        selectEnemy(null);
    }

    public void mouseMoved(double lx, double ly) {
        mouseLx = lx; mouseLy = ly;
        mouseWorld = toWorld(lx, ly);
        for (UIButton b : allButtons()) b.setHover(b.hit(lx, ly));
    }

    private Vec toWorld(double lx, double ly) {
        Vec w = new Vec(lx - Ui.WORLD_X, ly - Ui.WORLD_Y);
        return WorldView.insideWorld(w) ? w : null;
    }

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE: startWave(); break;
            case KeyEvent.VK_1: setSpeed(1); break;
            case KeyEvent.VK_2: setSpeed(2); break;
            case KeyEvent.VK_3: setSpeed(3); break;
            case KeyEvent.VK_P: togglePause(); break;
            case KeyEvent.VK_ESCAPE:
                if (placing != null) beginPlacing(null); else { selectTower(null); selectEnemy(null); }
                break;
            case KeyEvent.VK_U: upgradeSelected(); break;
            case KeyEvent.VK_S: sellSelected(); break;
            case KeyEvent.VK_A: abilitySelected(); break;
            case KeyEvent.VK_T: cycleTargeting(1); break;
            case KeyEvent.VK_B: toggleShopAll(); break;
            default: break;
        }
    }

    // ------------------------------------------------------------------ frame
    public void frame(double realDt) {
        game.advance(realDt);
        if (selectedTower != null && !selectedTower.isAlive()) selectTower(null);
        if (selectedEnemy != null && (!selectedEnemy.isAlive() && game.getTime() - selectedEnemy.getLastHitTime() > 3)) selectEnemy(null);
        world.update(realDt);
        hud.update(realDt);
        side.update(realDt);
        if (game.isVictorySequenceActive()) victoryFireworks(realDt);
        Game.State st = game.getState();
        if (st != shownState) { shownState = st; buildOverlay(); }
        else if (st == Game.State.PLAYING && game.isPaused() != pauseShown) buildOverlay();
    }

    private void victoryFireworks(double dt) {
        fxTimer -= dt;
        if (fxTimer > 0 || fireworks.size() > 60) return;
        fxTimer = 0.12;
        Vec c = game.getPalantir() != null ? game.getPalantir().getPosition() : new Vec(600, 450);
        double ang = game.getRng().range(0, Math.PI * 2), d = game.getRng().range(20, 260);
        Vec p = new Vec(c.x + Math.cos(ang) * d, c.y + Math.sin(ang) * d);
        int[] cols = { 0xffd166, 0xff5a5a, 0x7fd8a8, 0x7fd1ff, 0xff8cf0 };
        game.getEvents().add(lindefense.combat.VisualEvent.explosion(p, game.getRng().range(30, 90), cols[game.getRng().nextInt(cols.length)]));
    }

    private void buildOverlay() {
        overlay.removeAll();
        overlayButtons.clear();
        pauseShown = game.isPaused();
        Game.State st = game.getState();
        if (st == Game.State.PLAYING && !game.isPaused()) return;
        GRect dim = Ui.rect(Ui.WORLD_X, Ui.WORLD_Y, Ui.WORLD_W, Ui.WORLD_H, Ui.c(0x000000, st == Game.State.PLAYING ? 110 : 170), Ui.c(0x000000, 0));
        overlay.add(dim);
        double cx = Ui.WORLD_X + Ui.WORLD_W / 2.0, cy = Ui.WORLD_Y + Ui.WORLD_H / 2.0;
        String title; Color col; String sub;
        if (st == Game.State.VICTORY) {
            title = "VICTORY"; col = Ui.GOLD;
            sub = "Palantir has fallen. All 100 waves survived on " + game.getMap().getName() + ".";
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                GOval o = Ui.oval(cx + Math.cos(a) * 300 - 10, cy + Math.sin(a) * 220 - 10, 20, 20, Ui.c(i % 2 == 0 ? 0xffd166 : 0x7fd1ff, 200), Ui.c(0xffffff));
                overlay.add(o);
            }
        } else if (st == Game.State.DEFEAT) {
            title = "DEFEAT"; col = Ui.RED;
            sub = "Your defenses were breached on wave " + game.getWave() + ".";
        } else {
            title = "PAUSED"; col = Ui.TEXT;
            sub = "Press P or the play button to resume.";
        }
        GLabel t = Ui.label(title, 0, 0, Ui.F_TITLE, col);
        t.setLocation(cx - t.getWidth() / 2, cy - 60);
        overlay.add(t);
        GLabel s = Ui.label(sub, 0, 0, Ui.F_H2, Ui.TEXT);
        s.setLocation(cx - s.getWidth() / 2, cy - 10);
        overlay.add(s);
        if (st != Game.State.PLAYING) {
            String stats = "Waves cleared: " + game.getLastCompletedWave() + "    Enemies destroyed: " + game.getEnemyManager().getTotalKilled()
                + "    Cash earned: " + Ui.money(game.getTotalCashEarned()) + "    Lives left: " + game.getLives();
            GLabel st2 = Ui.label(stats, 0, 0, Ui.F_BODY, Ui.MUTED);
            st2.setLocation(cx - st2.getWidth() / 2, cy + 24);
            overlay.add(st2);
            UIButton again = new UIButton(cx - 190, cy + 60, 180, 44, "Play Again", () -> app.showMapSelect());
            UIButton menu = new UIButton(cx + 10, cy + 60, 180, 44, "Main Menu", () -> app.showMenu());
            overlay.add(again); overlay.add(menu);
            overlayButtons.add(again); overlayButtons.add(menu);
        } else {
            UIButton resume = new UIButton(cx - 190, cy + 40, 180, 44, "Resume", () -> game.setPaused(false));
            UIButton menu = new UIButton(cx + 10, cy + 40, 180, 44, "Main Menu", () -> app.showMenu());
            overlay.add(resume); overlay.add(menu);
            overlayButtons.add(resume); overlayButtons.add(menu);
        }
    }
}
