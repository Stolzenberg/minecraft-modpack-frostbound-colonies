#!/usr/bin/env python3
"""CurseForge-Abfragen über einen öffentlichen, nur lesenden API-Proxy.

usage:
  cf.py search <text>   -> Mods suchen, zeigt Verfügbarkeit für NeoForge 1.21.1
"""
import json, sys, urllib.parse, urllib.request

API = "https://cfproxy.bmpm.workers.dev/v1"
MC = "1.21.1"
NEOFORGE = 6


def get(path, params=None):
    url = API + path + ("?" + urllib.parse.urlencode(params) if params else "")
    req = urllib.request.Request(url, headers={"User-Agent": "modpack-builder", "Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def post(path, body):
    req = urllib.request.Request(API + path, data=json.dumps(body).encode(), method="POST",
                                 headers={"Content-Type": "application/json", "User-Agent": "modpack-builder"})
    with urllib.request.urlopen(req, timeout=120) as r:
        return json.load(r)["data"]


def neo_files(mod_id):
    data = get(f"/mods/{mod_id}/files", {"gameVersion": MC, "modLoaderType": NEOFORGE, "pageSize": 50})["data"]
    data = [f for f in data if f.get("isAvailable", True)]
    data.sort(key=lambda f: f["fileDate"], reverse=True)
    return data


def best(files):
    # releaseType 1=Release 2=Beta 3=Alpha
    for rt in (1, 2, 3):
        for f in files:
            if f["releaseType"] == rt:
                return f
    return None


def search(text):
    res = get("/mods/search", {"gameId": 432, "classId": 6, "searchFilter": text, "sortField": 2, "sortOrder": "desc", "pageSize": 15})
    for m in res["data"]:
        idx = [i for i in m.get("latestFilesIndexes", []) if i["gameVersion"] == MC and i.get("modLoader") == NEOFORGE]
        print(f'{m["id"]:>8}  {m["slug"]:<45} dl={m["downloadCount"]:>11,}  neo1211={"YES" if idx else "-":<3}  {m["dateModified"][:10]}  {m["summary"][:70]}')


if __name__ == "__main__" and len(sys.argv) > 2 and sys.argv[1] == "search":
    search(" ".join(sys.argv[2:]))
