#!/usr/bin/env python3
"""Paint the neon look of Curve City (no downloads, no samples).

Usage (from the project root):
  python3 tools/make_neon.py

Writes the battle maps, walkable corridors, special tiles, title screen, and a
night-lit copy of every character sprite with its constructed implant.
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
MAPS = ROOT / "res" / "maps"
UI = ROOT / "res" / "ui"
SPRITES = ROOT / "res" / "sprites"
NEON = SPRITES / "neon"
INK = (8, 8, 20, 255)

# wall dark, wall light, main neon, second neon, floor
PALETTES = {
    "classroom": ((26, 22, 58), (44, 40, 86), (70, 210, 255), (255, 70, 170), (14, 18, 46)),
    "hallway": ((16, 26, 66), (30, 52, 100), (60, 170, 255), (255, 90, 130), (10, 20, 48)),
    "cafeteria": ((44, 18, 56), (76, 30, 74), (255, 90, 190), (90, 240, 220), (26, 12, 44)),
    "library": ((30, 16, 52), (54, 28, 82), (190, 110, 255), (255, 180, 80), (18, 10, 40)),
    "gym": ((12, 34, 56), (20, 60, 86), (70, 255, 190), (255, 80, 120), (8, 24, 44)),
    "admissions": ((36, 10, 34), (70, 16, 54), (255, 50, 90), (120, 170, 255), (24, 6, 30)),
}


def font(size):
    for name in ("/System/Library/Fonts/Supplemental/Arial Bold.ttf", "/System/Library/Fonts/Helvetica.ttc", "DejaVuSans-Bold.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def screen(base, glow):
    return ImageChops.screen(base.convert("RGB"), glow.convert("RGB")).convert("RGBA")


def bloom(img, amount=9, strength=0.55):
    blurred = img.convert("RGB").filter(ImageFilter.GaussianBlur(amount))
    return screen(img, blurred.point(lambda v: int(max(0, v - 60) * strength)))


def light(w, h, spots):
    """A black layer with soft colored lights, to be screened over a scene."""
    g = Image.new("RGB", (w, h), (0, 0, 0))
    d = ImageDraw.Draw(g)
    for x, y, rx, ry, c in spots:
        d.ellipse([x - rx, y - ry, x + rx, y + ry], fill=c)
    return g.filter(ImageFilter.GaussianBlur(max(10, min(w, h) // 22)))


def dim(c, f):
    return tuple(int(v * f) for v in c[:3])


def locker(d, x, y, w, h, c):
    d.rectangle([x, y, x + w, y + h], fill=dim(c, 0.55) + (255,), outline=dim(c, 0.95) + (255,))
    for k in range(3):
        d.line([(x + w * 0.2, y + 6 + k * 5), (x + w * 0.8, y + 6 + k * 5)], fill=dim(c, 0.3) + (255,))
    d.rectangle([x + w - 6, y + h * 0.45, x + w - 3, y + h * 0.6], fill=(220, 230, 255, 255))


def vending(d, x, y, w, h, neon, rnd):
    d.rounded_rectangle([x, y, x + w, y + h], 4, fill=(16, 14, 32, 255), outline=dim(neon, 0.8) + (255,), width=2)
    d.rectangle([x + 6, y + 8, x + w * 0.68, y + h * 0.72], fill=dim(neon, 0.75) + (255,))
    for k in range(4):
        yy = y + 14 + k * (h * 0.15)
        d.line([(x + 8, yy), (x + w * 0.66, yy)], fill=(230, 250, 255, 255), width=2)
        for j in range(3):
            d.rectangle([x + 10 + j * w * 0.2, yy - 8, x + 15 + j * w * 0.2, yy - 1], fill=rnd.choice([(255, 90, 170, 255), (255, 210, 90, 255), (120, 255, 200, 255)]))
    d.rectangle([x + 8, y + h * 0.78, x + w - 8, y + h * 0.92], fill=(6, 6, 16, 255), outline=dim(neon, 0.6) + (255,))


def window_with_posters(d, x, y, w, h, a, b, rnd):
    x, y, w, h = int(x), int(y), int(w), int(h)
    d.rectangle([x, y, x + w, y + h], fill=(10, 12, 34, 255), outline=dim(a, 0.7) + (255,), width=2)
    d.rectangle([x + 4, y + 4, x + w - 4, y + h - 4], fill=dim(b, 0.25) + (255,))
    for _ in range(3):
        px = rnd.randint(x + 6, x + w - 26)
        py = rnd.randint(y + 6, y + max(7, h - 34))
        c = rnd.choice([(235, 235, 245), (250, 210, 120), (120, 240, 200), (255, 130, 190)])
        pw, ph = rnd.randint(14, 22), rnd.randint(20, 28)
        d.polygon([(px, py), (px + pw, py + 1), (px + pw - 1, py + ph), (px + 1, py + ph - 1)], fill=c + (235,))
        for k in range(3):
            d.line([(px + 3, py + 5 + k * 5), (px + pw - 4, py + 5 + k * 5)], fill=(40, 40, 70, 255))


def string_lights(d, x0, x1, y, sag, rnd):
    pts = []
    n = max(4, int((x1 - x0) // 34))
    for i in range(n + 1):
        t = i / n
        pts.append((x0 + (x1 - x0) * t, y + sag * math.sin(math.pi * t)))
    d.line(pts, fill=(6, 6, 16, 255), width=2)
    colors = [(255, 70, 110), (255, 200, 70), (80, 240, 170), (80, 170, 255), (255, 110, 230)]
    for px, py in pts[1:-1]:
        d.ellipse([px - 5, py, px + 5, py + 12], fill=rnd.choice(colors) + (255,), outline=(6, 6, 16, 255))


def wet_sign(d, x, y):
    d.polygon([(x, y), (x + 22, y), (x + 30, y + 44), (x - 8, y + 44)], fill=(214, 226, 40, 255), outline=(30, 34, 8, 255))
    d.polygon([(x + 11, y + 10), (x + 20, y + 28), (x + 2, y + 28)], outline=(30, 34, 8, 255))


def ledger_sign(d, x, y, w, text, c):
    d.rounded_rectangle([x, y, x + w, y + 24], 4, fill=(8, 8, 20, 255), outline=c + (255,), width=2)
    d.text((x + w / 2, y + 12), text, font=font(13), fill=c + (255,), anchor="mm")


SIGNS = ["ATTENDANCE IS MANDATORY", "YOUR GRADE IS WATCHING", "GRADUATION: DO NOT ASK", "REPORT ANY HOPE", "LATE IS ABSENT", "SMILE: YOU ARE RANKED", "THE CURVE PROVIDES", "ALL RECORDS ARE FINAL"]


def wall_props(img, x0, x1, top, bottom, pal, name, rnd, scale=1.0):
    d = ImageDraw.Draw(img, "RGBA")
    wall_d, wall_l, a, b, floor = pal
    h = bottom - top
    tile_top = int(top + h * 0.62)
    d.rectangle([x0, tile_top, x1, bottom], fill=dim(wall_l, 0.85) + (255,))
    bh = max(10, int(16 * scale))
    for row, yy in enumerate(range(int(tile_top), bottom, bh)):
        d.line([(x0, yy), (x1, yy)], fill=dim(wall_d, 0.8) + (255,))
        for xx in range(int(x0 + (row % 2) * bh), int(x1), int(bh * 2)):
            d.line([(xx, yy), (xx, yy + bh)], fill=dim(wall_d, 0.8) + (255,))
    x = x0 + rnd.randint(4, 30)
    while x < x1 - 40:
        pick = rnd.random()
        if name in ("hallway", "admissions") and pick < 0.55 or pick < 0.2:
            count = rnd.randint(3, 6)
            lw = int(26 * scale) + 2
            for i in range(count):
                c = rnd.choice([a, b, wall_l])
                locker(d, x + i * lw, top + h * 0.12, lw - 2, h * 0.86, c)
            x += count * lw + rnd.randint(14, 40)
        elif pick < 0.45:
            w = int(58 * scale) + 12
            vending(d, x, top + h * 0.06, w, h * 0.92, a if rnd.random() < 0.6 else b, rnd)
            x += w + rnd.randint(14, 44)
        elif pick < 0.8:
            w = int(120 * scale) + rnd.randint(10, 60)
            window_with_posters(d, x, top + h * 0.1, w, h * 0.46, a, b, rnd)
            x += w + rnd.randint(14, 40)
        else:
            w = int(150 * scale) + 40
            ledger_sign(d, x, top + h * 0.18, w, rnd.choice(SIGNS), a if rnd.random() < 0.5 else b)
            x += w + rnd.randint(14, 40)


def wet_floor(img, x0, x1, top, bottom, pal, rnd):
    """A floor of wet tiles that mirrors the wall above it."""
    wall_d, wall_l, a, b, floor = pal
    h = bottom - top
    src = img.crop((x0, max(0, top - h), x1, top)).convert("RGB").transpose(Image.FLIP_TOP_BOTTOM).filter(ImageFilter.GaussianBlur(2))
    base = Image.new("RGB", src.size, floor)
    mixed = Image.blend(base, src, 0.45)
    img.paste(mixed, (x0, top))
    d = ImageDraw.Draw(img, "RGBA")
    step = max(16, h // 3)
    for yy in range(top, bottom, max(6, h // 4)):
        d.line([(x0, yy), (x1, yy)], fill=(0, 0, 0, 90))
    for xx in range(x0, x1, step * 2):
        d.line([(xx, top), (xx, bottom)], fill=(0, 0, 0, 70))
    puddles = Image.new("RGBA", img.size, (0, 0, 0, 0))
    pd = ImageDraw.Draw(puddles)
    for _ in range((x1 - x0) // 130):
        px = rnd.randint(x0, x1)
        c = rnd.choice([a, b])
        y0 = top + rnd.randint(2, max(3, h // 2))
        pd.ellipse([px - rnd.randint(30, 90), y0, px + rnd.randint(30, 90), y0 + rnd.randint(3, max(4, h // 3))], fill=c + (70,))
    img.alpha_composite(puddles.filter(ImageFilter.GaussianBlur(max(2, h // 14))))


def battle_map(name):
    pal = PALETTES[name]
    wall_d, wall_l, a, b, floor = pal
    rnd = random.Random(sum(map(ord, name)))
    W, H = 720, 400
    img = Image.new("RGBA", (W, H), wall_d + (255,))
    d = ImageDraw.Draw(img, "RGBA")
    spots = []
    for r in range(5):
        top = r * 80
        floor_y = top + 56
        d = ImageDraw.Draw(img, "RGBA")
        d.rectangle([0, top, W, floor_y], fill=wall_d + (255,))
        wall_props(img, 0, W, top + 3, floor_y, pal, name, rnd, 0.62)
        shade = Image.new("RGBA", (W, floor_y - top), (6, 6, 22, 165))
        img.alpha_composite(shade, (0, top))
        wet_floor(img, 0, W, floor_y, top + 80, pal, rnd)
        d = ImageDraw.Draw(img, "RGBA")
        d.rectangle([0, top, W, top + 2], fill=(4, 4, 12, 255))
        for c in range(10):
            d.line([(c * 80, floor_y), (c * 80, top + 80)], fill=(255, 255, 255, 30))
        for lx in range(rnd.randint(40, 120), W, rnd.randint(190, 260)):
            spots.append((lx, top + 18, 70, 22, dim(rnd.choice([a, b]), 0.3)))
    img = screen(img, light(W, H, spots))
    d = ImageDraw.Draw(img, "RGBA")
    for r in range(5):
        floor_y = r * 80 + 56
        d.line([(0, floor_y), (W, floor_y)], fill=a + (255,), width=2)
        d.line([(0, floor_y + 23), (W, floor_y + 23)], fill=(0, 0, 0, 200), width=2)
    img.convert("RGB").save(MAPS / f"{name}.png")


def corridor(name):
    """A long side-on corridor the player walks through, painted for scrolling."""
    pal = PALETTES[name]
    wall_d, wall_l, a, b, floor = pal
    rnd = random.Random(sum(map(ord, name)) + 7)
    W, H = 1440, 554
    ceil = 74
    fl = 430
    img = Image.new("RGBA", (W, H), wall_d + (255,))
    d = ImageDraw.Draw(img, "RGBA")
    d.rectangle([0, 0, W, ceil], fill=dim(wall_l, 0.5) + (255,))
    for x in range(0, W, 60):
        d.line([(x, 0), (x, ceil)], fill=(0, 0, 0, 120))
    for y in range(0, ceil, 24):
        d.line([(0, y), (W, y)], fill=(0, 0, 0, 120))
    d.line([(0, ceil), (W, ceil)], fill=(0, 0, 0, 255), width=4)
    wall_props(img, 0, W, ceil + 4, fl, pal, name, rnd, 1.25)
    img.alpha_composite(Image.new("RGBA", (W, fl - ceil), (6, 6, 24, 90)), (0, ceil))
    d = ImageDraw.Draw(img, "RGBA")
    for x in range(120, W, 300):
        string_lights(d, x, x + 220, ceil + 6, 26, rnd)
    wet_floor(img, 0, W, fl, H, pal, rnd)
    d = ImageDraw.Draw(img, "RGBA")
    spots = []
    for x in range(80, W, 240):
        c = a if (x // 240) % 2 == 0 else b
        d.rounded_rectangle([x - 60, 12, x + 60, 26], 4, fill=(250, 250, 255, 255), outline=c + (255,), width=2)
        spots.append((x, 60, 130, 70, dim(c, 0.34)))
        spots.append((x, 500, 110, 30, dim(c, 0.22)))
    wet_sign(d, 560, 396)
    wet_sign(d, 1120, 396)
    img = screen(img, light(W, H, spots))
    img = bloom(img, 6, 0.3)
    img.convert("RGB").save(MAPS / f"corridor_{name}.png")


def tile(name, draw):
    img = Image.new("RGBA", (80, 80), (0, 0, 0, 0))
    draw(ImageDraw.Draw(img, "RGBA"))
    glow = img.filter(ImageFilter.GaussianBlur(4))
    out = Image.new("RGBA", (80, 80), (0, 0, 0, 0))
    out.alpha_composite(glow)
    out.alpha_composite(img)
    out.save(MAPS / f"tile_{name}.png")


def locker_tile(d):
    d.rectangle([12, 2, 68, 74], fill=(24, 30, 70, 255), outline=(255, 70, 110, 255), width=3)
    d.line([(40, 4), (40, 73)], fill=(255, 70, 110, 255), width=2)
    for x in (18, 46):
        for y in (12, 19, 26):
            d.line([(x, y), (x + 16, y)], fill=(90, 110, 190, 255), width=2)
    d.rectangle([20, 40, 34, 46], fill=(255, 60, 80, 255))
    d.rectangle([46, 40, 60, 46], fill=(255, 60, 80, 255))
    d.text((40, 62), "LOCK", font=font(11), fill=(255, 150, 170, 255), anchor="mm")


def milk_tile(d):
    d.ellipse([4, 54, 76, 78], fill=(60, 255, 220, 170), outline=(170, 255, 240, 255), width=2)
    d.ellipse([18, 59, 44, 68], fill=(200, 255, 245, 200))
    d.ellipse([52, 48, 72, 58], fill=(60, 255, 220, 170))


def carpet_tile(d):
    d.rounded_rectangle([3, 52, 77, 78], 5, fill=(70, 30, 150, 180), outline=(190, 120, 255, 255), width=2)
    for x in range(10, 74, 10):
        d.arc([x - 6, 58, x + 6, 72], 180, 360, fill=(220, 170, 255, 255), width=2)


def hype_tile(d):
    d.rounded_rectangle([4, 52, 76, 78], 5, fill=(10, 90, 70, 200), outline=(90, 255, 190, 255), width=3)
    for x in (24, 40, 56):
        d.polygon([(x - 6, 72), (x + 6, 72), (x, 58)], fill=(150, 255, 210, 255))


def ladder():
    img = Image.new("RGBA", (40, 78), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")
    c = (120, 240, 255, 255)
    d.rectangle([9, 0, 12, 77], fill=c)
    d.rectangle([28, 0, 31, 77], fill=c)
    for y in range(7, 76, 12):
        d.rectangle([12, y, 28, y + 2], fill=(220, 250, 255, 255))
    glow = img.filter(ImageFilter.GaussianBlur(3))
    out = Image.new("RGBA", (40, 78), (0, 0, 0, 0))
    out.alpha_composite(glow)
    out.alpha_composite(img)
    out.save(MAPS / "ladder.png")


def lerp_pt(p, q, t):
    return (p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t)


def title_scene():
    """A wet, empty hallway at night with one small figure at the far end."""
    S = 2
    W, H = 720 * S, 554 * S
    vp = (360 * S, 214 * S)
    img = Image.new("RGBA", (W, H), (6, 6, 22, 255))
    d = ImageDraw.Draw(img, "RGBA")
    far = (312 * S, 176 * S, 408 * S, 256 * S)
    tl, tr, bl, br = (far[0], far[1]), (far[2], far[1]), (far[0], far[3]), (far[2], far[3])
    # ceiling, floor, left wall, right wall
    d.polygon([(0, 0), (W, 0), tr, tl], fill=(46, 22, 76, 255))
    d.polygon([(0, H), (W, H), br, bl], fill=(10, 18, 54, 255))
    d.polygon([(0, 0), tl, bl, (0, H)], fill=(18, 20, 70, 255))
    d.polygon([(W, 0), tr, br, (W, H)], fill=(24, 18, 66, 255))
    # ceiling grid
    for i in range(1, 10):
        t = (i / 10) ** 1.8
        a, b = lerp_pt((0, 0), tl, t), lerp_pt((W, 0), tr, t)
        d.line([a, b], fill=(100, 40, 110, 200), width=2)
    for i in range(-5, 6):
        x = W / 2 + i * W / 10
        d.line([(x, 0), lerp_pt((x, 0), vp, 0.92)], fill=(100, 40, 110, 180), width=2)
    # floor tiles
    for i in range(1, 14):
        t = 1 - (1 - i / 14) ** 2.4
        a, b = lerp_pt((0, H), bl, t), lerp_pt((W, H), br, t)
        d.line([a, b], fill=(4, 8, 28, 255), width=max(1, int(4 * (1 - t))))
    for i in range(-7, 8):
        x = W / 2 + i * W / 7
        d.line([(x, H), lerp_pt((x, H), vp, 0.92)], fill=(4, 8, 28, 255), width=3)
    # tiled lower right wall
    for i in range(1, 8):
        t = i / 8
        y0 = H * 0.70 + (H - H * 0.70) * (i / 8) * 0.4
        d.line([(W, y0), lerp_pt((W, y0), vp, 0.9)], fill=(40, 90, 90, 170), width=2)
    d.polygon([(W, H * 0.62), lerp_pt((W, H * 0.62), vp, 0.9), lerp_pt((W, H), vp, 0.9), (W, H)], fill=(30, 74, 80, 140))
    d.line([(W, H * 0.62), lerp_pt((W, H * 0.62), vp, 0.9)], fill=(70, 150, 150, 200), width=3)
    # lockers near the far end
    for side, base in ((-1, (255, 70, 110)), (1, (80, 220, 240))):
        for i in range(5):
            t0 = 0.60 + i * 0.065
            t1 = t0 + 0.055
            xo = 0 if side < 0 else W
            p = [lerp_pt((xo, H * 0.15), vp, t0), lerp_pt((xo, H * 0.15), vp, t1), lerp_pt((xo, H * 0.88), vp, t1), lerp_pt((xo, H * 0.88), vp, t0)]
            d.polygon(p, fill=dim(base, 0.55) + (255,), outline=dim(base, 0.9) + (255,))
    # right window with posters
    p0 = [(W * 0.86, H * 0.20), lerp_pt((W * 0.86, H * 0.20), vp, 0.45), lerp_pt((W * 0.86, H * 0.60), vp, 0.45), (W * 0.86, H * 0.60)]
    d.polygon(p0, fill=(10, 14, 44, 255), outline=(60, 170, 255, 255))
    p1 = [(W, H * 0.12), (W * 0.9, H * 0.16), (W * 0.9, H * 0.6), (W, H * 0.62)]
    d.polygon(p1, fill=(18, 16, 56, 255), outline=(60, 170, 255, 255))
    rnd = random.Random(5)
    for x, y, c in ((0.93, 0.28, (235, 235, 245)), (0.96, 0.40, (120, 240, 200)), (0.91, 0.42, (250, 200, 120))):
        px, py = x * W, y * H
        d.polygon([(px, py), (px + 34 * S, py + 6), (px + 30 * S, py + 44 * S), (px - 4, py + 40 * S)], fill=c + (225,))
        for k in range(4):
            d.line([(px + 5, py + (8 + k * 7) * S), (px + 26 * S, py + (9 + k * 7) * S)], fill=(40, 40, 70, 255), width=2)
    # left vending machine, close to the viewer
    v = [(0, H * 0.02), (W * 0.2, H * 0.1), (W * 0.2, H * 0.9), (0, H)]
    d.polygon(v, fill=(14, 12, 28, 255), outline=(40, 40, 70, 255))
    pane = [(W * 0.09, H * 0.14), (W * 0.17, H * 0.17), (W * 0.17, H * 0.62), (W * 0.09, H * 0.64)]
    d.polygon(pane, fill=(30, 110, 170, 255), outline=(60, 200, 255, 255))
    for t in (0.2, 0.32, 0.44, 0.56):
        d.line([(W * 0.095, H * t), (W * 0.165, H * (t + 0.003))], fill=(230, 250, 255, 255), width=3)
    d.rectangle([W * 0.125, H * 0.17, W * 0.138, H * 0.6], fill=(190, 250, 255, 255))
    d.polygon([(0, H), (0, H * 0.58), (W * 0.08, H * 0.60), (W * 0.08, H * 0.9)], fill=(226, 120, 40, 255))
    # string of party lights, left over from a celebration nobody remembers
    string_lights(d, W * 0.2, W * 0.46, H * 0.18, H * 0.04, rnd)
    string_lights(d, W * 0.56, W * 0.84, H * 0.2, H * 0.04, rnd)
    # back wall light and figure
    d.rectangle(far, fill=(130, 140, 255, 255))
    fx, fy = 360 * S, 252 * S
    fig = (20, 18, 44, 255)
    d.ellipse([fx - 5 * S, fy - 56 * S, fx + 5 * S, fy - 45 * S], fill=fig)
    d.rounded_rectangle([fx - 7 * S, fy - 45 * S, fx + 7 * S, fy - 18 * S], 3 * S, fill=fig)
    d.rectangle([fx - 6 * S, fy - 18 * S, fx - 1 * S, fy], fill=fig)
    d.rectangle([fx + 1 * S, fy - 18 * S, fx + 6 * S, fy], fill=fig)
    d.ellipse([fx - 3 * S, fy - 38 * S, fx + 3 * S, fy - 31 * S], fill=(255, 70, 120, 255))
    # wet floor sign
    d.polygon([(W * 0.60, H * 0.62), (W * 0.65, H * 0.62), (W * 0.68, H * 0.76), (W * 0.58, H * 0.76)], fill=(214, 226, 40, 255), outline=(30, 34, 8, 255))
    # reflections and glow
    glow = Image.new("RGB", (W, H), (0, 0, 0))
    g = ImageDraw.Draw(glow)
    g.ellipse([vp[0] - 170 * S, vp[1] - 120 * S, vp[0] + 170 * S, vp[1] + 130 * S], fill=(120, 110, 255))
    g.ellipse([vp[0] - 60 * S, vp[1] - 40 * S, vp[0] + 60 * S, vp[1] + 50 * S], fill=(255, 150, 210))
    g.polygon([(fx - 30 * S, fy), (fx + 30 * S, fy), (fx + 110 * S, H), (fx - 110 * S, H)], fill=(140, 90, 170))
    g.polygon([(W * 0.2, H * 0.74), (W * 0.42, H * 0.56), (W * 0.46, H * 0.56), (W * 0.36, H)], fill=(20, 110, 150))
    g.polygon([(fx - 8 * S, fy), (fx + 8 * S, fy), (fx + 30 * S, H), (fx - 30 * S, H)], fill=(255, 90, 150))
    g.rectangle([W * 0.09, H * 0.14, W * 0.17, H * 0.62], fill=(30, 130, 190))
    glow = glow.filter(ImageFilter.GaussianBlur(26 * S))
    img = screen(img, glow)
    d = ImageDraw.Draw(img, "RGBA")
    d.ellipse([fx - 3 * S, fy - 38 * S, fx + 3 * S, fy - 31 * S], fill=(255, 150, 190, 255))
    img = bloom(img, 10 * S, 0.5)
    img = img.resize((720, 554), Image.LANCZOS)
    # logo
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    ld.text((360, 72), "11/1", font=font(92), fill=(80, 235, 255, 255), anchor="mm")
    g2 = layer.filter(ImageFilter.GaussianBlur(9))
    img.alpha_composite(g2)
    img.alpha_composite(g2)
    d = ImageDraw.Draw(img, "RGBA")
    d.text((360, 72), "11/1", font=font(92), fill=(225, 252, 255, 255), anchor="mm")
    v2 = Image.new("L", img.size, 0)
    vd = ImageDraw.Draw(v2)
    vd.ellipse([-120, -110, 840, 664], fill=255)
    v2 = v2.filter(ImageFilter.GaussianBlur(70))
    dark = Image.new("RGBA", img.size, (2, 2, 12, 255))
    dark.paste(img, (0, 0), v2)
    UI.mkdir(parents=True, exist_ok=True)
    dark.convert("RGB").save(UI / "title.png")


# Color of each classmate's implant, by what their construct does.
IMPLANT = {
    "aarav": (255, 210, 70), "tanish": (150, 170, 255), "joseph": (90, 255, 150), "luis": (80, 235, 255),
    "abhinav": (255, 130, 70), "matt": (255, 230, 90), "justin": (255, 90, 90), "hanbo": (255, 170, 60),
    "austin": (130, 220, 255), "nik": (255, 60, 110), "imraan": (140, 180, 220), "kai": (140, 255, 110),
    "tuhin": (200, 120, 255), "sam": (255, 110, 40), "camilo": (255, 70, 200), "james": (255, 240, 120), "arnav": (255, 255, 255),
}


def neon_sprite(path, out, rim, implant=None):
    src = Image.open(path).convert("RGBA")
    r, g, b, a = src.split()
    r = r.point(lambda v: int(v * 0.80))
    g = g.point(lambda v: int(v * 0.80))
    b = b.point(lambda v: min(255, int(v * 0.92 + 12)))
    graded = Image.merge("RGBA", (r, g, b, a))
    w, h = src.size
    pad = 0
    glow_mask = a.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.GaussianBlur(max(2, w // 50)))
    glow = Image.new("RGBA", src.size, rim + (0,))
    glow.putalpha(glow_mask.point(lambda v: int(v * 0.8)))
    canvas = Image.new("RGBA", src.size, (0, 0, 0, 0))
    canvas.alpha_composite(glow)
    canvas.alpha_composite(graded)
    if implant:
        d = ImageDraw.Draw(canvas, "RGBA")
        cx, cy = w * 0.70, h * 0.31
        s = w / 128
        layer = Image.new("RGBA", src.size, (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        ld.ellipse([cx - 7 * s, cy - 7 * s, cx + 7 * s, cy + 7 * s], fill=implant + (210,))
        canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(3 * s)))
        d.line([(cx, cy), (cx + 5 * s, cy - 9 * s)], fill=implant + (255,), width=max(1, int(2 * s)))
        d.ellipse([cx - 2.5 * s, cy - 2.5 * s, cx + 2.5 * s, cy + 2.5 * s], fill=(255, 255, 255, 255))
    out.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(out)


def main():
    MAPS.mkdir(parents=True, exist_ok=True)
    for name in PALETTES:
        battle_map(name)
        corridor(name)
        print("painted", name)
    tile("locker", locker_tile)
    tile("milk", milk_tile)
    tile("carpet", carpet_tile)
    tile("hype", hype_tile)
    ladder()
    title_scene()
    for p in sorted((SPRITES / "defenders").glob("*.png")):
        neon_sprite(p, NEON / "defenders" / p.name, (70, 220, 255), IMPLANT.get(p.stem, (80, 235, 255)))
    for p in sorted((SPRITES / "enemies").glob("*.png")):
        neon_sprite(p, NEON / "enemies" / p.name, (255, 60, 130))
    print("sprites done")


if __name__ == "__main__":
    main()
