// Frostbound Colonies – Wetter: Dauerschnee, angekündigte Whiteout-Stürme und klare Tage
//
// Primal Winter stellt neue Welten auf ein permanentes Gewitter. Das hieße: Monster spawnen auch
// tagsüber und Untote verbrennen nie. Dieses Skript übernimmt stattdessen das Overworld-Wetter:
//   - "snow": Schneefall (Regen ohne Gewitter), tagsüber normal hell. Dauer SNOW_LENGTH.
//   - Danach mit CLEAR_CHANCE ein klarer Himmel ("clear", CLEAR_LENGTH): kein Nebel, Fernsicht.
//   - Sonst ein Whiteout: nach einer Vorwarnung ("warning", WARNING) folgt ein Gewitter/Schneesturm
//     ("storm", STORM_LENGTH). Währenddessen spawnen Monster auch am Tag, und wer im Freien steht
//     (Himmel sichtbar), kühlt zusätzlich aus. Kälte-Isolierung (Pelz- und Wollkleidung) dämpft das.
//   - "manual": Wurde das Wetter von außen gesetzt (/weather mit Dauer bis MANUAL_MAX), gilt es für diese
//     Dauer, danach geht es mit Schneefall weiter. Längere Dauern (Primal Winters Dauergewitter bei neuen
//     Welten) und Dauer 0 (Zurücksetzen beim Schlafen) werden überschrieben.
// Der Zustand liegt in server.persistentData und übersteht Neustarts.
// Admin-Befehle: /frost status | /frost storm | /frost sky | /frost snow (Alias: /frost clear)

;(function () {
  const DATA_KEY = 'frostbound_weather'
  const DAY = 24000
  const SNOW_LENGTH = [36000, 72000] // 30–60 Minuten Schneefall zwischen zwei Ereignissen (Echtzeit; mit Better Days ½–1 Tag)
  const CLEAR_CHANCE = 0.4 // sonst Whiteout
  const CLEAR_LENGTH = [6000, 14400] // 5–12 Minuten klarer Himmel (mit Better Days 2–5 Spielstunden)
  const STORM_LENGTH = [6000, 12000] // 5–10 Minuten
  const WARNING = 1200 // 1 Minute Vorwarnung
  const MANUAL_MAX = 1000000 // /weather-Dauern darüber gelten nicht als Absicht (Primal Winter setzt ~2^31)
  const CHILL_PER_SECOND = 0.6 // Körpertemperatur-Abzug pro Sekunde ohne Isolierung (Cold-Sweat-Einheiten)
  const INSULATION_FULL = 50 // ab dieser Kälte-Isolierung bleibt nur MIN_CHILL_FACTOR übrig
  const MIN_CHILL_FACTOR = 0.15
  // erwartetes Wetter je Phase: [Regen/Schnee, Gewitter]
  const EXPECTED = { snow: [true, false], warning: [true, false], storm: [true, true], clear: [false, false] }

  function rand(range) {
    return range[0] + Math.floor(Math.random() * (range[1] - range[0]))
  }

  function state(server) {
    let data = server.persistentData
    if (!data.contains(DATA_KEY)) {
      let s = data.getCompound(DATA_KEY) // leerer Tag
      s.putString('phase', 'snow')
      s.putLong('next', server.overworld().getTime() + rand(SNOW_LENGTH))
      data.put(DATA_KEY, s)
    }
    return data.getCompound(DATA_KEY)
  }

  function setPhase(server, s, phase, until) {
    s.putString('phase', phase)
    s.putLong('next', until)
    server.persistentData.put(DATA_KEY, s)
  }

  function announce(server, title, subtitle, color) {
    server.runCommandSilent(`title @a times 10 80 20`)
    server.runCommandSilent(`title @a subtitle {"text":"${subtitle}","color":"gray"}`)
    server.runCommandSilent(`title @a title {"text":"${title}","color":"${color}"}`)
  }

  function weatherNow(level) {
    let d = level.getLevelData()
    return [d.isRaining(), d.isThundering()]
  }

  function applyWeather(level, phase) {
    let want = EXPECTED[phase]
    let now = weatherNow(level)
    if (now[0] == want[0] && now[1] == want[1]) return
    if (want[0]) level.setWeatherParameters(0, 2 * DAY, true, want[1])
    else level.setWeatherParameters(2 * DAY, 0, false, false)
  }

  // Wetter von außen geändert? Dann die gesetzte Dauer zurückgeben, sonst 0.
  function manualDuration(level, phase) {
    let want = EXPECTED[phase]
    let now = weatherNow(level)
    if (!want || (now[0] == want[0] && now[1] == want[1])) return 0
    let d = level.getLevelData()
    let dur = !now[0] ? d.getClearWeatherTime() : now[1] ? d.getThunderTime() : d.getRainTime()
    return dur > 0 && dur < MANUAL_MAX ? dur : 0
  }

  function chill(level) {
    if (typeof coldsweat == 'undefined') return
    level.players.forEach(p => {
      if (p.isCreative() || p.isSpectator() || !level.canSeeSky(p.blockPosition().above())) return
      let ins = coldsweat.getColdInsulation(p)
      let factor = Math.max(MIN_CHILL_FACTOR, 1 - ins / INSULATION_FULL)
      let core = coldsweat.getTemperature(p, 'core')
      coldsweat.setTemperature(p, 'core', core - CHILL_PER_SECOND * factor)
    })
  }

  function toSnow(server, s, now, msg) {
    setPhase(server, s, 'snow', now + rand(SNOW_LENGTH))
    if (msg) announce(server, msg[0], msg[1], 'white')
  }

  function toClear(server, s, now) {
    setPhase(server, s, 'clear', now + rand(CLEAR_LENGTH))
    announce(server, 'Der Himmel klart auf', 'Klare Sicht bis zum Horizont – nutzt die Zeit', 'yellow')
  }

  function toWarning(server, s, now) {
    setPhase(server, s, 'warning', now + WARNING)
    announce(server, 'Ein Whiteout zieht auf', 'Sucht Schutz und Wärme – in einer Minute bricht der Sturm los', 'aqua')
  }

  function tick(server) {
    let level = server.overworld()
    let s = state(server)
    let phase = s.getString('phase')
    let now = level.getTime()

    if (phase == 'manual') {
      if (now < s.getLong('next')) return // von Hand gesetztes Wetter laufen lassen
      toSnow(server, s, now, null)
    } else {
      let dur = manualDuration(level, phase)
      if (dur > 0) {
        console.info(`[frost_storms] Wetter von außen gesetzt, Zyklus pausiert für ${dur} Ticks`)
        setPhase(server, s, 'manual', now + dur)
        return
      }
    }

    if (now >= s.getLong('next')) {
      phase = s.getString('phase')
      if (phase == 'snow') {
        if (Math.random() < CLEAR_CHANCE) toClear(server, s, now)
        else toWarning(server, s, now)
      } else if (phase == 'warning') {
        setPhase(server, s, 'storm', now + rand(STORM_LENGTH))
        announce(server, 'Whiteout!', 'Im Freien droht der Kältetod', 'red')
      } else if (phase == 'storm') {
        toSnow(server, s, now, ['Der Sturm lässt nach', 'Es schneit wieder ruhig'])
      } else {
        toSnow(server, s, now, ['Es beginnt wieder zu schneien', 'Die Wolken ziehen zu'])
      }
    }
    phase = s.getString('phase')
    applyWeather(level, phase)
    if (phase == 'storm') chill(level)
  }

  ServerEvents.loaded(event => {
    event.server.runCommandSilent('gamerule doWeatherCycle false')
  })

  ServerEvents.tick(event => {
    let server = event.server
    if (server.tickCount % 20 != 0) return
    try {
      tick(server)
    } catch (e) {
      console.error('[frost_storms] ' + e)
    }
  })

  ServerEvents.commandRegistry(event => {
    const { commands: Commands } = event
    const info = ctx => {
      let server = ctx.source.server
      let s = state(server)
      let lv = server.overworld()
      let left = Math.max(0, s.getLong('next') - lv.getTime())
      let w = weatherNow(lv)
      let msg = `Phase: ${s.getString('phase')}, Wechsel in ${Math.round(left / 20)} s (Schnee: ${w[0]}, Sturm: ${w[1]})`
      if (typeof coldsweat != 'undefined') {
        let pos = BlockPos.containing(ctx.source.position)
        let level = ctx.source.level
        let temp = coldsweat.convertTemperature(coldsweat.getTemperatureAt(level, pos), 'mc', 'f', true)
        let biome = level.getBiome(pos).unwrapKey().get().location()
        msg += `; ${biome} bei ${pos.toShortString()}: ${temp.toFixed(1)} °F`
      }
      ctx.source.sendSystemMessage(Text.of(msg))
      return 1
    }
    const force = target => ctx => {
      let server = ctx.source.server
      try {
        let s = state(server)
        let now = server.overworld().getTime()
        if (target == 'storm') setPhase(server, s, 'warning', now) // Vorwarnung überspringen
        else if (target == 'sky') toClear(server, s, now)
        else toSnow(server, s, now, ['Es schneit wieder ruhig', ''])
        applyWeather(server.overworld(), s.getString('phase')) // sonst hielte tick() altes /weather-Wetter für Absicht
        tick(server)
      } catch (e) {
        console.error('[frost_storms] /frost ' + target + ': ' + e)
      }
      return info(ctx)
    }
    event.register(Commands.literal('frost')
      .requires(src => src.hasPermission(2))
      .then(Commands.literal('status').executes(info))
      .then(Commands.literal('storm').executes(force('storm')))
      .then(Commands.literal('sky').executes(force('sky')))
      .then(Commands.literal('snow').executes(force('snow')))
      .then(Commands.literal('clear').executes(force('snow'))))
  })
})()
