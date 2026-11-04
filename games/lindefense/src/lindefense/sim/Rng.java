package lindefense.sim;

import java.util.Random;

/**
 * Seedable random source shared by the simulation so that headless tests
 * are reproducible.
 */
public final class Rng {
    private final Random random;

    public Rng(long seed) {
        // Scramble the seed: java.util.Random yields near-identical first values for sequential seeds.
        this.random = new Random((seed + 1) * 0x9E3779B97F4A7C15L ^ 0x5DEECE66DL);
        for (int i = 0; i < 4; i++) random.nextDouble();
    }

    public double next() { return random.nextDouble(); }
    public int nextInt(int bound) { return random.nextInt(bound); }
    public boolean chance(double p) { return random.nextDouble() < p; }
    public double range(double lo, double hi) { return lo + (hi - lo) * random.nextDouble(); }
}
