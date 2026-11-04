#!/usr/bin/env python3
"""Draw the game's characters from scratch: MapleStory-style chibi defenders
and cartoon school-supply enemies. Nothing here reads photos.

Everything is drawn at 512x512 and shrunk to 128x128 for smooth edges.

Usage (from the project root):
  python3 tools/make_sprites.py            # defenders and enemies
  python3 tools/make_sprites.py dodger     # only these names

Edit the CHARACTERS table below to change anyone's look.
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
DEF_OUT = ROOT / "res" / "sprites" / "defenders"
ENEMY_OUT = ROOT / "res" / "sprites" / "enemies"
PREVIEW = ROOT / "art" / "preview"

W = 512
FINAL = 128
OUT = (38, 26, 30)
LW = 7

SKIN = {
    "fair": (255, 226, 205),
    "light": (250, 214, 180),
    "tan": (234, 182, 138),
    "medium": (214, 158, 112),
    "brown": (192, 132, 92),
    "dark": (146, 98, 68),
}
HAIR = {
    "black": (40, 34, 44),
    "darkbrown": (78, 52, 40),
    "brown": (122, 80, 52),
    "blond": (236, 204, 116),
}

# person: look, taken from that person's photo in art/source/<person>.png.
# style = hair shape, outfit = clothing detail.
CHARACTERS = {
    "aarav": dict(skin='tan', hair='black', style='parted', shirt=(206, 206, 214), outfit='crew', smile='soft'),
    "tanish": dict(skin='medium', hair='black', style='curly', shirt=(244, 138, 124), outfit='fleece', smile='open', jacket=(36, 36, 44)),
    "joseph": dict(skin='light', hair='black', style='bangs', shirt=(88, 84, 88), outfit='tee', smile='soft'),
    "luis": dict(skin='fair', hair='blond', style='messy', shirt=(36, 46, 92), outfit='polo', smile='open', collar=(170, 200, 235)),
    "sam": dict(skin='light', hair='black', style='swept', shirt=(150, 150, 154), outfit='crew', smile='soft'),
    "matt": dict(skin='light', hair='black', style='tousled', shirt=(54, 54, 62), outfit='hoodie', smile='flat'),
    "nik": dict(skin='light', hair='brown', style='long', shirt=(36, 34, 40), outfit='crewwhite', smile='soft'),
    "imraan": dict(skin='medium', hair='black', style='parted', shirt=(36, 42, 76), outfit='tee', smile='open'),
    "austin": dict(skin='light', hair='black', style='bangs', shirt=(122, 112, 64), outfit='tee', smile='flat'),
    "abhinav": dict(skin='brown', hair='black', style='messy', shirt=(66, 76, 170), outfit='dots', smile='open'),
    "hanbo": dict(skin='light', hair='black', style='swept', shirt=(244, 244, 248), outfit='tee', smile='open'),
    "tuhin": dict(skin='medium', hair='black', style='parted', shirt=(44, 56, 92), outfit='polo', smile='soft'),
    "james": dict(skin='light', hair='black', style='bangs', shirt=(92, 108, 134), outfit='tee', smile='flat', glasses=True),
    "arnav": dict(skin='medium', hair='black', style='parted', shirt=(36, 46, 92), outfit='polo', smile='open'),
    "camilo": dict(skin='fair', hair='darkbrown', style='swept', shirt=(38, 44, 68), outfit='tee', smile='open'),
    "justin": dict(skin='light', hair='black', style='wavy', shirt=(248, 248, 252), outfit='tee', smile='soft'),
    "kai": dict(skin='light', hair='black', style='swept', shirt=(34, 42, 76), outfit='zip', smile='soft'),
}


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


def ell(d, cx, cy, rx, ry, fill, outline=OUT, w=LW):
    d.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=fill, outline=outline, width=w)


def rrect(d, x0, y0, x1, y1, r, fill, outline=OUT, w=LW):
    d.rounded_rectangle([x0, y0, x1, y1], radius=r, fill=fill, outline=outline, width=w)


def poly(d, pts, fill, outline=OUT, w=LW):
    d.polygon(pts, fill=fill)
    if outline:
        d.line(list(pts) + [pts[0]], fill=outline, width=w, joint="curve")


def fringe(d, pts, fill):
    """Hair front piece: filled, outlined only along its lower edge so no seam shows on the dome."""
    d.polygon(pts, fill=fill)
    d.line(list(pts[3:]) + [pts[0]], fill=OUT, width=LW, joint="curve")


def mirror(pts):
    return [(W - x, y) for x, y in pts]


def font(size):
    try:
        return ImageFont.load_default(size=size)
    except TypeError:
        return ImageFont.load_default()


def finish(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    small = img.resize((FINAL, FINAL), Image.LANCZOS)
    small.save(path)
    return small


# ---------------------------------------------------------------- hair
def hair_back(d, spec, hc):
    if spec["style"] == "long":
        pts = [(112, 150), (96, 260), (104, 360), (130, 430), (180, 410), (200, 330),
               (312, 330), (332, 410), (382, 430), (408, 360), (416, 260), (400, 150)]
        poly(d, pts, hc)


def hair_front(d, spec, hc):
    style = spec["style"]
    hd = shade(hc, 0.78)
    # dome over the top of the head
    d.pieslice([112, 54, 400, 290], 180, 360, fill=hc)
    d.arc([112, 54, 400, 290], 180, 360, fill=OUT, width=LW)

    def side(x_out, x_in, y_to):
        pts = [(x_out, 160), (x_in, 170), (x_in + (6 if x_out < 256 else -6), y_to), (x_out + (4 if x_out < 256 else -4), y_to - 12)]
        poly(d, pts, hc)

    if style == "swept":
        fringe(d, [(112, 170), (112, 150), (400, 150), (400, 170), (382, 214), (364, 176),
                 (316, 160), (276, 196), (232, 156), (186, 178), (146, 214)], hc)
        d.line([(300, 100), (340, 150)], fill=hd, width=6)
        d.line([(240, 92), (270, 140)], fill=hd, width=6)
    elif style == "messy":
        for pts in ([(130, 110), (112, 40), (178, 84)], [(190, 80), (190, 18), (248, 66)],
                    [(252, 66), (290, 14), (318, 72)], [(318, 74), (376, 30), (390, 106)],
                    [(364, 100), (426, 90), (396, 160)], [(148, 100), (96, 90), (116, 160)]):
            poly(d, pts, hc)
        fringe(d, [(112, 170), (112, 140), (400, 140), (400, 170), (384, 220), (362, 168),
                 (330, 200), (298, 156), (262, 196), (226, 152), (190, 198), (156, 160), (140, 220)], hc)
    elif style == "tousled":
        for cx, cy, r in [(150, 112, 34), (206, 82, 34), (266, 72, 36), (326, 86, 34), (372, 122, 32)]:
            ell(d, cx, cy, r, r, hc)
        fringe(d, [(112, 170), (112, 140), (400, 140), (400, 170), (384, 218), (360, 176),
                   (322, 190), (290, 158), (250, 192), (214, 160), (176, 190), (146, 214)], hc)
        d.line([(210, 120), (232, 150)], fill=hd, width=6)
    elif style == "bangs":
        fringe(d, [(112, 170), (112, 140), (400, 140), (400, 170), (386, 226), (366, 186),
                 (256, 196), (146, 186), (126, 226)], hc)
        d.line([(200, 150), (196, 188)], fill=hd, width=6)
        d.line([(310, 150), (316, 190)], fill=hd, width=6)
    elif style == "parted":
        fringe(d, [(112, 170), (112, 140), (400, 140), (400, 170), (384, 214), (362, 170),
                 (300, 150), (236, 160), (186, 188), (146, 214)], hc)
        d.line([(268, 66), (236, 150)], fill=hd, width=6)
    elif style == "curly":
        for cx, cy, r in [(140, 120, 46), (200, 80, 48), (264, 66, 50), (326, 80, 48),
                          (378, 124, 46), (122, 180, 38), (396, 182, 38)]:
            ell(d, cx, cy, r, r, hc)
        fringe(d, [(128, 175), (128, 150), (384, 150), (384, 175), (360, 196), (316, 172),
                 (256, 198), (196, 172), (152, 196)], hc)
    elif style == "long":
        fringe(d, [(112, 170), (112, 140), (400, 140), (400, 170), (388, 300), (372, 190),
                 (300, 160), (256, 170), (212, 160), (140, 190), (124, 300)], hc)
        d.line([(256, 62), (256, 160)], fill=hd, width=6)
    elif style == "wavy":
        for cx, cy, r in [(134, 134, 40), (186, 94, 40), (246, 76, 42), (310, 82, 42),
                          (366, 110, 40), (394, 160, 34), (118, 190, 36)]:
            ell(d, cx, cy, r, r, hc)
        fringe(d, [(116, 176), (116, 140), (396, 140), (396, 176), (394, 250), (370, 190),
                 (336, 168), (300, 196), (256, 160), (210, 190), (176, 166), (142, 192), (118, 250)], hc)
        d.arc([180, 120, 250, 190], 200, 320, fill=hd, width=6)
        d.arc([270, 110, 340, 180], 200, 320, fill=hd, width=6)


# ---------------------------------------------------------------- body
def draw_body(d, spec, skin):
    shirt = spec["shirt"]
    outfit = spec["outfit"]
    jacket = spec.get("jacket")
    body = jacket or shirt
    pants = spec.get("pants", (62, 70, 98))
    shoes = (246, 246, 248)

    # legs and shoes
    for lx in (236, 276):
        rrect(d, lx - 18, 392, lx + 18, 452, 12, pants)
        ell(d, lx - 4 if lx < 256 else lx + 4, 466, 32, 18, shoes)
        d.line([(lx - 24, 470), (lx + 24, 470)], fill=shade(shoes, 0.8), width=5)

    # torso
    rrect(d, 194, 296, 318, 410, 30, body)
    # arms: sleeve and hand
    for sx, dirn in ((190, -1), (322, 1)):
        ell(d, sx + dirn * 6, 340, 26, 36, body)
        ell(d, sx + dirn * 14, 388, 19, 22, skin)

    cx = 256
    if outfit == "crew":
        d.arc([226, 288, 286, 330], 10, 170, fill=shade(shirt, 0.65), width=8)
        rrect(d, 196, 398, 316, 410, 6, shade(shirt, 0.85), outline=None)
    elif outfit == "crewwhite":
        d.arc([222, 284, 290, 336], 10, 170, fill=(246, 246, 248), width=12)
        d.arc([226, 292, 286, 334], 10, 170, fill=shade(shirt, 1.5), width=5)
    elif outfit == "tee":
        d.arc([228, 290, 284, 326], 10, 170, fill=shade(shirt, 0.7), width=7)
    elif outfit == "polo":
        white = spec.get("collar", (240, 240, 245))
        poly(d, [(222, 298), (256, 334), (240, 296)], white)
        poly(d, [(290, 298), (256, 334), (272, 296)], white)
        d.line([(cx, 334), (cx, 376)], fill=shade(shirt, 0.6), width=6)
        for by in (346, 364):
            ell(d, cx, by, 5, 5, white, w=3)
    elif outfit == "zip":
        rrect(d, 224, 288, 288, 322, 10, shade(shirt, 1.25))
        d.line([(cx, 322), (cx, 372)], fill=(210, 214, 226), width=6)
        ell(d, cx, 376, 7, 9, (200, 204, 216), w=3)
    elif outfit == "hoodie":
        ell(d, cx, 312, 66, 26, shade(shirt, 0.8))
        d.line([(238, 326), (232, 372)], fill=(235, 235, 240), width=6)
        d.line([(274, 326), (280, 372)], fill=(235, 235, 240), width=6)
    elif outfit == "dots":
        for dx, dy, r in ((236, 350, 17), (282, 336, 12), (270, 374, 10)):
            ell(d, dx, dy, r, r, (250, 150, 80), outline=None)
            ell(d, dx + 3, dy + 2, r // 2, r // 2, shirt, outline=None)
        d.arc([228, 290, 284, 326], 10, 170, fill=shade(shirt, 0.7), width=7)
    elif outfit == "fleece":
        rrect(d, 232, 300, 280, 406, 8, shirt, w=5)
        d.line([(cx, 306), (cx, 404)], fill=(150, 150, 160), width=4)
        d.arc([226, 288, 286, 326], 10, 170, fill=shade(jacket, 1.6), width=8)


# ---------------------------------------------------------------- face
def draw_face(d, spec, skin, hc):
    # eyes
    iris = (66, 44, 40)
    for ex in (204, 308):
        ell(d, ex, 226, 27, 37, iris, w=5)
        ell(d, ex, 238, 20, 22, shade((110, 78, 66), 1.05), outline=None)
        ell(d, ex - 8, 212, 11, 11, (255, 255, 255), outline=None)
        ell(d, ex + 9, 246, 5, 5, (255, 255, 255), outline=None)
        d.arc([ex - 30, 186, ex + 30, 266], 205, 335, fill=OUT, width=10)
    # brows
    bw = 9
    if spec["style"] not in ("bangs",):
        d.line([(170, 184), (236, 176)], fill=hc, width=bw)
        d.line([(276, 176), (342, 184)], fill=hc, width=bw)
    # glasses
    if spec.get("glasses"):
        frame = (34, 34, 40)
        for ex in (204, 308):
            rrect(d, ex - 44, 186, ex + 44, 270, 22, None, outline=frame, w=9)
        d.line([(248, 218), (264, 218)], fill=frame, width=8)
        d.line([(160, 214), (130, 204)], fill=frame, width=7)
        d.line([(352, 214), (382, 204)], fill=frame, width=7)
    # nose
    d.arc([248, 246, 266, 262], 20, 160, fill=shade(skin, 0.68), width=5)
    # mouth
    smile = spec["smile"]
    if smile == "open":
        d.chord([222, 254, 290, 296], 0, 180, fill=(122, 38, 48), outline=OUT, width=5)
        d.chord([232, 255, 280, 272], 0, 180, fill=(255, 255, 255))
        d.ellipse([244, 278, 268, 290], fill=(240, 120, 130))
    elif smile == "soft":
        d.arc([232, 250, 282, 284], 25, 155, fill=OUT, width=7)
    else:
        d.arc([238, 262, 276, 282], 30, 150, fill=OUT, width=6)


def blush(img, skin):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    for bx in (158, 354):
        ld.ellipse([bx - 28, 258, bx + 28, 286], fill=(255, 120, 130, 110))
    img.alpha_composite(layer)


def draw_defender(name, spec):
    skin = SKIN[spec["skin"]]
    hc = HAIR[spec["hair"]]
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    shadow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).ellipse([130, 474, 382, 504], fill=(0, 0, 0, 70))
    img.alpha_composite(shadow)
    d = ImageDraw.Draw(img)

    hair_back(d, spec, hc)
    draw_body(d, spec, skin)
    # ears
    for ex in (124, 388):
        ell(d, ex, 214, 17, 24, skin)
    # head
    ell(d, 256, 190, 134, 120, skin)
    blush(img, skin)
    d = ImageDraw.Draw(img)
    draw_face(d, spec, skin, hc)
    hair_front(d, spec, hc)
    return img


# ---------------------------------------------------------------- enemies
def angry_face(d, cx, cy, s=1.0, fangs=True):
    for sx in (-1, 1):
        ex = cx + sx * 40 * s
        ell(d, ex, cy, 22 * s, 26 * s, (255, 255, 255), w=5)
        ell(d, ex - sx * 4 * s, cy + 5 * s, 10 * s, 12 * s, (30, 20, 24), outline=None)
        ell(d, ex - sx * 7 * s, cy + 1 * s, 4 * s, 4 * s, (255, 255, 255), outline=None)
        d.line([(ex + sx * 28 * s, cy - 42 * s), (ex - sx * 26 * s, cy - 20 * s)], fill=OUT, width=int(9 * s) + 1)
    my = cy + 52 * s
    d.arc([cx - 34 * s, my - 6 * s, cx + 34 * s, my + 40 * s], 195, 345, fill=OUT, width=int(7 * s) + 1)
    if fangs:
        for sx in (-1, 1):
            x = cx + sx * 16 * s
            d.polygon([(x - 6 * s, my + 2 * s), (x + 6 * s, my + 2 * s), (x, my + 16 * s)], fill=(255, 255, 255), outline=OUT)


def limbs(d, left_x, right_x, top_y, bot_y, arm_y, arm_dx=46):
    for lx in (left_x, right_x):
        d.line([(lx, top_y), (lx, bot_y)], fill=OUT, width=12)
        ell(d, lx + (-8 if lx == left_x else 8), bot_y + 8, 24, 13, (50, 40, 52))
    d.line([(left_x - 40, arm_y), (left_x - 40 - arm_dx, arm_y + 60)], fill=OUT, width=11)
    d.line([(right_x + 40, arm_y), (right_x + 40 + arm_dx, arm_y + 60)], fill=OUT, width=11)
    ell(d, left_x - 40 - arm_dx, arm_y + 64, 13, 13, (255, 255, 255))
    ell(d, right_x + 40 + arm_dx, arm_y + 64, 13, 13, (255, 255, 255))


def new_canvas():
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    ImageDraw.Draw(img).ellipse([120, 474, 392, 504], fill=(0, 0, 0, 60))
    return img, ImageDraw.Draw(img)


def enemy_homework_sheet():
    img, d = new_canvas()
    limbs(d, 216, 296, 376, 452, 240)
    rrect(d, 140, 60, 372, 384, 14, (252, 252, 255))
    poly(d, [(318, 60), (372, 114), (318, 114)], (212, 214, 228))
    for y in range(150, 370, 36):
        d.line([(160, y), (352, y)], fill=(150, 190, 240), width=4)
    d.line([(188, 74), (188, 380)], fill=(240, 120, 120), width=4)
    angry_face(d, 266, 232, 0.95)
    return img


def enemy_sharp_pencil():
    img, d = new_canvas()
    limbs(d, 226, 286, 440, 462, 270, 34)
    rrect(d, 196, 124, 316, 400, 10, (250, 204, 50))
    for x in (226, 256, 286):
        d.line([(x, 130), (x, 394)], fill=(214, 164, 30), width=4)
    poly(d, [(196, 124), (316, 124), (256, 36)], (244, 206, 158))
    poly(d, [(236, 74), (276, 74), (256, 36)], (60, 60, 70))
    rrect(d, 196, 400, 316, 430, 6, (180, 186, 196))
    rrect(d, 204, 430, 308, 462, 14, (244, 150, 160))
    angry_face(d, 256, 248, 0.72)
    return img


def enemy_report_card():
    img, d = new_canvas()
    limbs(d, 216, 296, 396, 452, 250)
    rrect(d, 130, 60, 382, 400, 14, (252, 242, 216))
    rrect(d, 130, 60, 382, 128, 14, (214, 70, 70))
    d.rectangle([132, 100, 380, 128], fill=(214, 70, 70))
    t = "REPORT"
    d.text((256, 94), t, font=font(44), fill=(255, 255, 255), anchor="mm")
    angry_face(d, 256, 168, 0.55, fangs=False)
    rrect(d, 196, 214, 316, 316, 12, (255, 255, 255), w=5)
    for y in (340, 366):
        d.line([(160, y), (352, y)], fill=(170, 160, 140), width=5)
    return img


def enemy_paper_airplane():
    img, d = new_canvas()
    poly(d, [(40, 250), (470, 170), (300, 330)], (252, 252, 255))
    poly(d, [(40, 250), (300, 330), (230, 430)], (204, 210, 228))
    poly(d, [(300, 330), (470, 170), (330, 390)], (226, 230, 244))
    d.line([(40, 250), (300, 330)], fill=OUT, width=5)
    angry_face(d, 272, 262, 0.5, fangs=False)
    for y, x0 in ((300, 60), (340, 100)):
        d.line([(x0 - 40, y), (x0, y)], fill=(255, 255, 255, 180), width=8)
    return img


def enemy_eraser():
    img, d = new_canvas()
    limbs(d, 216, 296, 396, 452, 290)
    rrect(d, 120, 176, 392, 400, 22, (242, 150, 164))
    d.rounded_rectangle([300, 176, 392, 400], radius=22, fill=(70, 110, 210), outline=OUT, width=LW)
    d.rectangle([300, 190, 330, 386], fill=(70, 110, 210))
    d.line([(300, 178), (300, 398)], fill=OUT, width=5)
    angry_face(d, 206, 282, 0.95)
    return img


def enemy_midterm_exam():
    img, d = new_canvas()
    limbs(d, 206, 306, 410, 452, 270)
    rrect(d, 150, 72, 394, 420, 14, (236, 236, 244))
    rrect(d, 124, 56, 372, 410, 14, (255, 255, 255))
    rrect(d, 124, 56, 372, 124, 14, (200, 56, 56))
    d.rectangle([126, 96, 370, 124], fill=(200, 56, 56))
    d.text((248, 92), "MIDTERM", font=font(46), fill=(255, 255, 255), anchor="mm")
    d.text((248, 176), "EXAM", font=font(58), fill=(200, 56, 56), anchor="mm")
    angry_face(d, 248, 290, 1.15)
    poly(d, [(150, 62), (126, 6), (196, 54)], (200, 56, 56))
    poly(d, [(346, 62), (370, 6), (300, 54)], (200, 56, 56))
    return img


def enemy_application_fee():
    img, d = new_canvas()
    limbs(d, 216, 296, 360, 452, 240)
    rrect(d, 84, 140, 428, 366, 20, (136, 206, 128))
    rrect(d, 108, 162, 404, 344, 12, None, outline=(70, 140, 76), w=6)
    d.text((142, 200), "$", font=font(64), fill=(50, 110, 56), anchor="mm")
    d.text((370, 306), "$", font=font(64), fill=(50, 110, 56), anchor="mm")
    angry_face(d, 256, 250, 0.95)
    return img


def enemy_personal_essay():
    img, d = new_canvas()
    limbs(d, 216, 296, 410, 454, 250)
    rrect(d, 150, 84, 362, 410, 10, (255, 253, 244))
    for y in (140, 168, 196, 340, 368):
        d.line([(176, y), (336, y)], fill=(160, 160, 170), width=5)
    rrect(d, 126, 66, 386, 106, 20, (222, 190, 140))
    rrect(d, 126, 390, 386, 430, 20, (222, 190, 140))
    angry_face(d, 256, 262, 0.85)
    return img


def enemy_rejection_letter():
    img, d = new_canvas()
    limbs(d, 216, 296, 400, 452, 270)
    rrect(d, 100, 160, 412, 400, 14, (248, 238, 238))
    poly(d, [(100, 160), (412, 160), (256, 304)], (232, 196, 198))
    angry_face(d, 256, 214, 0.78, fangs=False)
    ell(d, 256, 318, 38, 38, (196, 40, 52))
    d.line([(238, 300), (274, 336)], fill=(255, 255, 255), width=9)
    d.line([(274, 300), (238, 336)], fill=(255, 255, 255), width=9)
    return img


def enemy_scantron_swarm():
    img, d = new_canvas()
    limbs(d, 216, 296, 390, 452, 250)
    rrect(d, 140, 84, 372, 390, 12, (206, 238, 206))
    rrect(d, 140, 84, 372, 122, 12, (50, 70, 56))
    d.rectangle([142, 104, 370, 122], fill=(50, 70, 56))
    for y in range(150, 370, 40):
        for x in (176, 204, 308, 336):
            ell(d, x, y, 10, 10, (255, 255, 255), w=3)
    angry_face(d, 256, 246, 0.6, fangs=False)
    return img


def enemy_the_deadline():
    img, d = new_canvas()
    limbs(d, 206, 306, 420, 456, 300, 30)
    ell(d, 168, 92, 50, 40, (240, 190, 70))
    ell(d, 344, 92, 50, 40, (240, 190, 70))
    ell(d, 256, 262, 178, 178, (206, 52, 60))
    ell(d, 256, 262, 150, 150, (255, 250, 240), w=6)
    for i in range(12):
        a = math.radians(i * 30)
        r0, r1 = 124, 144
        d.line([(256 + r0 * math.sin(a), 262 - r0 * math.cos(a)), (256 + r1 * math.sin(a), 262 - r1 * math.cos(a))], fill=OUT, width=6)
    d.line([(256, 318), (256, 366)], fill=OUT, width=10)
    d.line([(256, 318), (306, 340)], fill=OUT, width=10)
    angry_face(d, 256, 214, 1.15)
    return img


def enemy_hall_monitor():
    img, d = new_canvas()
    limbs(d, 216, 296, 376, 452, 240)
    rrect(d, 146, 70, 366, 384, 18, (250, 212, 90))
    poly(d, [(146, 110), (366, 300), (366, 350), (146, 160)], (236, 110, 50))
    rrect(d, 196, 40, 316, 84, 8, (120, 124, 140))
    d.text((256, 62), "HALL", font=font(30), fill=(255, 255, 255), anchor="mm")
    angry_face(d, 256, 196, 0.9)
    return img


def enemy_mystery_meat():
    img, d = new_canvas()
    limbs(d, 200, 312, 392, 452, 270, 30)
    rrect(d, 92, 366, 420, 404, 16, (176, 184, 196))
    ell(d, 256, 236, 158, 138, (178, 92, 92))
    ell(d, 206, 170, 40, 26, (214, 140, 130), outline=None)
    for x, y in ((150, 280), (350, 260), (300, 340), (190, 340)):
        ell(d, x, y, 14, 10, (120, 170, 80))
    angry_face(d, 256, 222, 1.0)
    return img


def enemy_overdue_notice():
    img, d = new_canvas()
    limbs(d, 226, 286, 376, 452, 230, 36)
    poly(d, [(128, 150), (38, 90), (132, 240)], (250, 240, 214))
    poly(d, [(384, 150), (474, 90), (380, 240)], (250, 240, 214))
    rrect(d, 128, 96, 384, 384, 10, (252, 240, 214))
    rrect(d, 128, 96, 384, 150, 10, (196, 50, 50))
    d.text((256, 124), "OVERDUE", font=font(36), fill=(255, 255, 255), anchor="mm")
    angry_face(d, 256, 234, 0.95)
    return img


def enemy_dodgeball():
    img, d = new_canvas()
    limbs(d, 216, 296, 410, 452, 280, 30)
    ell(d, 256, 262, 156, 156, (222, 60, 56))
    d.arc([120, 150, 392, 380], 200, 340, fill=(160, 30, 30), width=8)
    ell(d, 196, 180, 34, 20, (250, 140, 130), outline=None)
    angry_face(d, 256, 246, 1.05)
    return img


def enemy_the_substitute():
    img, d = new_canvas()
    limbs(d, 206, 306, 420, 456, 290, 40)
    rrect(d, 136, 62, 376, 424, 14, (146, 94, 54))
    rrect(d, 158, 96, 354, 410, 6, (252, 252, 255))
    rrect(d, 212, 46, 300, 96, 8, (190, 194, 206))
    for y in (340, 366, 392):
        d.line([(180, y), (332, y)], fill=(150, 190, 240), width=4)
    angry_face(d, 256, 220, 1.1)
    for x in (214, 298):
        d.ellipse([x - 28, 192, x + 28, 248], outline=OUT, width=6)
    return img


def enemy_vice_principal():
    img, d = new_canvas()
    limbs(d, 206, 306, 420, 456, 290, 40)
    rrect(d, 116, 40, 396, 424, 18, (60, 66, 100))
    poly(d, [(206, 40), (256, 120), (306, 40)], (250, 250, 250))
    poly(d, [(244, 76), (268, 76), (280, 196), (256, 226), (232, 196)], (200, 50, 50))
    rrect(d, 146, 260, 366, 404, 8, (252, 236, 150))
    d.text((256, 290), "DETENTION", font=font(32), fill=(190, 40, 40), anchor="mm")
    angry_face(d, 256, 150, 0.85, fangs=False)
    return img


def enemy_lunch_lady():
    img, d = new_canvas()
    limbs(d, 206, 306, 420, 456, 280, 40)
    ell(d, 256, 110, 136, 74, (250, 250, 250))
    for x in range(140, 380, 40):
        d.line([(x, 60), (x + 18, 160)], fill=(200, 205, 215), width=3)
    rrect(d, 112, 130, 400, 424, 26, (240, 170, 120))
    rrect(d, 154, 290, 358, 416, 10, (250, 250, 250))
    angry_face(d, 256, 204, 1.05)
    d.line([(398, 170), (472, 80)], fill=(150, 156, 168), width=14)
    ell(d, 476, 66, 36, 26, (176, 184, 196))
    return img


def enemy_librarian():
    img, d = new_canvas()
    limbs(d, 206, 306, 420, 456, 280, 40)
    rrect(d, 112, 72, 400, 424, 12, (110, 50, 120))
    rrect(d, 140, 72, 372, 416, 8, (146, 74, 150))
    d.rectangle([116, 72, 136, 424], fill=(70, 30, 86))
    ell(d, 256, 54, 54, 44, (94, 62, 44))
    angry_face(d, 256, 220, 1.05, fangs=False)
    for x in (214, 298):
        d.ellipse([x - 30, 190, x + 30, 250], outline=(240, 200, 90), width=6)
    d.line([(256, 300), (256, 360)], fill=(250, 220, 190), width=16)
    return img


def enemy_coach():
    img, d = new_canvas()
    limbs(d, 206, 306, 420, 456, 290, 40)
    ell(d, 256, 262, 164, 166, (240, 200, 70))
    rrect(d, 104, 90, 408, 146, 22, (50, 100, 200))
    rrect(d, 300, 126, 450, 158, 14, (40, 80, 170))
    ell(d, 256, 372, 40, 26, (200, 205, 215))
    d.line([(256, 345), (256, 300)], fill=(200, 50, 50), width=6)
    angry_face(d, 256, 230, 1.05)
    return img


ENEMIES = {
    "hall_monitor": enemy_hall_monitor,
    "mystery_meat": enemy_mystery_meat,
    "overdue_notice": enemy_overdue_notice,
    "dodgeball": enemy_dodgeball,
    "the_substitute": enemy_the_substitute,
    "vice_principal": enemy_vice_principal,
    "lunch_lady": enemy_lunch_lady,
    "librarian": enemy_librarian,
    "coach": enemy_coach,
    "homework_sheet": enemy_homework_sheet,
    "sharp_pencil": enemy_sharp_pencil,
    "report_card": enemy_report_card,
    "paper_airplane": enemy_paper_airplane,
    "eraser": enemy_eraser,
    "midterm_exam": enemy_midterm_exam,
    "application_fee": enemy_application_fee,
    "personal_essay": enemy_personal_essay,
    "rejection_letter": enemy_rejection_letter,
    "scantron_swarm": enemy_scantron_swarm,
    "the_deadline": enemy_the_deadline,
}


def contact_sheet(sprites, path, cols=6, cell=192):
    rows = (len(sprites) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (222, 200, 150, 255))
    for i, im in enumerate(sprites):
        big = im.resize((cell, cell), Image.LANCZOS)
        sheet.alpha_composite(big, ((i % cols) * cell, (i // cols) * cell))
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)


def main(names):
    wanted = set(names)
    made_def, made_enemy = [], []
    for name, spec in CHARACTERS.items():
        if wanted and name not in wanted:
            continue
        made_def.append(finish(draw_defender(name, spec), DEF_OUT / f"{name}.png"))
        print("defender", name)
    for name, fn in ENEMIES.items():
        if wanted and name not in wanted:
            continue
        made_enemy.append(finish(fn(), ENEMY_OUT / f"{name}.png"))
        print("enemy   ", name)
    if made_def:
        contact_sheet(made_def, PREVIEW / "_defenders.png")
    if made_enemy:
        contact_sheet(made_enemy, PREVIEW / "_enemies.png")


if __name__ == "__main__":
    main(sys.argv[1:])
