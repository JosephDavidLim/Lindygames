package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Tower;
import lindefense.tower.TowerType;
import lindefense.ui.sprites.towers.Art;
import lindefense.ui.sprites.towers.TowerArt;
import lindefense.ui.sprites.towers.TowerArtFactory;

/**
 * Visual model of a placed tower. The cartoon body is delegated to a
 * {@link TowerArt} from {@code lindefense.ui.sprites.towers} (one class per
 * tower type, four game tiers mapped to art stages 0, 2, 3, 4) and is rebuilt only when the tier changes.
 * This class owns the functional overlays: turret aiming at the last target,
 * firing recoil and muzzle flash, the radial ability cooldown ring (with a ready pulse),
 * the Palantir surveillance ring with its TARGETED / SURVEILLED label, the hero
 * badge and the three tier pips (no always-on name label; WorldView draws a
 * hover / selection nameplate). Between rebuilds only object state is mutated.
 */
@SuppressWarnings({ "serial", "this-escape" })
public final class TowerSprite extends GCompound {
    private static final long serialVersionUID = 1L;

    private static final double SLEW_RATE = 9.0; // rad/s turret turn speed
    private static final Color PIP_OFF = new Color(0x2a3140);

    private final Tower tower;
    private int builtTier = -1;
    private TowerArt art;
    private double aimAngle = TowerArt.IDLE_ANGLE;
    private RadialCooldown cdRing;
    private GOval surveilRing;
    private GLabel mark;
    private double pulse;
    private double clock;

    /** Creates the sprite for a tower and positions it at the tower's world position. */
    public TowerSprite(Tower t) {
        this.tower = t;
        setLocation(t.getPosition().x, t.getPosition().y);
        rebuild();
    }

    /** The tower this sprite draws. */
    public Tower getTower() { return tower; }

    private void rebuild() {
        removeAll();
        builtTier = clampTier(tower.getTier());
        TowerType type = tower.getType();
        double r = tower.getRadius();
        boolean temp = tower.isTemporary();

        art = TowerArtFactory.create(type, TowerArtFactory.artStage(builtTier), r);
        art.aim(aimAngle);
        add(art);

        // tier pips (3, one per upgrade) under the plate
        if (!temp) {
            double pw = r * 0.4, ph = r * 0.18, gap = r * 0.12;
            double x0 = -(3 * pw + 2 * gap) / 2;
            for (int i = 0; i < 3; i++) {
                Color fill = i < builtTier ? (builtTier >= 3 ? Art.GOLD : Ui.GREEN) : PIP_OFF;
                add(Ui.rect(x0 + i * (pw + gap), r * 1.22, pw, ph, fill, Art.INK));
            }
        }
        // hero star badge
        if (type.isHero()) {
            GPolygon star = new GPolygon();
            for (int i = 0; i < 10; i++) {
                double ang = -Math.PI / 2 + i * Math.PI / 5;
                double rad = (i % 2 == 0) ? r * 0.38 : r * 0.16;
                star.addVertex(r * 1.0 + Math.cos(ang) * rad, -r * 1.0 + Math.sin(ang) * rad);
            }
            star.setFilled(true); star.setFillColor(Art.GOLD); star.setColor(Art.GOLD_DARK);
            add(star);
        }
        // radial ability cooldown (fills clockwise; pulses when ready)
        cdRing = new RadialCooldown(r * 1.32, 3);
        cdRing.setVisible(false);
        add(cdRing);
        // surveillance ring + mark
        surveilRing = Ui.oval(-r * 1.7, -r * 1.7, r * 3.4, r * 3.4, Ui.c(0xff2020, 50), Ui.c(0xff4040));
        surveilRing.setVisible(false);
        add(surveilRing);
        mark = Ui.label("!", -4, -r * 1.8, Ui.F_H2, Ui.RED);
        mark.setVisible(false);
        add(mark);
        // No always-on name label: WorldView shows a hover / selection nameplate instead.
    }

    private static int clampTier(int t) { return Math.max(0, Math.min(3, t)); }

    /** Sets visibility only when it changes (every ACM setter walks up to the canvas to request a repaint). */
    private static void show(acm.graphics.GObject o, boolean visible) { if (o.isVisible() != visible) o.setVisible(visible); }

    /**
     * Per-frame update.
     *
     * @param game        the simulation (may be null in galleries; an internal clock is used)
     * @param dt          seconds since the previous frame
     * @param telegraphed whether Palantir is about to target this tower
     */
    public void sync(Game game, double dt, boolean telegraphed) {
        if (clampTier(tower.getTier()) != builtTier) rebuild();
        pulse += dt;
        clock += dt;
        double now = game != null ? game.getTime() : clock;
        double r = tower.getRadius();

        // aim at the last target while recently firing
        Enemy target = tower.getLastTarget();
        double sinceFire = now - tower.getLastFireTime();
        if (target != null && target.isAlive() && sinceFire < 1.5 && art.hasTurret()) {
            Vec d = target.getPosition().sub(tower.getPosition());
            if (d.length() > 0.5) {
                double want = Math.atan2(d.y, d.x);
                double delta = want - aimAngle;
                while (delta > Math.PI) delta -= 2 * Math.PI;
                while (delta < -Math.PI) delta += 2 * Math.PI;
                double maxStep = SLEW_RATE * Math.max(dt, 1e-3);
                if (Math.abs(delta) > maxStep) delta = Math.signum(delta) * maxStep;
                aimAngle += delta;
                while (aimAngle > Math.PI) aimAngle -= 2 * Math.PI;
                while (aimAngle < -Math.PI) aimAngle += 2 * Math.PI;
                art.aim(aimAngle);
            }
        }
        art.setFiring(sinceFire);
        art.animate(now, dt);

        boolean has = tower.getAbility() != null && !tower.isTemporary();
        show(cdRing, has);
        if (has) {
            cdRing.set(1 - tower.getAbilityCooldownFraction(), tower.isAbilityReady(), tower.isSurveilled(), Ui.c(tower.getType().primaryRgb).brighter(), pulse);
        }
        boolean surveilled = tower.isSurveilled();
        show(surveilRing, surveilled || telegraphed);
        if (surveilled) surveilRing.setFillColor(Ui.c(0xff2020, (int) (60 + 50 * Math.sin(pulse * 8))));
        else if (telegraphed) surveilRing.setFillColor(Ui.c(0xff8000, (int) (40 + 60 * Math.abs(Math.sin(pulse * 10)))));
        boolean showMark = telegraphed || surveilled;
        if (mark.isVisible() != showMark) mark.setVisible(showMark);
        if (showMark) {
            String want = surveilled ? "SURVEILLED" : "TARGETED";
            if (!want.equals(mark.getLabel())) mark.setLabel(want);
            mark.setLocation(-mark.getWidth() / 2, -r * 1.85);
        }
    }
}
