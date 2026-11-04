#!/usr/bin/env python3
"""Synthesize the game's sound effects from scratch (no samples, no downloads).

Usage (from the project root):
  python3 tools/make_sounds.py

Writes short 16-bit mono WAV files to res/sfx/.
"""
import wave
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "res" / "sfx"
RATE = 22050
RNG = np.random.default_rng(7)


def t(sec):
    return np.linspace(0, sec, int(RATE * sec), endpoint=False)


def env(n, attack=0.01, power=3.0):
    """Quick attack then exponential-ish decay."""
    x = np.linspace(0, 1, n)
    a = np.minimum(1, x / max(attack, 1e-4))
    return a * (1 - x) ** power


def sweep(f0, f1, sec, wave_fn=np.sin):
    tt = t(sec)
    f = np.linspace(f0, f1, tt.size)
    phase = 2 * np.pi * np.cumsum(f) / RATE
    return wave_fn(phase)


def square(p):
    return np.sign(np.sin(p))


def noise(sec):
    return RNG.uniform(-1, 1, int(RATE * sec))


def lowpass(x, k=8):
    kernel = np.ones(k) / k
    return np.convolve(x, kernel, mode="same")


def note(freq, sec, vol=1.0):
    tt = t(sec)
    return (np.sin(2 * np.pi * freq * tt) + 0.3 * np.sin(4 * np.pi * freq * tt)) * env(tt.size, 0.005, 2.0) * vol


def save(name, data, gain=0.6):
    data = np.asarray(data, dtype=float)
    peak = np.max(np.abs(data)) or 1.0
    pcm = (data / peak * gain * 32767).astype(np.int16)
    OUT.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT / f"{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm.tobytes())
    print("wrote", name)


def main():
    save("place", sweep(300, 640, 0.14) * env(int(RATE * 0.14), 0.005, 2.0))
    save("shoot", sweep(820, 380, 0.07, square) * env(int(RATE * 0.07), 0.003, 2.0), gain=0.25)
    hit = lowpass(noise(0.09), 10) * env(int(RATE * 0.09), 0.002, 2.5) + sweep(180, 90, 0.09) * env(int(RATE * 0.09), 0.002, 2.0)
    save("hit", hit, gain=0.35)
    pop = sweep(560, 140, 0.18) * env(int(RATE * 0.18), 0.003, 2.0) + 0.4 * lowpass(noise(0.18), 6) * env(int(RATE * 0.18), 0.003, 3.0)
    save("pop", pop, gain=0.5)
    save("coffee", np.concatenate([note(880, 0.1), note(1318, 0.22)]), gain=0.5)
    boom = lowpass(noise(0.55), 24) * env(int(RATE * 0.55), 0.002, 2.2) + sweep(90, 35, 0.55) * env(int(RATE * 0.55), 0.002, 1.8)
    save("boom", boom, gain=0.8)
    save("select", sweep(900, 1100, 0.05) * env(int(RATE * 0.05), 0.002, 2.0), gain=0.35)
    save("win", np.concatenate([note(523, 0.14), note(659, 0.14), note(784, 0.14), note(1047, 0.4)]), gain=0.6)
    save("lose", np.concatenate([note(392, 0.2), note(349, 0.2), note(330, 0.2), note(262, 0.5)]), gain=0.6)
    save("heal", np.concatenate([note(660, 0.08), note(880, 0.12)]), gain=0.35)
    save("levelup", np.concatenate([note(523, 0.08), note(659, 0.08), note(784, 0.08), note(1047, 0.1), note(1319, 0.34)]), gain=0.55)
    save("swing", lowpass(noise(0.11), 5) * env(int(RATE * 0.11), 0.2, 2.0), gain=0.3)
    save("coin", np.concatenate([note(1568, 0.04), note(2093, 0.1)]), gain=0.22)
    save("zap", sweep(1400, 200, 0.25, square) * env(int(RATE * 0.25), 0.003, 1.5), gain=0.4)


if __name__ == "__main__":
    main()
