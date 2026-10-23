#!/usr/bin/env python3
"""Chunks aus einer Welt löschen (region/, entities/, poi/), damit Minecraft sie neu generiert.

usage: delete_chunks.py <Weltordner> <Auswahl.csv> <Sicherungsordner> [--apply]

Auswahl im MCA-Selector-Format (regionX;regionZ;chunkX;chunkZ, z. B. per „Export selection“ aus MCA Selector). Ohne --apply wird nur
angezeigt, was passieren würde. Mit --apply werden alle betroffenen .mca-Dateien zuerst in den Sicherungsordner
kopiert, dann die Einträge der Chunks im Dateikopf (Position + Zeitstempel) auf 0 gesetzt. Minecraft behandelt die
Chunks danach als nie generiert. Das Spiel muss dabei geschlossen sein (session.lock wird nicht geprüft).
"""
import os, shutil, sys
from collections import defaultdict


def main():
    world, csv, backup = sys.argv[1:4]
    apply = "--apply" in sys.argv
    sel = defaultdict(set)
    for line in open(csv):
        rx, rz, cx, cz = map(int, line.strip().split(";"))
        sel[(rx, rz)].add(cx + cz * 32)  # Index im Regions-Dateikopf
    total = 0
    for sub in ("region", "entities", "poi"):
        for (rx, rz), idxs in sorted(sel.items()):
            path = os.path.join(world, sub, f"r.{rx}.{rz}.mca")
            if not os.path.exists(path) or os.path.getsize(path) < 8192:
                continue
            with open(path, "rb") as f:
                header = bytearray(f.read(8192))
            hit = [i for i in idxs if header[i * 4:i * 4 + 4] != b"\0\0\0\0"]
            print(f"{sub}/r.{rx}.{rz}.mca: {len(hit)} von {len(idxs)} Chunks vorhanden")
            total += len(hit)
            if not apply or not hit:
                continue
            dst = os.path.join(backup, sub)
            os.makedirs(dst, exist_ok=True)
            shutil.copy2(path, os.path.join(dst, os.path.basename(path)))
            for i in hit:
                header[i * 4:i * 4 + 4] = b"\0\0\0\0"          # Position/Länge
                header[4096 + i * 4:4096 + i * 4 + 4] = b"\0\0\0\0"  # Zeitstempel
            with open(path, "r+b") as f:
                f.write(header)
    print(f"{'Gelöscht' if apply else 'Würde löschen'}: {total} Chunk-Einträge" + (f", Sicherung in {backup}" if apply else ""))


if __name__ == "__main__":
    main()
