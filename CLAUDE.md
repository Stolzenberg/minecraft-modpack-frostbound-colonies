# Proud Mountain SMP – Hinweise für Claude

## Git
- **Direkt auf `main` committen und pushen.** Pull Requests sind auf GitHub deaktiviert (seit 2026-10-23), keine Feature-Branches oder PRs anlegen.
- Vor Änderungen `git fetch` und `main` per Fast-Forward auf `origin/main` bringen. Vor dem Push erneut prüfen, ob `origin/main` weitergelaufen ist, und ggf. rebasen (kein Force-Push).
- Ein Push auf `main` wird beim nächsten Start an alle Prism-Instanzen und den Docker-Server ausgeliefert (packwiz). Änderungen, die die Serverwelt betreffen (Weltgenerierung, entfernte Mods), vorher mit Tim absprechen.
- Repo: `Stolzenberg/minecraft-modpack-proud-mountain-smp` (früher `minecraft-modpack-frostbound-colonies`, die alte Adresse leitet weiter).
