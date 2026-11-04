package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GLine;
import acm.graphics.GPolygon;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import lindefense.map.GameMap;
import lindefense.map.MapCatalog;
import lindefense.map.Path;
import lindefense.sim.Vec;

/**
 * Map selection: a 4-column grid of cards (as many rows as
 * {@link MapCatalog#MAP_COUNT} needs) with a miniature track preview, name,
 * colour-coded difficulty badge, description and mechanic summary, styled to
 * match the cyberpunk title card. All geometry is in logical coordinates so
 * hit-testing stays correct at any window size.
 */
public final class MapSelectScreen extends GCompound {
    private static final long serialVersionUID = 1L;
    private static final int COLS = 4;

    private final List<NeonButton> buttons = new ArrayList<>();
    private final LogoEmblem mark;
    private double t;

    public MapSelectScreen(LindefenseProgram app) {
        add(Ui.rect(0, 0, Ui.W, Ui.H, MenuScreen.BG, MenuScreen.BG));
        for (int y = 0; y < Ui.H; y += 4) { GLine l = new GLine(0, y, Ui.W, y); l.setColor(Ui.withAlpha(Color.BLACK, 40)); add(l); }
        GLabel title = Ui.label("SELECT A MAP", 0, 0, MenuScreen.menuFont(Font.BOLD, 34), MenuScreen.CYAN);
        title.setLocation((Ui.W - title.getWidth()) / 2 + 30, 64);
        add(title);
        mark = new LogoEmblem(52);
        mark.setLocation((Ui.W - title.getWidth()) / 2 - 18, 52);
        add(mark);
        LogoWordmark small = new LogoWordmark(22, new Color(0xeafcff), MenuScreen.CYAN, true);
        small.setLocation(Ui.W - 190, 46);
        add(small);
        GLabel sub = Ui.label(spaced("TWO MAPS PER DIFFICULTY TIER  -  PICK YOUR BATTLEFIELD"), 0, 0, MenuScreen.menuFont(Font.PLAIN, 12), Ui.withAlpha(MenuScreen.MAGENTA, 200));
        sub.setLocation((Ui.W - sub.getWidth()) / 2, 88);
        add(sub);

        List<GameMap> maps = MapCatalog.createAll();
        int n = maps.size();
        int rows = Math.max(1, (n + COLS - 1) / COLS);
        double top = 108, bottom = Ui.H - 28, gap = 18;
        double cardW = (Ui.W - 50 - gap * (COLS - 1)) / COLS;
        double cardH = (bottom - top - gap * (rows - 1)) / rows;
        for (int i = 0; i < n; i++) {
            GameMap m = maps.get(i);
            int col = i % COLS, row = i / COLS;
            buildCard(app, m, i, 25 + col * (cardW + gap), top + row * (cardH + gap), cardW, cardH);
        }
        NeonButton back = new NeonButton(25, 28, 130, 42, "BACK", MenuScreen.menuFont(Font.BOLD, 18), MenuScreen.CYAN, app::showMenu);
        buttons.add(back);
        add(back);
    }

    private static String spaced(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) { sb.append(c); sb.append(' '); }
        return sb.toString().trim();
    }

    private static Color difficultyColor(String d) {
        switch (d) {
            case "Beginner": return new Color(0x3bff8a);
            case "Intermediate": return MenuScreen.YELLOW;
            case "Advanced": return new Color(0xff8c3b);
            default: return MenuScreen.MAGENTA;
        }
    }

    private void buildCard(LindefenseProgram app, GameMap m, int idx, double x, double y, double w, double h) {
        Color dc = difficultyColor(m.getDifficulty());
        GPolygon frame = cut(x, y, w, h, 16);
        frame.setFilled(true);
        frame.setFillColor(new Color(0x0a0e1e));
        frame.setColor(Ui.withAlpha(MenuScreen.CYAN, 150));
        add(frame);
        GPolygon inner = cut(x + 4, y + 4, w - 8, h - 8, 13);
        inner.setColor(Ui.withAlpha(dc, 70));
        add(inner);
        // preview thumbnail (uniform scale, centred)
        double ph = Math.min(230, h * 0.42);
        double s = Math.min((w - 24) / GameMap.WIDTH, ph / GameMap.HEIGHT);
        double pw = GameMap.WIDTH * s;
        ph = GameMap.HEIGHT * s;
        double px = x + (w - pw) / 2, py = y + 14;
        add(Ui.rect(px, py, pw, ph, Ui.c(m.getBackgroundRgb()), Ui.withAlpha(MenuScreen.CYAN, 90)));
        for (GameMap.Decoration d : m.getDecorations()) {
            if (d.kind == GameMap.Decoration.Kind.RECT) add(Ui.rect(px + d.x * s, py + d.y * s, d.w * s, d.h * s, d.filled ? Ui.c(d.rgb) : null, Ui.c(d.rgb)));
            if (d.kind == GameMap.Decoration.Kind.OVAL) add(Ui.oval(px + d.x * s, py + d.y * s, d.w * s, d.h * s, d.filled ? Ui.c(d.rgb) : null, Ui.c(d.rgb)));
        }
        Color track = Ui.c(m.getTrackRgb());
        for (Path r : m.getRoutes()) {
            List<Vec> wp = r.getWaypoints();
            for (int k = 0; k + 1 < wp.size(); k++) {
                for (int o = -1; o <= 1; o++) {
                    GLine l = new GLine(px + wp.get(k).x * s + o, py + wp.get(k).y * s, px + wp.get(k + 1).x * s + o, py + wp.get(k + 1).y * s);
                    l.setColor(track);
                    add(l);
                }
            }
            add(Ui.oval(px + r.getStart().x * s - 3, py + r.getStart().y * s - 3, 6, 6, Ui.GREEN, Ui.GREEN));
            add(Ui.oval(px + r.getEnd().x * s - 3, py + r.getEnd().y * s - 3, 6, 6, Ui.RED, Ui.RED));
        }
        // text block
        double tx = x + 16, ty = py + ph + 24, tw = w - 32;
        boolean compact = h < 520;
        Font nameF = MenuScreen.menuFont(Font.BOLD, compact ? 19 : 24);
        Font bodyF = new Font(Font.SANS_SERIF, Font.PLAIN, compact ? 12 : 14);
        Font tinyF = new Font(Font.SANS_SERIF, Font.PLAIN, compact ? 11 : 12);
        add(Ui.label(m.getName().toUpperCase(), tx, ty, nameF, Ui.TEXT));
        ty += compact ? 18 : 24;
        // difficulty badge
        String badge = m.getDifficulty().toUpperCase() + "  •  " + m.getRoutes().size() + (m.getRoutes().size() == 1 ? " LANE" : " LANES");
        GLabel bl = Ui.label(badge, tx + 8, ty, new Font(Font.MONOSPACED, Font.BOLD, compact ? 11 : 13), dc);
        add(Ui.rect(tx, ty - (compact ? 12 : 14), bl.getWidth() + 16, compact ? 17 : 20, Ui.withAlpha(dc, 35), Ui.withAlpha(dc, 160)));
        add(bl);
        ty += compact ? 10 : 14;
        double buttonY = y + h - (compact ? 54 : 70);
        double buttonH = compact ? 40 : 50;
        double textBottom = buttonY - 10;
        // description then mechanics, trimmed to fit
        List<String> desc = Ui.wrap(m.getDescription(), bodyF, tw);
        List<String> mech = Ui.wrap(m.getMechanicSummary(), tinyF, tw);
        double lineB = bodyF.getSize() + 4, lineT = tinyF.getSize() + 3;
        double need = desc.size() * lineB + 16 + mech.size() * lineT;
        double avail = textBottom - ty;
        while (need > avail && mech.size() > 1) { mech.remove(mech.size() - 1); mech.set(mech.size() - 1, ellipsis(mech.get(mech.size() - 1), tinyF, tw)); need -= lineT; }
        while (need > avail && desc.size() > 1) { desc.remove(desc.size() - 1); desc.set(desc.size() - 1, ellipsis(desc.get(desc.size() - 1), bodyF, tw)); need -= lineB; }
        for (String l : desc) { ty += lineB; add(Ui.label(l, tx, ty, bodyF, Ui.TEXT)); }
        ty += 16;
        add(Ui.label("MECHANICS", tx, ty, new Font(Font.MONOSPACED, Font.BOLD, compact ? 10 : 12), MenuScreen.CYAN));
        for (String l : mech) { ty += lineT; add(Ui.label(l, tx, ty, tinyF, Ui.MUTED)); }
        NeonButton play = new NeonButton(x + 16, buttonY, w - 32, buttonH, "DEPLOY", MenuScreen.menuFont(Font.BOLD, compact ? 17 : 20), dc, () -> app.startGame(idx));
        buttons.add(play);
        add(play);
    }

    private static String ellipsis(String line, Font f, double maxW) {
        GLabel probe = new GLabel(line + "…");
        probe.setFont(f);
        String s = line;
        while (probe.getWidth() > maxW && s.length() > 1) { s = s.substring(0, s.length() - 1).trim(); probe.setLabel(s + "…"); }
        return s + "…";
    }

    private static GPolygon cut(double x, double y, double w, double h, double c) {
        GPolygon p = new GPolygon();
        p.addVertex(x + c, y); p.addVertex(x + w, y); p.addVertex(x + w, y + h - c);
        p.addVertex(x + w - c, y + h); p.addVertex(x, y + h); p.addVertex(x, y + c);
        return p;
    }

    public void mousePressed(double lx, double ly) {
        for (NeonButton b : buttons) if (b.hit(lx, ly)) { b.click(); return; }
    }

    public void mouseMoved(double lx, double ly) {
        for (NeonButton b : buttons) b.setHover(b.hit(lx, ly));
    }

    public void frame(double dt) {
        t += dt;
        mark.animate(t);
        for (NeonButton b : buttons) b.animate(dt);
    }
}
