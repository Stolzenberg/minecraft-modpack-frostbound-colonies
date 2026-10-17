#!/usr/bin/env python3
"""Oberste Blöcke (Schnee/Eis-Anteil) generierter Region-Dateien auswerten, aufgeschlüsselt nach Biom.

usage: surface.py <region-Ordner> [<jeder n-te Chunk, Standard 4>]
"""
import os, struct, sys, zlib
from collections import Counter
from analyze import heights, read_nbt

FROZEN = ("snow", "ice", "powder")


def packed(arr, bits, index):
    per = 64 // bits
    return ((arr[index // per] & ((1 << 64) - 1)) >> ((index % per) * bits)) & ((1 << bits) - 1)


def main(rdir, step):
    top, biomes, cols = Counter(), {}, 0
    for fn in os.listdir(rdir):
        if not fn.endswith(".mca"):
            continue
        data = open(os.path.join(rdir, fn), "rb").read()
        if len(data) < 8192:
            continue
        for i in range(0, 1024, step):
            off = int.from_bytes(data[i * 4:i * 4 + 3], "big")
            if not off:
                continue
            o = off * 4096
            ln, _ = struct.unpack_from(">ib", data, o)
            nbt = read_nbt(zlib.decompress(data[o + 5:o + 4 + ln]))
            hm = nbt.get("Heightmaps", {}).get("WORLD_SURFACE")
            if not hm:
                continue
            secs = {s["Y"]: s for s in nbt.get("sections", [])}
            ymin = nbt.get("yPos", -4) * 16
            for idx, h in enumerate(heights(hm)):
                y = h + ymin - 1
                sec = secs.get(y >> 4)
                if not sec:
                    continue
                bs, bb = sec.get("block_states", {}), sec.get("biomes", {})
                pal, bpal = bs.get("palette", []), bb.get("palette", [])
                lx, lz, ly = idx & 15, idx >> 4, y & 15
                name = pal[0]["Name"] if len(pal) == 1 else \
                    pal[packed(bs["data"], max(4, (len(pal) - 1).bit_length()), (ly << 8) | (lz << 4) | lx)]["Name"]
                bio = bpal[0] if len(bpal) == 1 else \
                    bpal[packed(bb["data"], (len(bpal) - 1).bit_length(), ((ly >> 2) << 4) | ((lz >> 2) << 2) | (lx >> 2))]
                top[name] += 1
                cols += 1
                biomes.setdefault(bio, [0, 0])[0 if any(k in name for k in FROZEN) else 1] += 1
    frozen = sum(n for b, n in top.items() if any(k in b for k in FROZEN))
    print(f"{cols} Spalten, Schnee/Eis oben: {100 * frozen / max(cols, 1):.1f}%")
    for b, n in top.most_common(15):
        print(f"  {100 * n / cols:5.1f}% {b}")
    print("Biome mit den meisten Spalten ohne Schnee/Eis (frei / gesamt):")
    for b, (f, n) in sorted(biomes.items(), key=lambda kv: -kv[1][1])[:15]:
        print(f"  {n:7d}/{f + n:7d} {100 * n / (f + n):5.1f}% {b}")


if __name__ == "__main__":
    main(sys.argv[1], int(sys.argv[2]) if len(sys.argv) > 2 else 4)
