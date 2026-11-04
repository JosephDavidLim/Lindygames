package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GLine;
import acm.graphics.GObject;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import lindefense.ui.sprites.towers.ProjectileSprite;
import lindefense.ui.sprites.towers.ProjectileSprites;

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

    private final Map<Tower, TowerSprite> towerSprites = new HashMap<>();
    private final Map<Enemy, EnemySprite> enemySprites = new HashMap<>();
    private final Map<Enemy, Cyber.Bar> hpBars = new HashMap<>();
    private final Map<Projectile, ProjectileSprite> projectileSprites = new HashMap<>();
    private final List<Effect> effects = new ArrayList<>();
    private final List<Animated> animated = new ArrayList<>();
    private final List<GObject> telegraphLines = new ArrayList<>();

    private Cyber.Ring rangeCircle, ghost, ghostRange;
    private GLine ghostX1, ghostX2;
    private Nameplate ghostPlate, hoverPlate, selectPlate;
    private double viewScale = 1, viewDock;
    private final List<GOval> padFill = new ArrayList<>();
    private GRect ventRect;
    private GRect stormRect;
    private final List<GObject> shortFx = new ArrayList<>();
    private double time;

    public WorldView(GameScreen screen) {
        this.screen = screen;
        this.game = screen.getGame();
        this.map = game.getMap();
        setLocation(0, 0);
        buildBackground();
        add(background);
        add(mapFx);
        add(towerLayer);
        add(enemyLayer);
        add(hpLayer);
        add(projectileLayer);
        add(effectLayer);
        add(overlay);
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
            addMarker(r.getStart(), w.get(1).sub(w.get(0)).normalized(), Cyber.GREEN, "IN");
            addMarker(r.getEnd(), w.get(w.size() - 1).sub(w.get(w.size() - 2)).normalized(), Cyber.RED, "OUT");
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

    /** Track entry / exit marker: a ringed chevron inset from the world edge so it is never clipped. */
    private void addMarker(Vec at, Vec dir, Color col, String text) {
        double m = 26;
        double px = Math.max(m, Math.min(GameMap.WIDTH - m, at.x)), py = Math.max(m, Math.min(GameMap.HEIGHT - m, at.y));
        Cyber.Ring ring = new Cyber.Ring(px - 15, py - 15, 30, 30, Cyber.alpha(col, 70), Cyber.alpha(col, 235));
        ring.setLineWidth(2);
        background.add(ring);
        Vec n = new Vec(-dir.y, dir.x);
        GPolygon chev = new GPolygon();
        chev.addVertex(px + dir.x * 8, py + dir.y * 8);
        chev.addVertex(px - dir.x * 1 + n.x * 8, py - dir.y * 1 + n.y * 8);
        chev.addVertex(px - dir.x * 1 + n.x * 4, py - dir.y * 1 + n.y * 4);
        chev.addVertex(px + dir.x * 3, py + dir.y * 3);
        chev.addVertex(px - dir.x * 1 - n.x * 4, py - dir.y * 1 - n.y * 4);
        chev.addVertex(px - dir.x * 1 - n.x * 8, py - dir.y * 1 - n.y * 8);
        chev.setFilled(true); chev.setFillColor(col); chev.setColor(col);
        background.add(chev);
        GLabel l = Ui.label(text, 0, 0, Cyber.mono(Font.BOLD, 11), col);
        double ly = py + 31 < GameMap.HEIGHT - 4 ? py + 31 : py - 21;
        l.setLocation(px - l.getWidth() / 2, ly);
        background.add(l);
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
        rangeCircle = new Cyber.Ring(0, 0, 10, 10, Cyber.alpha(Cyber.CYAN, 22), Cyber.alpha(Cyber.CYAN, 215));
        rangeCircle.setVisible(false);
        overlay.add(rangeCircle);
        ghostRange = new Cyber.Ring(0, 0, 10, 10, Cyber.alpha(Cyber.CYAN, 22), Cyber.alpha(Cyber.CYAN, 215));
        ghostRange.setVisible(false);
        overlay.add(ghostRange);
        ghost = new Cyber.Ring(0, 0, 40, 40, Cyber.alpha(Cyber.CYAN, 90), Cyber.CYAN);
        ghost.setLineWidth(2.2);
        ghost.setVisible(false);
        overlay.add(ghost);
        ghostX1 = new GLine(0, 0, 1, 1); ghostX2 = new GLine(0, 0, 1, 1);
        ghostX1.setColor(Cyber.RED); ghostX2.setColor(Cyber.RED);
        ghostX1.setVisible(false); ghostX2.setVisible(false);
        overlay.add(ghostX1); overlay.add(ghostX2);
        hoverPlate = new Nameplate();
        selectPlate = new Nameplate();
        ghostPlate = new Nameplate();
        overlay.add(hoverPlate); overlay.add(selectPlate); overlay.add(ghostPlate);
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
            syncHpBar(e);
        }
        Iterator<Map.Entry<Enemy, EnemySprite>> it = enemySprites.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Enemy, EnemySprite> en = it.next();
            Enemy e = en.getKey();
            if (!e.isAlive() || e.isStored()) {
                enemyLayer.remove(en.getValue());
                it.remove();
                Cyber.Bar bar = hpBars.remove(e);
                if (bar != null) hpLayer.remove(bar);
                if (!e.isAlive() && !e.hasLeaked() && animated.size() < MAX_EFFECTS) {
                    Animated fx = e.getType().hasChildren() ? SpriteFactory.createSplitEffect(e) : SpriteFactory.createDeathEffect(e);
                    if (fx instanceof GObject) { effectLayer.add((GObject) fx); animated.add(fx); }
                }
            }
        }
    }

    /** Compact HP bar centred just above a damaged, non-boss enemy; created lazily, mutated every frame. */
    private void syncHpBar(Enemy e) {
        boolean show = !e.isBoss() && e.isAlive() && e.getHp() < e.getMaxHp() - 1e-9;
        Cyber.Bar bar = hpBars.get(e);
        if (!show) {
            if (bar != null) bar.setVisible(false);
            return;
        }
        double r = e.getRadius();
        double w = Math.max(16, Math.min(48, r * 2.0 + 4));
        double h = r > 14 ? 5 : 4;
        if (bar == null) {
            bar = new Cyber.Bar(0, 0, w, h, 0).colors(Cyber.GREEN, new Color(4, 6, 14, 215), new Color(0, 0, 0, 235));
            hpLayer.add(bar);
            hpBars.put(e, bar);
        }
        double f = e.getHpFraction();
        bar.setSize(w, h);
        bar.setLocation(e.getPosition().x - w / 2, e.getPosition().y - r - h - 6);
        bar.setValue(f);
        bar.setFill(Cyber.hpColor(f));
        bar.setVisible(true);
    }

    private void syncProjectiles() {
        double now = game.getTime();
        for (Projectile p : game.getCombat().getProjectiles()) {
            ProjectileSprite s = projectileSprites.get(p);
            if (s == null) {
                s = ProjectileSprites.create(p);
                projectileSprites.put(p, s);
                projectileLayer.add(s);
            }
            s.sync(p, now);
        }
        Iterator<Map.Entry<Projectile, ProjectileSprite>> it = projectileSprites.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Projectile, ProjectileSprite> en = it.next();
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
            if (ev.kind == VisualEvent.Kind.BEAM || ev.kind == VisualEvent.Kind.CHAIN
                    || ev.kind == VisualEvent.Kind.HIT || ev.kind == VisualEvent.Kind.EXPLOSION) {
                if (animated.size() >= MAX_EFFECTS) continue;
                Animated fx = ProjectileSprites.createAttackEffect(ev);
                if (fx != null) {
                    effectLayer.add((GObject) fx);
                    animated.add(fx);
                    continue;
                }
            }
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
                    GLabel l = Ui.label(ev.text, ev.a.x, ev.a.y, Cyber.mono(Font.BOLD, 13), col);
                    l.setLocation(ev.a.x - l.getWidth() / 2, ev.a.y);
                    o = l;
                    break;
                }
                case BANNER: screen.flashBanner(ev.text, col, ev.duration); break;
                default: break;
            }
            if (ev.kind == VisualEvent.Kind.PHASE) {
                GLabel l = Ui.label(ev.text, ev.a.x, ev.a.y - 60, Cyber.display(Font.BOLD, 36), col);
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

    private void syncOverlay() {
        Tower sel = screen.getSelectedTower();
        TowerType placing = screen.getPlacing();
        Vec mouse = screen.getMouseWorld();
        Tower hover = null;
        if (placing != null && mouse != null && insideWorld(mouse)) {
            double r = TowerCatalog.get(placing).statsForTier(0).range * map.towerRangeMultiplier(game, null);
            String problem = game.getTowerManager().placementProblem(placing, mouse);
            boolean ok = problem == null;
            Color col = ok ? Cyber.CYAN : Cyber.RED;
            ghostRange.set(mouse.x, mouse.y, r);
            ghostRange.setFill(Cyber.alpha(col, ok ? 20 : 30)); ghostRange.setLine(Cyber.alpha(col, ok ? 215 : 235));
            ghostRange.setDashed(!ok); ghostRange.setPhase(time * 14);
            ghostRange.setVisible(true);
            double tr = TowerCatalog.get(placing).radius;
            ghost.set(mouse.x, mouse.y, tr);
            ghost.setFill(Cyber.alpha(col, ok ? 85 : 110)); ghost.setLine(col);
            ghost.setVisible(true);
            ghostX1.setVisible(!ok); ghostX2.setVisible(!ok);
            if (!ok) {
                double k = tr * 0.55;
                ghostX1.setStartPoint(mouse.x - k, mouse.y - k); ghostX1.setEndPoint(mouse.x + k, mouse.y + k);
                ghostX2.setStartPoint(mouse.x - k, mouse.y + k); ghostX2.setEndPoint(mouse.x + k, mouse.y - k);
            }
            ghostPlate.set(ok ? placing.displayName : problem, ok ? Ui.money(TowerCatalog.get(placing).baseCost) : null, col);
            placePlate(ghostPlate, mouse, tr);
            ghostPlate.setVisible(true);
            rangeCircle.setVisible(false);
        } else {
            ghostRange.setVisible(false); ghost.setVisible(false); ghostPlate.setVisible(false);
            ghostX1.setVisible(false); ghostX2.setVisible(false);
            if (sel != null && sel.isAlive()) {
                rangeCircle.set(sel.getPosition().x, sel.getPosition().y, sel.effectiveRange(game));
                rangeCircle.setVisible(true);
            } else {
                rangeCircle.setVisible(false);
            }
            if (mouse != null && insideWorld(mouse)) hover = game.getTowerManager().towerAt(mouse);
        }
        // nameplates: tower under the cursor and the selected tower
        Tower selShow = sel != null && sel.isAlive() && placing == null ? sel : null;
        showPlate(selectPlate, selShow);
        showPlate(hoverPlate, hover != null && hover != selShow ? hover : null);
    }

    private void showPlate(Nameplate p, Tower t) {
        if (t == null) { p.setVisible(false); return; }
        TowerType ty = t.getType();
        p.set(ty.displayName, t.getTierLabel(), Cyber.lerp(Ui.c(ty.primaryRgb), Color.WHITE, 0.35));
        placePlate(p, t.getPosition(), t.getRadius());
        p.setVisible(true);
    }

    /** Puts a plate above (or below, near the top edge) a circle, clamped inside the world. */
    private void placePlate(Nameplate p, Vec c, double r) {
        double w = p.getPlateWidth();
        double x = Math.max(4, Math.min(GameMap.WIDTH - w - 4, c.x - w / 2));
        double y = c.y - r - Nameplate.HEIGHT - 12;
        if (y < 4) y = c.y + r + 12;
        y = Math.min(y, GameMap.HEIGHT - Nameplate.HEIGHT - 4);
        p.place(x, y);
    }

    /** Reserves {@code dock} logical px under the HUD for the boss dock by shrinking the playfield uniformly. */
    public void setDock(double dock) {
        viewDock = Math.max(0, Math.min(140, dock));
        viewScale = (Ui.WORLD_H - viewDock) / Ui.WORLD_H;
    }

    /** Maps a logical point (screen layout space) into world coordinates, or null outside the playfield. */
    public Vec toWorld(double lx, double ly) {
        double offX = (Ui.WORLD_W - GameMap.WIDTH * viewScale) / 2;
        Vec p = new Vec((lx - Ui.WORLD_X - offX) / viewScale, (ly - Ui.WORLD_Y - viewDock) / viewScale);
        return insideWorld(p) ? p : null;
    }

    @Override public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Cyber.quality(g2);
            double offX = (Ui.WORLD_W - GameMap.WIDTH * viewScale) / 2;
            g2.translate(Ui.WORLD_X + offX, Ui.WORLD_Y + viewDock);
            g2.scale(viewScale, viewScale);
            g2.clipRect(0, 0, GameMap.WIDTH, GameMap.HEIGHT);
            super.paint(g2);
        } finally {
            g2.dispose();
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
