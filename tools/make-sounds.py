#!/usr/bin/env python3
"""Synthesizes the feedback cues of DESIGN 10 as short mono WAVs: original sounds, no samples.
Run from the repo root; writes androidApp-learner/src/main/res/raw/cue_*.wav.

select  a soft wooden tick
correct two rising bell notes
retry   one low, soft note (a nudge, not a buzzer)
pass    a four-note arpeggio, the level-pass fanfare
"""
import math
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path("androidApp-learner/src/main/res/raw")


def bell(freq, dur, start=0.0, amp=0.5, decay=6.0):
    """A bell-ish note: a sine with two soft partials and an exponential decay."""
    return [(start, freq, dur, amp, decay)]


def render(notes, length):
    n = int(RATE * length)
    buf = [0.0] * n
    for start, freq, dur, amp, decay in notes:
        s0 = int(RATE * start)
        for i in range(int(RATE * dur)):
            t = i / RATE
            if s0 + i >= n:
                break
            attack = min(1.0, t / 0.004)  # 4 ms attack, so nothing clicks
            env = attack * math.exp(-decay * t)
            v = (math.sin(2 * math.pi * freq * t)
                 + 0.35 * math.sin(2 * math.pi * freq * 2 * t)
                 + 0.12 * math.sin(2 * math.pi * freq * 3 * t)) / 1.47
            buf[s0 + i] += amp * env * v
    peak = max(1e-9, max(abs(x) for x in buf))
    scale = min(1.0, 0.8 / peak)
    return [x * scale for x in buf]


def tick(length=0.05):
    """A short filtered click: a fast-decaying high sine burst."""
    n = int(RATE * length)
    return [0.6 * math.exp(-90 * (i / RATE)) * math.sin(2 * math.pi * 1800 * i / RATE) for i in range(n)]


def write(name, samples):
    OUT.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT / f"cue_{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, x)) * 32767)) for x in samples))
    print(OUT / f"cue_{name}.wav", f"{len(samples) / RATE:.2f}s")


write("select", tick())
write("correct", render(bell(784, 0.35) + bell(1175, 0.45, start=0.09), 0.55))       # G5, D6
write("retry", render(bell(330, 0.35, amp=0.35, decay=9.0), 0.35))                     # E4, quiet
write("pass", render(bell(523, 0.4) + bell(659, 0.4, 0.11) + bell(784, 0.4, 0.22)
                     + bell(1047, 0.7, 0.33, decay=4.0), 1.05))                          # C E G C
