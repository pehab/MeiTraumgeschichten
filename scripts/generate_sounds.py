"""Create original, quiet PCM effects with deterministic synthesis; no external samples."""
from array import array
import math
from pathlib import Path
import random
import sys
import wave

ROOT = Path(__file__).resolve().parents[1]
RATE = 22050
SOUNDS = {"wind": 4, "rain": 4, "steps": 2.6, "knock": 1.7, "owl": 2.8,
          "river": 4, "chime": 3.7, "rustle": 2.8, "snore": 3.8,
          "bell": 3.8, "creak": 2.3, "breathe": 4}


def render(name, seconds):
    rng = random.Random("MeiTraumgeschichten:" + name)
    samples = []
    smooth = 0
    for i in range(round(RATE * seconds)):
        t = i / RATE
        noise = rng.uniform(-1, 1)
        smooth = .97 * smooth + .03 * noise
        if name == "wind":
            value = smooth * (1.2 + .55 * math.sin(t * 2))
        elif name == "rain":
            value = noise * .14 + smooth * .8
            if rng.random() < .003:
                value += .15 * rng.random()
        elif name == "steps":
            phase = t % .57
            value = (noise * .34 + math.sin(2 * math.pi * 95 * phase) * .3) * math.exp(-phase * 36)
        elif name == "knock":
            phase = t % .42
            value = (math.sin(2 * math.pi * 280 * phase) * .5 + noise * .12) * math.exp(-phase * 58) if t < 1.15 else 0
        elif name == "owl":
            phase = t % 1.35
            env = max(0, math.sin(min(1, phase / .65) * math.pi)) ** 1.6 if phase < .65 else 0
            freq = 390 + 35 * math.sin(phase * 7)
            value = .32 * env * math.sin(2 * math.pi * freq * t)
        elif name == "river":
            value = smooth * 1.4 + noise * .06 + .035 * math.sin(2 * math.pi * 650 * t + 7 * math.sin(t * 20))
        elif name in ("chime", "bell"):
            value = 0
            notes = [523.25, 659.25, 783.99, 1046.5] if name == "chime" else [659.25, 830.61]
            for j, freq in enumerate(notes):
                elapsed = t - j * .33
                if elapsed >= 0:
                    value += .11 * math.exp(-elapsed * 2.5) * (1 - math.exp(-elapsed * 55)) * (
                        math.sin(2 * math.pi * freq * elapsed) + .23 * math.sin(2 * math.pi * freq * 2.76 * elapsed))
        elif name == "rustle":
            value = noise * .17 * (.2 + .8 * abs(math.sin(t * 7))) + smooth * .7
        elif name == "snore":
            env = max(0, math.sin(t * 2.4)) ** 2
            value = env * (.11 * math.sin(2 * math.pi * 105 * t + math.sin(t * 12)) + smooth)
        elif name == "creak":
            value = .12 * math.sin(2 * math.pi * 180 * t + 13 * math.sin(t * 4)) * (.5 + .5 * math.sin(t * 9)) + smooth * .25
        else:
            env = math.sin(math.pi * t / seconds) ** 2
            value = smooth * 1.4 * env
        # Fade all files both in and out, with headroom and no abrupt end.
        fade = min(1, t / .16, (seconds - t) / .6)
        samples.append(round(32767 * max(-.65, min(.65, value * max(0, fade)))))
    return samples


def generate():
    target = ROOT / "app/src/main/assets/sounds"
    target.mkdir(parents=True, exist_ok=True)
    for name, duration in SOUNDS.items():
        data = array("h", render(name, duration))
        if sys.byteorder != "little":
            data.byteswap()
        with wave.open(str(target / (name + ".wav")), "wb") as output:
            output.setnchannels(1)
            output.setsampwidth(2)
            output.setframerate(RATE)
            output.writeframes(data.tobytes())
    print("Generated 12 original, gently faded sound effects.")


if __name__ == "__main__":
    generate()
