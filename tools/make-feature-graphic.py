#!/usr/bin/env python3
"""Draws the Play Store feature graphic (1024x500) from the app's own art: the Prime Time
colours, the brand fonts, the learner icon and a field with a play drawn on it, the way the
lessons draw one. Run from the repo root; writes docs/play-store/assets/feature-graphic.png.
"""
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

W, H = 1024, 500
FONTS = Path("shared/ui/src/commonMain/composeResources/font")
OUT = Path("docs/play-store/assets/feature-graphic.png")

NAVY = (10, 16, 32)
TURF = (16, 40, 30)
LINE = (58, 92, 74)
INK = (242, 245, 250)
DIM = (163, 175, 196)
VOLT = (204, 245, 58)
OFFENSE = (121, 174, 255)
DEFENSE = (255, 154, 98)

S = 2  # draw at twice the size, then downsample for smooth edges
img = Image.new("RGB", (W * S, H * S), NAVY)
d = ImageDraw.Draw(img)


def font(name, size):
    return ImageFont.truetype(str(FONTS / name), size * S)


# The field fills the right side: yard lines every 5 yards, a line of scrimmage in volt.
FX = 520
d.rectangle([FX * S, 0, W * S, H * S], fill=TURF)
for i, x in enumerate(range(FX + 20, W, 50)):
    d.line([x * S, 0, x * S, H * S], fill=LINE, width=(3 if i % 2 else 2) * S)
for x in range(FX + 20, W, 10):
    for y in (150, 350):
        d.line([x * S, (y - 6) * S, x * S, (y + 6) * S], fill=LINE, width=1 * S)
LOS = 770
d.line([LOS * S, 30 * S, LOS * S, (H - 30) * S], fill=VOLT, width=4 * S)

# Offense (circles) left of the line, defense (X) right of it, plus two routes.
R = 15
# Seven on the line (five linemen, a tight end, a split end), four off it.
offense = [(745, 170), (745, 210), (745, 250), (745, 290), (745, 330), (745, 370), (745, 95),
           (725, 405), (725, 135), (715, 250), (675, 250)]
for x, y in offense:
    d.ellipse([(x - R) * S, (y - R) * S, (x + R) * S, (y + R) * S], outline=OFFENSE, width=4 * S)
defense = [(800, 230), (800, 270), (800, 190), (800, 310), (850, 250), (850, 170), (850, 330), (905, 125), (905, 378),
           (950, 180), (950, 320)]
for x, y in defense:
    k = 12
    d.line([(x - k) * S, (y - k) * S, (x + k) * S, (y + k) * S], fill=DEFENSE, width=4 * S)
    d.line([(x - k) * S, (y + k) * S, (x + k) * S, (y - k) * S], fill=DEFENSE, width=4 * S)


def route(points):
    pts = [(x * S, y * S) for x, y in points]
    d.line(pts, fill=INK, width=4 * S, joint="curve")
    (x1, y1), (x2, y2) = points[-2], points[-1]
    a = math.atan2(y2 - y1, x2 - x1)
    tip = (x2 * S, y2 * S)
    for side in (2.6, -2.6):
        d.line([tip, ((x2 + 16 * math.cos(a + side)) * S, (y2 + 16 * math.sin(a + side)) * S)], fill=INK, width=4 * S)


route([(762, 95), (900, 95), (960, 140)])   # a corner route
route([(742, 405), (870, 405), (835, 368)])  # a curl

# A soft fade from the navy panel into the field.
fade = Image.new("L", (160 * S, 1))
for i in range(160 * S):
    fade.putpixel((i, 0), int(255 * (1 - i / (160 * S))))
img.paste(Image.new("RGB", (160 * S, H * S), NAVY), (FX * S, 0), fade.resize((160 * S, H * S)))

# Icon and words on the left.
icon = Image.open("docs/play-store/assets/icon-512.png").convert("RGBA").resize((150 * S, 150 * S), Image.LANCZOS)
img.paste(icon, (56 * S, 60 * S), icon)
title = font("barlow_semi_condensed_bold_italic.ttf", 64)
d.text((56 * S, 232 * S), "GRIDIRON", font=title, fill=INK)
d.text((56 * S, 296 * S), "ACADEMY", font=title, fill=VOLT)
tag = font("atkinson_hyperlegible_regular.ttf", 24)
d.text((58 * S, 384 * S), "Learn football, from your first", font=tag, fill=DIM)
d.text((58 * S, 416 * S), "snap to reading the defense.", font=tag, fill=DIM)

OUT.parent.mkdir(parents=True, exist_ok=True)
img.resize((W, H), Image.LANCZOS).save(OUT)
print(OUT)
