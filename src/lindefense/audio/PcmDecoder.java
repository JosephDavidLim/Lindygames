package lindefense.audio;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Opens a WAV file (16-bit PCM, 8-bit PCM or G.711 mu-law / a-law, mono or stereo) as little-endian signed 16-bit
 * PCM. The JDK's own reader is tried first; if it rejects the file (it does not understand the
 * WAVE_FORMAT_EXTENSIBLE header that macOS {@code afconvert} writes for stereo mu-law) a small pure-Java RIFF
 * parser takes over and decodes mu-law / a-law with the standard G.711 expansion.
 */
public final class PcmDecoder {
    private PcmDecoder() { }

    /** Opens {@code f}; the returned stream is signed 16-bit little-endian with 1 or 2 channels. */
    public static AudioInputStream open(File f) throws IOException, UnsupportedAudioFileException {
        AudioInputStream in = null;
        try {
            in = AudioSystem.getAudioInputStream(f);
            AudioFormat src = in.getFormat();
            if (src.getChannels() < 1 || src.getChannels() > 2) throw new UnsupportedAudioFileException("channels " + src.getChannels());
            if (src.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && src.getSampleSizeInBits() == 16 && !src.isBigEndian()) return in;
            AudioFormat dst = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, src.getSampleRate(), 16,
                src.getChannels(), src.getChannels() * 2, src.getSampleRate(), false);
            if (!AudioSystem.isConversionSupported(dst, src)) throw new UnsupportedAudioFileException("unsupported encoding " + src);
            return AudioSystem.getAudioInputStream(dst, in);
        } catch (UnsupportedAudioFileException | RuntimeException e) {
            if (in != null) try { in.close(); } catch (IOException ignored) { /* closing after failure */ }
            // JDK reader refused the file: parse the RIFF chunks ourselves
            return decodeWav(new BufferedInputStream(new FileInputStream(f), 1 << 16));
        }
    }

    /**
     * Parses a RIFF/WAVE stream (plain or WAVE_FORMAT_EXTENSIBLE fmt chunk; PCM 8/16-bit, mu-law, a-law) positioned
     * at its start and returns 16-bit little-endian PCM. Closing the returned stream closes {@code raw}.
     */
    public static AudioInputStream decodeWav(InputStream raw) throws IOException, UnsupportedAudioFileException {
        try {
            byte[] hdr = readFully(raw, 12);
            if (hdr[0] != 'R' || hdr[1] != 'I' || hdr[2] != 'F' || hdr[3] != 'F' || hdr[8] != 'W' || hdr[9] != 'A' || hdr[10] != 'V' || hdr[11] != 'E')
                throw new UnsupportedAudioFileException("not a RIFF/WAVE file");
            int tag = -1, channels = 0, bits = 0;
            float rate = 0;
            while (true) {
                byte[] ch = readFully(raw, 8);
                String id = new String(ch, 0, 4, java.nio.charset.StandardCharsets.US_ASCII);
                long size = u32(ch, 4);
                if (id.equals("fmt ")) {
                    if (size < 16 || size > 4096) throw new UnsupportedAudioFileException("bad fmt chunk");
                    byte[] f = readFully(raw, (int) size);
                    tag = u16(f, 0); channels = u16(f, 2); rate = u32(f, 4); bits = u16(f, 14);
                    if (tag == 0xFFFE) {                       // WAVE_FORMAT_EXTENSIBLE: real format is the GUID's first 2 bytes
                        if (size < 40) throw new UnsupportedAudioFileException("short extensible fmt chunk");
                        tag = u16(f, 24);
                    }
                    skipFully(raw, size & 1);
                } else if (id.equals("data")) {
                    if (tag < 0) throw new UnsupportedAudioFileException("data before fmt");
                    return wrap(raw, tag, channels, bits, rate, size);
                } else {
                    skipFully(raw, size + (size & 1));
                }
            }
        } catch (EOFException e) {
            closeQuietly(raw);
            throw new UnsupportedAudioFileException("truncated WAV");
        } catch (IOException | UnsupportedAudioFileException | RuntimeException e) {
            closeQuietly(raw);
            throw e;
        }
    }

    private static AudioInputStream wrap(InputStream raw, int tag, int channels, int bits, float rate, long dataSize)
            throws UnsupportedAudioFileException {
        if (channels < 1 || channels > 2 || rate < 4000 || rate > 192000) throw new UnsupportedAudioFileException("unsupported layout");
        int mode;                                              // 0 = 16-bit PCM, 1 = 8-bit PCM, 2 = mu-law, 3 = a-law
        if (tag == 1 && bits == 16) mode = 0;
        else if (tag == 1 && bits == 8) mode = 1;
        else if (tag == 7 && bits == 8) mode = 2;
        else if (tag == 6 && bits == 8) mode = 3;
        else throw new UnsupportedAudioFileException("unsupported WAV format tag " + tag + " / " + bits + " bits");
        int srcFrame = channels * (mode == 0 ? 2 : 1);
        long frames = dataSize == 0 || dataSize == 0xFFFFFFFFL ? AudioSystem.NOT_SPECIFIED : dataSize / srcFrame;
        InputStream data = new G711Stream(raw, mode, dataSize == 0 || dataSize == 0xFFFFFFFFL ? Long.MAX_VALUE : dataSize);
        AudioFormat out = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, channels, channels * 2, rate, false);
        return new AudioInputStream(data, out, frames);
    }

    /** Standard G.711 mu-law expansion to a 16-bit sample. */
    static int ulaw(int b) {
        b = ~b & 0xff;
        int t = ((b & 0x0f) << 3) + 0x84;
        t <<= (b & 0x70) >> 4;
        return (b & 0x80) != 0 ? 0x84 - t : t - 0x84;
    }

    /** Standard G.711 a-law expansion to a 16-bit sample. */
    static int alaw(int b) {
        b ^= 0x55;
        int t = (b & 0x0f) << 4;
        int seg = (b & 0x70) >> 4;
        switch (seg) {
            case 0: t += 8; break;
            case 1: t += 0x108; break;
            default: t += 0x108; t <<= seg - 1; break;
        }
        return (b & 0x80) != 0 ? t : -t;
    }

    /** Expands the data chunk of a WAV into 16-bit little-endian PCM bytes on the fly. */
    private static final class G711Stream extends InputStream {
        private final InputStream in;
        private final int mode;
        private long remaining;
        private byte[] tmp = new byte[0];
        private final byte[] one = new byte[1];

        G711Stream(InputStream in, int mode, long remaining) { this.in = in; this.mode = mode; this.remaining = remaining; }

        @Override public int read() throws IOException {
            int n = read(one, 0, 1);
            return n <= 0 ? -1 : one[0] & 0xff;
        }

        @Override public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) return 0;
            if (mode == 0) {                                   // 16-bit PCM passes through
                if (remaining <= 0) return -1;
                int n = in.read(b, off, (int) Math.min(len, remaining));
                if (n > 0) remaining -= n;
                return n;
            }
            int want = (int) Math.min(Math.max(len / 2, 1), Math.min(remaining, 1 << 16));
            if (want <= 0) return -1;
            if (tmp.length < want) tmp = new byte[want];
            int n = in.read(tmp, 0, want);
            if (n <= 0) return -1;
            remaining -= n;
            int o = off;
            for (int i = 0; i < n; i++) {
                int v;
                switch (mode) {
                    case 1: v = ((tmp[i] & 0xff) - 128) << 8; break;
                    case 2: v = ulaw(tmp[i] & 0xff); break;
                    default: v = alaw(tmp[i] & 0xff); break;
                }
                b[o++] = (byte) v;
                b[o++] = (byte) (v >> 8);
            }
            return o - off;
        }

        @Override public int available() throws IOException { return mode == 0 ? in.available() : in.available() * 2; }
        @Override public void close() throws IOException { in.close(); }
    }

    private static byte[] readFully(InputStream in, int n) throws IOException {
        byte[] b = new byte[n];
        int got = 0;
        while (got < n) {
            int r = in.read(b, got, n - got);
            if (r < 0) throw new EOFException();
            got += r;
        }
        return b;
    }

    private static void skipFully(InputStream in, long n) throws IOException {
        while (n > 0) {
            long s = in.skip(n);
            if (s <= 0) {
                if (in.read() < 0) throw new EOFException();
                s = 1;
            }
            n -= s;
        }
    }

    private static int u16(byte[] b, int o) { return (b[o] & 0xff) | (b[o + 1] & 0xff) << 8; }
    private static long u32(byte[] b, int o) { return (b[o] & 0xffL) | (b[o + 1] & 0xffL) << 8 | (b[o + 2] & 0xffL) << 16 | (b[o + 3] & 0xffL) << 24; }
    private static void closeQuietly(InputStream in) { try { in.close(); } catch (IOException ignored) { /* best effort */ } }
}
