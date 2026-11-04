package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import lindefense.enemy.Boss;
import lindefense.sim.Game;

/**
 * Boss health dock shown directly under the top HUD, in space reserved by shrinking the playfield (so it never covers the
 * track or towers). The primary boss gets a large bar (phase notches and phase name for Palantir), further bosses compact
 * rows (max 3 rows, then "+N more"). Bars carry a trailing "recent damage" chunk; numbers are abbreviated, monospaced and
 * right-anchored so they never jitter. Built when the boss set changes, only mutated per frame.
 */
public final class BossDock extends GCompound {
    private static final long serialVersionUID = 1L;

    private static final double PRIMARY_H = 58, ROW_H = 24, MORE_H = 16, PAD_TOP = 3, PAD_BOTTOM = 7;
    private static final double LEFT = 12, RIGHT = Ui.WORLD_W - 12;
    private static final Color PALANTIR_FILL = new Color(0xff2b5c);

    /** One boss row. */
    private final class Row {
        final Boss boss;
        final boolean primary;
        final Cyber.Bar bar;
        final GLabel numbers, pct, phase, alert;
        final double rightEdge;
        String lastNums = "", lastPct = "", lastPhase = "";

        Row(GCompound host, Boss b, boolean primary, double y) {
            this.boss = b; this.primary = primary;
            Color accent = b.isPalantir() ? Cyber.MAGENTA : Cyber.RED;
            Font nameF = Cyber.display(Font.BOLD, primary ? 20 : 14);
            Font numF = Cyber.mono(Font.BOLD, primary ? 15 : 12);
            double base = primary ? y + 20 : y + 15;
            GLabel name = Ui.label(b.getType().displayName.toUpperCase(), LEFT + 10, base, nameF, Cyber.TEXT);
            host.add(name);
            String cls = b.getType().getBossClass().label;
            GLabel badgeL = Ui.label(cls, 0, 0, Cyber.mono(Font.BOLD, primary ? 11 : 10), accent);
            double bw = badgeL.getWidth() + 14, bh = primary ? 17 : 15;
            double bx = name.getX() + name.getWidth() + 10;
            Cyber.Chamfer badge = new Cyber.Chamfer(bx, base - (primary ? 14 : 12), bw, bh, 4).scan(false).fill(Cyber.alpha(accent, 40)).border(accent);
            host.add(badge);
            badgeL.setLocation(bx + 7, base - (primary ? 14 : 12) + bh - (primary ? 4.5 : 3.5));
            host.add(badgeL);
            double afterBadge = bx + bw + 12;
            phase = Ui.label("", afterBadge, base, Cyber.mono(Font.BOLD, primary ? 12 : 10), Cyber.alpha(accent, 235));
            host.add(phase);
            alert = Ui.label("", 0, base, Cyber.mono(Font.BOLD, 12), Cyber.RED);
            host.add(alert);
            rightEdge = RIGHT - 10;
            pct = Ui.label("100%", rightEdge, base, numF, Cyber.TEXT);
            pct.setLocation(rightEdge - pct.getWidth(), base);
            numbers = Ui.label("", 0, base, numF, Cyber.MUTED);
            host.add(pct); host.add(numbers);
            if (primary) {
                bar = new Cyber.Bar(LEFT + 10, y + 28, RIGHT - LEFT - 20, 18, 6);
                if (b.isPalantir()) bar.notches(0.70, 0.35);
            } else {
                double bxs = 330;
                bar = new Cyber.Bar(bxs, y + 5, rightEdge - 170 - bxs, 11, 4);
            }
            bar.colors(b.isPalantir() ? PALANTIR_FILL : Cyber.RED, new Color(0, 0, 0, 205), Cyber.alpha(accent, 190)).trail(true, new Color(255, 232, 196, 215));
            host.add(bar);
            update(0, true);
        }

        void update(double dt, boolean force) {
            double f = boss.getHpFraction();
            bar.setValue(f);
            bar.step(dt);
            String p = (int) Math.ceil(f * 100) + "%";
            if (force || !p.equals(lastPct)) {
                lastPct = p;
                pct.setLabel(p);
                pct.setLocation(rightEdge - pct.getWidth(), pct.getY());
            }
            String n = Cyber.abbrev(boss.getHp()) + " / " + Cyber.abbrev(boss.getMaxHp());
            if (force || !n.equals(lastNums)) {
                lastNums = n;
                numbers.setLabel(n);
                numbers.setLocation(pct.getX() - 14 - numbers.getWidth(), numbers.getY());
            }
            String ph = "";
            if (boss.isPalantir()) ph = "PHASE " + (boss.getPhase() + 1) + "/3 // " + boss.getPhaseName().toUpperCase();
            else {
                if (boss.isFortified()) ph += "FORTIFIED ";
                if (boss.isCamo()) ph += "CAMO";
            }
            if (force || !ph.equals(lastPhase)) { lastPhase = ph; phase.setLabel(ph); }
            if (primary && boss.isPalantir() && boss.isTelegraphing()) {
                String t = String.format("PREDICTIVE SURVEILLANCE %.1fs", boss.getTelegraphRemaining());
                Cyber.setText(alert, t);
                alert.setLocation(numbers.getX() - 24 - alert.getWidth(), alert.getY());
                boolean flash = (time * 8) % 2 < 1;
                Cyber.setColor(alert, flash ? Cyber.RED : Color.WHITE);
                bar.setFill(flash ? PALANTIR_FILL : new Color(0xffd0d8));
            } else {
                if (!alert.getLabel().isEmpty()) alert.setLabel("");
                bar.setFill(boss.isPalantir() ? PALANTIR_FILL : Cyber.RED);
            }
        }
    }

    private final Game game;
    private final List<Boss> shown = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final GCompound content = new GCompound();
    private int shownHidden = -1;
    private double height;
    private double time;

    public BossDock(Game game) {
        this.game = game;
        setLocation(0, Ui.WORLD_Y);
        add(content);
    }

    /** Pixel height the dock currently needs (0 when no boss is alive). */
    public double getHeight() { return height; }

    public void update(double dt) {
        time += dt;
        BossDockModel.Layout lay = BossDockModel.layout(game.getActiveBosses());
        List<Boss> now = new ArrayList<>();
        if (lay.primary != null) now.add(lay.primary);
        now.addAll(lay.compact);
        if (!now.equals(shown) || lay.hidden != shownHidden) rebuild(lay, now);
        for (Row r : rows) r.update(dt, false);
    }

    private void rebuild(BossDockModel.Layout lay, List<Boss> now) {
        content.removeAll();
        rows.clear();
        shown.clear();
        shown.addAll(now);
        shownHidden = lay.hidden;
        if (lay.primary == null) { height = 0; return; }
        double h = PAD_TOP + PRIMARY_H + lay.compact.size() * ROW_H + (lay.hidden > 0 ? MORE_H : 0) + PAD_BOTTOM;
        boolean pal = lay.primary.isPalantir();
        Color accent = pal ? Cyber.MAGENTA : Cyber.RED;
        Cyber.Chamfer back = new Cyber.Chamfer(LEFT - 2, PAD_TOP, RIGHT - LEFT + 4, h - PAD_TOP - 3, 12).fill(new Color(8, 6, 16, 230)).border(Cyber.alpha(accent, 200)).glow(0.9).ticks(true);
        content.add(back);
        double y = PAD_TOP + 4;
        rows.add(new Row(content, lay.primary, true, y));
        y += PRIMARY_H - 4;
        for (Boss b : lay.compact) { rows.add(new Row(content, b, false, y)); y += ROW_H; }
        if (lay.hidden > 0) {
            GLabel more = Ui.label("+" + lay.hidden + " MORE", 0, y + 11, Cyber.mono(Font.BOLD, 11), Cyber.MUTED);
            more.setLocation(RIGHT - 10 - more.getWidth(), y + 11);
            content.add(more);
        }
        height = h;
    }
}
