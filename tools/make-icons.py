#!/usr/bin/env python3
"""Builds an app's launcher icon from its Nano Banana source art (DESIGN 6.3).

Usage: python3 tools/make-icons.py [learner|coach]   (from the repo root; needs Pillow)

The source is a full-bleed square. Android adaptive icons crop a 108dp layer to a mask
and only promise the middle 66dp. Art drawn edge to edge is shrunk onto a larger canvas
(PAD) to keep its subject inside that safe circle; the new margin is filled by stretching
the art's own edge rows and columns outward and blurring them, so gradients and glows
continue without a seam. Art already framed for the whole 108dp layer uses a PAD of 1.
minSdk is 26, so only adaptive icons are needed: one foreground PNG per density, a
background colour, and the 512px Play Store icon.
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "docs/play-store/assets"
# app -> (source art, Android res dir, 512px Play Store icon)
APPS = {
    "learner": (ASSETS / "source/icon-source.png", ROOT / "androidApp-learner/src/main/res", ASSETS / "icon-512.png"),
    "coach": (ASSETS / "source/coach-icon-source.png", ROOT / "androidApp-coach/src/main/res", ASSETS / "coach-icon-512.png"),
}

# Source edge -> canvas edge. Both sources (2026-10-20) are framed for the full layer: the
# shield and the clipboard reach about 0.28 from the centre, inside the 0.305 safe radius.
PAD = {"learner": 1.0, "coach": 1.0}
# The part of the 108dp layer a launcher shows; the store icon is that same view.
VISIBLE = 72 / 108
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def extend(src: Image.Image, side: int) -> Image.Image:
    """`src` centred on a `side` square whose margin continues the art's own edges."""
    w, h = src.size
    m = (side - w) // 2
    far = side - m - w  # right/bottom margin (may differ from m by a pixel)
    canvas = Image.new("RGB", (side, side))
    # Each outermost row/column stretched across its margin; corners from corner pixels.
    canvas.paste(src.crop((0, 0, w, 1)).resize((w, m)), (m, 0))
    canvas.paste(src.crop((0, h - 1, w, h)).resize((w, far)), (m, m + h))
    canvas.paste(src.crop((0, 0, 1, h)).resize((m, h)), (0, m))
    canvas.paste(src.crop((w - 1, 0, w, h)).resize((far, h)), (m + w, m))
    for box, corner in [((0, 0, m, m), (0, 0)), ((m + w, 0, side, m), (w - 1, 0)),
                        ((0, m + h, m, side), (0, h - 1)), ((m + w, m + h, side, side), (w - 1, h - 1))]:
        canvas.paste(src.getpixel(corner), box)
    canvas.paste(src, (m, m))
    # Soften the stretched band, then feather the original art back over it.
    soft = canvas.filter(ImageFilter.GaussianBlur(side // 40))
    mask = Image.new("L", (side, side), 0)
    inset = m + side // 60
    ImageDraw.Draw(mask).rectangle((inset, inset, side - inset, side - inset), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(side // 80))
    return Image.composite(canvas, soft, mask)


def main():
    app = sys.argv[1] if len(sys.argv) > 1 else "learner"
    SOURCE, RES, STORE = APPS[app]
    src = Image.open(SOURCE).convert("RGB")
    side = round(src.width * PAD[app])
    canvas = extend(src, side) if side > src.width else src

    for name, scale in DENSITIES.items():
        px = round(108 * scale)
        out = RES / f"mipmap-{name}" / "ic_launcher_foreground.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        canvas.resize((px, px), Image.LANCZOS).save(out, optimize=True)

    # Behind the opaque foreground; only seen while a launcher animates the icon.
    bg = src.getpixel((src.width // 2, 8))
    hexcolor = "#{:02X}{:02X}{:02X}".format(*bg)
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

    # Play Store: 512px, 32-bit PNG, full bleed (Play applies its own rounded mask), showing
    # what a launcher shows, so the subject is as big in the store as on the phone.
    STORE.parent.mkdir(parents=True, exist_ok=True)
    cut = round(canvas.width * (1 - VISIBLE) / 2)
    view = canvas.crop((cut, cut, canvas.width - cut, canvas.width - cut))
    view.resize((512, 512), Image.LANCZOS).convert("RGBA").save(STORE, optimize=True)
    print(app, "icons written; background", hexcolor)


if __name__ == "__main__":
    main()
