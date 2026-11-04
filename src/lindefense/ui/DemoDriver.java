package lindefense.ui;

import acm.graphics.GCanvas;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import lindefense.enemy.EnemyType;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Tower;
import lindefense.tower.TowerType;

/**
 * Optional scripted walkthrough used for automated UI verification. Enabled
 * with {@code -Dlindefense.demo=<output dir>}; it drives the real screens
 * through the program's own mouse handlers (pixel coordinates mapped through
 * the scaled layer), resizes the window, verifies click-to-place lands on the
 * cursor at each size, writes PNG snapshots of the canvas, and exits.
 * Not used in normal play.
 */
final class DemoDriver {
    private final LindefenseProgram app;
    private final File out;
    private final List<double[]> steps = new ArrayList<>(); // {time, index}
    private double t;
    private int next;
    private final List<String> results = new ArrayList<>();

    DemoDriver(LindefenseProgram app, String dir) {
        this.app = app;
        this.out = new File(dir);
        out.mkdirs();
        double[] times = { 2.6, 2.8, 3.8, 4.0, 10.0, 10.5, 12.0, 12.5, 14.0, 14.5, 16.0, 18.5, 19.0, 10.2, 10.4, 0.5, 1.6, 1.9, 2.2, 2.5 };
        for (int i = 0; i < times.length; i++) steps.add(new double[] { times[i], i });
        steps.sort((a, b) -> Double.compare(a[0], b[0]));
    }

    void frame(double dt) {
        t += dt;
        while (next < steps.size() && t >= steps.get(next)[0]) {
            int idx = (int) steps.get(next)[1];
            next++;
            try { run(idx); } catch (Exception e) { results.add("FAIL step " + idx + ": " + e); e.printStackTrace(); }
        }
    }

    private void run(int idx) {
        GCanvas canvas = app.getGCanvas();
        switch (idx) {
            case 0: snap("01_menu"); break;
            case 1: clickLogical(Ui.W / 2.0, 528); break;              // Play
            case 2: snap("02_mapselect"); break;
            case 3: {
                {   // Play map 1: same layout math as MapSelectScreen (4 columns, rows by MAP_COUNT)
                    int rows = Math.max(1, (lindefense.map.MapCatalog.MAP_COUNT + 3) / 4);
                    double cardW = (Ui.W - 50 - 18 * 3) / 4.0;
                    double cardH = (Ui.H - 28 - 108 - 18 * (rows - 1)) / rows;
                    double buttonY = 108 + cardH - (cardH < 520 ? 54 : 70);
                    clickLogical(25 + cardW / 2, buttonY + 20);
                }
                Game g = app.getGame();
                g.addCash(5000, null);
                placeViaClick(TowerType.GPT, new Vec(380, 300));
                placeViaClick(TowerType.CLAUDE, new Vec(700, 250));
                placeViaClick(TowerType.PERPLEXITY, new Vec(460, 520));
                placeViaClick(TowerType.BRAD_LINDEMANN, new Vec(560, 480));
                placeViaClick(TowerType.SUNO, new Vec(820, 660));
                app.getGameScreen().keyPressed(new java.awt.event.KeyEvent(canvas, 0, 0, 0, java.awt.event.KeyEvent.VK_SPACE, ' '));
                app.getGameScreen().setSpeed(3);
                for (int k = 0; k < 12; k++) g.getWaveManager().startNextWave();
                break;
            }
            case 4: {
                for (Tower tw : app.getGame().getTowerManager().getTowers()) if (tw.getType() == TowerType.BRAD_LINDEMANN) app.getGameScreen().selectTower(tw);
                app.getGameScreen().frame(0.01);
                snap("03_game_default"); app.getGameScreen().setSpeed(1); break; }
            case 5: { resize(900, 600); break; }
            case 6: { snap("04_game_small_900x600"); verifyClick("small"); break; }
            case 7: { resize(2400, 1400); break; }
            case 8: { snap("05_game_large_2400x1400"); verifyClick("large"); break; }
            case 9: {
                resize(1500, 950);
                Game g = app.getGame();
                // jump to the finale: Palantir + a boss escort
                for (int k = g.getWave(); k < 99; k++) { g.getWaveManager().startNextWave(); g.getWaveManager().endWave(); }
                g.getEnemyManager().clear();
                g.startNextWave();
                g.getEnemyManager().spawn(EnemyType.BELLARMINE_COLLEGE_PREP, null, 1, 400);
                g.addCash(20000, null);
                for (Tower tw : new ArrayList<>(g.getTowerManager().getTowers())) for (int u = 0; u < 3; u++) g.upgradeTower(tw);
                g.setSpeed(3);
                break;
            }
            case 10: {
                Game g = app.getGame();
                if (g.getPalantir() != null) g.getPalantir().beginSurveillance(g);
                break;
            }
            case 11: snap("06_palantir"); break;
            case 13: {
                // ALL-towers shop view: toggle via the hotkey path, hover a cell, snapshot, then verify a cell click starts placement
                app.getGameScreen().toggleShopAll();
                ShopLayout.Cell cell = ShopLayout.allCells().get(19);
                MouseEvent mv = pixelEvent(cell.x + 5, cell.y + 5);
                app.mouseMoved(mv);
                app.getGameScreen().frame(0.01);
                snap("03b_shop_all");
                app.mousePressed(mv);
                boolean ok = app.getGameScreen().getPlacing() == cell.type && app.getGameScreen().getSidePanel().isAllMode();
                results.add((ok ? "PASS" : "FAIL") + " ALL-view cell click starts placement of " + cell.type + " (got " + app.getGameScreen().getPlacing() + ")");
                app.getGameScreen().beginPlacing(null);
                break;
            }
            case 14: { app.getGameScreen().toggleShopAll(); break; }
            case 15: snap("00_menu_intro"); break;
            case 16: { resize(900, 600); break; }
            case 17: { snap("00b_menu_small"); resize(2400, 1400); break; }
            case 18: { snap("00c_menu_large"); resize(1600, 1028); break; }
            case 19: { app.mouseMoved(pixelEvent(Ui.W / 2.0, 528)); break; }
            case 12: {
                for (String r : results) System.out.println(r);
                System.out.println("DEMO DONE");
                System.exit(0);
                break;
            }
            default: break;
        }
    }

    private void resize(int w, int h) {
        Window win = SwingUtilities.getWindowAncestor(app.getGCanvas());
        win.setSize(w, h);
        win.validate();
    }

    private MouseEvent pixelEvent(double lx, double ly) {
        ScaledLayer root = app.getRoot();
        int px = (int) Math.round(lx * root.getScale() + (app.getGCanvas().getWidth() - Ui.W * root.getScale()) / 2);
        int py = (int) Math.round(ly * root.getScale() + (app.getGCanvas().getHeight() - Ui.H * root.getScale()) / 2);
        return new MouseEvent(app.getGCanvas(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, px, py, 1, false, MouseEvent.BUTTON1);
    }

    /** Sends a press through the real program mouse handler at the pixel position of a logical point. */
    private void clickLogical(double lx, double ly) {
        MouseEvent ev = pixelEvent(lx, ly);
        app.mouseMoved(ev);
        app.mousePressed(ev);
    }

    private void placeViaClick(TowerType type, Vec world) {
        app.getGameScreen().beginPlacing(type);
        clickLogical(world.x + Ui.WORLD_X, world.y + Ui.WORLD_Y);
        app.getGameScreen().beginPlacing(null);
    }

    private void verifyClick(String label) {
        Game g = app.getGame();
        g.addCash(1000, null);
        Vec target = new Vec(label.equals("small") ? 150 : 320, 60);
        int before = g.getTowerManager().getTowers().size();
        placeViaClick(TowerType.QWEN, target);
        Tower placed = null;
        for (Tower tw : g.getTowerManager().getTowers()) if (tw.getType() == TowerType.QWEN && tw.getPosition().dist(target) < 1.5) placed = tw;
        boolean ok = placed != null && g.getTowerManager().getTowers().size() == before + 1;
        results.add((ok ? "PASS" : "FAIL") + " click-to-place after resize (" + label + ", canvas "
            + app.getGCanvas().getWidth() + "x" + app.getGCanvas().getHeight() + ", scale " + String.format("%.3f", app.getRoot().getScale()) + ")"
            + (placed != null ? " tower at " + placed.getPosition() : " no tower at " + target));
    }

    private void snap(String name) {
        GCanvas canvas = app.getGCanvas();
        int w = Math.max(1, canvas.getWidth()), h = Math.max(1, canvas.getHeight());
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        canvas.paint(g2);
        g2.dispose();
        try {
            ImageIO.write(img, "png", new File(out, name + ".png"));
            results.add("SNAP " + name + " " + w + "x" + h);
        } catch (Exception e) {
            results.add("FAIL snapshot " + name + ": " + e);
        }
    }
}
