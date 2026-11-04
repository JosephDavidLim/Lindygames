package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GLine;
import acm.graphics.GRect;
import java.awt.Color;
import java.awt.Font;
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
    private final BossDock dock;
    private final GCompound overlay = new GCompound();
    private final List<UIButton> overlayButtons = new ArrayList<>();
    private Cyber.GlitchText overlayTitle;
    private double glitchTimer;
    private double dockShown;
    private boolean helpShown;

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
        buildBackdrop();
        world = new WorldView(this);
        dock = new BossDock(game);
        hud = new HudPanel(this);
        side = new SidePanel(this);
        add(world);
        add(dock);
        add(hud);
        add(side);
        buildBrackets();
        add(overlay);
    }

    /** Faint grid behind the (possibly shrunken) playfield, visible in the margins while the boss dock is open. */
    private void buildBackdrop() {
        for (int x = 0; x <= Ui.WORLD_W; x += 40) { GLine l = new GLine(x + 0.5, Ui.WORLD_Y, x + 0.5, Ui.BOTTOM_Y); l.setColor(Cyber.alpha(Cyber.CYAN, 14)); add(l); }
        for (int y = Ui.WORLD_Y; y <= Ui.BOTTOM_Y; y += 40) { GLine l = new GLine(0, y + 0.5, Ui.WORLD_W, y + 0.5); l.setColor(Cyber.alpha(Cyber.CYAN, 14)); add(l); }
    }

    /** HUD corner brackets framing the playfield. */
    private void buildBrackets() {
        double len = 22, in = 3;
        double x0 = Ui.WORLD_X + in, y0 = Ui.WORLD_Y + in + 1, x1 = Ui.WORLD_X + Ui.WORLD_W - in, y1 = Ui.BOTTOM_Y - in - 1;
        double[][] c = { { x0, y0, 1, 1 }, { x1, y0, -1, 1 }, { x0, y1, 1, -1 }, { x1, y1, -1, -1 } };
        Color col = Cyber.alpha(Cyber.CYAN, 150);
        for (double[] k : c) {
            GLine a = new GLine(k[0], k[1], k[0] + len * k[2], k[1]);
            GLine b = new GLine(k[0], k[1], k[0], k[1] + len * k[3]);
            a.setColor(col); b.setColor(col);
            add(a); add(b);
        }
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

    /** Opens / closes the full controls and modifier reference overlay (H key or the HELP button). */
    public void toggleHelp() { helpShown = !helpShown; buildOverlay(); }

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
            if (game.getState() != Game.State.PLAYING || helpShown) return;
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

    private Vec toWorld(double lx, double ly) { return world.toWorld(lx, ly); }

    /** Re-applies hover to every button for the last mouse position (after panels rebuild). */
    public void refreshHover() { for (UIButton b : allButtons()) b.setHover(b.hit(mouseLx, mouseLy)); }

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE: startWave(); break;
            case KeyEvent.VK_1: setSpeed(1); break;
            case KeyEvent.VK_2: setSpeed(2); break;
            case KeyEvent.VK_3: setSpeed(3); break;
            case KeyEvent.VK_P: togglePause(); break;
            case KeyEvent.VK_H: toggleHelp(); break;
            case KeyEvent.VK_ESCAPE:
                if (helpShown) { toggleHelp(); break; }
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
        dock.update(realDt);
        double target = dock.getHeight();
        dockShown += (target - dockShown) * Math.min(1, realDt * 9);
        if (Math.abs(target - dockShown) < 0.3) dockShown = target;
        world.setDock(dockShown);
        mouseWorld = toWorld(mouseLx, mouseLy);
        world.update(realDt);
        hud.update(realDt);
        side.update(realDt);
        for (UIButton b : allButtons()) b.animate(realDt);
        if (overlayTitle != null) {
            glitchTimer -= realDt;
            if (glitchTimer <= 0) { overlayTitle.burst(0.25); glitchTimer = 2.2 + Math.random() * 2.5; }
            overlayTitle.animate(realDt);
        }
        if (game.isVictorySequenceActive()) victoryFireworks(realDt);
        Game.State st = game.getState();
        if (st != shownState) { shownState = st; buildOverlay(); }
        else if (st == Game.State.PLAYING && game.isPaused() != pauseShown) buildOverlay();
    }

    private void victoryFireworks(double dt) {
        fxTimer -= dt;
        if (fxTimer > 0) return;
        fxTimer = 0.12;
        Vec c = game.getPalantir() != null ? game.getPalantir().getPosition() : new Vec(600, 450);
        double ang = game.getRng().range(0, Math.PI * 2), d = game.getRng().range(20, 260);
        Vec p = new Vec(c.x + Math.cos(ang) * d, c.y + Math.sin(ang) * d);
        int[] cols = { 0xffe94a, 0xff3b4e, 0x3bff8a, 0x00f0ff, 0xff2bd6 };
        game.getEvents().add(lindefense.combat.VisualEvent.explosion(p, game.getRng().range(30, 90), cols[game.getRng().nextInt(cols.length)]));
    }

    private void buildOverlay() {
        overlay.removeAll();
        overlayButtons.clear();
        overlayTitle = null;
        pauseShown = game.isPaused();
        Game.State st = game.getState();
        if (helpShown && st == Game.State.PLAYING) { buildHelpOverlay(); return; }
        if (st == Game.State.PLAYING && !game.isPaused()) return;
        boolean ended = st != Game.State.PLAYING;
        overlay.add(Ui.rect(Ui.WORLD_X, Ui.WORLD_Y, Ui.WORLD_W, Ui.WORLD_H, Ui.c(0x02040a, ended ? 190 : 140), Ui.c(0x000000, 0)));
        double cx = Ui.WORLD_X + Ui.WORLD_W / 2.0, cy = Ui.WORLD_Y + Ui.WORLD_H / 2.0;
        String title, micro, sub; Color col;
        if (st == Game.State.VICTORY) {
            title = "VICTORY"; col = Cyber.YELLOW; micro = "// mission complete";
            sub = "Palantir has fallen. All " + lindefense.wave.WaveData.WAVE_COUNT + " waves survived on " + game.getMap().getName() + ".";
        } else if (st == Game.State.DEFEAT) {
            title = "DEFEAT"; col = Cyber.RED; micro = "// system breach";
            sub = "Your defenses were breached on wave " + game.getWave() + ".";
        } else {
            title = "PAUSED"; col = Cyber.CYAN; micro = "// simulation halted";
            sub = "Press P or the play button to resume.";
        }
        double pw = 720, ph = ended ? 380 : 270;
        double px = cx - pw / 2, py = cy - ph / 2;
        overlay.add(new Cyber.Chamfer(px, py, pw, ph, 26).fill(new Color(6, 10, 24, 238)).border(col).glow(1.1).lineWidth(2).ticks(true));
        overlay.add(Ui.rect(px + 24, py + 12, 140, 3, Cyber.MAGENTA, Cyber.MAGENTA));
        GLabel m = Ui.label(Cyber.spaced(micro), 0, py + 44, Cyber.mono(Font.BOLD, 12), Cyber.alpha(col, 230));
        m.setLocation(cx - m.getWidth() / 2, py + 44);
        overlay.add(m);
        Cyber.GlitchText t = new Cyber.GlitchText(title, Cyber.display(Font.BOLD, 88), Cyber.lerp(col, Color.WHITE, 0.3));
        t.setLocation(cx - t.textWidth() / 2, py + 130);
        overlay.add(t);
        overlayTitle = t;
        t.burst(0.5);
        glitchTimer = 2.5;
        double sy = py + 166;
        for (String line : Ui.wrap(sub, Cyber.body(Font.PLAIN, 16), pw - 100)) {
            GLabel s = Ui.label(line, 0, sy, Cyber.body(Font.PLAIN, 16), Cyber.TEXT);
            s.setLocation(cx - s.getWidth() / 2, sy);
            overlay.add(s);
            sy += 22;
        }
        if (ended) {
            String[][] stats = {
                { "waves cleared", String.valueOf(game.getLastCompletedWave()), "" },
                { "hostiles purged", Cyber.commas(game.getEnemyManager().getTotalKilled()), "" },
                { "credits earned", Ui.money(game.getTotalCashEarned()), "y" },
                { "integrity left", String.valueOf(game.getLives()), "r" } };
            double cw = 158, gap = 12, sx = cx - (4 * cw + 3 * gap) / 2, cyy = py + 214;
            for (int i = 0; i < 4; i++) {
                double x = sx + i * (cw + gap);
                Color ac = stats[i][2].equals("y") ? Cyber.YELLOW : stats[i][2].equals("r") ? Cyber.RED : Cyber.CYAN;
                overlay.add(new Cyber.Chamfer(x, cyy, cw, 58, 8).scan(false).fill(new Color(8, 14, 32, 235)).border(Cyber.alpha(ac, 170)));
                overlay.add(Ui.label(Cyber.spaced(stats[i][0]), x + 12, cyy + 19, Cyber.mono(Font.BOLD, 9), Cyber.alpha(ac, 210)));
                overlay.add(Ui.label(stats[i][1], x + 12, cyy + 45, Cyber.mono(Font.BOLD, 20), Cyber.TEXT));
            }
            UIButton again = new UIButton(cx - 250, py + ph - 76, 240, 50, "PLAY AGAIN", Cyber.display(Font.BOLD, 22), () -> app.showMapSelect());
            again.setPrimary(true);
            UIButton menu = new UIButton(cx + 10, py + ph - 76, 240, 50, "MAIN MENU", Cyber.display(Font.BOLD, 22), () -> app.showMenu());
            menu.setAccent(Cyber.MAGENTA);
            overlay.add(again); overlay.add(menu);
            overlayButtons.add(again); overlayButtons.add(menu);
        } else {
            UIButton resume = new UIButton(cx - 250, py + ph - 76, 240, 50, "RESUME", Cyber.display(Font.BOLD, 22), () -> game.setPaused(false));
            resume.setPrimary(true);
            resume.setIcon(Cyber.Kind.PLAY);
            UIButton menu = new UIButton(cx + 10, py + ph - 76, 240, 50, "MAIN MENU", Cyber.display(Font.BOLD, 22), () -> app.showMenu());
            menu.setAccent(Cyber.MAGENTA);
            overlay.add(resume); overlay.add(menu);
            overlayButtons.add(resume); overlayButtons.add(menu);
        }
        refreshHover();
    }

    /** Full controls / modifiers / economy reference, opened from the side panel. */
    private void buildHelpOverlay() {
        overlay.add(Ui.rect(Ui.WORLD_X, Ui.WORLD_Y, Ui.WORLD_W, Ui.WORLD_H, Ui.c(0x02040a, 205), Ui.c(0x000000, 0)));
        double cx = Ui.WORLD_X + Ui.WORLD_W / 2.0, cy = Ui.WORLD_Y + Ui.WORLD_H / 2.0;
        double pw = 860, ph = 640, px = cx - pw / 2, py = cy - ph / 2;
        overlay.add(new Cyber.Chamfer(px, py, pw, ph, 24).fill(new Color(6, 10, 24, 240)).border(Cyber.CYAN).glow(1).lineWidth(2).ticks(true));
        overlay.add(Ui.label(Cyber.spaced("// field manual"), px + 34, py + 40, Cyber.mono(Font.BOLD, 12), Cyber.alpha(Cyber.MAGENTA, 230)));
        overlay.add(Ui.label("HELP", px + 34, py + 80, Cyber.display(Font.BOLD, 40), Cyber.TEXT));
        String[][] keys = { { "SPACE", "start next wave" }, { "1  2  3", "game speed" }, { "P", "pause / resume" }, { "B", "all towers / faction tabs" },
            { "ESC", "cancel / deselect (or right-click)" }, { "U", "upgrade selected tower" }, { "S", "sell selected tower" }, { "A", "fire selected tower's ability" },
            { "T", "cycle targeting priority" }, { "H", "toggle this help" } };
        double ky = py + 126;
        overlay.add(Ui.label(Cyber.spaced("// controls"), px + 34, ky, Cyber.mono(Font.BOLD, 11), Cyber.CYAN));
        for (String[] k : keys) {
            ky += 24;
            overlay.add(Ui.label(k[0], px + 34, ky, Cyber.mono(Font.BOLD, 14), Cyber.CYAN));
            overlay.add(Ui.label(k[1], px + 130, ky, Cyber.body(Font.PLAIN, 15), Cyber.TEXT));
        }
        double rx = px + 470, ry = py + 126, rw = pw - 470 - 34;
        String[][] notes = {
            { "enemy modifiers", "Camo enemies need detection (Perplexity, Bellarmine Student + AI, Brad). Regrow enemies heal over time. Fortified enemies carry double HP." },
            { "economy", "Enemies pay cash when destroyed and every wave pays a bonus. Selling refunds most of what you spent on a tower." },
            { "bosses", "Boss health shows in the dock under the top bar. Palantir (wave " + lindefense.wave.WaveData.WAVE_COUNT + ") disrupts your strongest towers with Predictive Surveillance; watch the telegraph." } };
        for (String[] n : notes) {
            overlay.add(Ui.label(Cyber.spaced("// " + n[0]), rx, ry, Cyber.mono(Font.BOLD, 11), Cyber.CYAN));
            for (String l : Ui.wrap(n[1], Cyber.body(Font.PLAIN, 14), rw)) { ry += 20; overlay.add(Ui.label(l, rx, ry, Cyber.body(Font.PLAIN, 14), Cyber.MUTED)); }
            ry += 34;
        }
        UIButton close = new UIButton(cx - 100, py + ph - 70, 200, 46, "CLOSE", Cyber.display(Font.BOLD, 20), this::toggleHelp);
        close.setPrimary(true);
        overlay.add(close);
        overlayButtons.add(close);
        refreshHover();
    }
}
