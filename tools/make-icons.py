#!/usr/bin/env python3
"""Builds the learner app's launcher icon from the Nano Banana source art (DESIGN 6.3).

Usage: python3 tools/make-icons.py   (from the repo root; needs Pillow)

The source is a full-bleed square on turf green. Android adaptive icons crop to a mask
inside a 108dp canvas and only promise the middle 66dp, so the art is shrunk onto a
larger square of the same green (PAD) to keep the whole ball inside that safe circle.
minSdk is 26, so only adaptive icons are needed: one foreground PNG per density, a
background colour, and the 512px Play Store icon.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "docs/play-store/assets/source/icon-source.png"
RES = ROOT / "androidApp-learner/src/main/res"
STORE = ROOT / "docs/play-store/assets/icon-512.png"

PAD = 1.4  # source edge -> canvas edge; ball radius ~0.35 of source -> ~0.25 of canvas (< 0.305 safe)
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def main():
    src = Image.open(SOURCE).convert("RGB")
    green = src.getpixel((8, 8))  # the art's own background
    side = round(src.width * PAD)
    canvas = Image.new("RGB", (side, side), green)
    canvas.paste(src, ((side - src.width) // 2, (side - src.height) // 2))

    for name, scale in DENSITIES.items():
        px = round(108 * scale)
        out = RES / f"mipmap-{name}" / "ic_launcher_foreground.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        canvas.resize((px, px), Image.LANCZOS).save(out, optimize=True)

    hexcolor = "#{:02X}{:02X}{:02X}".format(*green)
    (RES / "values").mkdir(parents=True, exist_ok=True)
    (RES / "values/ic_launcher_background.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
        f'    <!-- Sampled from the icon art by tools/make-icons.py. -->\n'
        f'    <color name="ic_launcher_background">{hexcolor}</color>\n</resources>\n'
    )
    adaptive = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@color/ic_launcher_background" />\n'
        '    <foreground android:drawable="@mipmap/ic_launcher_foreground" />\n'
        '</adaptive-icon>\n'
    )
    (RES / "mipmap-anydpi-v26").mkdir(parents=True, exist_ok=True)
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        (RES / "mipmap-anydpi-v26" / name).write_text(adaptive)

    # Play Store: 512px, 32-bit PNG; Play applies its own corner mask.
    STORE.parent.mkdir(parents=True, exist_ok=True)
    src.resize((512, 512), Image.LANCZOS).convert("RGBA").save(STORE, optimize=True)
    print("icons written; background", hexcolor)


if __name__ == "__main__":
    main()
