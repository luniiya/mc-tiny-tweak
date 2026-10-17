#!/usr/bin/env python3
"""Synthesizes the damage number sounds from scratch: no samples, so they are all ours.

Run from the repository root: python3 tools/damage_sounds.py
Needs numpy and ffmpeg with libvorbis. Writes Ogg Vorbis files to
src/main/resources/assets/mc_tiny_tweak/sounds/damage/.
"""
import os
import sys
import subprocess
import tempfile
import wave
import zlib

import numpy as np

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "mc_tiny_tweak", "sounds", "damage")
rng = np.random.default_rng(1024)


def t(duration):
    return np.arange(int(SR * duration)) / SR


def silence(duration):
    return np.zeros(int(SR * duration))


def noise(duration):
    return rng.uniform(-1, 1, int(SR * duration))


def decay(duration, seconds, attack=0.002):
    x = t(duration)
    env = np.exp(-x / seconds)
    if attack > 0:
        env *= np.clip(x / attack, 0, 1)
    return env


def sweep(f0, f1, duration, curve=3.0):
    """A sine gliding from f0 to f1 Hz, fast at first."""
    x = t(duration) / duration
    freq = f1 + (f0 - f1) * np.exp(-curve * x)
    return np.sin(2 * np.pi * np.cumsum(freq) / SR)


def lowpass(x, cutoff):
    """One-pole lowpass, run twice; cutoff may be a number or one per sample."""
    cutoff = np.broadcast_to(np.asarray(cutoff, dtype=float), x.shape)
    a = 1 - np.exp(-2 * np.pi * cutoff / SR)
    for _ in range(2):
        y = np.empty_like(x)
        acc = 0.0
        for i in range(len(x)):
            acc += a[i] * (x[i] - acc)
            y[i] = acc
        x = y
    return x


def highpass(x, cutoff):
    return x - lowpass(x, cutoff)


def bandpass(x, low, high):
    return lowpass(highpass(x, low), high)


def saturate(x, drive):
    return np.tanh(drive * x) / np.tanh(drive)


def bell(freq, duration, seconds, partials=((1, 1), (2.76, 0.5), (5.4, 0.25), (8.93, 0.12))):
    """An inharmonic bell or glockenspiel tone."""
    x = t(duration)
    out = np.zeros_like(x)
    for ratio, gain in partials:
        out += gain * np.sin(2 * np.pi * freq * ratio * x) * np.exp(-x * ratio ** 0.5 / seconds)
    return out * np.clip(x / 0.002, 0, 1)


def saw(freq, duration):
    phase = (freq * t(duration)) % 1.0
    return 2 * phase - 1


def reverb(x, seconds, mix, tone=6000):
    """Convolves with a decaying burst of filtered noise, a simple but lush room."""
    ir = noise(seconds) * np.exp(-t(seconds) / (seconds / 5))
    ir = lowpass(ir, tone)
    ir /= np.sqrt(np.sum(ir ** 2))
    n = len(x) + len(ir)
    size = 1 << (n - 1).bit_length()
    wet = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[: len(x) + len(ir)]
    dry = np.concatenate([x, np.zeros(len(ir))])
    return (1 - mix) * dry + mix * wet * 0.6


def at(base, sound, start):
    """Mixes sound into base starting at start seconds, growing base if needed."""
    i = int(start * SR)
    end = i + len(sound)
    if end > len(base):
        base = np.concatenate([base, np.zeros(end - len(base))])
    base[i:end] += sound
    return base


def click(duration=0.006, cutoff=2000):
    return highpass(noise(duration), cutoff) * decay(duration, duration / 3, attack=0)


def finish(x, peak=0.89):
    x = x - np.mean(x)
    # Fade the last 20 ms so nothing clicks at the end.
    fade = min(len(x), int(0.02 * SR))
    x[-fade:] *= np.linspace(1, 0, fade)
    return x * peak / np.max(np.abs(x))


# --- Hits, one per tier ------------------------------------------------------------------------------


def heavy():
    d = 0.7
    sub = sweep(110, 42, d, 5) * decay(d, 0.18)
    punch = sweep(240, 90, d, 9) * decay(d, 0.05) * 0.5
    x = sub + punch
    x = at(x, click() * 0.8, 0)
    return finish(reverb(saturate(x, 2.0), 0.6, 0.15))


def brutal():
    d = 0.9
    sub = sweep(95, 34, d, 4) * decay(d, 0.25)
    crunch = saturate(bandpass(noise(d), 300, 2200) * decay(d, 0.07), 4) * 0.5
    clang = sum(np.sin(2 * np.pi * f * t(d)) * np.exp(-t(d) / s) for f, s in ((220, 0.3), (563, 0.2), (921, 0.15), (1430, 0.1))) * 0.18
    x = sub + crunch + clang
    x = at(x, click(0.008, 1500), 0)
    return finish(reverb(saturate(x, 2.5), 0.9, 0.2))


def devastating():
    d = 1.6
    sub = sweep(80, 24, d, 2.5) * decay(d, 0.5)
    sweep_cut = 6000 * np.exp(-t(d) / 0.25) + 200
    blast = lowpass(noise(d), sweep_cut) * decay(d, 0.35) * 0.9
    ring = sum(np.sin(2 * np.pi * f * t(d)) * np.exp(-t(d) / s) for f, s in ((147, 0.6), (391, 0.4), (733, 0.3), (1187, 0.2))) * 0.15
    x = sub + blast + ring
    x = at(x, click(0.01, 1000) * 1.2, 0)
    return finish(reverb(saturate(x, 2.2), 1.3, 0.3))


def max_hit():
    d = 2.6
    sub = sweep(70, 20, d, 2) * decay(d, 0.7)
    blast = lowpass(noise(d), 7000 * np.exp(-t(d) / 0.3) + 150) * decay(d, 0.45) * 0.8
    # The anime "DUN": a huge detuned chord swelling through a closing filter.
    chord = np.zeros(int(SR * d))
    for f in (55.0, 110.0, 164.8, 220.0, 329.6):
        for detune in (-0.006, 0.0, 0.006):
            chord += saw(f * (1 + detune), d)
    chord = lowpass(chord / 15, 3500 * np.exp(-t(d) / 0.6) + 300) * decay(d, 1.1, attack=0.006) * 1.4
    x = sub + blast + chord
    x = at(x, click(0.012, 800) * 1.4, 0)
    shimmer = silence(0.01)
    for i, f in enumerate((880.0, 1108.7, 1318.5, 1760.0, 2217.5)):
        shimmer = at(shimmer, bell(f, 1.2, 0.35) * 0.12, 0.12 + i * 0.07)
    x = at(x, shimmer, 0)
    return finish(reverb(saturate(x, 1.8), 2.4, 0.35))


# --- Effects ------------------------------------------------------------------------------------------


def shatter():
    d = 0.8
    x = highpass(noise(d), 3000) * decay(d, 0.1) * 0.5
    for _ in range(70):
        start = rng.exponential(0.09)
        if start > 0.45:
            continue
        f = rng.uniform(2500, 9500)
        length = rng.uniform(0.02, 0.09)
        ping = np.sin(2 * np.pi * f * t(length * 4)) * decay(length * 4, length, attack=0)
        x = at(x, ping * rng.uniform(0.15, 0.4), start)
    x = at(x, click(0.01, 1200), 0)
    return finish(reverb(x, 0.6, 0.25, tone=9000))


def slash():
    d = 0.35
    center = 700 * (7.0 ** (t(d) / d))
    x = bandpass(noise(d), center * 0.6, center * 1.6)
    env = np.clip(t(d) / 0.05, 0, 1) * np.exp(-np.maximum(t(d) - 0.05, 0) / 0.07)
    return finish(reverb(x * env, 0.4, 0.15, tone=9000))


def impact():
    d = 0.5
    crack = highpass(noise(d), 1500) * decay(d, 0.015, attack=0)
    thud = np.sin(2 * np.pi * 60 * t(d)) * decay(d, 0.1)
    return finish(reverb(crack + thud * 0.8, 0.5, 0.25))


def max_tag():
    d = 0.2
    x = silence(d)
    for i, f in enumerate((1046.5, 1318.5, 1568.0, 2093.0, 2637.0)):
        x = at(x, bell(f, 0.9, 0.3, ((1, 1), (2.0, 0.3), (3.0, 0.12), (4.07, 0.08))) * 0.5, i * 0.045)
    sparkle = highpass(noise(0.9), 7000) * decay(0.9, 0.25, attack=0.05) * 0.08
    x = at(x, sparkle, 0)
    return finish(reverb(x, 0.9, 0.3, tone=10000))


def glitch():
    """NULL: stuttering square bursts, bitcrushed noise and dropouts."""
    x = silence(0.0)
    time = 0.0
    while time < 0.6:
        length = rng.uniform(0.018, 0.045)
        kind = rng.integers(0, 4)
        n = int(SR * length)
        if kind == 0:
            f = rng.uniform(80, 1800)
            seg = np.sign(np.sin(2 * np.pi * f * t(length)))
        elif kind == 1:
            hold = rng.integers(8, 40)
            seg = np.repeat(rng.uniform(-1, 1, n // hold + 1), hold)[:n]
        elif kind == 2:
            seg = np.zeros(n)
        else:
            seg = np.round(np.sin(2 * np.pi * rng.uniform(200, 600) * t(length)) * 3) / 3
        x = at(x, seg * rng.uniform(0.4, 0.9), time)
        time += length
    buzz = np.sign(np.sin(2 * np.pi * 50 * t(len(x) / SR))) * 0.15
    return finish(x + buzz[: len(x)], peak=0.7)


def crit():
    d = 0.3
    x = sum(np.sin(2 * np.pi * f * t(d)) * np.exp(-t(d) / s) for f, s in ((3200, 0.12), (4700, 0.08), (6900, 0.05)))
    x = at(x, click(0.004, 3000), 0)
    return finish(reverb(x, 0.4, 0.2, tone=10000), peak=0.6)


# --- Combo counter ------------------------------------------------------------------------------------


def combo_milestone():
    d = 0.1
    x = silence(d)
    for i, f in enumerate((1046.5, 1318.5, 1568.0, 2093.0)):
        x = at(x, bell(f, 1.0, 0.35) * 0.4, i * 0.02)
    return finish(reverb(x, 0.8, 0.3, tone=9000), peak=0.7)


def combo_rank():
    d = 0.45
    center = 300 * (20.0 ** (t(d) / d))
    whoosh = bandpass(noise(d), center * 0.5, center * 1.5) * (t(d) / d) ** 2
    x = whoosh * 0.6
    stinger = silence(0.01)
    for f in (880.0, 1318.5, 1760.0, 2217.5):
        stinger = at(stinger, bell(f, 1.2, 0.45) * 0.35, 0)
    stinger = at(stinger, sweep(120, 45, 0.5, 6) * decay(0.5, 0.12) * 0.8, 0)
    x = at(x, stinger, 0.42)
    return finish(reverb(x, 1.2, 0.3))


def combo_break():
    d = 0.6
    x = sweep(600, 140, d, 3) * decay(d, 0.15) + lowpass(noise(d), 500) * decay(d, 0.1) * 0.3
    return finish(reverb(x, 0.6, 0.25), peak=0.6)


# --- Heals --------------------------------------------------------------------------------------------


def heal():
    d = 0.05
    x = silence(d)
    for i, f in enumerate((1318.5, 1661.2, 1975.5)):
        x = at(x, bell(f, 1.0, 0.4, ((1, 1), (2.0, 0.2), (3.0, 0.06))) * 0.35 * np.clip(t(1.0) / 0.01, 0, 1), i * 0.06)
    breath = highpass(noise(0.8), 5000) * np.sin(np.pi * t(0.8) / 0.8) * 0.05
    x = at(x, breath, 0)
    return finish(reverb(x, 1.0, 0.4, tone=9000), peak=0.6)


def heal_big():
    d = 2.0
    pad = sum(np.sin(2 * np.pi * f * t(d)) for f in (329.6, 493.9, 659.3)) / 3
    pad *= np.clip(t(d) / 0.25, 0, 1) * np.exp(-t(d) / 0.8)
    x = pad * 0.5
    for i, f in enumerate((659.3, 830.6, 987.8, 1318.5, 1661.2, 1975.5, 2637.0)):
        x = at(x, bell(f, 1.0, 0.4, ((1, 1), (2.0, 0.2), (3.0, 0.06))) * 0.3, i * 0.06)
    return finish(reverb(x, 1.8, 0.45, tone=8000), peak=0.75)


SOUNDS = {
    "heavy": heavy,
    "brutal": brutal,
    "devastating": devastating,
    "max": max_hit,
    "shatter": shatter,
    "slash": slash,
    "impact": impact,
    "max_tag": max_tag,
    "null": glitch,
    "crit": crit,
    "combo_milestone": combo_milestone,
    "combo_rank": combo_rank,
    "combo_break": combo_break,
    "heal": heal,
    "heal_big": heal_big,
}


def write(name, samples):
    pcm = (np.clip(samples, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        path = tmp.name
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    out = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", path, "-c:a", "libvorbis", "-q:a", "5", out], check=True)
    os.remove(path)
    print(f"{out}: {len(samples) / SR:.2f}s")


if __name__ == "__main__":
    # Name sounds to remake only those, so the others stay exactly as they are.
    names = sys.argv[1:] or list(SOUNDS)
    os.makedirs(OUT, exist_ok=True)
    for name in names:
        # Each sound gets its own noise, so changing one never shifts another's.
        rng = np.random.default_rng(zlib.crc32(name.encode()))
        write(name, SOUNDS[name]())
