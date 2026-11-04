package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GLine;
import acm.graphics.GObject;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lindefense.combat.Projectile;
import lindefense.combat.VisualEvent;
import lindefense.enemy.Boss;
import lindefense.enemy.Enemy;
import lindefense.map.GameMap;
import lindefense.map.HallucinationCanyon;
import lindefense.map.LegacyMonolith;
import lindefense.map.Path;
import lindefense.map.SiliconCircuit;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.Tower;
import lindefense.tower.TowerCatalog;
import lindefense.tower.TowerType;
import lindefense.ui.sprites.Animated;
import lindefense.ui.sprites.EnemySprite;
import lindefense.ui.sprites.SpriteFactory;

/**
 * Renders the playfield: map background and track, towers, enemies (via the
 * sprite package), projectiles, transient effects, boss health bars, Palantir's
 * surveillance telegraph and the placement ghost / range circle. All children
 * live in world coordinates (1200x900); the compound sits at the world origin
 * inside the scaled root.
 */
public final class WorldView extends GCompound {
    private static final long serialVersionUID = 1L;
    private static final int MAX_EFFECTS = 160;

    private final GameScreen screen;
    private final Game game;
    private final GameMap map;

    private final GCompound background = new GCompound();
    private final GCompound mapFx = new GCompound();
    private final GCompound towerLayer = new GCompound();
    private final GCompound enemyLayer = new GCompound();
    private final GCompound hpLayer = new GCompound();
    private final GCompound projectileLayer = new GCompound();
    private final GCompound effectLayer = new GCompound();
    private final GCompound overlay = new GCompound();
    private final GCompound bossLayer = new GCompound();

    private final Map<Tower, TowerSprite> towerSprites = new HashMap<>();
    private final Map<Enemy, EnemySprite> enemySprites = new HashMap<>();
    private final Map<Enemy, GRect[]> hpBars = new HashMap<>();
    private final Map<Projectile, GOval> projectileDots = new HashMap<>();
    private final List<Effect> effects = new ArrayList<>();
    private final List<Animated> animated = new ArrayList<>();
    private final List<GObject> telegraphLines = new ArrayList<>();
    private final List<GObject> bossBarObjects = new ArrayList<>();
    private final List<Boss> bossBarOrder = new ArrayList<>();

    private GOval rangeCircle, ghost, ghostRange;
    private GLabel ghostLabel;
    private final List<GOval> padFill = new ArrayList<>();
    private GRect ventRect;
    private GRect stormRect;
    private final List<GObject> shortFx = new ArrayList<>();
    private double time;

    public WorldView(GameScreen screen) {
        this.screen = screen;
        this.game = screen.getGame();
        this.map = game.getMap();
        setLocation(Ui.WORLD_X, Ui.WORLD_Y);
        buildBackground();
        add(background);
        add(mapFx);
        add(towerLayer);
        add(enemyLayer);
        add(hpLayer);
        add(projectileLayer);
        add(effectLayer);
        add(overlay);
        add(bossLayer);
        buildOverlay();
    }

    // ------------------------------------------------------------------ static map
    private void buildBackground() {
        background.add(Ui.rect(0, 0, GameMap.WIDTH, GameMap.HEIGHT, Ui.c(map.getBackgroundRgb()), Ui.c(map.getBackgroundRgb())));
        for (GameMap.Decoration d : map.getDecorations()) {
            Color col = Ui.c(d.rgb);
            switch (d.kind) {
                case RECT: background.add(Ui.rect(d.x, d.y, d.w, d.h, d.filled ? col : null, col)); break;
                case OVAL: background.add(Ui.oval(d.x, d.y, d.w, d.h, d.filled ? col : null, col)); break;
                case LINE: { GLine l = new GLine(d.x, d.y, d.x + d.w, d.y + d.h); l.setColor(col); background.add(l); break; }
                case LABEL: background.add(Ui.label(d.text, d.x, d.y, Ui.F_SMALL, col)); break;
                default: break;
            }
        }
        Color track = Ui.c(map.getTrackRgb());
        Color edge = Ui.withAlpha(Ui.c(0x000000), 110);
        double hw = GameMap.TRACK_HALF_WIDTH;
        for (Path r : map.getRoutes()) {
            List<Vec> w = r.getWaypoints();
            for (int i = 0; i + 1 < w.size(); i++) background.add(segment(w.get(i), w.get(i + 1), hw + 3, edge));
            for (Vec p : w) background.add(Ui.oval(p.x - hw - 3, p.y - hw - 3, 2 * (hw + 3), 2 * (hw + 3), edge, edge));
            for (int i = 0; i + 1 < w.size(); i++) background.add(segment(w.get(i), w.get(i + 1), hw, track));
            for (Vec p : w) background.add(Ui.oval(p.x - hw, p.y - hw, 2 * hw, 2 * hw, track, track));
            Color center = Ui.withAlpha(Ui.c(0xffffff), 60);
            for (int i = 0; i + 1 < w.size(); i++) { GLine l = new GLine(w.get(i).x, w.get(i).y, w.get(i + 1).x, w.get(i + 1).y); l.setColor(center); background.add(l); }
            Vec s = r.getStart(), e = r.getEnd();
            background.add(Ui.oval(s.x - 16, s.y - 16, 32, 32, Ui.c(0x7fd8a8, 200), Ui.c(0xffffff)));
            GLabel in = Ui.label("IN", s.x - 8, s.y + 5, Ui.F_BOLD, Ui.c(0x0b0e14));
            background.add(in);
            background.add(Ui.oval(e.x - 16, e.y - 16, 32, 32, Ui.c(0xff5a5a, 200), Ui.c(0xffffff)));
            background.add(Ui.label("OUT", e.x - 14, e.y + 5, Ui.F_SMALL, Ui.c(0x0b0e14)));
        }
        if (map instanceof SiliconCircuit) {
            for (Vec p : ((SiliconCircuit) map).getPads()) {
                GOval f = Ui.oval(p.x - 36, p.y - 36, 72, 72, Ui.c(0x7fd8a8, 40), Ui.c(0x7fd8a8, 0));
                padFill.add(f);
                mapFx.add(f);
            }
        }
        if (map instanceof LegacyMonolith) {
            ventRect = Ui.rect(0, 0, GameMap.WIDTH, 10, Ui.c(0xff6a00, 45), Ui.c(0xff6a00, 120));
            ventRect.setVisible(false);
            mapFx.add(ventRect);
        }
        if (map instanceof HallucinationCanyon) {
            stormRect = Ui.rect(0, 0, GameMap.WIDTH, GameMap.HEIGHT, Ui.c(0xff8cf0, 35), Ui.c(0xff8cf0, 0));
            stormRect.setVisible(false);
            mapFx.add(stormRect);
        }
    }

    private GPolygon segment(Vec a, Vec b, double hw, Color col) {
        Vec d = b.sub(a).normalized();
        Vec n = new Vec(-d.y * hw, d.x * hw);
        GPolygon p = new GPolygon();
        p.addVertex(a.x + n.x, a.y + n.y);
        p.addVertex(b.x + n.x, b.y + n.y);
        p.addVertex(b.x - n.x, b.y - n.y);
        p.addVertex(a.x - n.x, a.y - n.y);
        p.setFilled(true); p.setFillColor(col); p.setColor(col);
        return p;
    }

    private void buildOverlay() {
        rangeCircle = Ui.oval(0, 0, 10, 10, Ui.c(0xffffff, 28), Ui.c(0xffffff, 140));
        rangeCircle.setVisible(false);
        overlay.add(rangeCircle);
        ghostRange = Ui.oval(0, 0, 10, 10, Ui.c(0x7fd8a8, 28), Ui.c(0x7fd8a8, 160));
        ghostRange.setVisible(false);
        overlay.add(ghostRange);
        ghost = Ui.oval(0, 0, 40, 40, Ui.c(0x7fd8a8, 120), Ui.c(0xffffff));
        ghost.setVisible(false);
        overlay.add(ghost);
        ghostLabel = Ui.label("", 0, 0, Ui.F_SMALL, Ui.TEXT);
        ghostLabel.setVisible(false);
        overlay.add(ghostLabel);
    }

    // ------------------------------------------------------------------ per frame
    public void update(double dt) {
        time += dt;
        syncTowers(dt);
        syncEnemies(dt);
        syncProjectiles();
        consumeEvents();
        stepEffects(dt);
        syncMapFx();
        syncBossBars();
        syncOverlay();
    }

    private void syncTowers(double dt) {
        List<Tower> marked = null;
        Boss pal = game.getPalantir();
        if (pal != null && pal.isAlive() && pal.isTelegraphing()) marked = pal.getMarkedTowers();
        for (Tower t : game.getTowerManager().getTowers()) {
            TowerSprite s = towerSprites.get(t);
            if (s == null) { s = new TowerSprite(t); towerSprites.put(t, s); towerLayer.add(s); }
            s.sync(game, dt, marked != null && marked.contains(t));
        }
        Iterator<Map.Entry<Tower, TowerSprite>> it = towerSprites.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Tower, TowerSprite> en = it.next();
            if (!en.getKey().isAlive()) { towerLayer.remove(en.getValue()); it.remove(); }
        }
        // telegraph lines
        for (GObject o : telegraphLines) overlay.remove(o);
        telegraphLines.clear();
        if (marked != null) {
            for (Tower t : marked) {
                GLine l = new GLine(pal.getPosition().x, pal.getPosition().y, t.getPosition().x, t.getPosition().y);
                l.setColor(Ui.c(0xff3030, (int) (120 + 100 * Math.abs(Math.sin(time * 10)))));
                overlay.add(l);
                telegraphLines.add(l);
            }
        }
    }

    private void syncEnemies(double dt) {
        List<Enemy> enemies = game.getEnemyManager().getEnemies();
        for (Enemy e : enemies) {
            EnemySprite s = enemySprites.get(e);
            if (s == null) {
                s = SpriteFactory.create(e);
                enemySprites.put(e, s);
                enemyLayer.add(s);
            }
            s.sync(e, time);
            s.step(dt);
            if (e.getMaxHp() >= 4 || e.isBoss()) {
                GRect[] bar = hpBars.get(e);
                double w = Math.max(18, e.getRadius() * 2.2);
                if (bar == null) {
                    bar = new GRect[] { Ui.rect(0, 0, w, 4, Ui.c(0x000000, 170), null), Ui.rect(0, 0, w, 4, Ui.GREEN, null) };
                    hpLayer.add(bar[0]); hpLayer.add(bar[1]);
                    hpBars.put(e, bar);
                }
                double x = e.getPosition().x - w / 2, y = e.getPosition().y - e.getRadius() - 9;
                bar[0].setLocation(x, y);
                bar[1].setLocation(x, y);
                double f = e.getHpFraction();
                bar[1].setSize(Math.max(0.5, w * f), 4);
                bar[1].setFillColor(f > 0.5 ? Ui.GREEN : f > 0.25 ? Ui.GOLD : Ui.RED);
                bar[1].setColor(bar[1].getFillColor());
            }
        }
        Iterator<Map.Entry<Enemy, EnemySprite>> it = enemySprites.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Enemy, EnemySprite> en = it.next();
            Enemy e = en.getKey();
            if (!e.isAlive() || e.isStored()) {
                enemyLayer.remove(en.getValue());
                it.remove();
                GRect[] bar = hpBars.remove(e);
                if (bar != null) { hpLayer.remove(bar[0]); hpLayer.remove(bar[1]); }
                if (!e.isAlive() && !e.hasLeaked() && animated.size() < MAX_EFFECTS) {
                    Animated fx = e.getType().hasChildren() ? SpriteFactory.createSplitEffect(e) : SpriteFactory.createDeathEffect(e);
                    if (fx instanceof GObject) { effectLayer.add((GObject) fx); animated.add(fx); }
                }
            }
        }
    }

    private void syncProjectiles() {
        for (Projectile p : game.getCombat().getProjectiles()) {
            GOval dot = projectileDots.get(p);
            if (dot == null) {
                double s = p.getSize();
                dot = Ui.oval(0, 0, s * 2, s * 2, Ui.c(p.getRgb()), Ui.c(0xffffff, 200));
                projectileDots.put(p, dot);
                projectileLayer.add(dot);
            }
            dot.setLocation(p.getPosition().x - p.getSize(), p.getPosition().y - p.getSize());
        }
        Iterator<Map.Entry<Projectile, GOval>> it = projectileDots.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Projectile, GOval> en = it.next();
            if (!en.getKey().isAlive()) { projectileLayer.remove(en.getValue()); it.remove(); }
        }
    }

    /** A transient effect built from a VisualEvent. */
    private static final class Effect {
        final GObject obj; final VisualEvent ev; double t; final Color base;
        Effect(GObject obj, VisualEvent ev, Color base) { this.obj = obj; this.ev = ev; this.base = base; }
    }

    private void consumeEvents() {
        for (VisualEvent ev : game.drainEvents()) {
            if (effects.size() >= MAX_EFFECTS && ev.kind != VisualEvent.Kind.PHASE && ev.kind != VisualEvent.Kind.BANNER) continue;
            Color col = Ui.c(ev.rgb);
            GObject o = null;
            switch (ev.kind) {
                case BEAM: case CHAIN: {
                    GLine l = new GLine(ev.a.x, ev.a.y, ev.b.x, ev.b.y);
                    l.setColor(col);
                    o = l;
                    break;
                }
                case HIT: o = Ui.oval(ev.a.x - ev.radius, ev.a.y - ev.radius, ev.radius * 2, ev.radius * 2, Ui.withAlpha(col, 200), null); break;
                case EXPLOSION: case ABILITY: case TELEPORT: case DEATH: case SPLIT: case PHASE:
                    o = Ui.oval(ev.a.x - 4, ev.a.y - 4, 8, 8, ev.kind == VisualEvent.Kind.EXPLOSION ? Ui.withAlpha(col, 90) : null, col);
                    break;
                case TEXT: {
                    GLabel l = Ui.label(ev.text, ev.a.x, ev.a.y, Ui.F_BOLD, col);
                    l.setLocation(ev.a.x - l.getWidth() / 2, ev.a.y);
                    o = l;
                    break;
                }
                case BANNER: screen.flashBanner(ev.text, col, ev.duration); break;
                default: break;
            }
            if (ev.kind == VisualEvent.Kind.PHASE) {
                GLabel l = Ui.label(ev.text, ev.a.x, ev.a.y - 60, Ui.F_H1, col);
                l.setLocation(ev.a.x - l.getWidth() / 2, ev.a.y - 60);
                effectLayer.add(l);
                effects.add(new Effect(l, VisualEvent.text(ev.a, ev.text, ev.rgb), col));
            }
            if (o != null) { effectLayer.add(o); effects.add(new Effect(o, ev, col)); }
        }
    }

    private void stepEffects(double dt) {
        for (int i = effects.size() - 1; i >= 0; i--) {
            Effect e = effects.get(i);
            e.t += dt;
            double f = Math.min(1, e.t / e.ev.duration);
            if (f >= 1) { effectLayer.remove(e.obj); effects.remove(i); continue; }
            int alpha = (int) (255 * (1 - f));
            switch (e.ev.kind) {
                case EXPLOSION: case ABILITY: case TELEPORT: case DEATH: case SPLIT: case PHASE: {
                    double r = e.ev.radius * (e.ev.kind == VisualEvent.Kind.ABILITY ? (0.2 + 0.8 * f) : (0.3 + 0.7 * f));
                    GOval ov = (GOval) e.obj;
                    ov.setBounds(e.ev.a.x - r, e.ev.a.y - r, 2 * r, 2 * r);
                    ov.setColor(Ui.withAlpha(e.base, alpha));
                    if (ov.isFilled()) ov.setFillColor(Ui.withAlpha(e.base, alpha / 3));
                    break;
                }
                case TEXT: e.obj.move(0, -30 * dt); e.obj.setColor(Ui.withAlpha(e.base, alpha)); break;
                default: e.obj.setColor(Ui.withAlpha(e.base, alpha)); break;
            }
        }
        for (int i = animated.size() - 1; i >= 0; i--) {
            Animated a = animated.get(i);
            if (!a.step(dt)) { effectLayer.remove((GObject) a); animated.remove(i); }
        }
    }

    private void syncMapFx() {
        if (map instanceof SiliconCircuit) {
            SiliconCircuit sc = (SiliconCircuit) map;
            for (int i = 0; i < padFill.size(); i++) {
                double c = sc.getPadCharge(i);
                padFill.get(i).setFillColor(Ui.c(0x7fd8a8, (int) (20 + 120 * c)));
            }
            int lane = sc.getShortedLane();
            for (GObject o : shortFx) mapFx.remove(o);
            shortFx.clear();
            if (lane >= 0) {
                Path r = map.getRoutes().get(lane);
                List<Vec> w = r.getWaypoints();
                for (int i = 0; i + 1 < w.size(); i++) {
                    GPolygon p = segment(w.get(i), w.get(i + 1), GameMap.TRACK_HALF_WIDTH, Ui.c(0xffffff, (int) (90 + 90 * Math.abs(Math.sin(time * 30)))));
                    mapFx.add(p);
                    shortFx.add(p);
                }
            }
        }
        if (ventRect != null) {
            LegacyMonolith lm = (LegacyMonolith) map;
            int f = lm.getVentedFloor();
            ventRect.setVisible(f >= 0);
            if (f >= 0) {
                double[] band = lm.getFloors()[f];
                ventRect.setBounds(0, band[0], GameMap.WIDTH, band[1] - band[0]);
                ventRect.setFillColor(Ui.c(0xff6a00, (int) (35 + 25 * Math.sin(time * 4))));
            }
        }
        if (stormRect != null) {
            HallucinationCanyon hc = (HallucinationCanyon) map;
            stormRect.setVisible(hc.isStormActive());
            if (hc.isStormActive()) stormRect.setFillColor(Ui.c(0xff8cf0, (int) (25 + 25 * Math.abs(Math.sin(time * 6)))));
        }
    }

    private void syncBossBars() {
        List<Boss> bosses = game.getActiveBosses();
        if (bosses.size() > 4) bosses = bosses.subList(0, 4);
        if (!bosses.equals(bossBarOrder)) {
            for (GObject o : bossBarObjects) bossLayer.remove(o);
            bossBarObjects.clear();
            bossBarOrder.clear();
            bossBarOrder.addAll(bosses);
            int i = 0;
            for (Boss b : bosses) {
                boolean pal = b.isPalantir();
                double w = pal ? 760 : 420, h = pal ? 22 : 12;
                double x = (GameMap.WIDTH - w) / 2, y = 30 + i * (h + 30);
                GRect back = Ui.rect(x, y, w, h, Ui.c(0x000000, 190), Ui.c(0xffffff, 120));
                GRect fill = Ui.rect(x, y, w, h, pal ? Ui.c(0xff3b3b) : Ui.c(0xff9a5a), null);
                GLabel name = Ui.label("", x, y - 4, pal ? Ui.F_H2 : Ui.F_BOLD, Ui.TEXT);
                bossLayer.add(back); bossLayer.add(fill); bossLayer.add(name);
                bossBarObjects.add(back); bossBarObjects.add(fill); bossBarObjects.add(name);
                if (pal) {
                    for (double thr : new double[] { 0.70, 0.35 }) {
                        GRect tick = Ui.rect(x + w * thr - 1, y - 3, 2, h + 6, Ui.c(0xffffff), null);
                        bossLayer.add(tick); bossBarObjects.add(tick);
                    }
                }
                i++;
            }
        }
        for (int i = 0; i < bossBarOrder.size(); i++) {
            Boss b = bossBarOrder.get(i);
            int base = 0;
            for (int k = 0; k < i; k++) base += bossBarOrder.get(k).isPalantir() ? 5 : 3;
            GRect fill = (GRect) bossBarObjects.get(base + 1);
            GLabel name = (GLabel) bossBarObjects.get(base + 2);
            boolean pal = b.isPalantir();
            double w = pal ? 760 : 420;
            fill.setSize(Math.max(0.5, w * b.getHpFraction()), pal ? 22 : 12);
            String txt = b.getType().displayName + " [" + b.getType().getBossClass().label + "]  "
                + (long) Math.ceil(b.getHp()) + " / " + (long) b.getMaxHp();
            if (pal) txt += "   Phase " + (b.getPhase() + 1) + "/3: " + b.getPhaseName()
                + (b.isTelegraphing() ? "   PREDICTIVE SURVEILLANCE " + String.format("%.1f", b.getTelegraphRemaining()) : "");
            if (b.isFortified()) txt += "  (Fortified)";
            if (b.isCamo()) txt += "  (Camo)";
            name.setLabel(txt);
            if (pal && b.isTelegraphing()) fill.setFillColor(Ui.c((time * 8) % 2 < 1 ? 0xff3b3b : 0xffffff));
            else fill.setFillColor(pal ? Ui.c(0xff3b3b) : Ui.c(0xff9a5a));
        }
    }

    private void syncOverlay() {
        Tower sel = screen.getSelectedTower();
        TowerType placing = screen.getPlacing();
        Vec mouse = screen.getMouseWorld();
        if (placing != null && mouse != null && insideWorld(mouse)) {
            double r = TowerCatalog.get(placing).statsForTier(0).range * map.towerRangeMultiplier(game, null);
            String problem = game.getTowerManager().placementProblem(placing, mouse);
            boolean ok = problem == null;
            Color col = ok ? Ui.c(0x7fd8a8) : Ui.c(0xff5a5a);
            ghostRange.setBounds(mouse.x - r, mouse.y - r, 2 * r, 2 * r);
            ghostRange.setFillColor(Ui.withAlpha(col, 28)); ghostRange.setColor(Ui.withAlpha(col, 170));
            ghostRange.setVisible(true);
            double tr = TowerCatalog.get(placing).radius;
            ghost.setBounds(mouse.x - tr, mouse.y - tr, 2 * tr, 2 * tr);
            ghost.setFillColor(Ui.withAlpha(col, 140));
            ghost.setVisible(true);
            ghostLabel.setLabel(ok ? placing.displayName + "  " + Ui.money(TowerCatalog.get(placing).baseCost) : problem);
            ghostLabel.setLocation(mouse.x - ghostLabel.getWidth() / 2, mouse.y - tr - 8);
            ghostLabel.setColor(col);
            ghostLabel.setVisible(true);
            rangeCircle.setVisible(false);
        } else {
            ghostRange.setVisible(false); ghost.setVisible(false); ghostLabel.setVisible(false);
            if (sel != null && sel.isAlive()) {
                double r = sel.effectiveRange(game);
                rangeCircle.setBounds(sel.getPosition().x - r, sel.getPosition().y - r, 2 * r, 2 * r);
                rangeCircle.setVisible(true);
            } else {
                rangeCircle.setVisible(false);
            }
        }
    }

    public static boolean insideWorld(Vec p) {
        return p.x >= 0 && p.y >= 0 && p.x <= GameMap.WIDTH && p.y <= GameMap.HEIGHT;
    }

    /** Enemy under a world point, or null. */
    public Enemy pickEnemy(Vec p) {
        Enemy best = null;
        double bestD = Double.MAX_VALUE;
        for (Enemy e : game.getEnemyManager().getEnemies()) {
            double d = e.getPosition().dist(p);
            if (d <= e.getRadius() + 6 && d < bestD) { bestD = d; best = e; }
        }
        return best;
    }
}
