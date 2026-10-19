#!/usr/bin/env python3
"""Strukturdichte senken: Abstände aller Struktur-Sets (Mods + Vanilla) vergrößern.

usage: scale_structures.py <Ordner mit Mod-Jars> <Vanilla-Server-Jar mit data/> <packwiz/kubejs/data>

Für jedes Struktur-Set mit zufälliger Verteilung (random_spread und die erweiterten Varianten der Mods) werden
"spacing" und "separation" mit FACTOR multipliziert (Dichte ≈ 1/FACTOR²). Namespaces in FACTOR_BY_NAMESPACE bekommen
einen eigenen Faktor. Festungsringe (concentric_rings) bleiben unverändert. Überschreibt eine Mod ein Vanilla-Set,
wird die Mod-Version als Grundlage genommen. Die Kopien unter kubejs/data überschreiben die Originale.
"""
import glob, json, os, re, sys, zipfile

FACTOR = 1.4  # ≈ halbe Dichte
FACTOR_BY_NAMESPACE = {"mvs": 2.0}  # Moog's Voyager: viel Kleinkram (Karren, Heuballen, Holzstapel) -> ≈ ein Viertel
SKIP_TYPES = ("concentric_rings",)
MAX_SPACING = 4096


def collect(jars, vanilla):
    sets = {}
    for j in [vanilla] + sorted(jars):  # Mods nach Vanilla -> Mod-Version gewinnt
        z = zipfile.ZipFile(j)
        for n in z.namelist():
            m = re.match(r"data/([^/]+)/worldgen/structure_set/(.+)\.json$", n)
            if not m:
                continue
            try:
                sets[(m.group(1), m.group(2))] = json.loads(z.read(n))
            except ValueError:
                print(f"nicht lesbar: {j}: {n}")
    return sets


def main():
    jars_dir, vanilla, out = sys.argv[1:4]
    sets = collect(glob.glob(os.path.join(jars_dir, "*.jar")), vanilla)
    done = skipped = 0
    for (ns, path), d in sorted(sets.items()):
        p = d.get("placement", {})
        if any(t in p.get("type", "") for t in SKIP_TYPES) or "spacing" not in p or "separation" not in p:
            skipped += 1
            continue
        f = FACTOR_BY_NAMESPACE.get(ns, FACTOR)
        spacing = min(MAX_SPACING, max(2, round(p["spacing"] * f)))
        separation = min(spacing - 1, max(1, round(p["separation"] * f)))
        p["spacing"], p["separation"] = spacing, separation
        target = os.path.join(out, ns, "worldgen", "structure_set", path + ".json")
        os.makedirs(os.path.dirname(target), exist_ok=True)
        json.dump(d, open(target, "w"), indent=2)
        done += 1
    print(f"{done} Struktur-Sets skaliert (Faktor {FACTOR}, {FACTOR_BY_NAMESPACE}), {skipped} unverändert -> {out}")


if __name__ == "__main__":
    main()
