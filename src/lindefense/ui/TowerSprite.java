package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GObject;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.awt.Color;
import lindefense.enemy.Enemy;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Faction;
import lindefense.tower.Tower;
import lindefense.tower.TowerType;

/**
 * Visual model of a placed tower. Each faction has a distinct silhouette and
 * every progression tier adds visible hardware (rings, barrels, crown, glow),
 * so upgrades are apparent at a glance. The turret rotates toward the last
 * target; objects are built once per tier and mutated during {@link #sync}.
 */
public final class TowerSprite extends GCompound {
    private static final long serialVersionUID = 1L;

    private final Tower tower;
    private int builtTier = -1;
    private GPolygon turret;
    private double turretAngle;
    private GOval glow;
    private GOval surveilRing;
    private GLabel mark;
    private GOval abilityRing;
    private GRect cdBack, cdBar;
    private double pulse;

    public TowerSprite(Tower t) {
        this.tower = t;
        setLocation(t.getPosition().x, t.getPosition().y);
        rebuild();
    }

    public Tower getTower() { return tower; }

    private void rebuild() {
        removeAll();
        builtTier = tower.getTier();
        TowerType type = tower.getType();
        Color pri = Ui.c(type.primaryRgb);
        Color sec = Ui.c(type.secondaryRgb);
        double r = tower.getRadius();
        int tier = tower.getTier();
        boolean temp = tower.isTemporary();

        // Tier 3 glow
        glow = Ui.oval(-r * 1.7, -r * 1.7, r * 3.4, r * 3.4, Ui.withAlpha(sec, 50), Ui.withAlpha(sec, 90));
        glow.setVisible(tier >= 4);
        add(glow);
        // Tier 1 outer ring, Tier 2 double ring
        if (tier >= 1) add(Ui.oval(-r * 1.25, -r * 1.25, r * 2.5, r * 2.5, null, Ui.withAlpha(sec, 200)));
        if (tier >= 2) add(Ui.oval(-r * 1.4, -r * 1.4, r * 2.8, r * 2.8, null, Ui.withAlpha(pri, 160)));
        // Base plate
        add(Ui.oval(-r, -r, r * 2, r * 2, Ui.c(0x0b0e14, temp ? 120 : 220), Ui.c(0x3b4556)));
        // Faction silhouette
        GObject body = silhouette(type.faction, r * 0.82, pri, sec);
        add(body);
        // Tier 2 side barrels
        if (tier >= 2) {
            GRect l = Ui.rect(-r * 1.05, -r * 0.25, r * 0.5, r * 0.5, sec, pri);
            GRect rr = Ui.rect(r * 0.55, -r * 0.25, r * 0.5, r * 0.5, sec, pri);
            add(l); add(rr);
        }
        // Turret
        turret = new GPolygon(0, 0);
        double tl = r * (1.1 + 0.25 * tier);
        turret.addVertex(-r * 0.22, 0);
        turret.addVertex(r * 0.22, 0);
        turret.addVertex(r * 0.16, -tl);
        turret.addVertex(-r * 0.16, -tl);
        turret.setFilled(true);
        turret.setFillColor(tier >= 4 ? Ui.GOLD : sec);
        turret.setColor(Ui.c(0x0b0e14));
        turret.rotate(-Math.toDegrees(turretAngle) - 90);
        add(turret);
        // Core
        add(Ui.oval(-r * 0.3, -r * 0.3, r * 0.6, r * 0.6, tier >= 4 ? Ui.GOLD : sec, Ui.c(0x0b0e14)));
        // Superclass crown
        if (tier >= 4) {
            GPolygon crown = new GPolygon(0, -r * 1.05);
            crown.addVertex(-r * 0.55, 0); crown.addVertex(-r * 0.3, -r * 0.45); crown.addVertex(0, -r * 0.1);
            crown.addVertex(r * 0.3, -r * 0.45); crown.addVertex(r * 0.55, 0);
            crown.setFilled(true); crown.setFillColor(Ui.GOLD); crown.setColor(Ui.c(0x7a5a00));
            add(crown);
        }
        // Tier pips
        for (int i = 0; i < 4; i++) {
            GRect pip = Ui.rect(-r * 0.85 + i * r * 0.45, r * 1.1, r * 0.34, r * 0.22,
                i < tier ? Ui.GREEN : Ui.c(0x2a3140), Ui.c(0x0b0e14));
            add(pip);
        }
        // Hero star badge
        if (type.isHero()) {
            GPolygon star = new GPolygon(r * 0.9, -r * 0.9);
            for (int i = 0; i < 10; i++) {
                double ang = -Math.PI / 2 + i * Math.PI / 5;
                double rad = (i % 2 == 0) ? r * 0.42 : r * 0.18;
                star.addVertex(Math.cos(ang) * rad, Math.sin(ang) * rad);
            }
            star.setFilled(true); star.setFillColor(Ui.GOLD); star.setColor(Ui.c(0x7a5a00));
            add(star);
        }
        // Ability-ready ring + cooldown bar
        abilityRing = Ui.oval(-r * 1.15, -r * 1.15, r * 2.3, r * 2.3, null, Ui.withAlpha(Ui.GOLD, 220));
        abilityRing.setVisible(false);
        add(abilityRing);
        cdBack = Ui.rect(-r, -r * 1.55, r * 2, 4, Ui.c(0x0b0e14, 200), null);
        cdBar = Ui.rect(-r, -r * 1.55, r * 2, 4, Ui.GOLD, null);
        cdBack.setVisible(false); cdBar.setVisible(false);
        add(cdBack); add(cdBar);
        // Surveillance ring + mark
        surveilRing = Ui.oval(-r * 1.6, -r * 1.6, r * 3.2, r * 3.2, Ui.c(0xff2020, 50), Ui.c(0xff4040));
        surveilRing.setVisible(false);
        add(surveilRing);
        mark = Ui.label("!", -4, -r * 1.7, Ui.F_H2, Ui.RED);
        mark.setVisible(false);
        add(mark);
        if (!temp) {
            GLabel name = Ui.label(type.displayName, 0, r * 1.95, Ui.F_TINY, Ui.withAlpha(Ui.TEXT, 190));
            name.setLocation(-name.getWidth() / 2, r * 1.95);
            add(name);
        }
    }

    private GObject silhouette(Faction f, double r, Color pri, Color sec) {
        GPolygon p = new GPolygon(0, 0);
        int n;
        switch (f) {
            case HERO: {
                for (int i = 0; i < 10; i++) {
                    double ang = -Math.PI / 2 + i * Math.PI / 5;
                    double rad = (i % 2 == 0) ? r : r * 0.5;
                    p.addVertex(Math.cos(ang) * rad, Math.sin(ang) * rad);
                }
                break;
            }
            case PRIMARY: {
                GOval o = Ui.oval(-r, -r, 2 * r, 2 * r, pri, sec);
                return o;
            }
            case MILITARY: n = 4; poly(p, n, r, Math.PI / 4); break;
            case MAGIC: n = 4; poly(p, n, r, 0); break;
            case SUPPORT: n = 6; poly(p, n, r, 0); break;
            case TITAN: default: n = 8; poly(p, n, r, Math.PI / 8); break;
        }
        p.setFilled(true);
        p.setFillColor(pri);
        p.setColor(sec);
        return p;
    }

    private static void poly(GPolygon p, int n, double r, double start) {
        for (int i = 0; i < n; i++) {
            double ang = start + i * 2 * Math.PI / n;
            p.addVertex(Math.cos(ang) * r, Math.sin(ang) * r);
        }
    }

    /** Per-frame update. */
    public void sync(Game game, double dt, boolean telegraphed) {
        if (tower.getTier() != builtTier) rebuild();
        pulse += dt;
        Enemy target = tower.getLastTarget();
        if (target != null && target.isAlive() && game.getTime() - tower.getLastFireTime() < 1.5) {
            Vec d = target.getPosition().sub(tower.getPosition());
            double want = Math.atan2(d.y, d.x);
            double delta = want - turretAngle;
            while (delta > Math.PI) delta -= 2 * Math.PI;
            while (delta < -Math.PI) delta += 2 * Math.PI;
            if (Math.abs(delta) > 0.001) {
                turret.rotate(-Math.toDegrees(delta));
                turretAngle = want;
            }
        }
        if (glow.isVisible()) {
            double a = 40 + 30 * Math.sin(pulse * 3);
            glow.setFillColor(Ui.c(tower.getType().secondaryRgb, (int) a));
        }
        boolean ready = tower.getAbility() != null && tower.isAbilityReady() && !tower.isTemporary();
        abilityRing.setVisible(ready && (pulse % 1.0) < 0.6);
        boolean cooling = tower.getAbility() != null && !tower.isTemporary() && tower.getAbilityCooldownRemaining() > 0;
        cdBack.setVisible(cooling);
        cdBar.setVisible(cooling);
        if (cooling) {
            double frac = 1 - tower.getAbilityCooldownFraction();
            cdBar.setSize(Math.max(0.5, tower.getRadius() * 2 * frac), 4);
        }
        boolean surveilled = tower.isSurveilled();
        surveilRing.setVisible(surveilled || telegraphed);
        if (surveilled) surveilRing.setFillColor(Ui.c(0xff2020, (int) (60 + 50 * Math.sin(pulse * 8))));
        else if (telegraphed) surveilRing.setFillColor(Ui.c(0xff8000, (int) (40 + 60 * Math.abs(Math.sin(pulse * 10)))));
        mark.setVisible(telegraphed || surveilled);
        mark.setLabel(surveilled ? "SURVEILLED" : "TARGETED");
        if (telegraphed || surveilled) mark.setLocation(-mark.getWidth() / 2, -tower.getRadius() * 1.75);
    }
}
