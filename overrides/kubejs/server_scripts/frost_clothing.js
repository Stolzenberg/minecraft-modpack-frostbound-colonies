// Frostbound Colonies – warme Kleidung senkt die Kältegrenze
//
// In Cold Sweat verlangsamt Isolierung das Auskühlen nur (Rate × 0,1^(Isolierung/40)), stoppen kann sie es nie:
// Solange die Umgebung unter der Wohlfühlgrenze (50 °F) liegt, sinkt die Körpertemperatur weiter. In dieser Welt
// liegt aber fast alles darunter, Kleidung wäre also nur Zeitgewinn. Deshalb senkt hier jeder Kälteschutz-Punkt
// (Rüstung, eingenähte Felle/Wolle, Curios-Kleidung) zusätzlich die Kältegrenze des Spielers um SHIFT_PER_POINT.
// Umgesetzt über den Basiswert des Attributs cold_sweat:freezing_point (Cold Sweat nutzt ihn statt des
// Config-Werts, Attribut-Modifier würden ohne weitere TempModifier ignoriert).

;(function () {
  const BASE_FREEZING_POINT = 0.4 // = "Minimum Habitable Temperature" aus coldsweat/main.toml (MC-Einheiten, 50 °F)
  const SHIFT_PER_POINT = 0.02 // MC-Einheiten pro Kälteschutz-Punkt (= 0,9 °F)
  const MAX_SHIFT = 2.0 // höchstens 90 °F tiefer
  const KEY = 'frostbound_freezing_point'

  function update(server, p) {
    let ins = Math.max(0, coldsweat.getColdInsulation(p))
    let value = BASE_FREEZING_POINT - Math.min(MAX_SHIFT, ins * SHIFT_PER_POINT)
    value = Math.round(value * 1000) / 1000
    let data = p.persistentData
    if (data.contains(KEY) && Math.abs(data.getDouble(KEY) - value) < 0.0005) return
    data.putDouble(KEY, value)
    server.runCommandSilent(`attribute ${p.getGameProfile().getName()} cold_sweat:freezing_point base set ${value}`)
  }

  ServerEvents.tick(event => {
    let server = event.server
    if (server.tickCount % 20 != 5 || typeof coldsweat == 'undefined') return
    server.players.forEach(p => {
      try {
        update(server, p)
      } catch (e) {
        console.error('[frost_clothing] ' + e)
      }
    })
  })

  // Nach dem Einloggen neu setzen (Attribut-Basiswerte werden zwar gespeichert, die Config kann sich aber ändern)
  PlayerEvents.loggedIn(event => {
    event.player.persistentData.remove(KEY)
  })
})()
