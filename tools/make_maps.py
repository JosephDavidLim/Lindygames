#!/usr/bin/env python3
"""Paint the gear icons (no downloads, no samples). Maps are in make_neon.py.

Usage (from the project root):
  python3 tools/make_maps.py

Each map is a 720x400 scene with five floating platforms, in the spirit of a
MapleStory map. Ladders are a separate picture, because the game only draws the
ones that lead somewhere. Every lane has a platform along its bottom edge, marked into
nine tiles so the player can see where a classmate can stand.
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
MAPS = ROOT / "res" / "maps"
GEAR = ROOT / "res" / "gear"
W, H = 720, 400
CELL = 80
INK = (46, 34, 34, 255)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def gradient(top, bottom):
    img = Image.new("RGBA", (W, H))
    d = ImageDraw.Draw(img)
    for y in range(H):
        d.line([(0, y), (W, y)], fill=lerp(top, bottom, y / H) + (255,))
    return img


def platforms(img, top, side, under, trim):
    """Draw five platforms with a lit top edge, tile ticks and a shadow."""
    d = ImageDraw.Draw(img, "RGBA")
    for r in range(5):
        y = (r + 1) * CELL - 24
        d.rectangle([0, y + 24, W, y + 29], fill=(0, 0, 0, 45))
        d.rectangle([0, y, W, y + 24], fill=side + (255,))
        d.rectangle([0, y, W, y + 8], fill=top + (255,))
        d.line([(0, y), (W, y)], fill=trim + (255,), width=2)
        d.line([(0, y + 23), (W, y + 23)], fill=under + (255,), width=2)
        for c in range(10):
            d.line([(c * CELL, y + 1), (c * CELL, y + 22)], fill=under + (150,), width=2)
    return img


def ladder_tile():
    """One ladder, drawn by the game only where both lanes it joins are open."""
    img = Image.new("RGBA", (40, 78), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")
    rail = (112, 74, 44, 255)
    rung = (196, 150, 96, 255)
    d.rectangle([8, 0, 13, 77], fill=rail, outline=INK)
    d.rectangle([27, 0, 32, 77], fill=rail, outline=INK)
    for y in range(7, 76, 12):
        d.rectangle([13, y, 27, y + 4], fill=rung, outline=INK)
    img.save(MAPS / "ladder.png")


def window(d, x, y, w, h, sky=(150, 215, 245)):
    d.rounded_rectangle([x, y, x + w, y + h], 5, fill=(246, 240, 224, 255), outline=INK, width=2)
    d.rectangle([x + 5, y + 5, x + w - 5, y + h - 5], fill=sky + (255,))
    d.ellipse([x + 8, y + h - 18, x + w // 2 + 6, y + h - 6], fill=(255, 255, 255, 220))
    d.line([(x + w // 2, y + 5), (x + w // 2, y + h - 5)], fill=(246, 240, 224, 255), width=3)
    d.line([(x + 5, y + h // 2), (x + w - 5, y + h // 2)], fill=(246, 240, 224, 255), width=3)


def classroom():
    img = gradient((250, 226, 178), (232, 192, 134))
    d = ImageDraw.Draw(img, "RGBA")
    for x in range(0, W, 26):
        d.line([(x, 0), (x, H)], fill=(255, 255, 255, 18))
    for r in range(5):
        y = r * CELL + 6
        if r % 2 == 0:
            d.rounded_rectangle([90, y + 2, 390, y + 46], 4, fill=(44, 92, 72, 255), outline=(120, 80, 44, 255), width=4)
            for i, text_y in enumerate((y + 14, y + 26)):
                d.line([(108, text_y), (250 + i * 60, text_y)], fill=(235, 240, 225, 150), width=2)
            d.ellipse([310, y + 12, 340, y + 38], outline=(235, 240, 225, 150), width=2)
            window(d, 480, y + 2, 70, 44)
            window(d, 590, y + 2, 70, 44)
        else:
            for x in (130, 280, 430, 590):
                d.rounded_rectangle([x, y + 22, x + 74, y + 34], 3, fill=(176, 118, 66, 255), outline=INK, width=2)
                d.rectangle([x + 8, y + 34, x + 13, y + 50], fill=(110, 76, 46, 255))
                d.rectangle([x + 60, y + 34, x + 65, y + 50], fill=(110, 76, 46, 255))
                d.rectangle([x + 28, y + 13, x + 46, y + 22], fill=(226, 80, 70, 255), outline=INK)
    platforms(img, (214, 166, 104), (168, 118, 72), (112, 76, 46), (244, 214, 160))
    return img


def hallway():
    img = gradient((196, 222, 234), (150, 184, 204))
    d = ImageDraw.Draw(img, "RGBA")
    colors = [(64, 112, 176), (58, 100, 160)]
    for r in range(5):
        y = r * CELL + 2
        for i, x in enumerate(range(0, W, 44)):
            d.rectangle([x + 1, y, x + 42, y + 53], fill=colors[(i + r) % 2] + (255,), outline=(30, 52, 92, 255))
            for v in (6, 11, 16):
                d.line([(x + 12, y + v), (x + 31, y + v)], fill=(24, 40, 74, 255), width=2)
            d.rectangle([x + 33, y + 28, x + 37, y + 36], fill=(220, 225, 232, 255))
        if r % 2 == 1:
            d.rounded_rectangle([250, y + 6, 470, y + 44], 4, fill=(248, 242, 222, 255), outline=(150, 40, 40, 255), width=3)
            d.line([(268, y + 20), (452, y + 20)], fill=(150, 40, 40, 200), width=3)
            d.line([(268, y + 32), (410, y + 32)], fill=(70, 70, 80, 160), width=2)
    platforms(img, (214, 220, 226), (150, 158, 168), (92, 100, 112), (246, 250, 252))
    return img


def cafeteria():
    img = gradient((255, 238, 196), (250, 200, 150))
    d = ImageDraw.Draw(img, "RGBA")
    for y in range(0, H, 28):
        for x in range(0, W, 28):
            if (x + y) % 56 == 0:
                d.rectangle([x, y, x + 27, y + 27], fill=(255, 255, 255, 46))
    for r in range(5):
        y = r * CELL + 6
        if r % 2 == 0:
            d.rounded_rectangle([60, y, 320, y + 46], 6, fill=(198, 206, 212, 255), outline=INK, width=2)
            for i, c in enumerate([(236, 92, 74), (120, 190, 90), (245, 196, 66), (240, 240, 230)]):
                x = 78 + i * 58
                d.rounded_rectangle([x, y + 10, x + 46, y + 34], 4, fill=(120, 130, 138, 255))
                d.ellipse([x + 5, y + 13, x + 41, y + 31], fill=c + (255,))
            for sx in (420, 530, 640):
                for k in range(3):
                    off = int(4 * math.sin(k * 2))
                    d.arc([sx + off, y + 2 + k * 10, sx + 20 + off, y + 16 + k * 10], 200, 340, fill=(255, 255, 255, 170), width=2)
        else:
            for x in (120, 340, 560):
                d.rounded_rectangle([x, y + 22, x + 110, y + 34], 3, fill=(238, 112, 88, 255), outline=INK, width=2)
                d.rectangle([x + 14, y + 34, x + 20, y + 50], fill=(120, 130, 138, 255))
                d.rectangle([x + 90, y + 34, x + 96, y + 50], fill=(120, 130, 138, 255))
                d.rectangle([x + 42, y + 8, x + 62, y + 22], fill=(255, 255, 255, 255), outline=INK)
                d.polygon([(x + 42, y + 8), (x + 52, y + 1), (x + 62, y + 8)], fill=(92, 160, 230, 255), outline=INK)
    platforms(img, (240, 170, 120), (206, 124, 84), (146, 80, 56), (255, 222, 180))
    return img


def library():
    img = gradient((110, 76, 56), (74, 50, 42))
    d = ImageDraw.Draw(img, "RGBA")
    rnd = random.Random(4)
    book = [(186, 62, 56), (58, 112, 150), (70, 130, 88), (220, 170, 70), (130, 80, 150), (216, 206, 180)]
    for r in range(5):
        y = r * CELL + 2
        d.rectangle([0, y + 48, W, y + 54], fill=(60, 38, 28, 255))
        x = 2
        while x < W - 14:
            bw = rnd.randint(9, 17)
            bh = rnd.randint(30, 45)
            c = rnd.choice(book)
            d.rectangle([x, y + 48 - bh, x + bw, y + 48], fill=c + (255,), outline=(40, 26, 22, 255))
            d.line([(x + 3, y + 54 - bh), (x + bw - 3, y + 54 - bh)], fill=(255, 240, 200, 170))
            x += bw + (rnd.randint(14, 40) if rnd.random() < 0.06 else 1)
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    for cx, cy in ((150, 30), (560, 110), (290, 190), (620, 270), (100, 350)):
        gd.ellipse([cx - 90, cy - 70, cx + 90, cy + 70], fill=(255, 214, 130, 60))
    img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(26)))
    platforms(img, (150, 96, 62), (104, 66, 46), (62, 38, 28), (204, 150, 96))
    return img


def gym():
    img = gradient((246, 214, 150), (216, 164, 104))
    d = ImageDraw.Draw(img, "RGBA")
    for r in range(5):
        y = r * CELL + 2
        if r % 2 == 0:
            for row in range(3):
                d.rectangle([0, y + row * 18, W, y + row * 18 + 12], fill=(116, 136, 170, 255))
                d.line([(0, y + row * 18), (W, y + row * 18)], fill=(214, 224, 240, 255), width=2)
                for x in range(10 + row * 12, W, 44):
                    c = [(230, 96, 80), (90, 150, 220), (246, 200, 70), (120, 190, 110)][(x + row) % 4]
                    d.ellipse([x, y + row * 18 - 9, x + 11, y + row * 18 + 2], fill=(244, 206, 170, 255), outline=INK)
                    d.rounded_rectangle([x - 2, y + row * 18 + 2, x + 13, y + row * 18 + 12], 3, fill=c + (255,))
        else:
            for k in range(0, W, 36):
                c = (214, 60, 60) if k % 72 == 0 else (250, 240, 220)
                d.polygon([(k, y), (k + 34, y), (k + 17, y + 22)], fill=c + (255,), outline=INK)
            d.ellipse([320, y + 18, 400, y + 54], outline=(190, 60, 44, 255), width=4)
            d.rectangle([348, y + 2, 372, y + 20], fill=(255, 255, 255, 255), outline=INK)
    platforms(img, (226, 170, 94), (184, 122, 62), (124, 78, 40), (252, 216, 150))
    return img


def admissions():
    img = gradient((70, 76, 116), (44, 44, 84))
    d = ImageDraw.Draw(img, "RGBA")
    rnd = random.Random(9)
    for r in range(5):
        y = r * CELL + 3
        for x in range(10, W, 112):
            d.rounded_rectangle([x, y, x + 90, y + 50], 3, fill=(116, 92, 70, 255), outline=(40, 30, 30, 255), width=2)
            for row in range(3):
                d.line([(x + 6, y + 9 + row * 15), (x + 84, y + 9 + row * 15)], fill=(70, 52, 40, 255), width=2)
                for k in range(rnd.randint(1, 4)):
                    fx = x + 8 + k * 18
                    d.rectangle([fx, y + 2 + row * 15, fx + 14, y + 9 + row * 15], fill=(240, 226, 170, 255), outline=(120, 100, 60, 255))
        if r in (1, 3):
            d.rounded_rectangle([270, y + 2, 450, y + 46], 4, fill=(245, 236, 208, 255), outline=(180, 150, 70, 255), width=4)
            d.ellipse([405, y + 22, 433, y + 42], fill=(200, 50, 50, 255), outline=INK)
            d.line([(288, y + 16), (420, y + 16)], fill=(90, 80, 70, 200), width=2)
            d.line([(288, y + 28), (390, y + 28)], fill=(90, 80, 70, 200), width=2)
    platforms(img, (146, 120, 180), (100, 80, 136), (58, 44, 90), (204, 186, 232))
    return img


def tile(name, draw):
    img = Image.new("RGBA", (CELL, CELL), (0, 0, 0, 0))
    draw(ImageDraw.Draw(img, "RGBA"))
    img.save(MAPS / f"tile_{name}.png")


def locker_tile(d):
    d.rounded_rectangle([12, 2, 68, 72], 4, fill=(226, 110, 66, 255), outline=INK, width=3)
    d.line([(40, 4), (40, 71)], fill=INK, width=2)
    for x in (18, 46):
        for y in (12, 19, 26):
            d.line([(x, y), (x + 16, y)], fill=(130, 56, 34, 255), width=2)
    d.rectangle([33, 36, 37, 46], fill=(235, 235, 240, 255))
    d.rectangle([43, 36, 47, 46], fill=(235, 235, 240, 255))
    d.ellipse([26, 72, 54, 78], fill=(0, 0, 0, 60))


def milk_tile(d):
    d.ellipse([6, 52, 74, 76], fill=(255, 255, 255, 235), outline=(190, 210, 230, 255), width=2)
    d.ellipse([52, 46, 72, 58], fill=(255, 255, 255, 235), outline=(190, 210, 230, 255))
    d.ellipse([14, 58, 34, 66], fill=(214, 236, 250, 255))
    d.polygon([(22, 44), (38, 36), (54, 50), (38, 58)], fill=(245, 248, 255, 255), outline=INK)
    d.polygon([(22, 44), (38, 36), (34, 26), (24, 32)], fill=(90, 160, 230, 255), outline=INK)


def carpet_tile(d):
    d.rounded_rectangle([3, 50, 77, 78], 5, fill=(168, 40, 60, 235), outline=(236, 196, 90, 255), width=3)
    d.rectangle([12, 57, 68, 71], outline=(236, 196, 90, 200))
    for x in range(20, 64, 14):
        d.polygon([(x, 60), (x + 4, 64), (x, 68), (x - 4, 64)], fill=(236, 196, 90, 220))


def hype_tile(d):
    d.rounded_rectangle([4, 50, 76, 78], 5, fill=(60, 110, 220, 235), outline=(255, 226, 80, 255), width=3)
    pts = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        rad = 12 if i % 2 == 0 else 5
        pts.append((40 + rad * math.cos(a) * 1.5, 64 + rad * math.sin(a)))
    d.polygon(pts, fill=(255, 226, 80, 255), outline=INK)


TIER = [(190, 190, 196), (90, 150, 240), (178, 96, 230), (250, 190, 50)]


def icon(name, tier, draw):
    s = 256
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")
    draw(d, TIER[tier], tier)
    img = img.resize((64, 64), Image.LANCZOS)
    img.save(GEAR / f"{name}.png")


def weapon(d, c, tier):
    if tier < 2:
        pts = [(50, 190), (180, 50), (214, 82), (84, 222)]
        d.polygon(pts, fill=(216, 170, 104, 255) if tier == 0 else (190, 202, 214, 255), outline=INK, width=8)
        for i in range(1, 8):
            x, y = 50 + i * 16, 190 - i * 17
            d.line([(x, y), (x + 15, y + 15)], fill=INK, width=5)
    elif tier == 2:
        d.rounded_rectangle([70, 30, 186, 226], 14, fill=(66, 72, 92, 255), outline=INK, width=8)
        d.rectangle([86, 48, 170, 96], fill=(170, 220, 170, 255), outline=INK, width=5)
        d.line([(92, 86), (112, 62), (134, 78), (164, 54)], fill=(40, 90, 50, 255), width=5)
        for r in range(3):
            for k in range(3):
                d.ellipse([88 + k * 30, 112 + r * 34, 112 + k * 30, 136 + r * 34], fill=c + (255,), outline=INK, width=3)
    else:
        d.pieslice([28, 56, 228, 256], 180, 360, fill=c + (255,), outline=INK, width=8)
        d.pieslice([78, 106, 178, 206], 180, 360, fill=(0, 0, 0, 0), outline=INK, width=6)
        for a in range(190, 360, 20):
            r = math.radians(a)
            d.line([(128 + 84 * math.cos(r), 156 + 84 * math.sin(r)), (128 + 100 * math.cos(r), 156 + 100 * math.sin(r))], fill=INK, width=4)


def hat(d, c, tier):
    if tier == 0:
        d.pieslice([50, 60, 206, 220], 180, 360, fill=(226, 90, 80, 255), outline=INK, width=8)
        d.rounded_rectangle([44, 132, 212, 176], 12, fill=(240, 230, 210, 255), outline=INK, width=8)
        d.ellipse([110, 34, 146, 70], fill=(240, 230, 210, 255), outline=INK, width=6)
    elif tier == 1:
        d.pieslice([54, 60, 202, 220], 180, 360, fill=c + (255,), outline=INK, width=8)
        d.rounded_rectangle([40, 134, 216, 164], 12, fill=(250, 214, 80, 255), outline=INK, width=8)
        d.ellipse([112, 72, 144, 112], fill=(255, 240, 140, 255), outline=INK, width=5)
        d.line([(128, 60), (128, 34)], fill=INK, width=6)
    elif tier == 2:
        d.polygon([(128, 60), (236, 106), (128, 150), (20, 106)], fill=(52, 44, 84, 255), outline=INK)
        d.polygon([(72, 134), (184, 134), (176, 190), (80, 190)], fill=(70, 60, 110, 255), outline=INK)
        d.line([(128, 104), (204, 134), (204, 190)], fill=c + (255,), width=8)
        d.ellipse([190, 184, 218, 212], fill=c + (255,), outline=INK, width=4)
    else:
        for i in range(9):
            a = math.radians(200 + i * 18)
            x, y = 128 + 88 * math.cos(a), 170 + 88 * math.sin(a)
            d.ellipse([x - 24, y - 15, x + 24, y + 15], fill=(120, 190, 90, 255), outline=INK, width=4)
        d.rectangle([60, 170, 196, 186], fill=c + (255,), outline=INK)


def shoe(d, c, tier):
    body = [(190, 190, 190), (226, 90, 80), (90, 150, 240), (250, 190, 50)][tier]
    d.polygon([(48, 78), (110, 78), (124, 124), (214, 150), (222, 192), (48, 192)], fill=body + (255,), outline=INK)
    d.rectangle([44, 192, 226, 214], fill=(246, 246, 240, 255), outline=INK, width=6)
    for i in range(3):
        d.line([(118 + i * 18, 118 + i * 6), (106 + i * 18, 142 + i * 6)], fill=(250, 250, 250, 255), width=7)
    if tier == 1:
        for x in (66, 110, 154, 198):
            d.polygon([(x - 8, 214), (x + 8, 214), (x, 236)], fill=(90, 90, 100, 255), outline=INK)
    if tier == 3:
        d.polygon([(48, 96), (6, 62), (18, 108), (2, 118), (46, 146)], fill=(255, 255, 255, 255), outline=INK)


def charm(d, c, tier):
    if tier == 0:
        d.polygon([(60, 196), (180, 62), (206, 86), (86, 220)], fill=(250, 204, 60, 255), outline=INK)
        d.polygon([(60, 196), (86, 220), (42, 236)], fill=(244, 214, 170, 255), outline=INK)
        d.polygon([(180, 62), (206, 86), (222, 70), (196, 46)], fill=(240, 130, 150, 255), outline=INK)
    elif tier == 1:
        for cx, cy in ((98, 90), (158, 90), (98, 150), (158, 150)):
            d.ellipse([cx - 38, cy - 38, cx + 38, cy + 38], fill=(100, 190, 100, 255), outline=INK, width=7)
        d.line([(128, 130), (160, 240)], fill=(60, 120, 60, 255), width=12)
    else:
        d.polygon([(90, 20), (128, 96), (166, 20)], fill=(210, 60, 60, 255) if tier == 3 else (70, 110, 210, 255), outline=INK)
        d.ellipse([62, 76, 194, 208], fill=c + (255,), outline=INK, width=8)
        pts = []
        for i in range(10):
            a = -math.pi / 2 + i * math.pi / 5
            r = 46 if i % 2 == 0 else 20
            pts.append((128 + r * math.cos(a), 142 + r * math.sin(a)))
        d.polygon(pts, fill=(255, 250, 220, 255), outline=INK)


GEAR_ITEMS = {
    "wooden_ruler": (0, weapon), "steel_ruler": (1, weapon), "graphing_calculator": (2, weapon), "golden_protractor": (3, weapon),
    "beanie": (0, hat), "thinking_cap": (1, hat), "graduation_cap": (2, hat), "laurel_crown": (3, hat),
    "worn_sneakers": (0, shoe), "track_spikes": (1, shoe), "hall_pass_runners": (2, shoe), "winged_loafers": (3, shoe),
    "lucky_pencil": (0, charm), "four_leaf_bookmark": (1, charm), "attendance_pin": (2, charm), "valedictorian_medal": (3, charm),
}


def main():
    GEAR.mkdir(parents=True, exist_ok=True)
    for name, (tier, fn) in GEAR_ITEMS.items():
        icon(name, tier, fn)
    print("wrote", len(GEAR_ITEMS), "gear icons")


if __name__ == "__main__":
    main()
