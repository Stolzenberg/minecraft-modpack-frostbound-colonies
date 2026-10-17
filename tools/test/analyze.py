#!/usr/bin/env python3
"""Generierte Region-Dateien auswerten: Landanteil, Oberflächenhöhen, Biome, Strukturstarts.

usage: analyze.py <region-Ordner>
"""
import os, struct, sys, zlib
from collections import Counter


def read_nbt(buf):
    pos = 0

    def u(fmt, n):
        nonlocal pos
        v = struct.unpack_from(fmt, buf, pos)
        pos += n
        return v[0]

    def string():
        nonlocal pos
        n = u(">H", 2)
        s = buf[pos:pos + n].decode("utf-8", "replace")
        pos += n
        return s

    def payload(t):
        nonlocal pos
        if t == 1: return u(">b", 1)
        if t == 2: return u(">h", 2)
        if t == 3: return u(">i", 4)
        if t == 4: return u(">q", 8)
        if t == 5: return u(">f", 4)
        if t == 6: return u(">d", 8)
        if t == 7:
            n = u(">i", 4); v = buf[pos:pos + n]; pos += n; return v
        if t == 8: return string()
        if t == 9:
            et = u(">b", 1); n = u(">i", 4)
            return [payload(et) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = u(">b", 1)
                if tt == 0: return d
                k = string()
                d[k] = payload(tt)
        if t == 11:
            n = u(">i", 4); v = struct.unpack_from(f">{n}i", buf, pos); pos += 4 * n; return v
        if t == 12:
            n = u(">i", 4); v = struct.unpack_from(f">{n}q", buf, pos); pos += 8 * n; return v
        raise ValueError(t)

    t = u(">b", 1)
    string()
    return payload(t)


def heights(longs):
    out, per = [], 64 // 9
    for L in longs:
        L &= (1 << 64) - 1
        for i in range(per):
            out.append((L >> (9 * i)) & 0x1FF)
    return out[:256]


def main(rdir):
    chunks = land_cols = cols = 0
    biome_c, ns_c, struct_c, top_hist = Counter(), Counter(), Counter(), Counter()
    for fn in os.listdir(rdir):
        if not fn.endswith(".mca"):
            continue
        data = open(os.path.join(rdir, fn), "rb").read()
        if len(data) < 8192:
            continue
        for i in range(1024):
            off = int.from_bytes(data[i * 4:i * 4 + 3], "big")
            if not off:
                continue
            o = off * 4096
            ln, comp = struct.unpack_from(">ib", data, o)
            raw = data[o + 5:o + 4 + ln]
            nbt = read_nbt(zlib.decompress(raw) if comp == 2 else raw)
            if nbt.get("Status") not in ("minecraft:full", "full"):
                continue
            chunks += 1
            hm = nbt.get("Heightmaps", {}).get("WORLD_SURFACE")
            if hm:
                for h in heights(hm):
                    cols += 1
                    if h > 0:
                        land_cols += 1
                        top_hist[(h - 64) // 32 * 32] += 1
            for sec in nbt.get("sections", []):
                bp = sec.get("block_states", {}).get("palette", [])
                if all(p.get("Name") in ("minecraft:air", "minecraft:void_air", "minecraft:cave_air") for p in bp):
                    continue
                for b in sec.get("biomes", {}).get("palette", []):
                    biome_c[b] += 1
                    ns_c[b.split(":")[0]] += 1
            for sid in nbt.get("structures", {}).get("starts", {}):
                struct_c[sid] += 1
    print(f"chunks: {chunks}, land columns: {100 * land_cols / max(cols, 1):.1f}%")
    print(f"distinct biomes: {len(biome_c)}; by namespace: {dict(ns_c.most_common())}")
    print(f"structure starts: {sum(struct_c.values())} of {len(struct_c)} types")
    for s, n in struct_c.most_common():
        print(f"  {n:>4} {s}")


if __name__ == "__main__":
    main(sys.argv[1])
