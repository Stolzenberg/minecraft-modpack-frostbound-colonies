// Nur Testserver (nach kubejs/server_scripts/ kopieren): /biomemap <radius> <step> schreibt eine Biomkarte ins Log, ohne Chunks zu generieren. Auswerten mit tools/test/biomemap.py
ServerEvents.commandRegistry(event => {
  const { commands: Commands, arguments: Arguments } = event
  event.register(Commands.literal('biomemap')
    .then(Commands.argument('radius', Arguments.INTEGER.create(event))
      .then(Commands.argument('step', Arguments.INTEGER.create(event))
        .executes(ctx => {
          let radius = Arguments.INTEGER.getResult(ctx, 'radius')
          let step = Arguments.INTEGER.getResult(ctx, 'step')
          let level = ctx.source.server.overworld()
          let source = level.getChunkSource().getGenerator().getBiomeSource()
          let sampler = level.getChunkSource().randomState().sampler()
          let ids = {}
          let next = 0
          for (let z = -radius; z <= radius; z += step) {
            let row = []
            for (let x = -radius; x <= radius; x += step) {
              let holder = source.getNoiseBiome(x >> 2, 16, z >> 2, sampler)
              let id = String(holder.unwrapKey().get().location())
              if (!(id in ids)) ids[id] = next++
              row.push(ids[id])
            }
            console.info('BMAP ' + z + ' ' + row.join(','))
          }
          for (let id in ids) console.info('BLEG ' + ids[id] + ' ' + id)
          console.info('BEND')
          return 1
        }))))
})
