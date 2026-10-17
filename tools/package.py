#!/usr/bin/env python3
"""Client-Pack (CurseForge-Import) und Server-Pack aus mods.resolved.json bauen.

usage: package.py <mods.resolved.json> <overrides-Ordner> <Ausgabe-Ordner>

- Client-ZIP: manifest.json + modlist.html + overrides/ (alles außer serverPackOnly-Mods)
- Server-ZIP: Installationsskripte + mods.txt (Download-Liste) + Configs aus overrides/ (ohne reine
  Client-Configs) + server-overrides/ (liegt neben dem overrides-Ordner)
"""
import html, json, os, sys, urllib.parse, zipfile

NAME, VERSION, AUTHOR = "Frostbound Colonies", "1.0.0", "Tim"
SLUG = NAME.replace(" ", "")
MC, NEOFORGE = "1.21.1", "21.1.256"
CLIENT_ONLY_CONFIGS = ("config/iris.properties",)

all_mods = sorted(json.load(open(sys.argv[1])), key=lambda m: m["name"].lower())
mods = [m for m in all_mods if not m.get("serverPackOnly")]  # Client-Manifest
overrides, out = sys.argv[2], sys.argv[3]
server_overrides = os.path.join(os.path.dirname(os.path.abspath(overrides)), "server-overrides")
os.makedirs(out, exist_ok=True)

manifest = {
    "minecraft": {"version": MC, "modLoaders": [{"id": f"neoforge-{NEOFORGE}", "primary": True}], "recommendedRam": 8192},
    "manifestType": "minecraftModpack", "manifestVersion": 1,
    "name": NAME, "version": VERSION, "author": AUTHOR,
    "files": [{"projectID": m["projectID"], "fileID": m["fileID"], "required": True} for m in mods],
    "overrides": "overrides",
}
modlist = "<ul>\n" + "".join(
    f'<li><a href="{html.escape(m["url"])}">{html.escape(m["name"])} (by {html.escape(", ".join(m["authors"]))})</a></li>\n'
    for m in mods) + "</ul>\n"


def add_tree(z, src, prefix, skip=()):
    for root, _, files in os.walk(src):
        for f in files:
            p = os.path.join(root, f)
            rel = os.path.relpath(p, src)
            if rel.startswith(skip):
                continue
            z.write(p, os.path.join(prefix, rel))


def cdn(m):
    if m.get("downloadUrl"):
        return m["downloadUrl"]
    fid = m["fileID"]
    return f"https://edge.forgecdn.net/files/{fid // 1000}/{fid % 1000}/{urllib.parse.quote(m['fileName'])}"


client_zip = os.path.join(out, f"{SLUG}-{VERSION}.zip")
with zipfile.ZipFile(client_zip, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("manifest.json", json.dumps(manifest, indent=2))
    z.writestr("modlist.html", modlist)
    add_tree(z, overrides, "overrides")

server_mods = [m for m in all_mods if m["side"] != "client"]
mods_txt = "".join(f'{m["fileName"]}\t{cdn(m)}\n' for m in server_mods)

install_sh = f"""#!/usr/bin/env bash
# {NAME} {VERSION} - Server-Installation (benötigt Java 21, curl)
set -euo pipefail
cd "$(dirname "$0")"
NEOFORGE={NEOFORGE}
if [ ! -d libraries/net/neoforged/neoforge/$NEOFORGE ]; then
  curl -fL -o neoforge-installer.jar "https://maven.neoforged.net/releases/net/neoforged/neoforge/$NEOFORGE/neoforge-$NEOFORGE-installer.jar"
  java -jar neoforge-installer.jar --installServer .
  rm -f neoforge-installer.jar neoforge-installer.jar.log
fi
mkdir -p mods
while IFS=$'\\t' read -r file url; do
  [ -z "$file" ] && continue
  [ -f "mods/$file" ] || {{ echo "Lade $file"; curl -fsL -o "mods/$file" "$url"; }}
done < mods.txt
echo "Fertig. EULA in eula.txt akzeptieren, dann ./start.sh ausführen."
"""
start_sh = f"""#!/usr/bin/env bash
cd "$(dirname "$0")"
exec java @user_jvm_args.txt @libraries/net/neoforged/neoforge/{NEOFORGE}/unix_args.txt nogui "$@"
"""
start_bat = f"""@echo off
java @user_jvm_args.txt @libraries/net/neoforged/neoforge/{NEOFORGE}/win_args.txt nogui %*
pause
"""
install_ps1 = f"""# {NAME} {VERSION} - Server-Installation fuer Windows (benoetigt Java 21)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$nf = "{NEOFORGE}"
if (-not (Test-Path "libraries/net/neoforged/neoforge/$nf")) {{
  Invoke-WebRequest "https://maven.neoforged.net/releases/net/neoforged/neoforge/$nf/neoforge-$nf-installer.jar" -OutFile neoforge-installer.jar
  java -jar neoforge-installer.jar --installServer .
  Remove-Item neoforge-installer.jar*
}}
New-Item -ItemType Directory -Force mods | Out-Null
Get-Content mods.txt | Where-Object {{ $_ }} | ForEach-Object {{
  $f, $u = $_ -split "`t"
  if (-not (Test-Path "mods/$f")) {{ Write-Host "Lade $f"; Invoke-WebRequest $u -OutFile "mods/$f" }}
}}
Write-Host "Fertig. EULA in eula.txt akzeptieren, dann start.bat ausfuehren."
"""
server_properties = """# Frostbound Colonies: normale Weltgenerierung (Terralith + BOP), Primal Winter friert die Overworld ein
motd=Frostbound Colonies - MineColonies im ewigen Winter
view-distance=10
simulation-distance=8
spawn-protection=0
allow-flight=true
"""

server_zip = os.path.join(out, f"{SLUG}-{VERSION}-Server.zip")
with zipfile.ZipFile(server_zip, "w", zipfile.ZIP_DEFLATED) as z:
    for name, content in (("install.sh", install_sh), ("start.sh", start_sh)):
        info = zipfile.ZipInfo(name)
        info.external_attr = (0o100000 | 0o755) << 16
        info.compress_type = zipfile.ZIP_DEFLATED
        z.writestr(info, content)
    z.writestr("install.ps1", install_ps1)
    z.writestr("start.bat", start_bat)
    z.writestr("mods.txt", mods_txt)
    z.writestr("server.properties", server_properties)
    z.writestr("user_jvm_args.txt", "-Xms6G\n-Xmx10G\n")
    z.writestr("eula.txt", "# https://aka.ms/MinecraftEULA lesen und auf true setzen\neula=false\n")
    add_tree(z, overrides, "", skip=CLIENT_ONLY_CONFIGS)
    if os.path.isdir(server_overrides):
        add_tree(z, server_overrides, "")

client_only = sorted(m["slug"] for m in all_mods if m["side"] == "client")
print(client_zip, os.path.getsize(client_zip))
print(server_zip, os.path.getsize(server_zip))
print(f"{len(mods)} im Client-Manifest, {len(server_mods)} auf dem Server "
      f"(nur Server-Pack: {[m['slug'] for m in all_mods if m.get('serverPackOnly')]}), nur Client: {client_only}")
