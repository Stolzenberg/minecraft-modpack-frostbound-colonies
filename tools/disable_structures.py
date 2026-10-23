#!/usr/bin/env python3
"""Schwebende Strukturen abschalten: Struktur-JSON aus den Mod-Jars kopieren und auf einen leeren Biom-Tag umbiegen.

usage: disable_structures.py <Ordner mit Mod-Jars> <packwiz/kubejs/data>

Die Kopien landen unter kubejs/data/<namespace>/worldgen/structure/ und überschreiben die Originale. "biomes" zeigt
auf #proudmountain:none (leerer Tag), dadurch wird die Struktur nirgends mehr platziert. Alle übrigen Felder bleiben
unverändert, damit /locate und Advancements weiter gültige Einträge finden.
"""
import glob, json, os, sys, zipfile

# Schweben im Himmel (Luftschiffe, Himmelsinseln, Himmelsdungeons). Ganze Mods dafür sind bereits entfernt:
# MSS (Moog's Soaring Structures) und Sky Whale Ship.
FLOATING = [
    "mvs:large_floating_island", "mvs:floating_islands",
    "dungeons_arise:heavenly_challenger", "dungeons_arise:heavenly_conqueror", "dungeons_arise:heavenly_rider",
    "dungeons_arise:mechanical_nest", "dungeons_arise:small_blimp",
]
EMPTY_TAG = "proudmountain:none"


def main():
    jars, out = sys.argv[1], sys.argv[2]
    found = {}
    for j in glob.glob(os.path.join(jars, "*.jar")):
        z = zipfile.ZipFile(j)
        for sid in FLOATING:
            ns, path = sid.split(":")
            name = f"data/{ns}/worldgen/structure/{path}.json"
            if name in z.namelist():
                found[sid] = json.loads(z.read(name))
    missing = [s for s in FLOATING if s not in found]
    if missing:
        sys.exit(f"nicht gefunden: {missing}")
    for sid, d in found.items():
        ns, path = sid.split(":")
        d["biomes"] = "#" + EMPTY_TAG
        p = os.path.join(out, ns, "worldgen", "structure", path + ".json")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        json.dump(d, open(p, "w"), indent=2)
    tns, tpath = EMPTY_TAG.split(":")
    p = os.path.join(out, tns, "tags", "worldgen", "biome", tpath + ".json")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump({"replace": True, "values": []}, open(p, "w"), indent=2)
    print(f"{len(found)} Strukturen abgeschaltet -> {out}")


if __name__ == "__main__":
    main()
