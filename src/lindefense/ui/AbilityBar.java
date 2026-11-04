package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import lindefense.sim.Game;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;

/**
 * Compact ability strip along the bottom of the playfield (above the status strip): one slot per placed tower that has
 * an ability, each with the tower icon, a radial cooldown ring and click-to-fire. Slots appear and disappear as towers are
 * placed and sold; when there are more than fit, the arrows page through them. Hovering a slot shows the ability name,
 * description and cooldown; targeted abilities are marked with a reticle glyph. A one-line hint above the bar shows
 * targeting prompts and refusal reasons. Rebuilt only when the set of towers (or the page) changes; per frame only the
 * rings and hint are mutated.
 */
public final class AbilityBar extends GCompound {
    private static final long serialVersionUID = 1L;
    public static final double HEIGHT = 54, SLOT = 46, PITCH = 50;
    private static final double LEFT = 10, LABEL_W = 78, ARROW_W = 24;
    private static final double Y = Ui.BOTTOM_Y - HEIGHT - 6;

    private final GameScreen screen;
    private final Game game;
    private final GCompound body = new GCompound();
    private final List<UIButton> buttons = new ArrayList<>();
    private final List<Slot> slots = new ArrayList<>();
    private final GCompound tip = new GCompound();
    private final Cyber.Chamfer tipFrame = new Cyber.Chamfer(0, 0, 300, 90, 8).scan(false).fill(new Color(5, 9, 22, 245)).glow(0.7);
    private final GLabel tipTitle = Ui.label("", 12, 22, Cyber.display(Font.BOLD, 18), Cyber.TEXT);
    private final GLabel tipMeta = Ui.label("", 12, 40, Cyber.mono(Font.BOLD, 10), Cyber.MAGENTA);
    private final List<GLabel> tipLines = new ArrayList<>();
    private final GCompound hintBox = new GCompound();
    private final Cyber.Chamfer hintFrame = new Cyber.Chamfer(0, 0, 300, 26, 6).scan(false).fill(new Color(5, 9, 22, 235));
    private final GLabel hintLabel = Ui.label("", 12, 18, Cyber.mono(Font.BOLD, 12), Cyber.CYAN);
    private String key = "";
    private int page;
    private int capacity = 20;
    private String hint = "";
    private Color hintColor = Cyber.CYAN;
    private double hintTime;
    private String tipShown = "";

    private static final class Slot {
        final Tower tower; final UIButton button; final RadialCooldown ring;
        Slot(Tower t, UIButton b, RadialCooldown r) { tower = t; button = b; ring = r; }
    }

    AbilityBar(GameScreen screen) {
        this.screen = screen;
        this.game = screen.getGame();
        add(body);
        tip.add(tipFrame); tip.add(tipTitle); tip.add(tipMeta);
        for (int i = 0; i < 3; i++) { GLabel l = Ui.label("", 12, 58 + i * 15, Cyber.body(Font.PLAIN, 12), Cyber.MUTED); tipLines.add(l); tip.add(l); }
        tip.setVisible(false);
        hintBox.add(hintFrame); hintBox.add(hintLabel);
        hintBox.setVisible(false);
        add(hintBox);
        add(tip);
    }

    List<UIButton> getButtons() { return buttons; }

    /** Shows a one-line message above the bar for {@code seconds} (targeting prompts, refusal reasons); 0 = until replaced. */
    void showHint(String text, Color color, double seconds) {
        hint = text; hintColor = color; hintTime = seconds <= 0 ? 1e9 : seconds;
    }

    void clearHint() { hint = ""; hintTime = 0; }

    private List<Tower> abilityTowers() {
        List<Tower> l = new ArrayList<>();
        for (Tower t : game.getTowerManager().getTowers()) if (t.isAlive() && !t.isTemporary() && t.getAbility() != null) l.add(t);
        return l;
    }

    private void rebuild(List<Tower> all) {
        body.removeAll();
        buttons.clear();
        slots.clear();
        int n = all.size();
        double avail = Ui.WORLD_W - 2 * LEFT - LABEL_W - 8;
        capacity = Math.max(4, (int) Math.floor(avail / PITCH));
        boolean paged = n > capacity;
        if (paged) capacity = Math.max(3, (int) Math.floor((avail - 2 * ARROW_W) / PITCH));
        int pages = Math.max(1, (n + capacity - 1) / capacity);
        page = Math.max(0, Math.min(pages - 1, page));
        if (n == 0) return;
        int from = page * capacity, to = Math.min(n, from + capacity);
        double w = LABEL_W + (to - from) * PITCH + (paged ? 2 * ARROW_W + 8 : 0) + 6;
        body.add(new Cyber.Chamfer(LEFT, Y, w, HEIGHT, 10).fill(new Color(5, 9, 22, 214)).border(Cyber.alpha(Cyber.MAGENTA, 150)).scan(false));
        body.add(Ui.label("ABILITIES", LEFT + 10, Y + 22, Cyber.mono(Font.BOLD, 10), Cyber.alpha(Cyber.MAGENTA, 230)));
        body.add(Ui.label(paged ? (page + 1) + " / " + pages : n + (n == 1 ? " TOWER" : " TOWERS"), LEFT + 10, Y + 38, Cyber.mono(Font.PLAIN, 10), Cyber.MUTED));
        double x = LEFT + LABEL_W;
        if (paged) {
            UIButton prev = new UIButton(x, Y + 11, ARROW_W, 32, "<", Cyber.display(Font.BOLD, 16), () -> { page = Math.max(0, page - 1); key = ""; });
            prev.setAccent(Cyber.MAGENTA);
            body.add(prev); buttons.add(prev);
            x += ARROW_W + 4;
        }
        for (int i = from; i < to; i++) {
            Tower t = all.get(i);
            UIButton b = new UIButton(x, Y + (HEIGHT - SLOT) / 2, SLOT, SLOT, "", () -> screen.fireAbility(t));
            b.setAccent(Ui.c(t.getType().primaryRgb));
            body.add(b);
            buttons.add(b);
            double cx = x + SLOT / 2, cy = Y + HEIGHT / 2;
            IconImage icon = new IconImage(t.getType(), t.getTier(), 30);
            icon.setLocation(cx, cy);
            body.add(icon);
            RadialCooldown ring = new RadialCooldown(SLOT / 2 - 3, 3);
            ring.setLocation(cx, cy);
            body.add(ring);
            if (t.getAbility().needsTarget) {   // reticle glyph: this ability asks for a map point
                body.add(Ui.label("+", x + SLOT - 11, Y + HEIGHT / 2 + SLOT / 2 - 5, Cyber.mono(Font.BOLD, 13), Cyber.YELLOW));
            }
            slots.add(new Slot(t, b, ring));
            x += PITCH;
        }
        if (paged) {
            UIButton next = new UIButton(x + 2, Y + 11, ARROW_W, 32, ">", Cyber.display(Font.BOLD, 16), () -> { page = Math.min(pages - 1, page + 1); key = ""; });
            next.setAccent(Cyber.MAGENTA);
            body.add(next); buttons.add(next);
        }
        screen.refreshHover();
    }

    void update(double dt) {
        List<Tower> all = abilityTowers();
        StringBuilder sb = new StringBuilder().append(page);
        for (Tower t : all) sb.append(',').append(t.getId()).append(':').append(t.getTier());
        String k = sb.toString();
        if (!k.equals(key)) { key = k; rebuild(all); }
        Tower sel = screen.getSelectedTower();
        Tower arm = screen.getTargetingTower();
        double pulse = game.getTime();
        for (Slot s : slots) {
            Tower t = s.tower;
            s.ring.set(1 - t.getAbilityCooldownFraction(), t.isAbilityReady(), t.isSurveilled(), Ui.c(t.getType().primaryRgb).brighter(), pulse);
            s.button.setSelected(t == sel || t == arm);
        }
        updateHint(dt);
        updateTip();
    }

    private void updateHint(double dt) {
        hintTime -= dt;
        String h = hintTime > 0 ? hint : "";
        Tower arm = screen.getTargetingTower();
        Color hc = hintColor;
        if (h.isEmpty() && arm != null) {
            h = "TARGETING  " + arm.getAbility().name.toUpperCase() + "  -  click the track to cast  -  right-click / Esc cancels";
            hc = Cyber.YELLOW;
        }
        if (h.isEmpty()) { hintBox.setVisible(false); return; }
        Cyber.setText(hintLabel, h);
        Cyber.setColor(hintLabel, hc);
        double w = hintLabel.getWidth() + 24;
        hintFrame.size(w, 26).border(hc);
        hintBox.setLocation(Ui.WORLD_W / 2.0 - w / 2, Y - 34);
        hintBox.setVisible(true);
    }

    private void updateTip() {
        double[] m = screen.getMouseLogical();
        Slot hover = null;
        for (Slot s : slots) if (s.button.hit(m[0], m[1])) { hover = s; break; }
        if (hover == null) { tip.setVisible(false); return; }
        Tower t = hover.tower;
        AbilityDef a = t.getAbility();
        String meta = (t.isSurveilled() ? "OFFLINE (SURVEILLED)" : t.isAbilityReady() ? "READY" : String.format("%.0fs LEFT", Math.ceil(t.getAbilityCooldownRemaining())))
            + "  //  CD " + (int) a.cooldown + "s" + (a.needsTarget ? "  //  TARGETED" : "");
        String id = t.getId() + meta;
        if (!id.equals(tipShown)) {
            tipShown = id;
            Cyber.setText(tipTitle, a.name + "   (" + t.getType().displayName + ")");
            Cyber.setText(tipMeta, meta);
            List<String> lines = Ui.wrap(a.description, Cyber.body(Font.PLAIN, 12), 330);
            for (int i = 0; i < tipLines.size(); i++) Cyber.setText(tipLines.get(i), i < lines.size() ? lines.get(i) : "");
            double w = Math.max(tipTitle.getWidth(), 340) + 24;
            tipFrame.size(w, 62 + Math.min(3, lines.size()) * 15 + 6).border(Ui.c(t.getType().primaryRgb));
        }
        double w = tipFrame.getW(), h = tipFrame.getH();
        tip.setLocation(Math.max(6, Math.min(Ui.WORLD_W - w - 6, hover.button.getX() + SLOT / 2 - w / 2)), Y - h - 8);
        tip.setVisible(true);
    }
}
