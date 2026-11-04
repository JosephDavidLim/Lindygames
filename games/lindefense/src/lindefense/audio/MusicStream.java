package lindefense.audio;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * Streams one music file on its own daemon thread: decodes (PCM or mu-law WAV), resamples to the mixer rate
 * and fills a ring buffer of stereo float frames that the mixer thread drains. Never loads the whole track.
 * Looping reopens the file while the ring buffer still holds about a second of audio, so the loop is gapless.
 */
final class MusicStream {
    private static final int CAPACITY = Mixer.RATE * 2;      // 2 s of stereo frames

    private final File file;
    private final boolean loop;
    private final float[] ring = new float[CAPACITY * 2];
    private int head, size;                                   // guarded by this
    private volatile boolean closed, eof, failed;
    private final Thread thread;
    private final Runnable onFail;

    MusicStream(File file, boolean loop, Runnable onFail) {
        this.file = file; this.loop = loop; this.onFail = onFail;
        thread = new Thread(this::decodeLoop, "lindefense-music-" + file.getName());
        thread.setDaemon(true);
        thread.start();
    }

    boolean isFailed() { return failed; }

    /** True once a non-looping stream has been fully played. */
    synchronized boolean isFinished() { return (eof || failed) && size == 0; }

    void close() { closed = true; thread.interrupt(); }

    /** Fills {@code out} (interleaved stereo) with up to {@code frames} frames; the rest is zeroed (underrun). */
    synchronized void read(float[] out, int frames) {
        int n = Math.min(frames, size);
        for (int i = 0; i < n; i++) {
            int p = ((head + i) % CAPACITY) * 2;
            out[i * 2] = ring[p];
            out[i * 2 + 1] = ring[p + 1];
        }
        head = (head + n) % CAPACITY;
        size -= n;
        for (int i = n * 2; i < frames * 2; i++) out[i] = 0;
        notifyAll();
    }

    private synchronized void push(float l, float r) throws InterruptedException {
        while (size >= CAPACITY) {
            if (closed) throw new InterruptedException();
            wait(40);
        }
        int p = ((head + size) % CAPACITY) * 2;
        ring[p] = l; ring[p + 1] = r;
        size++;
    }

    private void decodeLoop() {
        try {
            boolean first = true;
            do {
                int frames = decodeOnce();
                if (first && frames == 0) throw new IOException("empty track");
                first = false;
            } while (loop && !closed);
            eof = true;
        } catch (InterruptedException e) {
            // closed
        } catch (Exception | LinkageError e) {
            failed = true;
            if (!closed && onFail != null) onFail.run();
        }
    }

    /** Decodes the file once from start to end, pushing resampled frames. Returns source frames decoded. */
    private int decodeOnce() throws Exception {
        try (AudioInputStream in = PcmDecoder.open(file)) {
            AudioFormat fmt = in.getFormat();
            int ch = fmt.getChannels();
            double ratio = fmt.getSampleRate() / (double) Mixer.RATE;
            byte[] buf = new byte[4096 * ch * 2];
            float prevL = 0, prevR = 0;
            double pos = 0;
            int total = 0;
            int carry = 0;
            int n;
            while (!closed && (n = in.read(buf, carry, buf.length - carry)) > 0) {
                n += carry;
                int fs = ch * 2;
                int frames = n / fs;
                for (int i = 0; i < frames; i++) {
                    int o = i * fs;
                    float l = (short) ((buf[o] & 0xff) | (buf[o + 1] << 8)) / 32768f;
                    float r = ch == 2 ? (short) ((buf[o + 2] & 0xff) | (buf[o + 3] << 8)) / 32768f : l;
                    while (pos < 1) {
                        push(prevL + (l - prevL) * (float) pos, prevR + (r - prevR) * (float) pos);
                        pos += ratio;
                    }
                    pos -= 1;
                    prevL = l; prevR = r;
                }
                total += frames;
                carry = n - frames * fs;
                if (carry > 0) System.arraycopy(buf, frames * fs, buf, 0, carry);
            }
            return total;
        }
    }
}
