#!/usr/bin/env python3
"""packwiz-Pack (Auto-Updates über GitHub) und Prism-Instanz aus mods.resolved.json bauen.

usage: packwiz.py <mods.resolved.json> <overrides-Ordner> <packwiz-Ordner> <Ausgabe-Ordner>

- packwiz-Ordner: pack.toml + index.toml + mods/*.pw.toml (CurseForge-Metadaten, keine Jars) + Overrides
  und server-overrides/. Wird komplett neu geschrieben, danach committen und pushen.
- Ausgabe-Ordner: <Slug>-Prism.zip, eine Prism-Launcher-Instanz, die vor jedem Start per
  packwiz-installer-bootstrap.jar den Stand von PACK_URL holt. Muss nur einmal importiert werden.
"""
import hashlib, json, os, shutil, sys, zipfile
from cf import post

NAME, VERSION, AUTHOR = "Frostbound Colonies", "1.0.0", "Tim"  # wie in package.py
SLUG = NAME.replace(" ", "")
MC, NEOFORGE = "1.21.1", "21.1.256"
PACK_URL = "https://raw.githubusercontent.com/Stolzenberg/minecraft-modpack-frostbound-colonies/main/packwiz/pack.toml"
# Persönliche Einstellungen: nur bei der Erstinstallation schreiben, Updates überschreiben sie nicht
PRESERVE = ("config/iris.properties",)
FOLDER = {6: "mods", 6552: "shaderpacks", 12: "resourcepacks"}
BOOTSTRAP = os.path.join(os.path.dirname(os.path.abspath(__file__)), "packwiz-installer-bootstrap.jar")


def q(s):
    return json.dumps(s, ensure_ascii=False)  # JSON-Strings sind gültige TOML-Basic-Strings


def sha256(path):
    return hashlib.sha256(open(path, "rb").read()).hexdigest()


all_mods = sorted(json.load(open(sys.argv[1])), key=lambda m: m["name"].lower())
overrides, pack, out = sys.argv[2], sys.argv[3], sys.argv[4]
server_overrides = os.path.join(os.path.dirname(os.path.abspath(overrides)), "server-overrides")

if os.path.isdir(pack):
    if not os.path.exists(os.path.join(pack, "pack.toml")):
        sys.exit(f"{pack} existiert, ist aber kein packwiz-Ordner, breche ab")
    shutil.rmtree(pack)
os.makedirs(pack)
os.makedirs(out, exist_ok=True)

sha1 = {f["id"]: next(h["value"] for h in f["hashes"] if h["algo"] == 1)
        for f in post("/mods/files", {"fileIds": [m["fileID"] for m in all_mods]})}

index = []  # (Pfad, metafile)
for m in all_mods:
    # CF-"Server" heißt nur "auf dem Server nötig", Einzelspieler braucht die Mods trotzdem
    side = "server" if m.get("serverPackOnly") else "client" if m["side"] == "client" else "both"
    rel = f'{FOLDER[m["classId"]]}/{m["slug"]}.pw.toml'
    os.makedirs(os.path.join(pack, os.path.dirname(rel)), exist_ok=True)
    with open(os.path.join(pack, rel), "w") as f:
        f.write(f'name = {q(m["name"])}\nfilename = {q(m["fileName"])}\nside = "{side}"\n\n'
                f'[download]\nhash-format = "sha1"\nhash = "{sha1[m["fileID"]]}"\nmode = "metadata:curseforge"\n\n'
                f'[update]\n[update.curseforge]\nfile-id = {m["fileID"]}\nproject-id = {m["projectID"]}\n')
    index.append((rel, True))

for src in (overrides, server_overrides):
    if not os.path.isdir(src):
        continue
    for root, _, files in os.walk(src):
        for name in files:
            rel = os.path.relpath(os.path.join(root, name), src).replace(os.sep, "/")
            os.makedirs(os.path.join(pack, os.path.dirname(rel)), exist_ok=True)
            shutil.copy2(os.path.join(root, name), os.path.join(pack, rel))
            index.append((rel, False))

with open(os.path.join(pack, "index.toml"), "w") as f:
    f.write('hash-format = "sha256"\n')
    for rel, meta in sorted(index):
        f.write(f'\n[[files]]\nfile = {q(rel)}\nhash = "{sha256(os.path.join(pack, rel))}"\n'
                + ("metafile = true\n" if meta else "") + ("preserve = true\n" if rel in PRESERVE else ""))

with open(os.path.join(pack, "pack.toml"), "w") as f:
    f.write(f'name = {q(NAME)}\nauthor = {q(AUTHOR)}\nversion = {q(VERSION)}\npack-format = "packwiz:1.1.0"\n\n'
            f'[index]\nfile = "index.toml"\nhash-format = "sha256"\nhash = "{sha256(os.path.join(pack, "index.toml"))}"\n\n'
            f'[versions]\nminecraft = "{MC}"\nneoforge = "{NEOFORGE}"\n')

instance_cfg = f"""InstanceType=OneSix
name={NAME}
iconKey=default
OverrideCommands=true
PreLaunchCommand="$INST_JAVA" -jar packwiz-installer-bootstrap.jar {PACK_URL}
OverrideMemory=true
MinMemAlloc=4096
MaxMemAlloc=8192
"""
mmc_pack = {"formatVersion": 1, "components": [
    {"uid": "net.minecraft", "version": MC, "important": True},
    {"uid": "net.neoforged", "version": NEOFORGE},
]}

prism_zip = os.path.join(out, f"{SLUG}-Prism.zip")
with zipfile.ZipFile(prism_zip, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("instance.cfg", instance_cfg)
    z.writestr("mmc-pack.json", json.dumps(mmc_pack, indent=2))
    z.write(BOOTSTRAP, "minecraft/packwiz-installer-bootstrap.jar")

manual = [m["slug"] for m in all_mods if not m.get("downloadUrl") and not m.get("serverPackOnly")]
print(os.path.join(pack, "pack.toml"), f"{len(all_mods)} Mods, {len(index) - len(all_mods)} Dateien aus Overrides")
print(prism_zip, os.path.getsize(prism_zip))
print(f"manueller Download nötig (Drittanbieter-Downloads vom Autor gesperrt): {manual}")
