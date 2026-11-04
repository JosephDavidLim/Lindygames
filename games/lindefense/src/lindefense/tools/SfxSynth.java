package lindefense.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Procedurally synthesises the game's soft sci-fi interface sounds (clicks, hovers, selects, tower place /
 * upgrade / sell and the "denied" buzz) as 16-bit PCM mono WAV files. Everything is generated from sine / triangle
 * oscillators with pitch sweeps, gentle FM, short attack and exponential decay envelopes, a one-pole low-pass for
 * softness and an optional light bit-crush; there is no noise. The output is original and therefore CC0.
 *
 * <p>Usage: {@code java -cp build lindefense.tools.SfxSynth assets/sfx}</p>
 */
public final class SfxSynth {
    /** Output sample rate. */
    public static final int RATE = 22050;

    private SfxSynth() { }

    /** Growable mono float buffer with mixing helpers. */
    private static final class Buf {
        float[] d = new float[0];

        void ensure(int n) { if (d.length < n) d = java.util.Arrays.copyOf(d, n); }

        /**
         * Adds one enveloped voice.
         * @param at      start time in seconds
         * @param dur     length in seconds
         * @param f0,f1   start / end frequency in Hz (exponential sweep)
         * @param tri     0 = pure sine, 1 = pure triangle
         * @param fm      FM index (decays with the note)
         * @param ratio   FM modulator frequency ratio
         * @param amp     peak amplitude
         * @param attack  attack time in seconds
         * @param decay   decay time constant in seconds
         */
        void voice(double at, double dur, double f0, double f1, double tri, double fm, double ratio, double amp, double attack, double decay) {
            int start = (int) (at * RATE), n = (int) (dur * RATE);
            ensure(start + n);
            double phase = 0, mphase = 0;
            for (int i = 0; i < n; i++) {
                double t = i / (double) RATE, u = i / (double) Math.max(1, n - 1);
                double f = f0 * Math.pow(f1 / f0, u);
                phase += 2 * Math.PI * f / RATE;
                mphase += 2 * Math.PI * f * ratio / RATE;
                double env = Math.min(1, t / attack) * Math.exp(-t / decay) * Math.min(1, (dur - t) / 0.006);
                double p = phase + fm * Math.exp(-t / (decay * 1.5)) * Math.sin(mphase);
                double sine = Math.sin(p);
                double triangle = 2 / Math.PI * Math.asin(Math.sin(p));
                d[start + i] += (float) ((sine * (1 - tri) + triangle * tri) * env * amp);
            }
        }

        /** Soft feedback echo that gives the sounds a little glassy space. */
        void echo(double delay, double feedback, int taps) {
            int dl = (int) (delay * RATE);
            ensure(d.length + dl * taps);
            for (int i = dl; i < d.length; i++) d[i] += d[i - dl] * (float) feedback;
        }

        void lowpass(double cutoff) {
            double a = 1 - Math.exp(-2 * Math.PI * cutoff / RATE), y = 0;
            for (int i = 0; i < d.length; i++) { y += a * (d[i] - y); d[i] = (float) y; }
        }

        /** Light digital grit: sample-and-hold by {@code hold} samples at {@code bits} resolution. */
        void crush(int bits, int hold) {
            double q = Math.pow(2, bits - 1);
            float held = 0;
            for (int i = 0; i < d.length; i++) {
                if (i % hold == 0) held = (float) (Math.round(d[i] * q) / q);
                d[i] = held;
            }
        }

        /** Normalises to the given peak and adds a short fade-out. */
        void finish(double peak) {
            float max = 1e-6f;
            for (float v : d) max = Math.max(max, Math.abs(v));
            int fade = (int) (0.01 * RATE);
            for (int i = 0; i < d.length; i++) {
                float g = (float) (peak / max);
                if (i > d.length - fade) g *= (d.length - i) / (float) fade;
                d[i] *= g;
            }
        }
    }

    /** Short soft tick used on hover. */
    static Buf hover() {
        Buf b = new Buf();
        b.voice(0, 0.05, 1900, 2300, 0, 0.3, 2, 1, 0.003, 0.016);
        b.lowpass(5000);
        b.finish(0.40);
        return b;
    }

    /** Rounded glassy click: a quick upward chirp with a soft sub body. */
    static Buf click() {
        Buf b = new Buf();
        b.voice(0, 0.09, 700, 1500, 0.2, 0.8, 3, 0.8, 0.002, 0.03);
        b.voice(0, 0.07, 350, 450, 0, 0, 1, 0.35, 0.002, 0.03);
        b.lowpass(6000);
        b.finish(0.55);
        return b;
    }

    /** Confident select: two rising notes a fifth apart with a little echo. */
    static Buf select() {
        Buf b = new Buf();
        b.voice(0, 0.12, 660, 700, 0.3, 0.6, 2, 0.8, 0.003, 0.05);
        b.voice(0.07, 0.22, 990, 1050, 0.3, 0.7, 2, 0.8, 0.003, 0.08);
        b.echo(0.09, 0.28, 2);
        b.lowpass(5500);
        b.finish(0.55);
        return b;
    }

    /** Tower placed: low confirming thump plus a bright digital chirp. */
    static Buf place() {
        Buf b = new Buf();
        b.voice(0, 0.16, 240, 95, 0, 0, 1, 0.9, 0.003, 0.06);
        b.voice(0.03, 0.18, 600, 1700, 0.3, 1.2, 3, 0.55, 0.004, 0.06);
        b.echo(0.07, 0.22, 2);
        b.lowpass(5200);
        b.crush(11, 2);
        b.finish(0.65);
        return b;
    }

    /** Upgrade: a soft ascending four-note arpeggio. */
    static Buf upgrade() {
        Buf b = new Buf();
        double[] notes = { 523.25, 659.25, 783.99, 1046.5 };
        for (int i = 0; i < notes.length; i++) b.voice(i * 0.07, 0.2, notes[i], notes[i] * 1.01, 0.4, 0.5, 2, 0.7, 0.004, 0.07);
        b.echo(0.11, 0.3, 3);
        b.lowpass(6000);
        b.finish(0.55);
        return b;
    }

    /** Sell: two quick descending blips with a coin-like shimmer. */
    static Buf sell() {
        Buf b = new Buf();
        b.voice(0, 0.1, 1318.5, 1250, 0.2, 0.9, 3.5, 0.7, 0.003, 0.04);
        b.voice(0.08, 0.2, 987.8, 880, 0.2, 0.9, 3.5, 0.7, 0.003, 0.07);
        b.echo(0.08, 0.25, 2);
        b.lowpass(6000);
        b.finish(0.55);
        return b;
    }

    /** Denied: a soft low double-buzz, rounded by a low-pass. */
    static Buf invalid() {
        Buf b = new Buf();
        b.voice(0, 0.1, 150, 135, 0.8, 1.2, 1, 0.9, 0.004, 0.08);
        b.voice(0.12, 0.14, 128, 105, 0.8, 1.2, 1, 0.9, 0.004, 0.1);
        b.lowpass(900);
        b.crush(10, 3);
        b.lowpass(1400);
        b.finish(0.50);
        return b;
    }

    static void write(File dir, String name, Buf b) throws IOException {
        byte[] pcm = new byte[b.d.length * 2];
        for (int i = 0; i < b.d.length; i++) {
            int v = Math.max(-32767, Math.min(32767, Math.round(b.d[i] * 32767f)));
            pcm[i * 2] = (byte) v;
            pcm[i * 2 + 1] = (byte) (v >> 8);
        }
        AudioFormat fmt = new AudioFormat(RATE, 16, 1, true, false);
        try (AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(pcm), fmt, b.d.length)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, new File(dir, name + ".wav"));
        }
    }

    public static void main(String[] args) throws IOException {
        File dir = new File(args.length > 0 ? args[0] : "assets/sfx");
        dir.mkdirs();
        write(dir, "ui_hover", hover());
        write(dir, "ui_click", click());
        write(dir, "ui_select", select());
        write(dir, "tower_place", place());
        write(dir, "tower_upgrade", upgrade());
        write(dir, "tower_sell", sell());
        write(dir, "invalid", invalid());
        System.out.println("Wrote 7 sounds to " + dir);
    }
}
