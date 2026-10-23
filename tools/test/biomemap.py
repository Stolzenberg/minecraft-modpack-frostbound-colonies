"""Biomkarte aus dem KubeJS-Log (Zeilen BMAP/BLEG von tools/test/biomemap.js) als PNG: biomemap.py <kubejs/server.log> <out.png> [Skalierung]"""
import re, struct, sys, zlib
from collections import Counter

log, out = sys.argv[1], sys.argv[2]
scale = int(sys.argv[3]) if len(sys.argv) > 3 else 3
rows, legend = [], {}
for line in open(log, encoding="utf-8", errors="replace"):
    m = re.search(r"BMAP (-?\d+) ([\d,]+)", line)
    if m:
        rows.append((int(m.group(1)), [int(v) for v in m.group(2).split(",")]))
    m = re.search(r"BLEG (\d+) (\S+)", line)
    if m:
        legend[int(m.group(1))] = m.group(2)
# nur den letzten Lauf nehmen
last = {}
for z, r in rows:
    last[z] = r
rows = [last[z] for z in sorted(last)]
if not rows:
    sys.exit(f"keine BMAP-Zeilen in {log} (KubeJS leert das Log bei /reload, /biomemap danach erneut ausführen)")

RULES = [  # (Teilstring, Farbe, Gruppe)
    ("caves", (255, 0, 255), "Höhle (y=64 unter Gelände)"), ("spider_nest", (255, 0, 255), "Höhle (y=64 unter Gelände)"),
    ("glowing_grotto", (255, 0, 255), "Höhle (y=64 unter Gelände)"), ("deep_dark", (255, 0, 255), "Höhle (y=64 unter Gelände)"),
    ("wintry", (245, 245, 250), "Schnee"), ("auroral", (245, 245, 250), "Schnee"), ("snowblossom", (245, 245, 250), "Schnee"),
    ("muskeg", (200, 210, 200), "Tundra"), ("tundra", (190, 200, 170), "Tundra"),
    ("wasteland", (200, 170, 110), "Trocken"), ("scrubland", (205, 185, 110), "Trocken"), ("dryland", (215, 195, 120), "Trocken"),
    ("cold_desert", (220, 210, 170), "Wüste"), ("lush_desert", (220, 200, 100), "Wüste"),
    ("tropics", (30, 170, 40), "Dschungel"), ("rainforest", (30, 170, 40), "Dschungel"), ("fungal_jungle", (30, 170, 40), "Dschungel"),
    ("bayou", (70, 110, 90), "Sumpf"), ("marsh", (70, 110, 90), "Sumpf"), ("floodplain", (70, 110, 90), "Sumpf"),
    ("wetland", (70, 110, 90), "Sumpf"), ("bog", (70, 110, 90), "Sumpf"),
    ("crag", (130, 130, 130), "Berg"), ("jade_cliffs", (130, 130, 130), "Berg"), ("volcan", (90, 60, 60), "Berg"),
    ("coniferous", (40, 90, 60), "Taiga"), ("fir_clearing", (40, 90, 60), "Taiga"),
    ("lavender", (150, 200, 110), "Wiese/Ebene"), ("orchard", (150, 200, 110), "Wiese/Ebene"), ("pasture", (150, 200, 110), "Wiese/Ebene"),
    ("prairie", (150, 200, 110), "Wiese/Ebene"), ("field", (150, 200, 110), "Wiese/Ebene"), ("grassland", (150, 200, 110), "Wiese/Ebene"),
    ("shrubland", (150, 200, 110), "Wiese/Ebene"), ("highland", (150, 200, 110), "Wiese/Ebene"), ("moor", (150, 200, 110), "Wiese/Ebene"),
    ("origin_valley", (150, 200, 110), "Wiese/Ebene"), ("pumpkin_patch", (150, 200, 110), "Wiese/Ebene"),
    ("overgrown_greens", (150, 200, 110), "Wiese/Ebene"), ("hot_springs", (150, 200, 110), "Wiese/Ebene"),
    ("maple", (190, 90, 40), "Wald"), ("aspen", (60, 130, 50), "Wald"), ("jacaranda", (60, 130, 50), "Wald"),
    ("ominous", (60, 130, 50), "Wald"), ("woodland", (60, 130, 50), "Wald"), ("redwood", (60, 130, 50), "Wald"),
    ("mystic", (60, 130, 50), "Wald"), ("dead_forest", (60, 130, 50), "Wald"), ("undergrowth", (60, 130, 50), "Wald"),
    ("deep_frozen", (150, 170, 220), "Ozean gefroren"), ("frozen_ocean", (170, 190, 230), "Ozean gefroren"),
    ("deep_", (20, 40, 110), "Ozean tief"), ("ocean", (40, 80, 170), "Ozean"),
    ("frozen_river", (190, 210, 240), "Fluss gefroren"), ("river", (80, 140, 230), "Fluss"),
    ("snowy_beach", (235, 235, 245), "Schnee"), ("beach", (230, 215, 160), "Strand"), ("shore", (150, 150, 140), "Strand"),
    ("ice_spikes", (200, 230, 255), "Schnee"), ("frozen_peaks", (240, 245, 255), "Schnee"), ("jagged_peaks", (235, 240, 250), "Schnee"),
    ("snowy", (245, 245, 250), "Schnee"), ("grove", (215, 230, 225), "Schnee"),
    ("taiga", (40, 90, 60), "Taiga"),
    ("desert", (240, 220, 120), "Wüste"), ("badlands", (210, 110, 50), "Badlands"),
    ("savanna", (180, 170, 70), "Savanne"),
    ("jungle", (30, 170, 40), "Dschungel"), ("mangrove", (60, 120, 60), "Sumpf"), ("swamp", (70, 110, 90), "Sumpf"),
    ("mushroom", (170, 90, 170), "Pilz"),
    ("peaks", (130, 130, 130), "Berg"), ("windswept", (120, 140, 120), "Berg"), ("stony", (140, 140, 140), "Berg"),
    ("meadow", (150, 200, 110), "Wiese/Ebene"), ("cherry", (240, 170, 200), "Wald"),
    ("dark_forest", (30, 80, 30), "Wald"), ("birch", (110, 170, 90), "Wald"), ("forest", (60, 130, 50), "Wald"),
    ("plains", (140, 200, 90), "Wiese/Ebene"),
]


def color(bid):
    for key, col, grp in RULES:
        if key in bid:
            return col, grp
    return (255, 0, 255), "sonstige:" + bid


cnt, grp_cnt = Counter(), Counter()
h, w = len(rows), len(rows[0])
pix = []
for zi, r in enumerate(rows):
    line = []
    for xi, v in enumerate(r):
        bid = legend.get(v, "?")
        col, grp = color(bid)
        cnt[bid] += 1
        grp_cnt[grp] += 1
        if zi == h // 2 or xi == w // 2:  # Achsen durch den Spawn
            col = tuple(c // 2 for c in col)
        line.append(col)
    pix.append(line)

raw = b""
for line in pix:
    row = b"".join(bytes(c) * scale for c in line)
    raw += (b"\x00" + row) * scale


def chunk(t, d):
    return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)


png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w * scale, h * scale, 8, 2, 0, 0, 0))
png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
open(out, "wb").write(png)
total = sum(grp_cnt.values())
print(f"{w}x{h} Proben, Gruppen:")
for g, n in grp_cnt.most_common():
    print(f"  {g:<18} {100 * n / total:5.1f} %")
