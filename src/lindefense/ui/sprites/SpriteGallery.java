package lindefense.ui.sprites;

import acm.graphics.GCanvas;
import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GLine;
import acm.program.GraphicsProgram;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.imageio.ImageIO;
import lindefense.enemy.Boss;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyType;
import lindefense.enemy.Modifier;
import lindefense.enemy.StatusEffect;
import lindefense.map.Path;

/**
 * Visual self-check for the sprite package. Lays out every ordinary enemy
 * type in a grid of states (base, camo, regrow, fortified, all three, two
 * damage levels and five status cues), loops the death and split effects
 * underneath, and shows the five bosses large with Palantir in each phase and
 * while telegraphing. Enemies crawl around small loops so facing is exercised.
 *
 * <p>Run with {@code -Dgallery.shots=/some/dir} to have the canvas rendered to
 * PNG files at a few points in time and then exit; otherwise it runs until the
 * window is closed. The telegraph state is driven through reflection on
 * {@code Boss.telegraphRemaining} because starting a real surveillance needs a
 * full {@code Game}.</p>
 */
@SuppressWarnings("serial")
public class SpriteGallery extends GraphicsProgram {
    private static final int W = 1440, H = 1000;
    private static final int X0 = 72, COL = 76, Y0 = 46, ROW = 54;
    private static final String[] ROWS = { "base", "camo", "regrow", "fortified", "all three", "hp 50%", "hp 15%",
        "slowed", "stunned", "marked", "revealed", "disrupted" };

    private static final class Cell {
        Enemy e; EnemySprite s; boolean hitLoop; Boss telegraph;
    }

    private static final class FxSlot {
        Enemy e; Animated fx; GCompound g; boolean split; double restart; double period;
    }

    private final List<Cell> cells = new ArrayList<>();
    private final List<FxSlot> fx = new ArrayList<>();
    private double t;

    /** Entry point. */
    public static void main(String[] args) { new SpriteGallery().start(args); }

    @Override public void init() { setSize(W, H); }

    private static Path loop(double cx, double cy, double hw, double hh) {
        return new Path("loop", cx - hw, cy - hh, cx + hw, cy - hh, cx + hw, cy + hh, cx - hw, cy + hh, cx - hw, cy - hh);
    }

    private Cell place(EnemyType type, EnumSet<Modifier> mods, double cx, double cy, double hw, double hh, double speedScale) {
        Path p = loop(cx, cy, hw, hh);
        Enemy e = type.isBoss() ? new Boss(type, mods, p, 0, 0, 1.0, speedScale) : new Enemy(type, mods, p, 0, 0, 1.0, speedScale);
        e.setDistance(Math.random() * p.getTotalLength());
        Cell c = new Cell();
        c.e = e;
        c.s = SpriteFactory.create(e);
        add(c.s);
        cells.add(c);
        return c;
    }

    private void label(String text, double x, double y, int size, Color c) {
        GLabel l = new GLabel(text, x, y);
        l.setFont(new Font("SansSerif", Font.PLAIN, size));
        l.setColor(c);
        add(l);
    }

    @Override public void run() {
        setBackground(new Color(78, 132, 70));
        GCanvas canvas = getGCanvas();
        canvas.setBackground(new Color(78, 132, 70));
        List<EnemyType> normal = new ArrayList<>();
        for (EnemyType et : EnemyType.values()) if (!et.isBoss()) normal.add(et);

        EnumSet<Modifier> none = EnumSet.noneOf(Modifier.class);
        EnumSet<Modifier> camo = EnumSet.of(Modifier.CAMO);
        EnumSet<Modifier> regrow = EnumSet.of(Modifier.REGROW);
        EnumSet<Modifier> fort = EnumSet.of(Modifier.FORTIFIED);
        EnumSet<Modifier> all = EnumSet.allOf(Modifier.class);

        for (int row = 0; row < ROWS.length; row++) label(ROWS[row], 4, Y0 + row * ROW + 4, 10, Color.WHITE);
        for (int col = 0; col < normal.size(); col++) {
            EnemyType type = normal.get(col);
            String n = type.displayName.replace("Linked List", "LL").replace(" Sort", "").replace(" Array", "Arr").replace("Primitive Variable", "Primitive").replace("Quick Partition", "QPartition");
            label(n, X0 + col * COL - 30, 14, 9, Color.WHITE);
            double cx = X0 + col * COL;
            for (int row = 0; row < ROWS.length; row++) {
                double cy = Y0 + row * ROW;
                EnumSet<Modifier> mods = row == 1 || row == 10 ? camo : row == 2 ? regrow : row == 3 ? fort : row == 4 ? all : none;
                Cell c = place(type, mods, cx, cy, 10, 5, 0.18);
                Enemy e = c.e;
                switch (row) {
                    case 5: e.takeDamage(e.getMaxHp() * 0.5, 0); c.hitLoop = true; e.setShellJumpIndex(1); break;
                    case 6: e.takeDamage(e.getMaxHp() * 0.85, 0); c.hitLoop = true; e.setShellJumpIndex(3); break;
                    case 7: e.applyStatus(StatusEffect.Kind.SLOW, 0.5, 1e9); break;
                    case 8: e.applyStatus(StatusEffect.Kind.STUN, 1, 1e9); break;
                    case 9: e.applyStatus(StatusEffect.Kind.MARKED, 0.25, 1e9); break;
                    case 10: e.applyStatus(StatusEffect.Kind.REVEALED, 1, 1e9); break;
                    case 11: e.applyStatus(StatusEffect.Kind.DISRUPTED, 1, 1e9); break;
                    default: break;
                }
                if (type == EnemyType.BUCKET_SORT && row != 0) {
                    int n2 = Math.min(6, row);
                    EnemyType[] kinds = { EnemyType.PRIMITIVE_VARIABLE, EnemyType.BUBBLE_SORT, EnemyType.STATIC_ARRAY,
                        EnemyType.SINGLY_LINKED_LIST, EnemyType.INSERTION_SORT, EnemyType.MERGE_RUN };
                    for (int k = 0; k < n2; k++) e.getStoredEnemies().add(new Enemy(kinds[k], none, e.getRoute(), 0, 0, 1, 1));
                }
                if (type == EnemyType.DOUBLY_LINKED_LIST && row >= 5 && row <= 6) {
                    for (int k = 0; k < (row == 5 ? 3 : 6); k++) { e.takeDamage(0.001, 0); e.heal(0.001); }
                }
            }
            // effects: death loop and split loop
            double fy = Y0 + ROWS.length * ROW + 12;
            label("death", 4, fy + 4, 10, Color.WHITE);
            label("split", 4, fy + 58, 10, Color.WHITE);
            FxSlot d = new FxSlot();
            d.e = new Enemy(type, none, loop(cx, fy, 1, 1), 0, 0, 1, 1);
            d.e.setDistance(1);
            d.period = 0.9; d.restart = col * 0.11;
            fx.add(d);
            if (type.hasChildren()) {
                FxSlot s = new FxSlot();
                s.e = new Enemy(type, none, loop(cx, fy + 54, 1, 1), 0, 0, 1, 1);
                s.e.setDistance(1);
                if (type == EnemyType.BUCKET_SORT) {
                    s.e.getStoredEnemies().add(new Enemy(EnemyType.PRIMITIVE_VARIABLE, none, s.e.getRoute(), 0, 0, 1, 1));
                    s.e.getStoredEnemies().add(new Enemy(EnemyType.BUBBLE_SORT, none, s.e.getRoute(), 0, 0, 1, 1));
                    s.e.getStoredEnemies().add(new Enemy(EnemyType.QUICK_PARTITION, none, s.e.getRoute(), 0, 0, 1, 1));
                }
                s.split = true; s.period = 1.0; s.restart = 0.3 + col * 0.11;
                fx.add(s);
            }
        }
        GLine sep = new GLine(0, Y0 + ROWS.length * ROW + 92, W, Y0 + ROWS.length * ROW + 92);
        sep.setColor(new Color(255, 255, 255, 120));
        add(sep);

        // bosses
        double by = Y0 + ROWS.length * ROW + 205;
        label("bosses: Bellarmine (MOAB) | SW Resource Consultant (BFB) | 3Com (ZOMG) | Lockheed Martin (DDT, camo; below: revealed + 70% dmg) | "
            + "Palantir phase 0 / phase 1 / phase 2 + telegraph | Bellarmine fortified 80% dmg | Lockheed death loop", 4, by - 110, 10, Color.WHITE);
        place(EnemyType.BELLARMINE_COLLEGE_PREP, none, 80, by, 20, 10, 0.5);
        place(EnemyType.SW_RESOURCE_CONSULTANT, none, 215, by, 20, 10, 0.5);
        place(EnemyType.THREE_COM, none, 380, by, 20, 10, 0.5);
        place(EnemyType.LOCKHEED_MARTIN, none, 540, by - 55, 20, 8, 0.3);
        Cell lr = place(EnemyType.LOCKHEED_MARTIN, none, 540, by + 65, 20, 8, 0.3);
        lr.e.applyStatus(StatusEffect.Kind.REVEALED, 1, 1e9);
        lr.e.takeDamage(lr.e.getMaxHp() * 0.7, 0);
        lr.hitLoop = true;
        place(EnemyType.PALANTIR, none, 730, by, 20, 8, 0.6);
        Cell p1 = place(EnemyType.PALANTIR, none, 940, by, 20, 8, 0.6);
        p1.e.takeDamage(p1.e.getMaxHp() * 0.5, 0);
        ((Boss) p1.e).updatePhase();
        Cell p2 = place(EnemyType.PALANTIR, none, 1150, by, 20, 8, 0.6);
        p2.e.takeDamage(p2.e.getMaxHp() * 0.8, 0);
        ((Boss) p2.e).updatePhase();
        p2.telegraph = (Boss) p2.e;
        Cell bf = place(EnemyType.BELLARMINE_COLLEGE_PREP, fort, 1320, by - 40, 20, 10, 0.5);
        bf.e.takeDamage(bf.e.getMaxHp() * 0.8, 0);
        bf.hitLoop = true;
        FxSlot bd = new FxSlot();
        bd.e = new Boss(EnemyType.LOCKHEED_MARTIN, none, loop(1340, by + 60, 1, 1), 0, 0, 1, 1);
        bd.e.setDistance(1);
        bd.period = 3.0; bd.restart = 0.5;
        fx.add(bd);
        FxSlot pd = new FxSlot();
        pd.e = new Boss(EnemyType.PALANTIR, none, loop(730, by, 1, 1), 0, 0, 1, 1);
        pd.e.setDistance(1);
        pd.period = 7.0; pd.restart = 4.0;
        fx.add(pd);

        String shots = System.getProperty("gallery.shots");
        if (shots != null) {
            // keep the window on top while capturing so other windows cannot occlude the grab
            java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(canvas);
            if (w != null) { w.setAlwaysOnTop(true); w.toFront(); }
        }
        double[] shotTimes = { 1.2, 2.0, 2.6, 3.3, 5.3, 6.4, 8.2 };
        int shot = 0;
        double dt = 1.0 / 60;
        long last = System.nanoTime();
        while (true) {
            t += dt;
            for (Cell c : cells) {
                Enemy e = c.e;
                if (e.advance(dt, 1.0)) e.setDistance(0);
                if (c.hitLoop && ((int) (t / 1.5)) != ((int) ((t - dt) / 1.5))) { e.takeDamage(0.001, t); e.heal(0.001); }
                if (c.telegraph != null) setTelegraph(c.telegraph, Math.max(0, Boss.TELEGRAPH_TIME - (t % 4.2)));
                c.s.sync(e, t);
            }
            for (FxSlot f : fx) {
                if (f.fx != null) {
                    if (!f.fx.step(dt)) { remove(f.g); f.fx = null; f.restart = t + f.period; }
                } else if (t >= f.restart) {
                    f.fx = f.split ? SpriteFactory.createSplitEffect(f.e) : SpriteFactory.createDeathEffect(f.e);
                    f.g = (GCompound) f.fx;
                    add(f.g);
                }
            }
            if (shots != null && shot < shotTimes.length && t >= shotTimes[shot]) {
                capture(canvas, new File(shots, "gallery-" + shot + ".png"));
                shot++;
                if (shot == shotTimes.length) {
                    pause(400);
                    System.exit(0);
                }
            }
            long now = System.nanoTime();
            double sleep = 16 - (now - last) / 1e6;
            last = now;
            pause(Math.max(1, sleep));
        }
    }

    private static void setTelegraph(Boss b, double remaining) {
        try {
            Field f = Boss.class.getDeclaredField("telegraphRemaining");
            f.setAccessible(true);
            f.setDouble(b, remaining);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // field renamed: telegraph preview simply stays off
        }
    }

    private static void capture(GCanvas canvas, File file) {
        try {
            file.getParentFile().mkdirs();
            BufferedImage img = null;
            try {
                java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(canvas);
                if (w != null) w.toFront();
                java.awt.Point p = canvas.getLocationOnScreen();
                java.awt.Rectangle rect = new java.awt.Rectangle(p, canvas.getSize());
                java.awt.Image best = null;
                for (java.awt.Image v : new java.awt.Robot().createMultiResolutionScreenCapture(rect).getResolutionVariants()) {
                    if (best == null || v.getWidth(null) > best.getWidth(null)) best = v;
                }
                if (best != null) {
                    img = new BufferedImage(best.getWidth(null), best.getHeight(null), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = img.createGraphics();
                    g.drawImage(best, 0, 0, null);
                    g.dispose();
                }
            } catch (Exception robotFailed) {
                System.err.println("robot capture failed, painting offscreen instead: " + robotFailed);
            }
            if (img == null) {
                img = new BufferedImage(Math.max(1, canvas.getWidth()), Math.max(1, canvas.getHeight()), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                canvas.paint(g);
                g.dispose();
            }
            ImageIO.write(img, "png", file);
            System.out.println("wrote " + file.getAbsolutePath());
        } catch (Exception ex) {
            System.err.println("capture failed: " + ex);
        }
    }
}
