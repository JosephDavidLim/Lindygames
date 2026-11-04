package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * The on-screen card of the guided tutorial: step counter, title, instructions and NEXT / SKIP / END buttons, drawn
 * in the top-left corner of the playfield. The steps and their completion rules live in {@link TutorialScript}.
 */
final class TutorialGuide extends GCompound {
    private static final long serialVersionUID = 1L;
    static final double X = 12, Y = Ui.WORLD_Y + 10, W = 480;

    private final TutorialScript script;
    private final List<UIButton> buttons = new ArrayList<>();
    private int shownIndex = -1;
    private boolean shownFinished;
    private double h = 170;
    private double pulse;

    TutorialGuide(TutorialScript script) { this.script = script; rebuild(); }

    TutorialScript getScript() { return script; }
    List<UIButton> getButtons() { return script.isFinished() ? new ArrayList<>() : buttons; }

    /** True if the point is on the card (clicks there never reach the map). */
    boolean covers(double lx, double ly) { return !script.isFinished() && lx >= X && lx <= X + W && ly >= Y && ly <= Y + h; }

    void update(double dt) {
        pulse += dt;
        script.update();
        if (script.index() != shownIndex || script.isFinished() != shownFinished) rebuild();
        for (UIButton b : buttons) b.animate(dt);
    }

    private void rebuild() {
        removeAll();
        buttons.clear();
        shownIndex = script.index();
        shownFinished = script.isFinished();
        if (script.isFinished()) { setVisible(false); return; }
        setVisible(true);
        TutorialScript.Step s = script.current();
        Font tf = Cyber.body(Font.PLAIN, 14);
        List<String> lines = Ui.wrap(s.text, tf, W - 32);
        h = 92 + lines.size() * 19 + 46;
        add(new Cyber.Chamfer(X, Y, W, h, 14).fill(new Color(6, 10, 24, 238)).border(Cyber.YELLOW).glow(0.9).lineWidth(1.8));
        add(Ui.rect(X + 18, Y + 8, 110, 3, Cyber.MAGENTA, Cyber.MAGENTA));
        add(Ui.label(Cyber.spaced("tutorial  " + (script.index() + 1) + " / " + script.count()), X + 18, Y + 30, Cyber.mono(Font.BOLD, 11), Cyber.YELLOW));
        if (s.action) {
            GLabel goal = Ui.label("GOAL", 0, Y + 30, Cyber.mono(Font.BOLD, 11), Cyber.GREEN);
            goal.setLocation(X + W - 18 - goal.getWidth(), Y + 30);
            add(goal);
        }
        add(Ui.label(s.title, X + 18, Y + 62, Cyber.display(Font.BOLD, 24), Cyber.TEXT));
        double ly = Y + 70;
        for (String l : lines) { ly += 19; add(Ui.label(l, X + 18, ly, tf, Cyber.MUTED)); }
        double by = Y + h - 44;
        boolean last = script.index() == script.count() - 1;
        UIButton main = new UIButton(X + 18, by, 150, 32, last ? "FINISH" : s.action ? "SKIP STEP" : "NEXT", Cyber.display(Font.BOLD, 15), script::next);
        if (!s.action) main.setPrimary(true); else main.setAccent(Cyber.MUTED);
        add(main); buttons.add(main);
        if (!last) {
            UIButton end = new UIButton(X + W - 18 - 150, by, 150, 32, "END TUTORIAL", Cyber.display(Font.BOLD, 15), script::skipAll);
            end.setAccent(Cyber.RED);
            add(end); buttons.add(end);
        }
    }
}
