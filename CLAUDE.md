# Proud Mountain SMP – Hinweise für Claude

## Git
- **Direkt auf `main` committen und pushen.** Pull Requests sind auf GitHub deaktiviert (seit 2026-10-23), keine Feature-Branches oder PRs anlegen.
- Vor Änderungen `git fetch` und `main` per Fast-Forward auf `origin/main` bringen. Vor dem Push erneut prüfen, ob `origin/main` weitergelaufen ist, und ggf. rebasen (kein Force-Push).
- Ein Push auf `main` wird beim nächsten Start an alle Prism-Instanzen und den Docker-Server ausgeliefert (packwiz). Änderungen, die die Serverwelt betreffen (Weltgenerierung, entfernte Mods), vorher mit Tim absprechen.
- Repo: `Stolzenberg/minecraft-modpack-proud-mountain-smp` (früher `minecraft-modpack-frostbound-colonies`, die alte Adresse leitet weiter).

## Server
- Der MC-Server läuft per `ssh fedora-server` als Docker-Compose-Setup in `/home/tim/docker/minecraft` (`compose.yaml`, Serverdaten in `data/`).
- Container: `minecraft-mc-1` (itzg/minecraft-server, zieht das Pack beim Start per `PACKWIZ_URL` von `main`), dazu `minecraft-db-1` (MariaDB) und `minecraft-backups-1`.
- Befehle: `docker exec minecraft-mc-1 rcon-cli <befehl>`, Neustart (lädt Pack-Updates): `docker restart minecraft-mc-1`, vorher mit `rcon-cli list` prüfen, ob jemand online ist.
