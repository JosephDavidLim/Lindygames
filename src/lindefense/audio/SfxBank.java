package lindefense.audio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * All sound effects, decoded once at start-up into mono float arrays at the mixer rate. Missing or unreadable
 * files are silently skipped (that effect just stays silent).
 */
final class SfxBank {
    private final float[][] data = new float[Sfx.values().length][];

    /** Loads every effect from {@code dir} (no-op if the directory does not exist). */
    void load(File dir) {
        for (Sfx s : Sfx.values()) {
            File f = new File(dir, s.file + ".wav");
            if (!f.isFile()) continue;
            try {
                data[s.ordinal()] = decode(f);
            } catch (Exception | LinkageError ignored) {
                // unsupported or corrupt file: leave this effect silent
            }
        }
    }

    float[] get(Sfx s) { return data[s.ordinal()]; }

    int loadedCount() {
        int n = 0;
        for (float[] d : data) if (d != null) n++;
        return n;
    }

    private static float[] decode(File f) throws Exception {
        try (AudioInputStream in = PcmDecoder.open(f)) {
            AudioFormat fmt = in.getFormat();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1 << 16);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
                if (bos.size() > 16 << 20) throw new IOException("effect too large");
            }
            byte[] b = bos.toByteArray();
            int ch = fmt.getChannels();
            int frames = b.length / (2 * ch);
            float[] mono = new float[frames];
            for (int i = 0; i < frames; i++) {
                float sum = 0;
                for (int c = 0; c < ch; c++) {
                    int o = (i * ch + c) * 2;
                    sum += (short) ((b[o] & 0xff) | (b[o + 1] << 8)) / 32768f;
                }
                mono[i] = sum / ch;
            }
            return resample(mono, fmt.getSampleRate() / (double) Mixer.RATE);
        }
    }

    /** Linear resample; {@code ratio} = source rate / target rate. */
    static float[] resample(float[] src, double ratio) {
        if (Math.abs(ratio - 1) < 1e-9 || src.length < 2) return src;
        int outLen = (int) (src.length / ratio);
        float[] out = new float[outLen + 1];
        for (int i = 0; i < outLen; i++) {
            double p = i * ratio;
            int k = (int) p;
            float fr = (float) (p - k);
            out[i] = src[k] + (src[Math.min(k + 1, src.length - 1)] - src[k]) * fr;
        }
        out[outLen] = 0;
        return out;
    }
}
