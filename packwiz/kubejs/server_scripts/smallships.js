// Small Ships ohne Kanonen: Schiffe gibt es weiter in der Werft, Kanonen und Munition sind nicht herstellbar.
//
// Kanonen sind bei Small Ships eigene Items, die man an Schiffen anbringt oder an Land aufstellt; die Werft baut
// Schiffe ohne Kanonen. Einen Config-Schalter dafür gibt es nicht, deshalb fallen hier die Rezepte weg.
// Ausgeblendet in JEI per Tag (kubejs/data/c/tags/item/hidden_from_recipe_viewers.json). Falls doch eine Kanone
// ins Spiel kommt (Creative, /give): Schaden und Zerstörung stehen in config/smallships-server.toml auf 0.

;(function () {
  const CANNON_ITEMS = [
    'smallships:cannon',
    'smallships:cannon_ball',
    'smallships:chained_shot',
    'smallships:grape_shot',
    'smallships:fine_grain_powder', // Treibladung, wird nur beim Nachladen der Kanone verbraucht
  ]

  ServerEvents.recipes((event) => {
    CANNON_ITEMS.forEach((item) => event.remove({ output: item }))
  })
})()
