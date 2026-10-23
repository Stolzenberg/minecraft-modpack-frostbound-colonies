#!/usr/bin/env python3
"""modlist.txt (+ Pflicht-Abhängigkeiten) zu konkreten CurseForge-Dateien für NeoForge 1.21.1 auflösen.

usage: build.py <modlist.txt> <mods.resolved.json>

Optionen pro Zeile: pin=<fileID>  side=client|server  class=<CF-Klasse, 6=Mod, 6552=Shader>  pack=server
                    id=<projectID> (falls die CurseForge-Suche den Slug nicht findet)
Abhängigkeiten ohne pin behalten die Datei aus der bestehenden mods.resolved.json (Lock),
nur neue Abhängigkeiten werden auf die neueste NeoForge-1.21.1-Datei aufgelöst.
"""
import json, os, sys
from cf import best, get, neo_files

# Fabric-Metadaten, die auf NeoForge-Dateien fälschlich als Pflicht auftauchen
IGNORE_DEPS = {306612: "fabric-api", 624165: "create-fabric"}


def parse(path):
    out = []
    for line in open(path):
        line = line.split("#", 1)[0].strip()
        if not line:
            continue
        parts = line.split()
        out.append((parts[0], dict(p.split("=", 1) for p in parts[1:])))
    return out


def main():
    modlist, lock_path = sys.argv[1], sys.argv[2]
    lock = {m["projectID"]: m["fileID"] for m in json.load(open(lock_path))} if os.path.exists(lock_path) else {}
    mods, queue = {}, []
    for slug, opts in parse(modlist):
        if "id" in opts:
            res = [get(f"/mods/{opts['id']}")["data"]]
        else:
            res = get("/mods/search", {"gameId": 432, "classId": int(opts.get("class", 6)), "slug": slug})["data"]
        if not res:
            sys.exit(f"nicht gefunden: {slug}")
        queue.append((res[0], opts, "listed"))

    while queue:
        mod, opts, why = queue.pop(0)
        if mod["id"] in mods:
            continue
        pin = opts.get("pin") or lock.get(mod["id"])
        f = get(f"/mods/{mod['id']}/files/{pin}")["data"] if pin else best(neo_files(mod["id"]))
        if not f:
            sys.exit(f"keine NeoForge-1.21.1-Datei: {mod['slug']}")
        env = [g for g in f.get("gameVersions", []) if g in ("Client", "Server")]
        side = opts.get("side") or ("client" if env == ["Client"] else "server" if env == ["Server"] else "both")
        req = [d["modId"] for d in f.get("dependencies", []) if d["relationType"] == 3 and d["modId"] not in IGNORE_DEPS]
        mods[mod["id"]] = {"slug": mod["slug"], "name": mod["name"], "projectID": mod["id"], "fileID": f["id"],
                           "fileName": f["fileName"], "date": f["fileDate"][:10], "releaseType": f["releaseType"],
                           "downloadUrl": f.get("downloadUrl"), "side": side, "why": why, "requires": req,
                           "url": mod["links"]["websiteUrl"], "authors": [a["name"] for a in mod.get("authors", [])],
                           "classId": mod.get("classId", 6), "serverPackOnly": opts.get("pack") == "server"}
        changed = "" if lock.get(mod["id"]) in (None, f["id"]) else f"  (vorher {lock[mod['id']]})"
        new = "" if mod["id"] in lock else "  NEU"
        print(f'{mod["slug"]:<42} {f["fileName"]:<60} {side:<6} {why}{new}{changed}', file=sys.stderr)
        for dep in req:
            if dep not in mods:
                queue.append((get(f"/mods/{dep}")["data"], {}, f"dep of {mod['slug']}"))

    removed = [pid for pid in lock if pid not in mods]
    json.dump(list(mods.values()), open(lock_path, "w"), indent=1, ensure_ascii=False)
    print(f"{len(mods)} Einträge aufgelöst, entfernt: {removed}", file=sys.stderr)


if __name__ == "__main__":
    main()
