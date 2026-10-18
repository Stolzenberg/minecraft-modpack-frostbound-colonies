# Frostbound Colonies 1.0.0

CurseForge-Modpack für **Minecraft 1.21.1 / NeoForge 21.1.256**, inspiriert von *Frostpunk*: Ein ewiger Winter hat die ganze Welt erfasst. Du gründest eine **MineColonies**-Kolonie, hältst sie mit Feuer, Kaminen und dem Hearth warm, versorgst sie mit **Farmer's Delight**-Eintöpfen und automatisierst mit **Create**. Draußen warten verschneite Ruinen, Dungeons und Bosse. Weihnachts-Mods bringen Deko und Festtagsessen dazu.

Basis ist *Skyward Frontiers* (`../skyward-frontiers`): gleiche Struktur- und Mob-Mods, gleiche QoL-, Performance- und Grafik-Mods in denselben Versionen, gleiche Toolchain.

## Dateien

| Datei | Zweck |
|---|---|
| `dist/FrostboundColonies-Prism.zip` | Prism-Launcher-Instanz mit Auto-Update (empfohlen, siehe [Installation](#installation-prism-launcher-mit-auto-update)) |
| `dist/FrostboundColonies-1.0.0.zip` | Client-Pack für die CurseForge-App (veraltet: Stand 1.0.0, wird nicht mehr gebaut) |
| `dist/FrostboundColonies-1.0.0-Server.zip` | Server-ZIP ohne Docker (veraltet: Stand 1.0.0, wird nicht mehr gebaut) |
| `overrides/` | Mitgelieferte Configs, KubeJS-Skripte und das Schneeverwehungs-Datapack (`kubejs/data/`) (Quelle) |
| `mods.resolved.json` | Exakte Projekt- und Datei-IDs aller Mods (Lock-Datei) |
| `packwiz/` | packwiz-Pack, aus dem die Prism-Instanzen ihre Updates holen (erzeugt, nicht von Hand bearbeiten). Mods, die als identische Datei auf Modrinth liegen (87 von 110), werden von dort geladen, der Rest von CurseForge |
| `tools/` | `scale_structures.py` (Strukturdichte senken), `disable_structures.py` (schwebende Strukturen abschalten), `modlist.txt` (Modliste, alle Versionen gepinnt), `build.py` (Abhängigkeiten auflösen), `package.py` (ZIPs bauen), `packwiz.py` (packwiz-Pack und Prism-Instanz bauen), `packwiz-installer-bootstrap.jar` (v0.0.3, steckt in der Prism-Instanz), `coldsweat_frost.py` (Temperatur-Configs erzeugen), `coldsweat-defaults/` (Cold-Sweat-Standardwerte als Eingabe), `test/` (Testserver-Werkzeuge; `surface.py` zählt Schnee/Eis an der Oberfläche je Biom) |
| `server-overrides/` | Configs nur für den Server (AntiXray) |
| `screenshots/` | 15 Screenshots aus dem Client mit Bildunterschriften (`screenshots/README.md`) |

Neu bauen: `python3 tools/build.py tools/modlist.txt mods.resolved.json`, dann `cd tools && python3 packwiz.py ../mods.resolved.json ../overrides ../packwiz ../dist`. Verteilt wird nur noch über packwiz (Prism Launcher und Docker-Server). `tools/package.py` baut bei Bedarf weiterhin die CurseForge-ZIPs.

## Installation (Prism Launcher mit Auto-Update)

1. [Prism Launcher](https://prismlauncher.org) installieren und mit dem Microsoft-Konto anmelden.
2. [`FrostboundColonies-Prism.zip`](https://github.com/Stolzenberg/minecraft-modpack-frostbound-colonies/raw/main/dist/FrostboundColonies-Prism.zip) herunterladen und in Prism über *Instanz hinzufügen* → *Importieren* laden (oder die ZIP ins Prism-Fenster ziehen).
3. Instanz starten. Vor jedem Start gleicht `packwiz-installer` die Instanz mit `packwiz/` in diesem Repo ab und lädt nur Geändertes. Neue Mods kommen dazu, entfernte werden gelöscht. In der Serverliste steht schon „Frostbound Colonies“ (`server.proud-mountain.com`), das kommt aus der `servers.dat` in der ZIP und wird nur beim Import angelegt.
4. Beim ersten Start erscheint ein Fenster für 4 Mods, deren Autoren Downloads über Launcher gesperrt haben und die es nicht auf Modrinth gibt: CreateColonies (`createcolonies-2.0.7.jar`), Christmas Delight (`farmers_delight_christmas_edition-V1.0.2-neoforge-1.20.1.jar`, „1.20.1“ im Namen ist richtig), Christmas Music Discs (`christmasmusicdiscs-1.21.1-v1.3.jar`) und Armor Series - Norsemen (`armor_series__norsemen-1.0.0-neoforge-1.21.1.jar`, **nicht** die Forge-1.20.1-Datei daneben, sonst trennt der Server die Verbindung). Jeden Link im Browser öffnen, genau diese Datei herunterladen und mit unverändertem Dateinamen nach `minecraft/mods/` der Instanz legen (in Prism: Rechtsklick auf die Instanz → *Ordner*, `mods` ggf. anlegen), danach im Fenster weiter. Das ist nur nötig, wenn eine dieser Mods eine neue Version bekommt.

Was ein Update **nicht** anfasst: `options.txt` (Grafik, Tasten, Sound), Welten, Serverliste, Screenshots, Minimap-Wegpunkte, `config/iris.properties` (eigene Shader-Einstellungen) und `config/coldsweat/client.toml` (Cold-Sweat-Anzeige, Standard **°C**, umschaltbar unter *Optionen → Cold Sweat*). Beide sind als `preserve` markiert und werden nur bei der Erstinstallation geschrieben. Mitgelieferte Gameplay-Configs und KubeJS-Skripte werden bei Änderungen überschrieben, damit alle mit denselben Regeln spielen.

**Update veröffentlichen:** Neu bauen (siehe oben), committen, pushen. GitHub liefert die neue Version nach spätestens ca. 5 Minuten aus, beim nächsten Start ziehen alle Instanzen nach.

**Server (Docker, `itzg/minecraft-server`):** `PACKWIZ_URL=https://raw.githubusercontent.com/Stolzenberg/minecraft-modpack-frostbound-colonies/main/packwiz/pack.toml` setzen (zusammen mit `TYPE=NEOFORGE`, `VERSION`, `NEOFORGE_VERSION`). Der Container gleicht bei jedem Start ab und installiert nur die Server-Seite (ohne Client-Mods, mit AntiXray und CoreProtect). Nach einem Push `docker compose restart mc`. Die 4 gesperrten Mods (siehe Installation, Schritt 4) müssen einmal unter ihrem Dateinamen in `data/mods/` liegen, sonst bricht der Start ab. Ändert sich die NeoForge-Version, auch `NEOFORGE_VERSION` anpassen. Außerdem nötig: UDP-Port 24454 für Simple Voice Chat veröffentlichen (`24454:24454/udp`) und ein MariaDB-Container für CoreProtect, siehe [Server: Voice Chat und CoreProtect](#server-voice-chat-und-coreprotect). Ohne Docker: `java -jar packwiz-installer-bootstrap.jar -g -s server <URL>` im Serverordner vor dem Start.

## Welt: ewiger Winter
- **Normale Weltgenerierung** mit **Tectonic** (`config/tectonic.json`): weite Kontinente, große zusammenhängende Gebirgszüge und Ebenen mit Vanilla-Biomen, kein eigener Welttyp nötig. Gegenüber dem Standard: Biome doppelt so breit (`temperature_scale`/`vegetation_scale` 0,125, etwa 4× Fläche), breitere Gebirgszüge (`erosion_scale` 0,18), keine kleinen Inseln, keine Dschungel-Felssäulen. Gemessen: typische Biomgröße (Median) 3,6 ha statt 0,4 ha mit dem vorher genutzten Terralith, 56 % statt 14 % der Fläche liegen in großen Biomen (> 25 ha). Tectonic braucht Lithostitched, das nicht als Abhängigkeit eingetragen ist und deshalb in der Modliste steht.
- **Colder Primal Winter** friert die komplette Overworld ein: Schnee und Eis auf jedem Biom, gefrorene Ozeane, verschneite Bäume, Eisspitzen und Pulverschnee überall, Nebel und Sturmgeräusche. Vanilla-Dörfer sind verlassen.
  - Im Test liegt auf **95 % der Oberfläche Schnee oder Eis** (Tectonic-Welt, 3 × 3 km; Rest vor allem Wasser, Strukturböden, Steinplatten). Schwebendes Gelände: 3D-Prüfung auf 512 × 512 m ohne Inseln oder Brocken, nur Kleinstteile an Höhlendecken wie in Vanilla.
  - **Keine Schneetürme und keine Pulverschnee-Flecken von Primal Winter:** Primal Winter setzt normalerweise in jeden achten Chunk eine Eisspitze aus Schneeblöcken und zusätzlich Pulverschnee-Flecken, in jedem Biom. Beides ist per Datapack abgeschaltet (`kubejs/data/primalwinter/neoforge/biome_modifier/instance.json`, `ice_spikes` und `powder_snow_patch` aus der Liste gestrichen; Eis- und Schneeflecken bleiben). Pulverschnee: 13,3 % → 7,9 % der Oberfläche auf 500 × 500 m. Der Rest liegt bewusst unverändert in den Vanilla-Biomen Schneehänge und Hain. Gemessen auf 500 × 500 m: Spalten mit mindestens 8 Schneeblöcken übereinander 1.146 → 113, der Rest sind natürliche Schneehänge (`snowy_slopes`).
  - **Keine Schneehaufen auf dem Meer:** Primal Winter schaltet per Mixin (`useIcebergExtensionOnAllOceans`, ohne Config-Option) Minecrafts Eisberg-Erweiterung des Oberflächensystems für alle Ozeane ein, normal gibt es sie nur in gefrorenen Ozeanen. Die Säulen entstehen aus der Rauschfunktion `minecraft:iceberg_pillar`, deren Stärke per Datapack auf 0 gesetzt ist (`kubejs/data/minecraft/worldgen/noise/iceberg_pillar.json`). Gemessen: normaler Ozean 17,9 % → 0 %, lauwarmer 11,4 % → 0 % der Fläche mit Eisbergen. In gefrorenen Ozeanen bleiben die normalen Vanilla-Eisberge (1,9–4 %).
  - Pflanzen und Felder wachsen normal, nur die verschneiten Deko-Varianten von Gras und Ranken wachsen nicht.
  - **Schneeverwehungen** (eigenes Worldgen-Feature in `kubejs/data/frostbound/`): Die Schneedecke ist unterschiedlich hoch. Eine Rauschfunktion bildet weiche Verwehungen von 2–5 Schichten, die über etwa 16 Blöcke an- und abschwellen. Gemessen an Land (2 Seeds): 67 % des Schnees mit 1 Schicht, 11 % mit 2, 10 % mit 3, 8 % mit 4, 5 % mit 5. Ozean-Eisberge bleiben flach.
    - Mobs können nur auf einer einzelnen Schicht spawnen (ab 2 Schichten kollidiert die Schneeschicht). Deshalb bleibt bewusst der Großteil einschichtig. Primal Winters eigene Option `enableSnowAccumulationDuringWorldgen` ist aus: Sie legt 86 % des Schnees mehrschichtig und unterdrückt damit fast alle Spawns (im Test 0 statt 18 Eisbären).
    - Feinjustierung: Die Schwellen stehen als 100er-Liste in `worldgen/configured_feature/snow_drift.json` (mehr Einträge mit höheren `layers` = mehr Verwehungen). `firstOctave` (−4 ≈ 16 Blöcke) bestimmt die Größe der Verwehungen.
    - Kosten: Vorgenerierung ca. 68 statt 80 Chunks/s.
  - Schnee häuft sich bei Schneefall nicht über eine Schicht hinaus an (`enableSnowAccumulationDuringWeather = false`), damit Kolonie-Wege und Baustellen frei bleiben.
- **3× längere Tage** (Better Days, `config/betterdays-common.toml`: `daySpeed`/`nightSpeed` = 0,333): Ein Minecraft-Tag dauert 60 statt 20 Minuten (30 Minuten Tag, 30 Minuten Nacht). Gemessen: 200 statt 600 Ticks Tageszeit in 30 Sekunden. Beim Schlafen läuft die Zeit sanft schneller bis zum Morgen statt zu springen. Pflanzen wachsen weiter in Echtzeit, pro Tag also dreimal so viel.
- **Wetter** (`kubejs/server_scripts/frost_storms.js`): Primal Winter würde dauerhaftes Gewitter setzen, also Monster auch am Tag. Stattdessen gilt ein Zyklus:
  - Normalerweise schneit es ruhig (30–60 Minuten am Stück, also ½–1 der längeren Tage), tagsüber ist es hell und sicher.
  - Danach kommt mit 40 % **klarer Himmel** (5–12 Minuten bzw. 2–5 Spielstunden, Titel „Der Himmel klart auf“): kein Schneenebel, Fernsicht, durch Distant Horizons bis weit zum Horizont. Ohne Wolkendecke ist es etwa 5 °F milder.
  - Sonst ein **Whiteout**: eine Minute Vorwarnung per Titel, dann 5–10 Minuten Schneesturm. Währenddessen spawnen Monster auch am Tag, und wer im Freien steht, kühlt zusätzlich aus. Warme Kleidung dämpft das bis auf 15 %.
  - **`/weather` wird respektiert:** Von Hand gesetztes Wetter gilt für die angegebene Dauer (bis 1.000.000 Ticks), danach schneit es wieder. Längere Dauern (Primal Winters Dauergewitter bei neuen Welten) und das Zurücksetzen beim Schlafen werden überschrieben, Schlafen beendet den Schneefall also nicht.
  - Der Zustand übersteht Neustarts. Admins: `/frost status` (Phase, Restzeit, Temperatur an der eigenen Position), `/frost storm`, `/frost sky` (klarer Himmel), `/frost snow` (zurück zum Schneefall, Alias `/frost clear`).
  - Getestet: neue Welt, `/weather clear 600` (nach 600 Ticks wieder Schnee), `/weather thunder 400` mit vorzeitigem `/frost snow`, `/frost sky`, `/frost storm`. Das Zurücksetzen beim Schlafen ist nur im Code abgefangen, nicht im Spiel getestet.
- **Strukturen:** aus Skyward Frontiers Moog's Voyager, When Dungeons Arise, Explorify und Create: Structures Arise, dazu Moog's Temples/Mineshafts/Nether/End, Dungeons and Taverns, YUNG's Better Dungeons, Repurposed Structures und Iglus.
- **Weniger Strukturen** (`tools/scale_structures.py` → `kubejs/data/<mod>/worldgen/structure_set/`): Die Abstände aller 400 Struktur-Sets (Mods und Vanilla, außer Festungen) sind ×1,4 vergrößert, Moog's Voyager mit seinem vielen Kleinkram (Karren, Heuballen, Holzstapel) ×2. Create: Structures Arise ignoriert Datapacks und wird über `config/create_structures_arise-server.toml` gesteuert (`spawn_multiplier` 0,7; gemessen wirkt der Wert quadratisch, 0,5 ergab ein Viertel). Gemessen auf 2 × 2 km (Seed `frostbound2`): 643 → 254 Strukturstarts (mit CSA 0,5), ohne Moog's-Kleinkram 470 → 220 (47 %), Moog's Voyager 173 → 34. Die Positionen ändern sich dabei, Bauwerke in bestehenden Welten bleiben.
- **Keine schwebenden Strukturen:** MSS (Moog's Soaring Structures) und Sky Whale Ship sind entfernt. Zehn weitere Himmelsbauwerke aus anderen Mods sind per Datapack abgeschaltet (die drei von Create: Structures Arise zusätzlich in dessen Config) (`tools/disable_structures.py` → `kubejs/data/<mod>/worldgen/structure/`, verweisen auf den leeren Biom-Tag `#frostbound:none`): die drei Heavenly-Dungeons, Mechanical Nest und Small Blimp (When Dungeons Arise), Luftschiff, Mini-Himmelsdorf und Airdrop (Create: Structures Arise) sowie die beiden Floating Islands (Moog's Voyager). Geprüft über 1.260 Oberflächenbauwerke: keines davon schwebt (nur eine Taverne an einer Klippenkante und ein Iglu-Keller über einer Höhle haben Luft darunter).

## Kälte: Unterkühlung, Wärme, Kleidung (Cold Sweat)
Kälte ist der eigentliche Gegner. **Cold Sweat** gibt dem Spieler eine Körpertemperatur: Wer zu lange zu kalt ist, wird langsamer, baut schlechter ab, verliert maximale Gesundheit und bekommt schließlich Frostschaden.
- **Temperaturen** (`config/coldsweat/world.toml`, erzeugt mit `tools/coldsweat_frost.py`): Jede Biom-Temperatur ist `alt × 0,5 − 5 °F`. Alles liegt damit unter der Wohlfühlgrenze von 50 °F. Ehemalige Wüsten und Savannen sind die „wärmsten“ Gegenden, Gipfel und alte Schneebiome die gefährlichsten. Nachts ist es kälter als mittags. Höhlen (Deep Dark, Lush Caves) bleiben milde Rückzugsorte.
  - Gemessen: Ebene −0,8 °F nachts, Windswept Hills ca. 1–7 °F.
- **Aufwärmen:**
  - **Lagerfeuer** und Feuer: +65 °F (Cold-Sweat-Standard ×2,6, sonst zu schwach für die kältere Welt). Gemessen in der Ebene bei Nacht: 65 °F direkt daneben, 54 °F bei 3 Blöcken (noch warm), 44 °F bei 4, 34 °F bei 5 und 0 °F bei 7 Blöcken. Ein unterkühlter Körper erholt sich am Feuer in etwa einer Minute. Auf Gipfeln und in alten Schneebiomen braucht man zwei Feuer (Obergrenze +90 °F) oder einen Hearth.
  - Außerdem Lava, Magma, Öfen/Hochöfen/Räucheröfen im Betrieb (+25 °F, kleine Reichweite) und brennende Kerzen (+4 °F).
  - **Weihnachtskamine** (ManyIdeas Christmas, alle Holzarten) als dauerhafte Wärmequelle (+60 °F).
  - **Create Blaze Burner** heizen je nach Stufe (+12 bis +72 °F).
  - Eis kühlt die Umgebung (Cold-Sweat-Standard): An gefrorenen Seen und Meeren ist es noch kälter.
  - Der **Hearth** ist der „Generator“ der Kolonie: Er füllt ein ganzes Gebäude mit warmer Luft und lässt sich über Rauchabzüge weiterleiten. Er läuft mit Kohle, Holz oder Lava. **Zusätzliches Overworld-Rezept:** Rauchabzug oben, 3 Ziegelblöcke in der Mitte, unten Eisen – Hochofen – Eisen (`frost_recipes.js`). Das Originalrezept mit Netherziegeln funktioniert weiterhin.
  - **Warme Speisen** heizen für eine Weile auf: Suppen und Eintöpfe (Farmer's Delight, MineColonies, Vanilla) +15 für 2 Min., heiße Getränke (Kakao, Cider, Weihnachtstee, Zimt-Eierpunsch, heiße Schokolade) +20 für 1 Min., Braten und Festtagsplatten +10 für 1 Min.
  - **Wasserschlauch** mit heißem Wasser (am Lagerfeuer füllen).
- **Warme Kleidung** (Kälteschutz-Punkte; Lederrüstung hat 23, Ziegenpelz 46 für das ganze Set):
  - Lederrüstung isoliert von Haus aus, Ziegenpelz-Kleidung (Ziegen scheren) schützt am stärksten.
  - **Fellrüstungen** (Armor Series – Norsemen, Schmiedetisch): Wolf-, Bär-, Eisbär- und Puma-Sets wärmen wie Ziegenpelz (46), Elch-, Hirsch- und Hörner-Sets halb so viel (23). Die Werte für Rüstung und Schaden entsprechen dem Material im Rezept.
  - **Kleidung aus Stoff** (Weavers Paradise): Baumwolle anbauen, Wolle und Seide zu Garn spinnen, zu Stoff weben und daraus Kleidung nähen. Getragen wird sie in eigenen Curios-Slots **zusätzlich** zur Rüstung, eine Lage unter dem Panzer. Wolle wärmt: Uschanka 5, Wollpullover 6, Wollhose 4, Bommelmütze/Weste/Poncho/Wollumhang je 3, Handwärmer/Lederhandschuhe/Strümpfe je 2. Baumwolle und Seide geben 1 Punkt. Anleitung im Patchouli-Buch, das man beim ersten Betreten bekommt.
  - **Felle** (Leather Working – Pelts & Hides): Tiere lassen statt Leder Felle und Häute fallen, die man auf dem **Gerbgestell** zu Leder gerbt (braucht Licht, dauert wie Pflanzenwachstum). Am **Nähtisch** (2 Wolle über 4 Bretter) lassen sie sich auch direkt in jede Rüstung einnähen: Eisbärfell 3, Wolf-, Fuchs- und Ziegenfell je 2, Schaffell 1,5, Wolle 1,5, Leder 1.
  - Achtung: Weil Kühe kein Leder mehr fallen lassen, liefern auch die Kuhhirten von MineColonies Häute statt Leder. Leder entsteht nur noch über das Gerbgestell.
- **So wirkt Kleidung** (`kubejs/server_scripts/frost_clothing.js`): In Cold Sweat verlangsamt Isolierung das Auskühlen nur (Rate × 0,1^(Punkte/40)), stoppt es aber nie, solange es draußen unter der Kältegrenze (50 °F) ist. Weil hier fast alles darunter liegt, senkt jeder Kälteschutz-Punkt zusätzlich die eigene Kältegrenze um 0,9 °F (über den Basiswert des Attributs `cold_sweat:freezing_point`). Außerdem ist die Abkühlrate halbiert (`Rate Multiplier` = 0,5). Richtwerte für die Ebene:

  | Ausrüstung | Kältegrenze | nachts (≈ −10 °F) | tagsüber (≈ 20 °F) |
  |---|---|---|---|
  | nichts | 50 °F | erfroren nach ~1 Min. | ~2 Min. |
  | Lederbrust + -hose (13) | 38 °F | ~2,5 Min. | ~6 Min. |
  | volle Lederrüstung (23) | 29 °F | ~5 Min. | ~20 Min. |
  | Fell/Ziegenpelz (46) | 9 °F | ~40 Min. | dauerhaft warm |
  | Fell + Wollkleidung (≈ 66) | −9 °F | dauerhaft warm | dauerhaft warm |

  Getestet am Chamäleon: Bei 0 °F kühlt es mit normaler Grenze in etwa einer Minute auf −100 (Erfrieren beginnt) aus, mit gesenkter Grenze bleibt es bei 0. Nicht getestet: das Auslesen des Kälteschutzes beim echten Spieler. Stellschrauben: `SHIFT_PER_POINT` im Skript, `RATE_MULTIPLIER` in `tools/coldsweat_frost.py`.
- **Schonfrist:** Nach dem ersten Spawn ist man 30 Minuten (den ersten halben Tag) immun gegen Temperatureffekte, um Unterschlupf und Feuer zu bauen.
- Kolonisten von MineColonies frieren nicht, nur Spieler.

## Weihnachten
- **Deko:** Macaw's Holidays (Weihnachtsbäume, Lichterketten, Girlanden, Socken, Geschenke), Snowy Spirit (Lebkuchenhäuser, Zuckerstangen, Kränze, Schlitten, Schneekugel, Lebkuchen-Golems), ManyIdeas Christmas (Baum, Kamine, Kranz, Stern), Christmas Culinary (Adventskalender, Lichter, Geschenke).
- **Essen für Farmer's Delight:** Christmas Delight (heiße Schokolade in vier Varianten, Eierpunsch, Schokolade, Eis), Festive Delight (Lebkuchenmänner, Festtagshähnchen, Weihnachtstee, Zimt), Christmas Culinary (Weihnachtsgerichte aus aller Welt).
- **Create Wrapped:** Geschenke mit Create automatisch verpacken. **Christmas Music Discs:** Weihnachtslieder.
- **ChristmasColonies:** Winterdorf-Baustil für MineColonies, dazu weitere Stile aus Stylecolonies.

## Getestet (Headless-Server, Java 21)
- 2 × 35.721 Chunks vorgeneriert (3 × 3 km, mit Chunky nur auf dem Testserver) ohne Absturz oder Hänger, ca. 80 Chunks/s.
- Server-Pack frisch aus der ZIP installiert und gestartet, mit Distant Horizons: neue Welt, LOD-Datenbanken für alle drei Dimensionen angelegt, Neustart ohne Fehler.
- 21 Mobs und Bosse aus allen Mob-Mods (u. a. Frost Bear, Frostmaw, Ferrous Wroughtnaut, Night Lich, Ender Golem, Iceologer, Mutant Zombie, Lebkuchen-Golem, Chamäleon) 60 s gleichzeitig nachts und im Schneesturm: keine Fehler.
- Wetterzyklus: Phasenwechsel, `/frost`-Befehle und Persistenz über einen Neustart funktionieren. Temperaturmessung über Cold Sweats KubeJS-Schnittstelle liefert die erwarteten Werte.
- Inventory Sorter 24.0.24: Server aus der neuen ZIP frisch installiert, startet ohne neue Fehler, `/invsorter` antwortet. Sortieren im Client ist nicht getestet.
- Coffee Delight 1.4.1, Create Cobblestone 1.5.0, Simple Voice Chat 2.6.22 und CoreProtectNeo 24.1: Testserver (Kopie der Server-Mods, frische Welt) startet ohne neue Fehler, Voice-Chat-Server lauscht auf Port 24454, CoreProtect meldet „Using MySQL for data storage“ und legt seine Tabellen in MariaDB an. Voice Chat mit echten Clients und CoreProtect-Rollbacks sind nicht getestet.
- Hearth-Rezept und Milch-Tag werden geladen (KubeJS: 1 Rezept hinzugefügt, keine Parse-Fehler).
- Schneeverwehungen: 16.129 Chunks (2 × 2 km, zweiter Seed) ohne neue Fehler generiert, Schichtverteilung wie oben.
- Im Log stehen nur bekannte, harmlose Meldungen: fehlende Advancements von Dungeons Arise, leere Items in Struktur-Truhen, „Cold Sweat requires primalwinter 6.0.2 or lower for compat“ (die eingebaute Kompatibilität wird durch die eigene Config ersetzt).
- **Client (CurseForge, RTX 4080, Complementary-Shader, Distant Horizons):** ca. 25 Minuten auf dem Testserver im Zuschauermodus, 18 Szenen aufgenommen (15 davon in `screenshots/`, die drei mit Himmelsbauwerken sind entfernt). Kein Absturz, Shader und DH laufen, im Log nur Asset-Warnungen (Snowy Spirit, GeckoLib). Beobachtungen:
  - Der Primal-Winter-Nebel ist bei Schneefall sehr dicht, die Sicht reicht nur gut 100 Blöcke. Distant Horizons bringt deshalb vor allem in den klaren Phasen des Wetterzyklus etwas. Stellschraube: `fogDensity` in `config/primalwinter-common.toml`.
  - Der Whiteout sieht mit Shadern kaum anders aus als normaler Schneefall. Spürbar sind nur die Wirkungen (Monster am Tag, Auskühlen) und die Titel-Warnung.
  - In der Luft schweben vereinzelt blassrosa Kreuz-Partikel, vermutlich Primal-Winter-Schneepartikel unter dem Shader.
- **Nicht getestet:** das Zusammenspiel mit echten Spielern (Unterkühlung im Freien, Hearth-Reichweite, Auskühlen im Sturm) und eine echte Kolonie mit Kolonisten. Die Abkühlrate im Sturm (`CHILL_PER_SECOND` in `frost_storms.js`) und die Temperaturformel (`SCALE`/`SHIFT` in `tools/coldsweat_frost.py`) sind die ersten Stellschrauben, falls es sich zu hart oder zu leicht anfühlt.

## Bewusst nicht enthalten
- **Biomes O' Plenty:** BOP-Biome werden über TerraBlender erst nach Primal Winter registriert und bleiben deshalb grün (im Test 50–90 % unbedeckte Fläche in Marsch, Wetland, Highland usw.).
- **Primal Winter (Original 6.0.2):** stürzt auf dedizierten Servern ab (Issue #115, Projekt archiviert). Stattdessen der gepflegte Fork *Colder Primal Winter* 6.1.1.
- **MineColonies Compatibility / Tweaks:** Ein Mixin von Tweaks 3.30 passt nicht zu MineColonies 1.1.1403 und bringt den Server beim Start zum Absturz.
- **Towns and Towers:** setzt Blöcke außerhalb der erlaubten Chunks (abgeschnittene Bauten), bekannter Server-Deadlock.
- **Particle Rain:** ersetzt dasselbe Wetter-Rendering, das Primal Winter verändert. Das Konfliktrisiko lässt sich ohne Client-Test nicht ausschließen.
- **MSS (Moog's Soaring Structures), Sky Whale Ship** und die schwebenden Bauwerke anderer Mods: Himmelsbauwerke passen nicht zur Frostpunk-Stimmung.
- **Aeronautics, Sky Archipelago, Forgiving Void, Clear Void** und die Void-Bauregel aus Skyward Frontiers: gehören zur Himmelswelt.

## Anti-Cheat (Server)
- **AntiXray 1.4.8** läuft nur im Server-Pack (`config/antixray.toml`, Engine-Modus 3): Verdeckte Erze werden für Clients durch zufällige Fake-Erze ersetzt.
- **Distant Horizons** lässt verdeckte Erze in seinen LODs weg (`worldCompression = "VISUALLY_EQUAL"`). Nicht auf `MERGE_SAME_BLOCKS` ändern.
- `allow-flight=true` ist gesetzt, weil Paraglider sonst Kicks auslösen.
- **Empfehlung:** `white-list=true` und den Seed geheim halten.

## Server: Voice Chat und CoreProtect
- **Simple Voice Chat** (Client und Server): Sprechen in der Nähe, Hörweite 48 Blöcke, Flüstern 24. Im Spiel öffnet `V` das Menü (Mikrofon, Gruppen), Sprechen per Push-to-Talk (Standard: Feststelltaste). Der Server spricht auf **UDP 24454** (Standard in `config/voicechat/voicechat-server.properties`, `voice_host` leer: Clients nutzen dieselbe Adresse wie für das Spiel). Im Docker-Compose ist `24454:24454/udp` veröffentlicht. Wer ohne VPN von außen verbindet, braucht zusätzlich eine UDP-Portfreigabe 24454 im Router.
- **CoreProtectNeo 24.1** (nur Server, `pack=server`): protokolliert Blockänderungen, Truhenzugriffe usw. und erlaubt Rollbacks. Befehle für OPs: `/co i` (Inspektor: Block anklicken), `/co lookup u:<Spieler> t:1d`, `/co rollback u:<Spieler> t:1h r:20`, `/co restore …`.
  - Daten in **MariaDB 11.4** (eigener Dienst `db` im Compose, nur im internen Docker-Netz, Daten in `./db`). Gründe: Create und MineColonies erzeugen sehr viele Block-Ereignisse, und eine SQLite-Datei in `data/` würde jedes Backup aufblähen und während des Schreibens inkonsistent gesichert.
  - Die Config `data/coreprotect/config.yml` liegt **nur auf dem Server** und nicht im Repo, weil sie das DB-Passwort enthält: `use-mysql: true`, `mysql-host: db`, `mysql-database`/`mysql-username: coreprotect`, `mysql-password` = `DB_PASSWORD` aus der `.env`, `table-prefix: co_`, `language: de`. Ohne diese Datei legt CoreProtect beim ersten Start eine SQLite-Config an.
  - Die Datenbank ist **nicht** in den tar-Backups des Backup-Containers enthalten.

## Wichtig: kein Chunky
**Chunky nicht ins Pack aufnehmen.** Distant Horizons 3.3.3 und Chunky 1.4.23 stürzen zusammen beim Erstellen einer neuen Welt ab (siehe Skyward Frontiers). Zum Vorgenerieren auf dem Server Chunky nur vorübergehend und ohne DH verwenden.

## Mods und Shader (86 gelistet: 84 im Client, AntiXray und CoreProtectNeo nur auf dem Server, dazu 24 Bibliotheken)

**Kern: MineColonies**

- [MineColonies](https://www.curseforge.com/minecraft/mc-mods/minecolonies) – `minecolonies-1.1.1403-1.21.1.jar`
- [Stylecolonies](https://www.curseforge.com/minecraft/mc-mods/stylecolonies) – `stylecolonies-1.15.64-1.21.1.jar`
- [ChristmasColonies](https://www.curseforge.com/minecraft/mc-mods/christmascolonies) – `christmascolonies-1.21.1-1.9.jar`
- [CreateColonies](https://www.curseforge.com/minecraft/mc-mods/createcolonies) – `createcolonies-2.0.7.jar`
- [JadeColonies](https://www.curseforge.com/minecraft/mc-mods/jadecolonies) – `jadecolonies-1.21.1-1.6.1.jar`
- [Domum Ornamentum](https://www.curseforge.com/minecraft/mc-mods/domum-ornamentum) – `domum-ornamentum-1.0.236-snapshot-main.jar`

**Kern: Create + Farmer's Delight**

- [Create](https://www.curseforge.com/minecraft/mc-mods/create) – `create-1.21.1-6.0.10.jar`
- [Farmer's Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-delight) – `FarmersDelight-1.21.1-1.3.4.jar`
- [Coffee Delight](https://www.curseforge.com/minecraft/mc-mods/coffee-delight) – `coffee_delight-1.4.1.jar`
- [Create Cobblestone](https://www.curseforge.com/minecraft/mc-mods/create-cobblestone) – `createcobblestone-1.5.0+neoforge-1.21.1-153.jar`

**Ewiger Winter**

- [Colder Primal Winter](https://www.curseforge.com/minecraft/mc-mods/colder-primal-winter) – `primalwinter-1.21.1-6.1.1.jar`
- [Cold Sweat](https://www.curseforge.com/minecraft/mc-mods/cold-sweat) – `ColdSweat-2.4.4.jar`
- [Create: Cold Sweat](https://www.curseforge.com/minecraft/mc-mods/create-cold-sweat) – `create_cold_sweat-1.1.2.jar`

**Weihnachten**

- [Snowy Spirit](https://www.curseforge.com/minecraft/mc-mods/snowy-spirit) – `snowyspirit-1.21.1-3.1.16-neoforge.jar`
- [Macaw's Holidays](https://www.curseforge.com/minecraft/mc-mods/macaws-holidays) – `mcw-holidays-1.1.2-mc1.21.1neoforge.jar`
- [ManyIdeas Christmas](https://www.curseforge.com/minecraft/mc-mods/manyideas-christmas) – `ManyIdeasChristmas-1.21.1-2.0.1.jar`
- [Christmas Delight (Farmers Delight Addon)](https://www.curseforge.com/minecraft/mc-mods/christmas-delight) – `farmers_delight_christmas_edition-V1.0.2-neoforge-1.20.1.jar`
- [Festive Delight - Christmas eve](https://www.curseforge.com/minecraft/mc-mods/festive-delight) – `festive_delight-2.0.0-neoforge-1.21.1.jar`
- [Christmas Culinary & Decorations ](https://www.curseforge.com/minecraft/mc-mods/christmas-culinary) – `christmas_culinary_and_decorations-1.6.0-neoforge-1.21.1.jar`
- [Create: Wrapped](https://www.curseforge.com/minecraft/mc-mods/create-wrapped) – `create_wrapped-1.0.3-neoforge-1.21.1.jar`
- [Christmas Music Discs](https://www.curseforge.com/minecraft/mc-mods/christmas-music-discs) – `christmasmusicdiscs-1.21.1-v1.3.jar`

**Welt / Biome**

- [Tectonic](https://www.curseforge.com/minecraft/mc-mods/tectonic) – `tectonic-3.0.28-neoforge-21.1.jar`
- [Lithostitched](https://www.curseforge.com/minecraft/mc-mods/lithostitched) – `lithostitched-1.8.1-neoforge-21.1.jar`
- [Yeetus Experimentus](https://www.curseforge.com/minecraft/mc-mods/yeetusexperimentus) – `yeetusexperimentus-neoforge-87.0.0.jar`

**Strukturen / Entdecken**

- [MVS - Moog's Voyager Structures](https://www.curseforge.com/minecraft/mc-mods/moogs-voyager-structures) – `MoogsVoyagerStructures-universal-1.21-5.1.3.jar`
- [MTR - Moog's Temples Reimagined](https://www.curseforge.com/minecraft/mc-mods/mtr-moogs-temples-reimagined) – `MoogsTemplesReimagined-universal-1.21-2.0.4.jar`
- [MMR - Moog's Mineshafts Reimagined](https://www.curseforge.com/minecraft/mc-mods/mmr-moogs-mineshafts-reimagined) – `MoogsMineshaftsReimagined-universal-1.21-1.1.0.jar`
- [MNS - Moog's Nether Structures](https://www.curseforge.com/minecraft/mc-mods/mns-moogs-nether-structures) – `MoogsNetherStructures-universal-1.21-3.1.1.jar`
- [MES - Moog's End Structures](https://www.curseforge.com/minecraft/mc-mods/moogs-end-structures) – `MoogsEndStructures-universal-1.21-2.1.1.jar`
- [When Dungeons Arise - Forge!](https://www.curseforge.com/minecraft/mc-mods/when-dungeons-arise) – `DungeonsArise-1.21.1-2.1.68-release.jar`
- [Explorify – Dungeons & Structures](https://www.curseforge.com/minecraft/mc-mods/explorify) – `Explorify v1.6.5.mod.jar`
- [Create: Structures Arise](https://www.curseforge.com/minecraft/mc-mods/create-structures-arise) – `Create-Structures-Arise-1.21.1-NeoForge-176.49.49.jar`
- [Dungeons and Taverns](https://www.curseforge.com/minecraft/mc-mods/dungeon-and-taverns) – `dungeons-and-taverns-v4.4.4 [NeoForge].jar`
- [YUNG's Better Dungeons (NeoForge) [1.20.4 - 1.21.1 ONLY]](https://www.curseforge.com/minecraft/mc-mods/yungs-better-dungeons-neoforge) – `YungsBetterDungeons-1.21.1-NeoForge-5.1.4.jar`
- [Repurposed Structures (Neoforge/Forge)](https://www.curseforge.com/minecraft/mc-mods/repurposed-structures) – `repurposed_structures-7.5.22+1.21.1-neoforge.jar`
- [Epic Structures: Igloo](https://www.curseforge.com/minecraft/mc-mods/epic-structures-igloo) – `epic-structures-igloo-1.0.5 1.21+ 26+.jar`
- [Lootr (Forge & NeoForge)](https://www.curseforge.com/minecraft/mc-mods/lootr) – `lootr-neoforge-1.21.1-1.11.38.127.jar`
- [KubeJS](https://www.curseforge.com/minecraft/mc-mods/kubejs) – `kubejs-neoforge-2101.7.2-build.377.jar`
- [Rhino](https://www.curseforge.com/minecraft/mc-mods/rhino) – `rhino-2101.2.8-build.91.jar`
- [Nature's Compass](https://www.curseforge.com/minecraft/mc-mods/natures-compass) – `NaturesCompass-1.21.1-3.4.0-neoforge.jar`
- [Explorer's Compass](https://www.curseforge.com/minecraft/mc-mods/explorers-compass) – `ExplorersCompass-1.21.1-3.4.0-neoforge.jar`
- [Waystones](https://www.curseforge.com/minecraft/mc-mods/waystones) – `waystones-neoforge-1.21.1-21.1.46.jar`

**Mobs & Bosse**

- [Mowzie's Mobs](https://www.curseforge.com/minecraft/mc-mods/mowzies-mobs) – `mowziesmobs-1.21.1-1.8.2.jar`
- [L_Ender 's Cataclysm](https://www.curseforge.com/minecraft/mc-mods/lendercataclysm) – `L_Ender's Cataclysm 1.21.1-3.33.jar`
- [Bosses of Mass Destruction [Forge | NeoForge]](https://www.curseforge.com/minecraft/mc-mods/bosses-of-mass-destruction-forge) – `BOMD-NeoForge-1.21-1.3.3.jar`
- [Friends&Foes (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/friends-and-foes-forge) – `friendsandfoes-neoforge-5.0.2+mc1.21.1.jar`
- [Naturalist](https://www.curseforge.com/minecraft/mc-mods/naturalist) – `naturalist-2.0.5-neoforge-1.21.1.jar`
- [Critters and Companions](https://www.curseforge.com/minecraft/mc-mods/critters-and-companions) – `crittersandcompanions-neoforge-1.21.1-2.7.0.jar`
- [Creeper Overhaul](https://www.curseforge.com/minecraft/mc-mods/creeper-overhaul) – `CreeperOverhaul-neoforge-1.21.1-4.0.6.jar`
- [Enderman Overhaul](https://www.curseforge.com/minecraft/mc-mods/enderman-overhaul) – `endermanoverhaul-neoforge-1.21.1-2.0.3.jar`
- [Mutant Monsters](https://www.curseforge.com/minecraft/mc-mods/mutant-monsters) – `MutantMonsters-v21.1.1-1.21.1-NeoForge.jar`
- [Born in Chaos](https://www.curseforge.com/minecraft/mc-mods/born-in-chaos) – `born_in_chaos_[Neoforge]_1.21.1_1.7.6.jar`
- [Ribbits](https://www.curseforge.com/minecraft/mc-mods/ribbits) – `Ribbits-1.21.1-NeoForge-4.1.6.jar`
- [Frost Bear](https://www.curseforge.com/minecraft/mc-mods/frost-bear) – `frostbear-neoforge-1.21.1-1.1.0.jar`

**Überleben**

- [Paragliders](https://www.curseforge.com/minecraft/mc-mods/paragliders) – `Paraglider-neoforge-21.1.5.jar`
- [GraveStone Mod](https://www.curseforge.com/minecraft/mc-mods/gravestone-mod) – `gravestone-neoforge-1.21.1-1.0.40.jar`
- [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks) – `sophisticatedbackpacks-1.21.1-3.26.12.2208.jar`
- [Better Days](https://www.curseforge.com/minecraft/mc-mods/betterdays) – `betterdays-1.21.1-3.3.6.3-NEOFORGE.jar`

**Kleidung: Felle & Stoffe (Kälteschutz über Cold Sweat, siehe coldsweat_frost.py)**

- [Weavers Paradise](https://www.curseforge.com/minecraft/mc-mods/weavers-paradise) – `weaversparadise-1.6.3.1.jar`
- [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli) – `Patchouli-1.21.1-93-NEOFORGE.jar`
- [Leather Working - Pelts & Hides](https://www.curseforge.com/minecraft/mc-mods/leather-working-pelts-hides) – `leatherworking-1.0.1.jar`
- [Armor Series - Norsemen](https://www.curseforge.com/minecraft/mc-mods/armor-series-norsemen) – `armor_series__norsemen-1.0.0-neoforge-1.21.1.jar`

**Multiplayer**

- [Simple Voice Chat](https://www.curseforge.com/minecraft/mc-mods/simple-voice-chat) – `voicechat-neoforge-1.21.1-2.6.22.jar`
- [CoreProtectNeo](https://www.curseforge.com/minecraft/mc-mods/coreprotectneo) – `coreprotect-24.1-neoforge-all.jar` *(nur Server)*

**QoL**

- [Just Enough Items (JEI)](https://www.curseforge.com/minecraft/mc-mods/jei) – `jei-1.21.1-neoforge-19.51.0.418.jar`
- [Jade 🔍](https://www.curseforge.com/minecraft/mc-mods/jade) – `Jade-1.21.1-NeoForge-15.10.6.jar`
- [AppleSkin](https://www.curseforge.com/minecraft/mc-mods/appleskin) – `appleskin-neoforge-mc1.21-3.0.9.jar`
- [Xaero's Minimap](https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap) – `xaerominimap-neoforge-1.21.1-26.6.0.jar`
- [Xaero's World Map](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map) – `xaeroworldmap-neoforge-1.21.1-1.47.0.jar`
- [Mouse Tweaks](https://www.curseforge.com/minecraft/mc-mods/mouse-tweaks) – `MouseTweaks-neoforge-mc1.21-2.26.1.jar`
- [Controlling](https://www.curseforge.com/minecraft/mc-mods/controlling) – `Controlling-neoforge-1.21.1-19.0.5.jar`
- [Inventory Sorter](https://www.curseforge.com/minecraft/mc-mods/inventory-sorter) – `inventorysorter-1.21.1-24.0.24.jar`

**Performance / Stabilität**

- [Sodium](https://www.curseforge.com/minecraft/mc-mods/sodium) – `sodium-neoforge-0.8.13+mc1.21.1.jar`
- [Iris Shaders](https://www.curseforge.com/minecraft/mc-mods/irisshaders) – `iris-neoforge-1.8.14-beta.1+mc1.21.1.jar`
- [Complementary Shaders - Reimagined](https://www.curseforge.com/minecraft/shaders/complementary-reimagined) – `ComplementaryReimagined_r5.9.3.zip`
- [Distant Horizons: A Level of Detail mod](https://www.curseforge.com/minecraft/mc-mods/distant-horizons) – `DistantHorizons-3.3.3-1.21.1-fabric-neoforge.jar`
- [ImmediatelyFast](https://www.curseforge.com/minecraft/mc-mods/immediatelyfast) – `ImmediatelyFast-NeoForge-1.6.15+1.21.1.jar`
- [Entity Culling Fabric/Forge](https://www.curseforge.com/minecraft/mc-mods/entityculling) – `entityculling-neoforge-1.11.3-mc1.21.1.jar`
- [FerriteCore ((Neo)Forge)](https://www.curseforge.com/minecraft/mc-mods/ferritecore) – `ferritecore-7.0.3-neoforge.jar`
- [ModernFix](https://www.curseforge.com/minecraft/mc-mods/modernfix) – `modernfix-neoforge-5.27.26+mc1.21.1.jar`
- [Lithium (Fabric/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/lithium) – `lithium-neoforge-0.15.4+mc1.21.1.jar`
- [AllTheLeaks (Memory Leak Fix)](https://www.curseforge.com/minecraft/mc-mods/alltheleaks) – `alltheleaks-1.1.13+1.21.1-neoforge.jar`
- [Clumps](https://www.curseforge.com/minecraft/mc-mods/clumps) – `Clumps-neoforge-1.21.1-19.0.0.1.jar`
- [spark](https://www.curseforge.com/minecraft/mc-mods/spark) – `spark-1.10.124-neoforge.jar`
- [Crash Assistant](https://www.curseforge.com/minecraft/mc-mods/crash-assistant) – `CrashAssistant-neoforge-1.20.6-1.21.4-1.11.12.jar`
- [AntiXray ((Neo)Forge)](https://www.curseforge.com/minecraft/mc-mods/antixray-forge) – `antixray-neoforge-1.4.8+1.21.1.jar` *(nur Server-Pack)*

**Automatisch mitinstallierte Bibliotheken**

Architectury API, Balm, Better Advanced Tooltips, BlockUI, CERBON's API [Forge | Fabric | NeoForge], Cloth Config API (Fabric/Forge/NeoForge), Curios API, GeckoLib, Lionfish API, ManyIdeas Core, Moog's Structure Lib (moogs_structures), Moonlight Lib, Multi-Piston, Puzzles Lib, Recipes Library, Resourceful Config, Resourceful Lib, Searchables, Sophisticated Core, Structurize, TownTalk, VortyLib, YUNG's API (NeoForge) [1.20.4-1.21.1 ONLY], YetAnotherConfigLib
