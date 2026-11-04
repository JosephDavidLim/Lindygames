package lindefense.combat;

import lindefense.sim.Vec;

/**
 * A transient visual notification produced by the simulation (beam flashes,
 * explosions, floating text, boss announcements). The renderer drains these
 * each frame; headless runs simply discard them.
 */
public final class VisualEvent {
    public enum Kind { BEAM, CHAIN, HIT, EXPLOSION, TEXT, DEATH, TELEPORT, ABILITY, BANNER, PHASE, SPLIT }

    public final Kind kind;
    public final Vec a;
    public final Vec b;
    public final double radius;
    public final int rgb;
    public final String text;
    public final double duration;

    public VisualEvent(Kind kind, Vec a, Vec b, double radius, int rgb, String text, double duration) {
        this.kind = kind; this.a = a; this.b = b; this.radius = radius; this.rgb = rgb; this.text = text; this.duration = duration;
    }

    public static VisualEvent beam(Vec a, Vec b, int rgb) { return new VisualEvent(Kind.BEAM, a, b, 2, rgb, null, 0.12); }
    public static VisualEvent chain(Vec a, Vec b, int rgb) { return new VisualEvent(Kind.CHAIN, a, b, 2, rgb, null, 0.15); }
    public static VisualEvent hit(Vec a, int rgb) { return new VisualEvent(Kind.HIT, a, null, 6, rgb, null, 0.15); }
    public static VisualEvent explosion(Vec a, double r, int rgb) { return new VisualEvent(Kind.EXPLOSION, a, null, r, rgb, null, 0.35); }
    public static VisualEvent text(Vec a, String t, int rgb) { return new VisualEvent(Kind.TEXT, a, null, 0, rgb, t, 1.2); }
    public static VisualEvent death(Vec a, double r, int rgb) { return new VisualEvent(Kind.DEATH, a, null, r, rgb, null, 0.4); }
    public static VisualEvent split(Vec a, double r, int rgb) { return new VisualEvent(Kind.SPLIT, a, null, r, rgb, null, 0.5); }
    public static VisualEvent teleport(Vec a) { return new VisualEvent(Kind.TELEPORT, a, null, 24, 0xd0b0ff, null, 0.5); }
    public static VisualEvent ability(Vec a, double r, int rgb) { return new VisualEvent(Kind.ABILITY, a, null, r, rgb, null, 0.6); }
    public static VisualEvent banner(String t, int rgb, double dur) { return new VisualEvent(Kind.BANNER, null, null, 0, rgb, t, dur); }
    public static VisualEvent phase(Vec a, String t, int rgb) { return new VisualEvent(Kind.PHASE, a, null, 120, rgb, t, 1.5); }
}
