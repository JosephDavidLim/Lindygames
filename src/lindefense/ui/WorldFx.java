package lindefense.ui;

import acm.graphics.GObject;
import acm.graphics.GRectangle;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import lindefense.combat.VisualEvent;
import lindefense.effects.Drone;
import lindefense.effects.NodeHazard;
import lindefense.effects.TrackWalker;
import lindefense.effects.TrackZone;
import lindefense.enemy.Enemy;
import lindefense.map.GameMap;
import lindefense.sim.Game;
import lindefense.sim.Vec;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerStats;
import lindefense.tower.TowerType;
import lindefense.tower.behavior.GptBehavior;

/**
 * In-world renderer for the tower-engine entities that are not plain sprites: {@link TrackZone}s (paint, loop,
 * tornado, vortex, outpaint, tempest), {@link TrackWalker}s (DALL-E creatures, Llama charges and the stampede herd),
 * {@link Drone}s (companions, study buddies, Qwen patrol drones, MiniMax clones), {@link NodeHazard}s (GLM nodes,
 * laser links, slow grid), GPT's orbiting moons and Astra Beam, the Copilot strike drone and Gemini's singularity.
 *
 * <p>Style matches the sprite package: thick dark outline, saturated fills, a white gloss highlight. It is
 * <b>immediate mode</b>: two lightweight {@link GObject}s ({@link #ground()} under the enemies, {@link #top()} above
 * them) paint the live lists of {@code game.getEffects()} straight into the graphics context with a handful of
 * reusable shapes, so there is no scene-graph churn at all and nothing to remove when an entity expires. The only
 * state kept is the first-seen time of each entity (for pop-in animations), pruned a few times a second.</p>
 *
 * <p>Level of detail ({@link #setLod}): 0 = full, 1 = no small decorations, 2 = flat silhouettes only. At most
 * {@link #MAX_DRAWN} entities of each kind are drawn.</p>
 */
final class WorldFx {
    static final int MAX_DRAWN = 40;

    /** Entities of each kind drawn at the current level of detail. */
    private int cap() { return lod == 0 ? MAX_DRAWN : lod == 1 ? 24 : 12; }
    private static final Color INK = new Color(24, 22, 40);
    private static final Color CREAM = new Color(252, 244, 222);
    private static final Color WHITE_GLOSS = new Color(255, 255, 255, 150);

    private final Game game;
    private final GObject ground = new Layer(true), top = new Layer(false);
    private double time, pruneTimer;
    private int lod;

    private final Map<Object, Double> born = new IdentityHashMap<>();
    private final Map<Integer, BasicStroke> strokes = new HashMap<>();
    private final Ellipse2D.Double ell = new Ellipse2D.Double();
    private final Path2D.Double path = new Path2D.Double();
    private final Line2D.Double line = new Line2D.Double();

    /** A live Gemini singularity: centre, radius, start time (fx clock) and hold time. */
    private static final class Sing { Vec at; double radius, t0, hold; int rgb; }
    private final List<Sing> sings = new ArrayList<>();

    WorldFx(Game game) { this.game = game; }

    /** Drawn below the enemy layer (zones, grids, singularities). */
    GObject ground() { return ground; }
    /** Drawn above enemies (walkers, drones, nodes, moons, beams). */
    GObject top() { return top; }

    void setLod(int lod) { this.lod = lod; }

    void update(double dt) {
        time += dt;
        List<TrackZone> zs = game.getEffects().getZones();
        for (int i = 0; i < zs.size(); i++) bornOf(zs.get(i));   // stamp first-seen times in the frame they appear
        List<TrackWalker> ws = game.getEffects().getWalkers();
        for (int i = 0; i < ws.size(); i++) bornOf(ws.get(i));
        List<Drone> ds = game.getEffects().getDrones();
        for (int i = 0; i < ds.size(); i++) bornOf(ds.get(i));
        List<NodeHazard> hz = game.getEffects().getHazards();
        for (int i = 0; i < hz.size(); i++) bornOf(hz.get(i));
        pruneTimer -= dt;
        if (pruneTimer <= 0) {
            pruneTimer = 1.5;
            born.keySet().removeIf(o -> !isLive(o));
        }
        for (int i = sings.size() - 1; i >= 0; i--) if (time - sings.get(i).t0 > sings.get(i).hold + 0.5) sings.remove(i);
    }

    private static boolean isLive(Object o) {
        if (o instanceof TrackZone) return ((TrackZone) o).isAlive();
        if (o instanceof TrackWalker) return ((TrackWalker) o).isAlive();
        if (o instanceof Drone) return ((Drone) o).isAlive();
        if (o instanceof NodeHazard) return ((NodeHazard) o).isAlive();
        return false;
    }

    /** Feeds an ability event: Gemini's Argon Singularity has no sim entity, so it is recognised here. */
    void onAbilityEvent(VisualEvent ev) {
        if (ev.a == null) return;
        for (Tower t : game.getTowerManager().getTowers()) {
            if (t.getType() != TowerType.GEMINI || t.getAbility() == null || !t.getAbility().has("holdTime")) continue;
            AbilityDef a = t.getAbility();
            if (Math.abs(ev.radius - a.p("radius", -1)) > 0.01 || ev.rgb != t.getType().primaryRgb) continue;
            Sing s = new Sing();
            s.at = ev.a; s.radius = ev.radius; s.t0 = time; s.hold = a.p("holdTime"); s.rgb = ev.rgb;
            sings.add(s);
            return;
        }
    }

    private double bornOf(Object o) {
        Double b = born.get(o);
        if (b == null) { born.put(o, time); return time; }
        return b;
    }

    // ------------------------------------------------------------------ the two layers
    private final class Layer extends GObject {
        private static final long serialVersionUID = 1L;
        private final boolean isGround;
        Layer(boolean ground) { this.isGround = ground; }
        @Override public GRectangle getBounds() { return new GRectangle(0, 0, GameMap.WIDTH, GameMap.HEIGHT); }
        @Override public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Cyber.quality(g2);
                if (isGround) paintGround(g2); else paintTop(g2);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            } finally {
                g2.dispose();
            }
        }
    }

    private void paintGround(Graphics2D g) {
        List<NodeHazard> hz = game.getEffects().getHazards();
        for (int i = 0, n = Math.min(hz.size(), cap()); i < n; i++) drawGrid(g, hz.get(i));
        List<TrackZone> zs = game.getEffects().getZones();
        for (int i = 0, n = Math.min(zs.size(), cap()); i < n; i++) {
            TrackZone z = zs.get(i);
            if (z.isAlive()) drawZone(g, z);
        }
        for (int i = 0; i < sings.size(); i++) drawSingularity(g, sings.get(i));
    }

    private void paintTop(Graphics2D g) {
        List<NodeHazard> hz = game.getEffects().getHazards();
        for (int i = 0, n = Math.min(hz.size(), cap()); i < n; i++) drawLinksAndNodes(g, hz.get(i));
        List<TrackWalker> ws = game.getEffects().getWalkers();
        for (int i = 0, n = Math.min(ws.size(), cap()); i < n; i++) {
            TrackWalker w = ws.get(i);
            if (w.isAlive()) drawWalker(g, w);
        }
        List<Drone> ds = game.getEffects().getDrones();
        for (int i = 0, n = Math.min(ds.size(), cap()); i < n; i++) {
            Drone d = ds.get(i);
            if (d.isAlive()) drawDrone(g, d);
        }
        List<Tower> towers = game.getTowerManager().getTowers();
        for (int i = 0; i < towers.size(); i++) {
            Tower t = towers.get(i);
            if (!t.isAlive()) continue;
            TowerType ty = t.getType();
            if (ty == TowerType.GPT) { drawMoons(g, t); if (GptBehavior.beamActive(t)) drawAstraBeam(g, t); }
            else if (ty == TowerType.COPILOT && t.getStats().has("droneInterval")) drawCopilotDrone(g, t);
        }
    }

    // ------------------------------------------------------------------ small helpers
    private BasicStroke st(double w) {
        int k = (int) Math.round(w * 4);
        BasicStroke s = strokes.get(k);
        if (s == null) { s = new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); strokes.put(k, s); }
        return s;
    }

    private static Color col(int rgb, double alpha) {
        return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, (int) Math.max(0, Math.min(255, alpha)));
    }

    private static Color col(Color c, double alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.max(0, Math.min(255, alpha)));
    }

    private static Color mix(int rgb, Color to, double f) {
        int r = (rgb >> 16) & 255, gg = (rgb >> 8) & 255, b = rgb & 255;
        return new Color((int) (r + (to.getRed() - r) * f), (int) (gg + (to.getGreen() - gg) * f), (int) (b + (to.getBlue() - b) * f));
    }

    /** Dashed stroke at full detail; a plain solid one in crowded scenes (dash rasterization is the expensive part). */
    private java.awt.Stroke dashed(double w, double on, double off, double phase) {
        if (lod >= 1) return st(w);
        return new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[] { (float) on, (float) off }, (float) dashPhase(phase));
    }

    /** Dash phases must be non-negative. */
    private static double dashPhase(double p) { double m = p % 1000; return m < 0 ? m + 1000 : m; }

    private static double easeOut(double t) { double u = 1 - t; return 1 - u * u * u; }

    private void oval(Graphics2D g, double cx, double cy, double rx, double ry, Color fill, Color outline, double lw) {
        ell.setFrame(cx - rx, cy - ry, rx * 2, ry * 2);
        if (fill != null) { g.setColor(fill); g.fill(ell); }
        if (outline != null && lw > 0) { g.setStroke(st(lw)); g.setColor(outline); g.draw(ell); }
    }

    private void ring(Graphics2D g, double cx, double cy, double r, Color c, double lw) {
        ell.setFrame(cx - r, cy - r, r * 2, r * 2);
        g.setStroke(st(lw)); g.setColor(c); g.draw(ell);
    }

    private void seg(Graphics2D g, double x1, double y1, double x2, double y2, Color c, double lw) {
        line.setLine(x1, y1, x2, y2);
        g.setStroke(st(lw)); g.setColor(c); g.draw(line);
    }

    /** Dark outline stroke under a coloured stroke of the same shape (the cartoon "thick outline" look). */
    private void outlinedStroke(Graphics2D g, Shape s, Color c, double lw) {
        g.setStroke(st(lw + 3)); g.setColor(INK); g.draw(s);
        g.setStroke(st(lw)); g.setColor(c); g.draw(s);
    }

    private void gloss(Graphics2D g, double cx, double cy, double r) {
        oval(g, cx - r * 0.33, cy - r * 0.38, r * 0.34, r * 0.2, WHITE_GLOSS, null, 0);
    }

    /** Wobbly closed blob (paint splotch). */
    private Path2D blob(double cx, double cy, double r, double wob, int seed, double ph) {
        path.reset();
        int n = lod >= 2 ? 10 : 18;
        for (int i = 0; i <= n; i++) {
            double a = Math.PI * 2 * i / n;
            double rr = r * (1 + wob * Math.sin(a * 3 + seed * 1.7 + ph) + wob * 0.6 * Math.sin(a * 5 + seed * 0.9 - ph * 0.7));
            double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.closePath();
        return path;
    }

    private static int seedOf(Object o) { return (System.identityHashCode(o) >> 3) & 0xff; }

    // ------------------------------------------------------------------ track zones
    private void drawZone(Graphics2D g, TrackZone z) {
        double age = time - bornOf(z);
        double pop = easeOut(Math.min(1, age / 0.3));
        double fade = z.permanent ? 1 : Math.max(0, Math.min(1, z.getRemaining() / 0.6));
        double R = z.radius * pop;
        if (R < 1) return;
        double cx = z.center.x, cy = z.center.y, A = 255 * fade;
        String kind = z.kind == null ? "" : z.kind;
        switch (kind) {
            case "paint": zonePaint(g, z, cx, cy, R, A); break;
            case "loop": zoneLoop(g, z, cx, cy, R, A); break;
            case "tornado": zoneTornado(g, z, cx, cy, R, A); break;
            case "vortex": zoneVortex(g, z, cx, cy, R, A); break;
            case "outpaint": zoneOutpaint(g, z, cx, cy, R, A); break;
            case "tempest": zoneTempest(g, z, cx, cy, R, A); break;
            default:
                oval(g, cx, cy, R, R, col(z.rgb, A * 0.25), col(INK, A * 0.8), 2.5);
                ring(g, cx, cy, R - 3, col(z.rgb, A * 0.9), 2);
        }
    }

    /** DALL-E paint: a glossy wobbling splotch with droplets and (armour crack) a few dark cracks. */
    private void zonePaint(Graphics2D g, TrackZone z, double cx, double cy, double R, double A) {
        int seed = seedOf(z);
        Color body = mix(z.rgb, Color.WHITE, 0.15);
        Path2D b = blob(cx, cy, R * 0.92, 0.07, seed, time * 1.6);
        g.setColor(col(body, A * 0.42)); g.fill(b);
        g.setStroke(st(3)); g.setColor(col(INK, A * 0.85)); g.draw(b);
        Path2D inner = blob(cx, cy, R * 0.6, 0.1, seed + 3, -time * 1.3);
        g.setColor(col(mix(z.rgb, Color.WHITE, 0.45), A * 0.35)); g.fill(inner);
        if (lod >= 2) return;
        for (int i = 0; i < 4; i++) {
            double a = (seed * 0.37 + i * 1.7) % (Math.PI * 2);
            double d = R * (1.0 + 0.08 * Math.sin(time * 3 + i));
            oval(g, cx + Math.cos(a) * d, cy + Math.sin(a) * d, 4.5 - i * 0.6, 4.5 - i * 0.6, col(body, A * 0.8), col(INK, A * 0.85), 2);
        }
        if (lod == 0) {
            gloss(g, cx - R * 0.15, cy - R * 0.1, R * 1.1);
            if (z.armorCrack) {
                g.setStroke(st(1.8)); g.setColor(col(INK, A * 0.7));
                for (int i = 0; i < 3; i++) {
                    double a = seed * 0.5 + i * 2.1, r0 = R * 0.15, r1 = R * 0.7;
                    path.reset();
                    path.moveTo(cx + Math.cos(a) * r0, cy + Math.sin(a) * r0);
                    path.lineTo(cx + Math.cos(a + 0.25) * (r0 + r1) * 0.5, cy + Math.sin(a + 0.25) * (r0 + r1) * 0.5);
                    path.lineTo(cx + Math.cos(a - 0.1) * r1, cy + Math.sin(a - 0.1) * r1);
                    g.draw(path);
                }
            }
        }
    }

    /** Sora loop: a double clock ring with rotating tick marks and two chasing arrowheads. */
    private void zoneLoop(Graphics2D g, TrackZone z, double cx, double cy, double R, double A) {
        oval(g, cx, cy, R, R, col(z.rgb, A * 0.2), null, 0);
        ring(g, cx, cy, R - 2, col(INK, A * 0.9), 6);
        ring(g, cx, cy, R - 2, col(z.rgb, A * 0.95), 3);
        ring(g, cx, cy, R * 0.62, col(INK, A * 0.8), 5);
        ring(g, cx, cy, R * 0.62, col(mix(z.rgb, Color.WHITE, 0.5), A * 0.9), 2);
        if (lod >= 2) return;
        int ticks = lod == 0 ? 12 : 6;
        double rot = time * 1.4;
        for (int i = 0; i < ticks; i++) {
            double a = rot + Math.PI * 2 * i / ticks;
            seg(g, cx + Math.cos(a) * R * 0.66, cy + Math.sin(a) * R * 0.66, cx + Math.cos(a) * R * 0.88, cy + Math.sin(a) * R * 0.88, col(Color.WHITE, A * 0.85), 2);
        }
        for (int k = 0; k < 2; k++) {
            double a = -rot * 1.6 + k * Math.PI, rr = R * 0.78;
            double px = cx + Math.cos(a) * rr, py = cy + Math.sin(a) * rr;
            double tx = -Math.sin(a), ty = Math.cos(a), nx = Math.cos(a), ny = Math.sin(a);
            path.reset();
            path.moveTo(px + tx * 9, py + ty * 9);
            path.lineTo(px - tx * 4 + nx * 7, py - ty * 4 + ny * 7);
            path.lineTo(px - tx * 4 - nx * 7, py - ty * 4 - ny * 7);
            path.closePath();
            g.setColor(col(Art_GOLD, A)); g.fill(path);
            g.setStroke(st(2)); g.setColor(col(INK, A)); g.draw(path);
        }
    }

    private static final Color Art_GOLD = new Color(255, 205, 60);

    /** Mistral tornado: stacked whirling funnel rings with orbiting debris. */
    private void zoneTornado(Graphics2D g, TrackZone z, double cx, double cy, double R, double A) {
        oval(g, cx, cy, R, R, col(z.rgb, A * 0.16), col(INK, A * 0.35), 2);
        int layers = lod >= 2 ? 2 : 4;
        for (int i = 0; i < layers; i++) {
            double f = 1 - i / (double) layers;
            double rx = R * (0.35 + 0.6 * f), ry = rx * 0.5;
            double ox = Math.sin(time * 3 + i) * R * 0.05;
            double yy = cy + (i - layers / 2.0) * R * 0.22;
            double ph = time * (4 + i) * (i % 2 == 0 ? 1 : -1);
            ell.setFrame(cx + ox - rx, yy - ry, rx * 2, ry * 2);
            g.setStroke(st(5)); g.setColor(col(INK, A * 0.7)); g.draw(ell);
            g.setStroke(dashed(3, rx * 0.9, rx * 0.5, ph * 8));
            g.setColor(col(mix(z.rgb, Color.WHITE, 0.55), A * 0.95)); g.draw(ell);
        }
        if (lod == 0) {
            for (int i = 0; i < 5; i++) {
                double a = time * 3.2 + i * 1.26, rr = R * (0.55 + 0.35 * ((i * 37) % 10) / 10.0);
                oval(g, cx + Math.cos(a) * rr, cy + Math.sin(a) * rr * 0.55, 3, 3, col(Color.WHITE, A * 0.9), col(INK, A * 0.8), 1.5);
            }
        }
    }

    /** Mistral Hurricane: a permanent dark eye with three spiral arms and a gold rim. */
    private void zoneVortex(Graphics2D g, TrackZone z, double cx, double cy, double R, double A) {
        oval(g, cx, cy, R, R, col(z.rgb, A * 0.14), null, 0);
        int arms = lod >= 2 ? 2 : 3;
        for (int k = 0; k < arms; k++) {
            path.reset();
            int pts = lod >= 2 ? 10 : 22;
            double rot = time * 2.2 + k * Math.PI * 2 / arms;
            for (int i = 0; i <= pts; i++) {
                double f = i / (double) pts, rr = R * (0.12 + 0.86 * f), a = rot + f * 3.4;
                double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            outlinedStroke(g, path, col(mix(z.rgb, Color.WHITE, 0.5), A * 0.95), 4);
        }
        oval(g, cx, cy, R * 0.22, R * 0.22, col(new Color(10, 14, 30), A * 0.9), col(INK, A), 2.5);
        ring(g, cx, cy, R - 1.5, col(INK, A * 0.9), 6);
        ring(g, cx, cy, R - 1.5, col(Art_GOLD, A * (0.7 + 0.25 * Math.sin(time * 4))), 3);
    }

    /** DALL-E Outpainting: a dashed canvas frame that keeps growing outwards, rainbow wash and corner brackets. */
    private void zoneOutpaint(Graphics2D g, TrackZone z, double cx, double cy, double R, double A) {
        oval(g, cx, cy, R, R, col(z.rgb, A * 0.22), null, 0);
        if (lod < 2) {
            for (int i = 0; i < 3; i++) {
                double h = (time * 0.15 + i / 3.0) % 1.0;
                oval(g, cx + Math.cos(i * 2.1 + time) * R * 0.3, cy + Math.sin(i * 2.1 + time) * R * 0.3, R * 0.5, R * 0.5,
                    col(Color.getHSBColor((float) h, 0.55f, 1f), A * 0.22), null, 0);
            }
        }
        g.setStroke(st(5));
        ell.setFrame(cx - R + 2, cy - R + 2, R * 2 - 4, R * 2 - 4);
        g.setColor(col(INK, A * 0.9)); g.draw(ell);
        g.setStroke(dashed(2.5, 14, 9, -time * 24));
        g.setColor(col(mix(z.rgb, Color.WHITE, 0.55), A)); g.draw(ell);
        if (lod >= 2) return;
        double s = R * 0.55, c = 10 + 3 * Math.sin(time * 4);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                double x = cx + sx * s, y = cy + sy * s;
                path.reset();
                path.moveTo(x, y - sy * c); path.lineTo(x, y); path.lineTo(x - sx * c, y);
                outlinedStroke(g, path, col(Color.WHITE, A), 2.5);
            }
        }
    }

    /** Mistral Stratosphere Tempest: a dark storm cloud, orbiting gusts and flickering lightning; bosses only. */
    private void zoneTempest(Graphics2D g, TrackZone z, double cx, double cy, double R, double A) {
        oval(g, cx, cy, R, R, col(new Color(36, 44, 88), A * 0.5), null, 0);
        oval(g, cx, cy, R * 0.8, R * 0.8, col(z.rgb, A * 0.16), null, 0);
        int arms = lod >= 2 ? 2 : 4;
        for (int k = 0; k < arms; k++) {
            path.reset();
            double rot = time * 3 + k * Math.PI * 2 / arms;
            for (int i = 0; i <= 14; i++) {
                double f = i / 14.0, rr = R * (0.1 + 0.85 * f), a = rot + f * 2.8;
                double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            outlinedStroke(g, path, col(mix(z.rgb, Color.WHITE, 0.6), A * 0.95), 3.5);
        }
        if (lod < 2) {
            int step = (int) (time * 7);
            for (int b = 0; b < 2; b++) {
                if (((step + b * 3) & 3) == 0) continue;   // flicker
                java.util.Random r = new java.util.Random(step * 31L + b * 977L + seedOf(z));
                double a = r.nextDouble() * Math.PI * 2, x = cx + Math.cos(a) * R * 0.2, y = cy + Math.sin(a) * R * 0.2;
                path.reset();
                path.moveTo(x, y);
                for (int i = 1; i <= 4; i++) {
                    x = cx + Math.cos(a + (r.nextDouble() - 0.5) * 0.7) * R * (0.2 + 0.2 * i);
                    y = cy + Math.sin(a + (r.nextDouble() - 0.5) * 0.7) * R * (0.2 + 0.2 * i);
                    path.lineTo(x, y);
                }
                outlinedStroke(g, path, col(Color.YELLOW, A), 2.5);
            }
        }
        g.setStroke(dashed(3, 12, 8, time * 20));
        ell.setFrame(cx - R + 1.5, cy - R + 1.5, R * 2 - 3, R * 2 - 3);
        g.setColor(col(Cyber.RED, A * 0.9)); g.draw(ell);
    }

    // ------------------------------------------------------------------ Gemini singularity
    private void drawSingularity(Graphics2D g, Sing s) {
        double age = time - s.t0;
        double f = Math.min(1, age / s.hold);
        double out = age > s.hold ? Math.max(0, 1 - (age - s.hold) / 0.5) : 1;
        double R = s.radius * (0.35 + 0.65 * easeOut(Math.min(1, age / 0.3)));
        double A = 255 * out, cx = s.at.x, cy = s.at.y;
        oval(g, cx, cy, R, R, col(new Color(14, 8, 40), A * 0.35), null, 0);
        int arms = lod >= 2 ? 2 : 5;
        for (int k = 0; k < arms; k++) {
            path.reset();
            double rot = time * 5 + k * Math.PI * 2 / arms;
            for (int i = 0; i <= 18; i++) {
                double u = i / 18.0, rr = R * (1.0 - 0.9 * u) * (1 - 0.3 * f), a = rot + u * 4.2;
                double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            outlinedStroke(g, path, col(Color.getHSBColor((float) ((k / (double) arms + time * 0.4) % 1.0), 0.65f, 1f), A), 3.5);
        }
        double core = R * (0.16 + 0.06 * f);
        oval(g, cx, cy, core, core, col(new Color(6, 4, 20), A), col(INK, A), 2.5);
        ring(g, cx, cy, core + 3, col(Color.WHITE, A * 0.9), 2);
        ring(g, cx, cy, R - 1.5, col(INK, A * 0.85), 5);
        ring(g, cx, cy, R - 1.5, col(s.rgb, A * 0.9), 2.5);
    }

    // ------------------------------------------------------------------ walkers
    private void drawWalker(Graphics2D g, TrackWalker w) {
        double age = time - bornOf(w);
        Vec p = w.getPosition();
        double dir = w.isForward() ? 1 : -1;
        Vec ahead = w.route.positionAt(Math.max(0, Math.min(w.route.getTotalLength(), w.distance + dir * 6)));
        double dx = ahead.x - p.x;
        double face = Math.abs(dx) < 0.01 ? 1 : Math.signum(dx);
        double u = Math.max(0.6, w.radius / 14.0) * Math.min(1, 0.4 + age * 3);
        String kind = w.kind == null ? "" : w.kind;
        switch (kind) {
            case "llama": drawLlama(g, p.x, p.y, u, face, age, w.rgb, false); break;
            case "herd": drawLlama(g, p.x, p.y, u, face, age, w.rgb, true); break;
            default: drawCreature(g, p.x, p.y, u, face, age, w.rgb);
        }
    }

    /** DALL-E illustrated creature: a bouncing paint-blob with big eyes and stubby feet. */
    private void drawCreature(Graphics2D g, double x, double y, double u, double face, double age, int rgb) {
        double bob = Math.abs(Math.sin(age * 9)) * 4 * u;
        double sq = 1 + 0.08 * Math.sin(age * 18);
        oval(g, x, y + 10 * u, 11 * u, 4 * u, new Color(0, 0, 0, 60), null, 0);
        double step = Math.sin(age * 12);
        oval(g, x - 5 * u, y + 7 * u - Math.max(0, step) * 3 * u, 3.5 * u, 3 * u, mix(rgb, Color.BLACK, 0.25), INK, 2);
        oval(g, x + 5 * u, y + 7 * u - Math.max(0, -step) * 3 * u, 3.5 * u, 3 * u, mix(rgb, Color.BLACK, 0.25), INK, 2);
        Path2D b = blob(x, y - bob, 11 * u, 0.06, 7, age * 5);
        g.setColor(mix(rgb, Color.WHITE, 0.1)); g.fill(b);
        g.setStroke(st(2.6)); g.setColor(INK); g.draw(b);
        if (lod >= 2) return;
        double ey = y - bob - 2 * u;
        oval(g, x + face * 3 * u - 3.5 * u, ey, 3.8 * u, 4.4 * u * sq, Color.WHITE, INK, 1.6);
        oval(g, x + face * 3 * u + 3.5 * u, ey, 3.8 * u, 4.4 * u * sq, Color.WHITE, INK, 1.6);
        oval(g, x + face * 5 * u - 3.5 * u, ey + 0.5 * u, 1.6 * u, 1.8 * u, INK, null, 0);
        oval(g, x + face * 5 * u + 3.5 * u, ey + 0.5 * u, 1.6 * u, 1.8 * u, INK, null, 0);
        if (lod == 0) {
            gloss(g, x, y - bob, 12 * u);
            oval(g, x - 5 * u + face * 3 * u, y - bob - 12 * u, 2.2 * u, 3 * u, mix(rgb, Color.WHITE, 0.2), INK, 1.5);   // paint drip
        }
    }

    /** Llama (charge) and stampede herd: a cartoon llama with a coloured blanket, dust puffs and speed lines. */
    private void drawLlama(Graphics2D g, double x, double y, double u, double face, double age, int rgb, boolean herd) {
        double run = age * (herd ? 16 : 13);
        double bounce = Math.abs(Math.sin(run)) * 3 * u;
        double cy = y - bounce;
        Color coat = herd ? new Color(238, 200, 140) : CREAM;
        oval(g, x - face * 4 * u, y + 11 * u, 14 * u, 4 * u, new Color(0, 0, 0, 55), null, 0);
        if (lod < 2) {
            for (int i = 0; i < 3; i++) {
                double f = ((age * 3 + i / 3.0) % 1.0);
                oval(g, x - face * (14 + 16 * f) * u, y + 6 * u - f * 5 * u, (4 + 4 * f) * u, (4 + 4 * f) * u, new Color(230, 220, 200, (int) (150 * (1 - f))), null, 0);
            }
            if (herd) {
                for (int i = 0; i < 3; i++) seg(g, x - face * 18 * u, cy - (6 - i * 6) * u, x - face * (30 + 8 * i) * u, cy - (6 - i * 6) * u, new Color(255, 255, 255, 140), 2);
            }
        }
        // legs
        for (int i = 0; i < 4; i++) {
            double ph = run + i * Math.PI / 2;
            double lx = x + face * (-8 + i * 5) * u, sw = Math.sin(ph) * 5 * u;
            seg(g, lx, cy + 4 * u, lx + face * sw, y + 12 * u - Math.max(0, Math.cos(ph)) * 3 * u, INK, 5 * u * 0.7 + 1);
            seg(g, lx, cy + 4 * u, lx + face * sw, y + 12 * u - Math.max(0, Math.cos(ph)) * 3 * u, coat, 3 * u * 0.7 + 0.8);
        }
        // body, neck, head
        oval(g, x, cy, 13 * u, 7.5 * u, coat, INK, 2.4);
        path.reset();
        path.moveTo(x + face * 6 * u, cy - 3 * u);
        path.lineTo(x + face * 10 * u, cy - 15 * u);
        path.lineTo(x + face * 15 * u, cy - 14 * u);
        path.lineTo(x + face * 12 * u, cy);
        path.closePath();
        g.setColor(coat); g.fill(path);
        g.setStroke(st(2.4)); g.setColor(INK); g.draw(path);
        oval(g, x + face * 14.5 * u, cy - 17 * u, 6.5 * u, 4.6 * u, coat, INK, 2.2);
        // ears
        for (int e = 0; e < 2; e++) {
            double ex = x + face * (11 + e * 4) * u;
            path.reset();
            path.moveTo(ex - 1.5 * u, cy - 20 * u); path.lineTo(ex + face * 0.5 * u, cy - 28 * u); path.lineTo(ex + 2 * u, cy - 20 * u); path.closePath();
            g.setColor(coat); g.fill(path);
            g.setStroke(st(2)); g.setColor(INK); g.draw(path);
        }
        oval(g, x + face * 17 * u, cy - 18 * u, 1.2 * u, 1.2 * u, INK, null, 0);
        // blanket in the tower colour
        Color blanket = new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255);
        oval(g, x - face * 1 * u, cy - 5.5 * u, 6 * u, 3.2 * u, blanket, INK, 2);
        if (lod == 0) gloss(g, x - 2 * u, cy, 12 * u);
        if (herd && lod < 2) oval(g, x + face * 3 * u, cy - 3 * u, 3 * u, 3 * u, Art_GOLD, INK, 1.5);   // herd leader star-ish medal
    }

    // ------------------------------------------------------------------ drones
    private void drawDrone(Graphics2D g, Drone d) {
        double age = time - bornOf(d);
        Vec p = d.getPosition();
        String kind = d.kind == null ? "" : d.kind;
        double pop = Math.min(1, 0.3 + age * 4);
        double rem = d.lifetime > 0 ? d.lifetime - d.getAge() : 99;
        double blink = rem < 1.5 && ((int) (time * 8) & 1) == 0 ? 0.45 : 1;   // expiring drones blink
        java.awt.Composite c0 = beginAlpha(g, blink);
        double bob = Math.sin(time * 5 + seedOf(d)) * 2;
        double x = p.x, y = p.y + bob;
        switch (kind) {
            case "companion": drawBot(g, x, y, 9 * pop, d.rgb, true, false); break;
            case "buddy": drawBot(g, x, y, 6.5 * pop, d.rgb, false, true); break;
            case "qwen": drawQwenDrone(g, x, y, 8 * pop, d.rgb); break;
            case "clone": drawClone(g, x, y, 9 * pop, d.rgb); break;
            default: drawBot(g, x, y, 7 * pop, d.rgb, false, false);
        }
        g.setComposite(c0);
    }

    private java.awt.Composite beginAlpha(Graphics2D g, double a) {
        java.awt.Composite old = g.getComposite();
        if (a < 0.999) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) a));
        return old;
    }

    /** Round robot with a spinning rotor, a glowing lens "eye" and a gloss; the study buddy has a pencil antenna. */
    private void drawBot(Graphics2D g, double x, double y, double r, int rgb, boolean halo, boolean pencil) {
        Color body = mix(rgb, Color.WHITE, 0.2);
        oval(g, x + 2, y + r + 5, r * 0.9, r * 0.35, new Color(0, 0, 0, 55), null, 0);
        if (halo && lod < 2) ring(g, x, y, r + 5 + Math.sin(time * 6), col(rgb, 90), 2);
        // rotor
        double spin = Math.abs(Math.cos(time * 28));
        oval(g, x, y - r - 2, (r + 5) * (0.25 + 0.75 * spin), 2, new Color(255, 255, 255, 190), INK, 1.2);
        seg(g, x, y - r + 1, x, y - r - 2, INK, 2);
        if (pencil) {
            seg(g, x + r * 0.7, y - r * 0.6, x + r * 1.5, y - r * 1.5, INK, 4);
            seg(g, x + r * 0.7, y - r * 0.6, x + r * 1.5, y - r * 1.5, Art_GOLD, 2);
        }
        oval(g, x, y, r, r, body, INK, 2.4);
        oval(g, x, y + r * 0.1, r * 0.55, r * 0.5, new Color(12, 18, 40), INK, 1.6);
        oval(g, x, y + r * 0.1, r * 0.3, r * 0.3, mix(rgb, Color.WHITE, 0.7), null, 0);
        if (lod == 0) gloss(g, x, y, r * 1.1);
    }

    private void drawQwenDrone(Graphics2D g, double x, double y, double r, int rgb) {
        oval(g, x + 2, y + r + 5, r, r * 0.35, new Color(0, 0, 0, 55), null, 0);
        path.reset();
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 6 + i * Math.PI / 3, rr = r * (i % 2 == 0 ? 1.15 : 1.0);
            double px = x + Math.cos(a) * rr, py = y + Math.sin(a) * rr;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        path.closePath();
        g.setColor(mix(rgb, Color.WHITE, 0.15)); g.fill(path);
        g.setStroke(st(2.4)); g.setColor(INK); g.draw(path);
        if (lod < 2) {
            double sp = time * 4;
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[] { 5f, 4f }, (float) dashPhase(sp * 6)));
            ell.setFrame(x - r - 4, y - r - 4, (r + 4) * 2, (r + 4) * 2);
            g.setColor(col(Art_GOLD, 220)); g.draw(ell);
        }
        oval(g, x, y, r * 0.4, r * 0.4, Art_GOLD, INK, 1.6);
        if (lod == 0) gloss(g, x, y, r * 1.1);
    }

    /** MiniMax clone: a semi-transparent ghost of the tower orb with a wavy skirt and two eyes. */
    private void drawClone(Graphics2D g, double x, double y, double r, int rgb) {
        double shimmer = 0.62 + 0.18 * Math.sin(time * 6);
        java.awt.Composite old = beginAlpha(g, shimmer);
        Color body = mix(rgb, Color.WHITE, 0.25);
        path.reset();
        path.moveTo(x - r, y + r * 0.9);
        path.lineTo(x - r, y);
        path.quadTo(x - r, y - r * 1.3, x, y - r * 1.3);
        path.quadTo(x + r, y - r * 1.3, x + r, y);
        path.lineTo(x + r, y + r * 0.9);
        int teeth = 4;
        for (int i = 0; i < teeth; i++) {
            double tx = x + r - (i + 0.5) * 2 * r / teeth;
            path.lineTo(tx, y + r * (0.55 + 0.1 * Math.sin(time * 8 + i)));
            path.lineTo(x + r - (i + 1) * 2 * r / teeth, y + r * 0.9);
        }
        path.closePath();
        g.setColor(body); g.fill(path);
        g.setStroke(st(2.4)); g.setColor(INK); g.draw(path);
        oval(g, x - r * 0.38, y - r * 0.25, r * 0.22, r * 0.3, Color.WHITE, INK, 1.4);
        oval(g, x + r * 0.38, y - r * 0.25, r * 0.22, r * 0.3, Color.WHITE, INK, 1.4);
        oval(g, x - r * 0.34, y - r * 0.2, r * 0.1, r * 0.14, INK, null, 0);
        oval(g, x + r * 0.42, y - r * 0.2, r * 0.1, r * 0.14, INK, null, 0);
        if (lod == 0) gloss(g, x, y - r * 0.2, r * 1.1);
        g.setComposite(old);
    }

    // ------------------------------------------------------------------ GLM nodes
    private void drawGrid(Graphics2D g, NodeHazard h) {
        if (h.region == null || h.region.size() < 3) return;
        double pop = easeOut(Math.min(1, (time - bornOf(h)) / 0.4));
        path.reset();
        for (int i = 0; i < h.region.size(); i++) {
            Vec v = h.region.get(i);
            if (i == 0) path.moveTo(v.x, v.y); else path.lineTo(v.x, v.y);
        }
        path.closePath();
        g.setColor(col(h.rgb, 38 * pop)); g.fill(path);
        if (lod == 0) {
            Shape oldClip = g.getClip();
            g.clip(path);
            g.setStroke(st(1));
            g.setColor(col(h.rgb, 70 * pop));
            java.awt.Rectangle b = path.getBounds();
            double off = (time * 8) % 22;
            for (double yy = b.y - 22 + off; yy < b.y + b.height; yy += 22) { line.setLine(b.x, yy, b.x + b.width, yy); g.draw(line); }
            for (double xx = b.x - 22 + off; xx < b.x + b.width; xx += 22) { line.setLine(xx, b.y, xx, b.y + b.height); g.draw(line); }
            g.setClip(oldClip);
        }
    }

    private void drawLinksAndNodes(Graphics2D g, NodeHazard h) {
        double pop = easeOut(Math.min(1, (time - bornOf(h)) / 0.4));
        double fade = h.permanent ? 1 : Math.max(0, Math.min(1, h.duration / 0.6));
        double A = 255 * pop * fade;
        for (int[] l : h.links) {
            Vec a = h.nodes.get(l[0]), b = h.nodes.get(l[1]);
            double w = h.linkWidth;
            seg(g, a.x, a.y, b.x, b.y, col(INK, A * 0.85), w + 4);
            seg(g, a.x, a.y, b.x, b.y, col(h.rgb, A), w + 0.5);
            seg(g, a.x, a.y, b.x, b.y, col(Color.WHITE, A * (0.75 + 0.25 * Math.sin(time * 18))), 1.6);
            if (lod < 2) {
                double len = Math.hypot(b.x - a.x, b.y - a.y);
                for (int k = 0; k < 2; k++) {
                    double f = (time * 0.7 + k * 0.5) % 1.0;
                    oval(g, a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f, 3.2, 3.2, col(Color.WHITE, A), col(INK, A * 0.8), 1.2);
                }
                if (len < 1) continue;
            }
        }
        for (Vec n : h.nodes) {
            double r = 9 * pop;
            if (lod < 2) ring(g, n.x, n.y, r + 4 + 2 * Math.sin(time * 5), col(h.rgb, A * 0.45), 2);
            oval(g, n.x, n.y, r, r, mix(h.rgb, Color.WHITE, 0.15), col(INK, A), 2.6);
            oval(g, n.x, n.y, r * 0.5, r * 0.5, col(Color.WHITE, A), null, 0);
            if (lod == 0) gloss(g, n.x, n.y, r * 1.1);
        }
    }

    // ------------------------------------------------------------------ GPT moons and Astra Beam
    private void drawMoons(Graphics2D g, Tower t) {
        TowerStats s = t.getStats();
        if (!s.has("moonCount")) return;
        int n = s.pi("moonCount");
        double size = s.p("moonSize", 8);
        Vec c = t.getPosition();
        if (lod == 0) {
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[] { 3f, 7f }, 0f));
            ell.setFrame(c.x - s.p("moonRadius"), c.y - s.p("moonRadius"), s.p("moonRadius") * 2, s.p("moonRadius") * 2);
            g.setColor(col(t.getType().secondaryRgb, 70)); g.draw(ell);
        }
        for (int i = 0; i < n; i++) {
            Vec m = GptBehavior.moonPosition(t, i);
            if (m == null) continue;
            if (lod < 2) oval(g, m.x, m.y, size + 5, size + 5, col(t.getType().secondaryRgb, 55), null, 0);
            oval(g, m.x + 1.5, m.y + size + 3, size * 0.9, size * 0.3, new Color(0, 0, 0, 50), null, 0);
            oval(g, m.x, m.y, size, size, new Color(246, 236, 196), INK, 2.4);
            if (lod < 2) {
                oval(g, m.x - size * 0.3, m.y - size * 0.15, size * 0.22, size * 0.22, new Color(214, 200, 160), null, 0);
                oval(g, m.x + size * 0.25, m.y + size * 0.3, size * 0.16, size * 0.16, new Color(214, 200, 160), null, 0);
                // night-side shade
                oval(g, m.x + size * 0.35, m.y + size * 0.2, size * 0.6, size * 0.75, new Color(120, 110, 150, 70), null, 0);
            }
            if (lod == 0) gloss(g, m.x, m.y, size * 1.1);
        }
    }

    private void drawAstraBeam(Graphics2D g, Tower t) {
        Object o = t.state().get("beamTarget");
        if (!(o instanceof Enemy)) return;
        Enemy e = (Enemy) o;
        if (!e.isAlive() || e.isStored()) return;
        Vec a = t.getPosition(), b = e.getPosition();
        int rgb = t.getType().primaryRgb;
        double pulse = 0.8 + 0.2 * Math.sin(time * 40);
        seg(g, a.x, a.y, b.x, b.y, col(rgb, 70), 20 * pulse);
        seg(g, a.x, a.y, b.x, b.y, col(INK, 200), 11 * pulse);
        seg(g, a.x, a.y, b.x, b.y, col(mix(rgb, Color.WHITE, 0.35), 255), 8 * pulse);
        seg(g, a.x, a.y, b.x, b.y, col(Color.WHITE, 255), 3.5);
        ring(g, a.x, a.y, 16 + 3 * Math.sin(time * 20), col(rgb, 230), 3);
        oval(g, a.x, a.y, 9, 9, col(Color.WHITE, 210), null, 0);
        double r = 13 + 4 * Math.sin(time * 30);
        oval(g, b.x, b.y, r, r, col(rgb, 120), col(INK, 200), 2);
        oval(g, b.x, b.y, r * 0.5, r * 0.5, Color.WHITE, null, 0);
        if (lod < 2) {
            for (int i = 0; i < 4; i++) {
                double an = time * 9 + i * Math.PI / 2;
                seg(g, b.x + Math.cos(an) * r, b.y + Math.sin(an) * r, b.x + Math.cos(an) * (r + 8), b.y + Math.sin(an) * (r + 8), col(Color.WHITE, 230), 2);
            }
        }
    }

    // ------------------------------------------------------------------ Copilot strike drone
    /** A little drone hovering over a Copilot tower; its charge ring fills until the next barrage is dropped. */
    private void drawCopilotDrone(Graphics2D g, Tower t) {
        TowerStats s = t.getStats();
        double iv = s.p("droneInterval");
        double charge = Math.max(0, Math.min(1, t.counter("drone") / iv));
        Vec c = t.getPosition();
        double ang = time * 1.6 + t.getId();
        double x = c.x + Math.cos(ang) * 30, y = c.y - 34 + Math.sin(ang) * 6 + Math.sin(time * 5) * 2;
        double r = 7;
        oval(g, x + 3, y + 22, r * 1.1, r * 0.4, new Color(0, 0, 0, 50), null, 0);
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            double spin = Math.abs(Math.cos(time * 30 + sgn));
            oval(g, x + sgn * (r + 6), y - 2, 6 * (0.3 + 0.7 * spin), 1.8, new Color(255, 255, 255, 190), INK, 1.2);
            seg(g, x + sgn * r * 0.6, y, x + sgn * (r + 6), y - 2, INK, 2.2);
        }
        oval(g, x, y, r, r * 0.8, mix(t.getType().primaryRgb, Color.WHITE, 0.2), INK, 2.4);
        oval(g, x, y + 1, r * 0.4, r * 0.36, charge > 0.85 ? Cyber.RED : new Color(12, 18, 40), INK, 1.4);
        if (lod == 0) gloss(g, x, y, r * 1.15);
        // bomb-bay charge: arc under the drone
        if (lod < 2) {
            g.setStroke(st(2.5));
            g.setColor(col(INK, 220));
            path.reset();
            java.awt.geom.Arc2D.Double arc = new java.awt.geom.Arc2D.Double(x - r - 3, y - r - 3, (r + 3) * 2, (r + 3) * 2, 90, -360 * charge, java.awt.geom.Arc2D.OPEN);
            g.setStroke(st(5)); g.setColor(INK); g.draw(arc);
            g.setStroke(st(2.5)); g.setColor(charge > 0.85 ? Cyber.RED : Cyber.YELLOW); g.draw(arc);
        }
    }
}
