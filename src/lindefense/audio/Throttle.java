package lindefense.audio;

/**
 * Per-sound rate limiter: a sound is accepted only if at least {@link Sfx#minGapMs} milliseconds have passed
 * since the last accepted play of the same sound. Keeps big fights from flooding the mixer.
 */
public final class Throttle {
    private final long[] last = new long[Sfx.values().length];
    private final boolean[] seen = new boolean[Sfx.values().length];

    /** Returns true (and records the play) if {@code s} may sound at time {@code nowNanos}. */
    public synchronized boolean allow(Sfx s, long nowNanos) {
        int i = s.ordinal();
        if (seen[i] && nowNanos - last[i] < s.minGapMs * 1_000_000L) return false;
        seen[i] = true;
        last[i] = nowNanos;
        return true;
    }
}
