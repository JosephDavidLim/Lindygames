package lindefense.ui.sprites;

import acm.graphics.GArc;
import acm.graphics.GCompound;
import acm.graphics.GFillable;
import acm.graphics.GLabel;
import acm.graphics.GLine;
import acm.graphics.GObject;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import lindefense.enemy.Enemy;
import lindefense.enemy.EnemyType;
import lindefense.sim.Vec;

/**
 * Base class for every enemy character model.
 *
 * <p>A sprite is a {@link GCompound} whose origin sits at the enemy's world
 * position. Subclasses build their body <em>once</em> in the constructor using
 * the {@link #part}, {@link #fixed}, {@link #stroke} and {@link #rpoly}
 * helpers, and then only mutate those objects from {@link #updateBody}.</p>
 *
 * <p>This class owns everything that is common to all enemies:</p>
 * <ul>
 *   <li>a soft drop shadow (light comes from the top-left),</li>
 *   <li>colour management: every registered part is re-tinted in one pass when
 *       the enemy's damage, slow, hit-flash, camo or revealed state changes,</li>
 *   <li>modifier overlays: Camo (alpha), Regrow (pulsing glow and rising motes),
 *       Fortified (steel band and rivets),</li>
 *   <li>status overlays: slowed (frost ring), stunned (orbiting stars),
 *       marked (red reticle), revealed (white ring), disrupted (magenta glitch),</li>
 *   <li>facing: rotation of polygons registered with {@link #rpoly} and
 *       re-placement of objects registered with {@link #orbit}.</li>
 * </ul>
 */
@SuppressWarnings({ "serial", "this-escape" })
public abstract class EnemySprite extends GCompound implements Animated {
    /** Logical radius the model was built for. */
    protected final double r;
    /** The enemy type this model represents. */
    protected final EnemyType type;

    /** Direction of travel (unit vector) from the last sync. */
    protected double dirX = 1, dirY = 0;
    /** Facing angle in ACM degrees (counter-clockwise, 0 = +x). */
    protected double facingDeg = 0;
    /** Time of the last sync, in seconds. */
    protected double now;
    /** HP fraction from the last sync. */
    protected double hpFrac = 1;
    /** Damage fraction (1 - hpFrac) from the last sync. */
    protected double dmg = 0;
    /** Whether the mirrored enemy was alive at the last sync. */
    protected boolean alive = true;
    /**
     * 0..1: how far a camo enemy is currently pushed back towards full opacity
     * (decloak flashes). Subclasses may set this in {@link #updateBody}.
     */
    protected double decloak = 0;

    // ---- part registry (for one-pass re-tinting) ----
    private static final int TINT_FULL = 1;
    private final List<GObject> parts = new ArrayList<>();
    private final List<Color> baseFill = new ArrayList<>();
    private final List<Color> baseLine = new ArrayList<>();
    private final List<Integer> tintFlags = new ArrayList<>();
    private int lastTintKey = Integer.MIN_VALUE;

    // ---- rotation registry ----
    private final List<GPolygon> rpolys = new ArrayList<>();
    private final List<GObject> orbiters = new ArrayList<>();
    private final List<double[]> orbitLocal = new ArrayList<>();
    private double appliedDeg = 0;

    // ---- common overlays ----
    private final GOval shadow;
    private GOval regrowGlow;
    private GOval[] regrowMotes;
    private GArc fortBandA, fortBandB;
    private GOval[] rivets;
    private GOval frostRing;
    private GLine[] frostFlake;
    private GPolygon[] stunStars;
    private GOval markRing;
    private GLine[] markTicks;
    private GOval revealRing;
    private GPolygon disruptZig;
    private boolean overlaysBuilt;
    private boolean lastCamo, lastRegrow, lastFort;
    private boolean lastSlow, lastStun, lastMark, lastReveal, lastDisrupt;

    /**
     * Creates the shadow and remembers size/type. Subclasses must build their
     * body afterwards and may call {@link #finishOverlays()}; if they do not,
     * the overlays are created lazily at the first {@link #sync}.
     */
    protected EnemySprite(Enemy e) {
        this.type = e.getType();
        this.r = Math.max(4, e.getRadius());
        shadow = Art.ovalAt(r * 0.2, r * 0.95, r * 2.0, r * 0.75);
        shadow.setFilled(true);
        shadow.setFillColor(Art.SHADOW);
        shadow.setColor(new Color(0, 0, 0, 0));
        ((Thick.Oval) shadow).setLineWidth(0);
        add(shadow);
        if (e.isRegrow()) buildRegrow();
    }

    // =====================================================================
    // Build helpers (constructor-time only)
    // =====================================================================

    /** Adds a filled, outlined part that takes full damage/slow/flash tinting. */
    protected <T extends GObject> T part(T g, Color fill) { return part(g, fill, Art.outline(fill)); }

    /** Adds a part with explicit fill and outline that takes full tinting. */
    protected <T extends GObject> T part(T g, Color fill, Color line) { return register(g, fill, line, TINT_FULL); }

    /** Adds a part that only receives the camo alpha (highlights, glows, emblems). */
    protected <T extends GObject> T fixed(T g, Color fill, Color line) { return register(g, fill, line, 0); }

    /** Adds an unfilled stroke-only object (GLine, unfilled polygon, label). */
    protected <T extends GObject> T stroke(T g, Color line) { return register(g, null, line, TINT_FULL); }

    /** Adds a stroke-only object that is not damage-tinted. */
    protected <T extends GObject> T fixedStroke(T g, Color line) { return register(g, null, line, 0); }

    /** Adds a top-left highlight blob (not tinted, no outline). */
    protected GOval highlight(double cx, double cy, double w, double h) {
        Thick.Oval o = Art.ovalAt(cx, cy, w, h);
        o.setLineWidth(0);
        return fixed(o, Art.HIGHLIGHT, new Color(255, 255, 255, 0));
    }

    /**
     * Cartoon gloss for a body of the given size centred at (cx,cy): a big
     * highlight blob at the upper-left plus a small secondary sparkle.
     */
    protected GOval gloss(double cx, double cy, double w, double h) {
        GOval big = highlight(cx - w * 0.2, cy - h * 0.25, w * 0.4, h * 0.28);
        highlight(cx - w * 0.36, cy + h * 0.02, w * 0.11, h * 0.11);
        return big;
    }

    /** Marks a registered part as a small detail: thin outline. */
    protected <T extends GObject> T thin(T g) {
        if (g instanceof Thick.Outlined) ((Thick.Outlined) g).setLineWidth(Thick.DETAIL);
        return g;
    }

    /** Sets a custom outline width on a part. */
    protected <T extends GObject> T lineWidth(T g, double w) {
        if (g instanceof Thick.Outlined) ((Thick.Outlined) g).setLineWidth(w);
        return g;
    }

    /**
     * Chunky shaded oval body: a dark rim with the thick outline, and a lighter
     * inner oval offset towards the light. Returns the rim (the outlined shape).
     */
    protected GOval bodyOval(double cx, double cy, double w, double h, Color fill) {
        GOval rim = part(Art.ovalAt(cx, cy, w, h), Art.shade(fill, 0.3));
        Thick.Oval inner = Art.ovalAt(cx - w * 0.04, cy - h * 0.06, w * 0.84, h * 0.82);
        inner.setLineWidth(0);
        part(inner, fill, fill);
        return rim;
    }

    /**
     * Chunky shaded rectangle body: dark rim with the thick outline and a
     * lighter inner face offset towards the light. Returns the rim.
     */
    protected GRect bodyRect(double x, double y, double w, double h, Color fill) {
        GRect rim = part(Art.rect(x, y, w, h), Art.shade(fill, 0.3));
        Thick.Rect inner = Art.rect(x + w * 0.05, y + h * 0.06, w * 0.86, h * 0.72);
        inner.setLineWidth(0);
        part(inner, fill, fill);
        return rim;
    }

    /**
     * Chunky shaded polygon body from flat x,y vertex pairs (relative to the
     * sprite origin): the dark rim with the thick outline plus a lighter inner
     * face shrunk towards the polygon's centroid and nudged towards the light.
     * Returns the rim.
     */
    protected GPolygon bodyPoly(Color fill, double... xy) {
        return bodyPolyParts(fill, xy)[0];
    }

    /** As {@link #bodyPoly} but returns both pieces: {rim, inner face}. */
    protected GPolygon[] bodyPolyParts(Color fill, double... xy) {
        GPolygon rim = part(Art.poly(xy), Art.shade(fill, 0.3));
        int n = xy.length / 2;
        double cx = 0, cy = 0, minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
        for (int i = 0; i < n; i++) {
            cx += xy[2 * i] / n; cy += xy[2 * i + 1] / n;
            minX = Math.min(minX, xy[2 * i]); maxX = Math.max(maxX, xy[2 * i]);
            minY = Math.min(minY, xy[2 * i + 1]); maxY = Math.max(maxY, xy[2 * i + 1]);
        }
        Thick.Poly inner = new Thick.Poly();
        inner.setLineWidth(0);
        for (int i = 0; i < n; i++) {
            inner.addVertex(cx + (xy[2 * i] - cx) * 0.8 - (maxX - minX) * 0.03,
                cy + (xy[2 * i + 1] - cy) * 0.78 - (maxY - minY) * 0.05);
        }
        part(inner, fill, fill);
        return new GPolygon[] { rim, inner };
    }

    /**
     * Two cartoon eyes (white with dark pupils) centred at (cx,cy).
     * Returns {leftWhite, leftPupil, rightWhite, rightPupil} so callers can move them.
     */
    protected GOval[] eyes(double cx, double cy, double spacing, double size) {
        GOval[] out = new GOval[4];
        for (int i = 0; i < 2; i++) {
            double ex = cx + (i == 0 ? -1 : 1) * spacing / 2;
            Thick.Oval white = Art.ovalAt(ex, cy, size, size * 1.1);
            white.setLineWidth(Thick.DETAIL);
            out[2 * i] = fixed(white, Color.WHITE, Art.OUTLINE);
            Thick.Oval pupil = Art.ovalAt(ex + size * 0.12, cy + size * 0.08, size * 0.45, size * 0.5);
            pupil.setLineWidth(0);
            out[2 * i + 1] = fixed(pupil, Art.OUTLINE, Art.OUTLINE);
        }
        return out;
    }

    private <T extends GObject> T register(T g, Color fill, Color line, int flags) {
        if (g instanceof Thick.Outlined) {
            Thick.Outlined o = (Thick.Outlined) g;
            if (o.getLineWidth() < 0) {
                boolean filled = fill != null && !(g instanceof GLine);
                o.setLineWidth(filled ? (type.isBoss() ? Thick.BOSS : Thick.NORMAL) : Thick.LINE);
            }
        }
        if (g instanceof GFillable) {
            GFillable f = (GFillable) g;
            f.setFilled(fill != null);
            if (fill != null) f.setFillColor(fill);
        }
        g.setColor(line != null ? line : (fill != null ? fill : Art.OUTLINE));
        parts.add(g);
        baseFill.add(fill);
        baseLine.add(line != null ? line : fill);
        tintFlags.add(flags);
        add(g);
        return g;
    }

    /** Replaces the base colours of a registered part (e.g. Radix phase change). */
    protected void recolor(GObject g, Color fill, Color line) {
        int i = parts.indexOf(g);
        if (i < 0) return;
        baseFill.set(i, fill);
        baseLine.set(i, line != null ? line : fill);
        lastTintKey = Integer.MIN_VALUE;
    }

    /**
     * Creates a polygon whose vertices are given in ship-local coordinates
     * around the sprite origin and registers it so {@link #setFacing} rotates
     * it with the rest of the model.
     */
    protected GPolygon rpoly(Color fill, double... xy) {
        GPolygon p = Art.poly(xy);
        rpolys.add(p);
        return fill == null ? stroke(p, Art.OUTLINE) : part(p, fill);
    }

    /** Registers an already-built polygon (vertices around the origin) for rotation. */
    protected <T extends GPolygon> T rotating(T p) { rpolys.add(p); return p; }

    /** Registers a non-polygon object to be kept at the rotated local position (lx,ly). */
    protected <T extends GObject> T orbit(T g, double lx, double ly) {
        orbiters.add(g);
        orbitLocal.add(new double[] { lx, ly, g.getWidth(), g.getHeight() });
        placeOrbiter(orbiters.size() - 1, 0);
        return g;
    }

    /** Updates the local anchor of an orbiting object. */
    protected void setOrbit(GObject g, double lx, double ly) {
        int i = orbiters.indexOf(g);
        if (i < 0) return;
        double[] o = orbitLocal.get(i);
        o[0] = lx; o[1] = ly; o[2] = g.getWidth(); o[3] = g.getHeight();
        placeOrbiter(i, appliedDeg);
    }

    private void placeOrbiter(int i, double deg) {
        double[] o = orbitLocal.get(i);
        double a = Math.toRadians(deg);
        // ACM rotation is counter-clockwise on screen, so y is negated.
        double x = o[0] * Math.cos(a) + o[1] * Math.sin(a);
        double y = -o[0] * Math.sin(a) + o[1] * Math.cos(a);
        GObject g = orbiters.get(i);
        if (g instanceof GLabel) g.setLocation(x - o[2] / 2, y + o[3] / 3);
        else if (g instanceof GCompound) g.setLocation(x, y);
        else g.setLocation(x - o[2] / 2, y - o[3] / 2);
    }

    /** Local (x,y) in ship space to sprite space using the current facing. */
    protected double[] local(double lx, double ly) {
        double a = Math.toRadians(appliedDeg);
        return new double[] { lx * Math.cos(a) + ly * Math.sin(a), -lx * Math.sin(a) + ly * Math.cos(a) };
    }

    /** Rotates all registered polygons and re-places orbiters to face {@code deg}. */
    protected void setFacing(double deg) {
        if (Math.abs(deg - appliedDeg) < 0.25) return;
        double delta = deg - appliedDeg;
        for (GPolygon p : rpolys) p.rotate(delta);
        appliedDeg = deg;
        for (int i = 0; i < orbiters.size(); i++) placeOrbiter(i, deg);
    }

    /** Builds crack slivers around the body, hidden until damage reveals them. */
    protected GPolygon[] makeCracks(int n, double radius) {
        GPolygon[] cs = new GPolygon[n];
        for (int i = 0; i < n; i++) {
            double a = 35 + i * (360.0 / n) + (i % 2) * 20;
            double sx = Math.cos(Math.toRadians(a)) * radius * 0.25;
            double sy = Math.sin(Math.toRadians(a)) * radius * 0.25;
            cs[i] = Art.crack(sx, sy, radius * 0.7, a, i + 3);
            fixed(cs[i], Art.OUTLINE, Art.OUTLINE);
            cs[i].setVisible(false);
        }
        return cs;
    }

    /** Shows the first {@code floor(dmg * n)} cracks. */
    protected static void showCracks(GPolygon[] cracks, double dmg) {
        int show = (int) Math.floor(dmg * cracks.length + 1e-6);
        for (int i = 0; i < cracks.length; i++) cracks[i].setVisible(i < show);
    }

    /** Layer count still intact for an enemy with {@code layers} segments. */
    protected static int layersLeft(Enemy e, int layers) {
        return (int) Math.ceil(e.getHpFraction() * layers - 1e-6);
    }

    // =====================================================================
    // Overlays
    // =====================================================================

    private void buildRegrow() {
        regrowGlow = Art.ovalAt(0, 0, r * 2.7, r * 2.7);
        regrowGlow.setFilled(true);
        regrowGlow.setFillColor(Art.alpha(Art.REGROW, 60));
        regrowGlow.setColor(Art.alpha(Art.REGROW, 0));
        add(regrowGlow);
        regrowMotes = new GOval[3];
        for (int i = 0; i < 3; i++) {
            regrowMotes[i] = Art.ovalAt(0, 0, r * 0.22, r * 0.22);
            regrowMotes[i].setFilled(true);
            regrowMotes[i].setFillColor(Art.alpha(Art.REGROW, 220));
            regrowMotes[i].setColor(Art.alpha(Art.REGROW, 220));
            add(regrowMotes[i]);
        }
    }

    /**
     * Creates the fortified plating and status overlays on top of the body.
     * Called automatically on first sync if a subclass does not call it.
     */
    protected final void finishOverlays() {
        if (overlaysBuilt) return;
        overlaysBuilt = true;
        double R = r * (type.isBoss() ? 1.06 : 1.12);
        double bandW = type.isBoss() ? r * 0.09 : r * 0.26;
        // Fortified: steel band (two arcs for weight) + rivets
        fortBandA = Art.arc(-R, -R, 2 * R, 2 * R, 0, 360);
        fortBandA.setColor(Art.OUTLINE);
        ((Thick.Arc) fortBandA).setLineWidth(bandW);
        fortBandB = Art.arc(-R, -R, 2 * R, 2 * R, 0, 360);
        fortBandB.setColor(Art.STEEL);
        ((Thick.Arc) fortBandB).setLineWidth(bandW * 0.5);
        add(fortBandA); add(fortBandB);
        rivets = new GOval[6];
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(i * 60 + 30);
            rivets[i] = Art.ovalAt(Math.cos(a) * R, Math.sin(a) * R, type.isBoss() ? r * 0.1 : r * 0.26, type.isBoss() ? r * 0.1 : r * 0.26);
            rivets[i].setFilled(true);
            rivets[i].setFillColor(Art.STEEL);
            rivets[i].setColor(Art.OUTLINE);
            ((Thick.Oval) rivets[i]).setLineWidth(Thick.DETAIL);
            add(rivets[i]);
        }
        setFort(false);
        // Slowed: frost ring + snowflake
        frostRing = Art.ovalAt(0, 0, r * 2.5, r * 2.5);
        frostRing.setColor(Art.alpha(Art.FROST, 230));
        ((Thick.Oval) frostRing).setLineWidth(2.0);
        add(frostRing);
        frostFlake = new GLine[3];
        double fx = -r * 0.95, fy = -r * 1.05, fl = r * 0.32;
        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(i * 60);
            frostFlake[i] = Art.line(fx - Math.cos(a) * fl, fy - Math.sin(a) * fl, fx + Math.cos(a) * fl, fy + Math.sin(a) * fl);
            frostFlake[i].setColor(Color.WHITE);
            ((Thick.Line) frostFlake[i]).setLineWidth(1.6);
            add(frostFlake[i]);
        }
        // Stunned: orbiting stars
        stunStars = new GPolygon[3];
        for (int i = 0; i < 3; i++) {
            stunStars[i] = Art.star(r * 0.26, r * 0.11);
            stunStars[i].setFilled(true);
            stunStars[i].setFillColor(Art.STUN);
            stunStars[i].setColor(Art.OUTLINE);
            ((Thick.Poly) stunStars[i]).setLineWidth(Thick.DETAIL);
            add(stunStars[i]);
        }
        // Marked: red reticle
        markRing = Art.ovalAt(0, 0, r * 2.3, r * 2.3);
        markRing.setColor(Art.MARK);
        ((Thick.Oval) markRing).setLineWidth(2.0);
        add(markRing);
        markTicks = new GLine[4];
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(i * 90);
            double x1 = Math.cos(a) * r * 0.9, y1 = Math.sin(a) * r * 0.9;
            double x2 = Math.cos(a) * r * 1.45, y2 = Math.sin(a) * r * 1.45;
            markTicks[i] = Art.line(x1, y1, x2, y2);
            markTicks[i].setColor(Art.MARK);
            ((Thick.Line) markTicks[i]).setLineWidth(2.0);
            add(markTicks[i]);
        }
        // Revealed: white ring
        revealRing = Art.ovalAt(0, 0, r * 2.15, r * 2.15);
        revealRing.setColor(Art.REVEAL);
        ((Thick.Oval) revealRing).setLineWidth(1.8);
        add(revealRing);
        // Disrupted: magenta glitch zig-zag
        disruptZig = Art.poly(-r, 0, -r * 0.5, -r * 0.45, 0, 0.2 * r, r * 0.5, -r * 0.4, r, 0.1 * r,
            r * 0.5, -r * 0.25, 0, 0.35 * r, -r * 0.5, -r * 0.3);
        disruptZig.setFilled(true);
        disruptZig.setFillColor(Art.alpha(Art.DISRUPT, 150));
        disruptZig.setColor(Art.DISRUPT);
        ((Thick.Poly) disruptZig).setLineWidth(1.6);
        add(disruptZig);
        setStatusVisible(false, false, false, false, false);
    }

    private void setFort(boolean on) {
        if (on == lastFort && fortBandA.isVisible() == on) return;
        lastFort = on;
        fortBandA.setVisible(on); fortBandB.setVisible(on);
        for (GOval o : rivets) o.setVisible(on);
    }

    private void setStatusVisible(boolean slow, boolean stun, boolean mark, boolean reveal, boolean disrupt) {
        if (frostRing.isVisible() != slow) {
            frostRing.setVisible(slow);
            for (GLine l : frostFlake) l.setVisible(slow);
        }
        if (stunStars[0].isVisible() != stun) for (GPolygon s : stunStars) s.setVisible(stun);
        if (markRing.isVisible() != mark) {
            markRing.setVisible(mark);
            for (GLine l : markTicks) l.setVisible(mark);
        }
        if (revealRing.isVisible() != reveal) revealRing.setVisible(reveal);
        if (disruptZig.isVisible() != disrupt) disruptZig.setVisible(disrupt);
        lastSlow = slow; lastStun = stun; lastMark = mark; lastReveal = reveal; lastDisrupt = disrupt;
    }

    // =====================================================================
    // Per-frame
    // =====================================================================

    /**
     * Mirrors the enemy's current state: position, facing, animation frame,
     * damage state, modifier overlays and status cues.
     *
     * @param e           the enemy being drawn
     * @param timeSeconds game time in seconds, used to drive animation
     */
    public void sync(Enemy e, double timeSeconds) {
        if (!overlaysBuilt) finishOverlays();
        now = timeSeconds;
        alive = e.isAlive();
        hpFrac = Math.max(0, Math.min(1, e.getHpFraction()));
        dmg = 1 - hpFrac;
        Vec p = e.getPosition();
        setLocation(p.x, p.y);
        if (e.getRoute() != null) {
            Vec d = e.getRoute().directionAt(e.getDistance());
            if (d.x != 0 || d.y != 0) {
                dirX = d.x; dirY = d.y;
                facingDeg = Math.toDegrees(Math.atan2(-d.y, d.x));
            }
        }

        updateBody(e, timeSeconds);

        boolean camo = e.isCamo();
        boolean revealed = camo && e.isRevealed();
        boolean slow = e.getSlowFraction() > 0;
        boolean flash = timeSeconds - e.getLastHitTime() < 0.11;
        retint(camo, revealed, slow, flash);

        // modifiers
        setFort(e.isFortified());
        if (regrowGlow != null) {
            boolean on = e.isRegrow();
            if (on != lastRegrow || on) {
                lastRegrow = on;
                regrowGlow.setVisible(on);
                if (on) {
                    double pulse = 0.5 + 0.5 * Math.sin(timeSeconds * 4.0);
                    double s = r * (2.5 + 0.4 * pulse);
                    regrowGlow.setBounds(-s / 2, -s / 2, s, s);
                    regrowGlow.setFillColor(Art.alpha(Art.REGROW, hpFrac < 1 ? 50 + (int) (50 * pulse) : 35));
                    for (int i = 0; i < regrowMotes.length; i++) {
                        double ph = (timeSeconds * 0.7 + i / 3.0) % 1.0;
                        double mx = Math.cos((timeSeconds * 1.3 + i * 2.1)) * r * 0.9;
                        double my = r * 0.9 - ph * r * 2.0;
                        Art.center(regrowMotes[i], mx, my);
                        regrowMotes[i].setVisible(hpFrac < 1 || ph < 0.5);
                        regrowMotes[i].setFillColor(Art.alpha(Art.REGROW, (int) (230 * (1 - ph))));
                    }
                } else {
                    for (GOval m : regrowMotes) m.setVisible(false);
                }
            }
        }

        // statuses
        boolean stun = e.isStunned();
        boolean mark = e.getMarkedBonus() > 0;
        boolean disrupt = e.isDisrupted();
        if (slow != lastSlow || stun != lastStun || mark != lastMark || revealed != lastReveal || disrupt != lastDisrupt) {
            setStatusVisible(slow, stun, mark, revealed, disrupt);
        }
        if (stun) {
            for (int i = 0; i < 3; i++) {
                double a = timeSeconds * 5.0 + i * 2.094;
                double sx = Math.cos(a) * r * 0.9;
                double sy = -r * 1.25 + Math.sin(a) * r * 0.25;
                stunStars[i].setLocation(sx, sy);
            }
        }
        if (revealed) {
            revealRing.setColor(Art.alpha(Art.REVEAL, 140 + (int) (100 * Math.sin(timeSeconds * 8))));
        }
        if (disrupt) {
            boolean show = ((int) (timeSeconds * 12) % 5) != 0;
            disruptZig.setVisible(show);
            disruptZig.setLocation(0, ((int) (timeSeconds * 9) % 3 - 1) * r * 0.25);
        }
        shadow.setVisible(!camo || revealed);
    }

    /**
     * Subclass hook: update animation frames and damage state. The base class has
     * already set {@link #now}, {@link #hpFrac}, {@link #dmg}, {@link #dirX},
     * {@link #dirY} and {@link #facingDeg}.
     */
    protected abstract void updateBody(Enemy e, double t);

    private void retint(boolean camo, boolean revealed, boolean slow, boolean flash) {
        int dc = camo ? (int) Math.round(Math.max(0, Math.min(1, decloak)) * 8) : 0;
        int key = (int) (dmg * 12) | (slow ? 1 << 4 : 0) | (flash ? 1 << 5 : 0)
            | (camo ? 1 << 6 : 0) | (revealed ? 1 << 7 : 0) | (dc << 8);
        if (key == lastTintKey) return;
        lastTintKey = key;
        int base = camo ? (revealed ? 175 : 110) : 255;
        int camoAlpha = base + (int) ((255 - base) * dc / 8.0);
        for (int i = 0; i < parts.size(); i++) {
            GObject g = parts.get(i);
            boolean full = tintFlags.get(i) == TINT_FULL;
            Color f = baseFill.get(i);
            Color l = baseLine.get(i);
            if (f != null) {
                ((GFillable) g).setFillColor(effective(f, full, camoAlpha, slow, flash));
            }
            if (l != null) g.setColor(effective(l, full, camoAlpha, slow, flash));
        }
    }

    private Color effective(Color c, boolean full, int camoAlpha, boolean slow, boolean flash) {
        Color out = c;
        if (full) {
            if (dmg > 0.05) out = Art.mix(out, Art.DAMAGE, dmg * 0.5);
            if (slow) out = Art.mix(out, Art.FROST, 0.45);
            if (flash) out = Art.mix(out, Color.WHITE, 0.7);
        }
        if (camoAlpha < 255) out = Art.alpha(out, out.getAlpha() * camoAlpha / 255);
        return out;
    }

    /** Sprites live as long as the enemy does; {@link #sync} drives animation. */
    @Override public boolean step(double dt) { return alive; }
}
