// Frostbound Colonies – Rezepte
//
// Der Hearth (Cold Sweat) ist der "Generator" der Siedlung: Er heizt ein ganzes Gebäude und lässt sich über
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

  // Create-Millstone: Diorite zu Nether Quartz, damit Quartz nicht an einem Nether-Ausflug hängt.
  // 50 %, weil 2 Diorite im Crafting 2 Quartz kosten, sonst wäre es nur das Rezept rückwärts.
  event.custom({
    type: 'create:milling',
    ingredients: [{ item: 'minecraft:diorite' }],
    processing_time: 200,
    results: [{ id: 'minecraft:quartz', chance: 0.5 }]
  }).id('frostbound:milling/diorite_to_quartz')
})
