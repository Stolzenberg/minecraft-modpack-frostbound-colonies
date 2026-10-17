// Frostbound Colonies – Rezepte
//
// Der Hearth (Cold Sweat) ist der "Generator" der Kolonie: Er heizt ein ganzes Gebäude und lässt sich über
// Rohre (Smokestacks) verteilen. Im Original braucht er Netherziegel und Seelensand. Hier gibt es zusätzlich
// ein Overworld-Rezept, damit die erste Wärmequelle nicht an einem Nether-Ausflug hängt.

// Christmas Delight nutzt den Tag c:foods/milk (Schokoriegel, Vanilleeis, Frischkäse-Rolle), den sonst keine Mod füllt
ServerEvents.tags('item', event => {
  event.add('c:foods/milk', ['minecraft:milk_bucket', 'farmersdelight:milk_bottle'])
})

ServerEvents.recipes(event => {
  event.shaped('cold_sweat:hearth', [
    ' S ',
    'BBB',
    'IFI'
  ], {
    S: 'cold_sweat:smokestack',
    B: 'minecraft:bricks',
    I: '#c:ingots/iron',
    F: 'minecraft:blast_furnace'
  }).id('frostbound:hearth_overworld')
})
