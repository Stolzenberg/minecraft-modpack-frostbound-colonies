#!/usr/bin/env python3
"""Cold-Sweat-Configs (world/item/main.toml) für die gefrorene Welt aus Cold Sweats Standardwerten erzeugen.

usage: coldsweat_frost.py <Ordner mit Standard-Configs> <Ausgabe-Ordner> [<latest.log>]

- world.toml: Alle Biom-Temperaturen (°F) aus der Standardliste werden gestaucht und abgesenkt:
      neu = alt * SCALE + SHIFT
  Die Reihenfolge bleibt erhalten (ehemalige Wüsten sind die "wärmsten" Orte), aber jede Oberfläche liegt
  unter der Wohlfühlgrenze von 50 °F. Mit einem latest.log fallen Biome weg, die Cold Sweat als
  "does not exist" meldet. Dazu kommen zusätzliche Wärmequellen (EXTRA_BLOCKS).
- item.toml: warme Speisen und Getränke (WARM_FOODS) heizen den Körper für eine Weile auf.
- main.toml: längere Schonfrist nach dem Spawn, kein Auto-Update (würde die Werte zurücksetzen).
"""
import os, re, sys

SCALE, SHIFT = 0.5, 5  # SHIFT war -5; +10 °F überall für entspannteres Spiel (2026-10-23)
# Unverändert: Nether/End und Höhlenbiome (Höhlen bleiben ein milder Rückzugsort)
KEEP = {"minecraft:soul_sand_valley", "minecraft:small_end_islands", "minecraft:deep_dark", "minecraft:lush_caves"}
# Biome-Mods, die nicht im Pack sind (BOP würde von Primal Winter nicht eingefroren, Terralith ist durch Tectonic ersetzt)
DROP_NAMESPACES = {"biomesoplenty", "terralith"}

# Zusätzliche Wärmequellen: Weihnachtskamine (ManyIdeas, brennen immer; nur der mittlere untere Block zählt),
# brennende Kerzen, Fackeln, Laternen, Öfen und Herde. Format: [block, temp, reichweite, einheit, maxEffekt, "zustand", "", limit]
# Das ist nur die Wärme direkt am Block (durch Wände gedämpft); ganze Räume heizt die Raumwärme von Frostbound Tweaks.
FIREPLACES = ",".join(f"manyideas_christmas:fireplace_{w}" for w in
                      ("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "crimson", "warped"))
STOVES = ",".join(["farmersdelight:stove", "farm_and_charm:stove", "handcrafted:oven"] +
                  [f"candlelight:{s}_stove" for s in ("bamboo", "basalt", "cobblestone", "deepslate", "end", "granite",
                                                      "mud", "quartz", "red_nether_bricks", "sandstone", "stone_bricks")])
EXTRA_BLOCKS = [
    f'["{FIREPLACES}", 60, 6, "f", 85, "y=0,z=1", "", 400]',
    '["#minecraft:candles", 4, 2, "f", 8, "lit=true", "", 400]',
    '["minecraft:torch,minecraft:wall_torch", 4, 2, "f", 8, "", "", 400]',
    '["minecraft:lantern,candlelight:lamp", 5, 2, "f", 10, "", "", 400]',
    '["minecraft:furnace,minecraft:blast_furnace,minecraft:smoker", 25, 3, "f", 40, "lit=true", "", 400]',
    f'["{STOVES}", 25, 3, "f", 40, "lit=true", "", 400]',
]
# Vorhandene Wärmequellen verstärken (Faktor auf Temperatur, Faktor auf maxEffekt): Die Biome sind ca. 40 °F kälter
# als in Vanilla-Cold-Sweat, deshalb müssen Feuer stärker heizen. Gemessen: Lagerfeuer +25 °F -> direkt daneben nur
# ca. 25 °F (Ebene, Nacht), Wohlfühlgrenze 50 °F. Mit Faktor 2,6 wärmt ein Lagerfeuer im Umkreis von ca. 3 Blöcken.
HEAT_BOOST = {"#minecraft:fire": (2.6, 1.8), "#minecraft:campfires": (2.6, 1.8), "create:blaze_burner": (2.4, 1.8)}

# Warme Speisen: (Items, Körpertemperatur, Dauer in Ticks)
SOUPS = ["farmersdelight:" + i for i in ("beef_stew", "chicken_soup", "vegetable_soup", "fish_stew", "pumpkin_soup",
                                         "baked_cod_stew", "noodle_soup", "onion_soup", "bone_broth")] + \
        ["minecraft:mushroom_stew", "minecraft:rabbit_stew", "minecraft:beetroot_soup",
         "christmas_culinary_decorations:fsoup"]
DRINKS = ["farmersdelight:hot_cocoa", "farmersdelight:apple_cider", "festive_delight:christmas_tea",
          "manyideas_christmas:hot_chocolate"] + \
         ["farmers_delight_christmas_editio:" + i for i in (
             "hot_cocoa_with_marshmallows", "hot_cocoa_with_marshmallows_and_peppermint",
             "hot_cocoa_with_marshmallows_and_candy_cane", "hot_cocoa_with_marshmallows_and_cinnamon",
             "cinnamon_eggnog")]
MEALS = ["farmersdelight:" + i for i in ("roasted_mutton_chops", "steak_and_potatoes", "roast_chicken", "shepherds_pie")] + \
        ["festive_delight:festive_chiken"]
WARM_FOODS = [(SOUPS, 15, 2400), (DRINKS, 20, 1200), (MEALS, 10, 1200)]

# Kleidung (Kälteschutz-Punkte; zum Vergleich: Lederrüstung 23, Ziegenpelz-Kleidung 46 für das ganze Set)
# Felle aus "Leather Working - Pelts & Hides" als Zutat am Nähtisch: (Item, Kälte, Hitze)
PELTS = [("leatherworking:polar_bear_hide", 3, 0), ("leatherworking:wolf_hide", 2, 0), ("leatherworking:fox_hide", 2, 0),
         ("leatherworking:goat_hide", 2, 0), ("leatherworking:sheep_hide", 1.5, 0),
         ("leatherworking:cow_hide,leatherworking:pig_hide,leatherworking:mooshroom_hide,leatherworking:panda_hide", 1, 1),
         ("leatherworking:camel_hide,leatherworking:ocelot_hide", 0, 1.5)]
# Rüstungen aus "Armor Series - Norsemen": Fellsets wie Ziegenpelz, Schädel-/Hörnersets etwa halb so warm
NORSE = "armor_series__norsemen"
PIECES = ("helmet", "chestplate", "leggings", "boots")
NORSE_ARMOR = [(("bjornset", "hvita_bjorn_set", "ulfrset", "skorgr_set", "pickish_light"), (10, 14, 12, 10)),
               (("elgr_set", "veioimaor_set", "fjall_set", "drengr"), (5, 7, 6, 5))]
# Kleidung aus "Weavers Paradise" (Curios-Slots, zusätzlich zur Rüstung tragbar): Wolle wärmt, Baumwolle/Seide kaum
CLOTHES = [("ushanka", 5), ("pompon_hat", 3), ("sweater_wool", 6), ("wool_vest", 3), ("poncho", 3), ("wool_cape", 3),
           ("pants_wool", 4), ("thigh_highs_wool", 2), ("hand_warmers_wool", 2), ("leather_gloves", 2),
           ("cotton_cape", 1), ("silk_cape", 1), ("pants_cotton", 1), ("pants_jeans", 1), ("pants_silk", 1),
           ("long_sleeve_cotton", 1), ("hand_warmers_cotton", 1), ("hand_warmers_silk", 1)]

GRACE_TICKS = 36000  # 30 Minuten Schonfrist zum Ankommen (mit Better Days der erste halbe Tag)
RATE_MULTIPLIER = 0.5  # halb so schnelles Auskühlen/Aufwärmen; Kleidung senkt zusätzlich die Kältegrenze (frost_clothing.js)


def frost(v):
    return round(float(v) * SCALE + SHIFT, 1)


def replace_list(text, key, fn):
    empty = f'"{key}" = []'
    if empty in text:  # leere Liste steht in einer Zeile
        return text.replace(empty, f'"{key}" = [' + fn("") + "\n\t]")
    m = re.search(r'("' + re.escape(key) + r'" = \[)(.*?)(\n\t\])', text, re.S)
    if not m:
        sys.exit(f"Liste {key} nicht gefunden")
    return text[:m.start(2)] + fn(m.group(2)) + text[m.end(2):]


def world(text, missing):
    def biomes(body):
        out = []
        for line in body.splitlines():
            e = re.match(r'\s*\["([^"]+)", (-?[\d.]+), (-?[\d.]+), "F"(?:, (-?[\d.]+))?\],?', line)
            if not e or e.group(1) in missing or e.group(1).split(":")[0] in DROP_NAMESPACES:
                continue  # "disable"-Einträge fallen weg -> Cold Sweat nutzt die (von Primal Winter gesenkte) Biomtemperatur
            biome, lo, hi, water = e.groups()
            f = float if biome in KEEP else frost
            out.append(f'\t    ["{biome}", {f(lo)}, {f(hi)}, "F"' + (f', {f(water)}' if water else "") + "]")
        print(f"world.toml: {len(out)} Biome, {len(missing)} fehlende entfernt")
        return "\n" + ",\n".join(out)

    def blocks(body):
        def boost(m):
            t, mx = HEAT_BOOST[m.group(1)]
            return f'["{m.group(1)}", {round(float(m.group(2)) * t)}, {m.group(3)}, "f", {round(float(m.group(4)) * mx)},'
        ids = "|".join(re.escape(k) for k in HEAT_BOOST)
        body = re.sub(r'\["(' + ids + r')", (-?[\d.]+), (\d+), "f", (-?[\d.]+),', boost, body)
        return body.rstrip() + ",\n" + ",\n".join("\t    " + x for x in EXTRA_BLOCKS)

    text = replace_list(text, "Biome Temperatures", biomes)
    return replace_list(text, "Block Temperatures", blocks)


def append(text, key, entries):
    lines = ",\n".join("\t    " + e for e in entries)
    return replace_list(text, key, lambda body: (body.rstrip() + ",\n" if body.strip() else "\n") + lines)


def item(text):
    text = append(text, "Temperature-Affecting Foods",
                  [f'["{",".join(items)}", {temp}, "{{}}", {ticks}]' for items, temp, ticks in WARM_FOODS])
    text = append(text, "Insulation Ingredients", [f'["{i}", {c}, {h}]' for i, c, h in PELTS])
    text = append(text, "Insulating Armor",
                  [f'["{NORSE}:{s}_{p}", {v}, 0]' for sets, vals in NORSE_ARMOR for s in sets for p, v in zip(PIECES, vals)])
    return append(text, "Insulating Curios", [f'["weaversparadise:{i}", {c}, 0]' for i, c in CLOTHES])


def main_toml(text):
    text = text.replace('"Auto Update" = true', '"Auto Update" = false')
    text = re.sub(r'"Rate Multiplier" = [\d.]+', f'"Rate Multiplier" = {RATE_MULTIPLIER}', text)
    return re.sub(r'"Grace Period Length" = \d+', f'"Grace Period Length" = {GRACE_TICKS}', text)


def main():
    src, dst = sys.argv[1], sys.argv[2]
    missing = set()
    if len(sys.argv) > 3:
        missing = set(re.findall(r'worldgen/biome "([^"]+)" does not exist', open(sys.argv[3], errors="replace").read()))
    os.makedirs(dst, exist_ok=True)
    for name, fn in (("world.toml", lambda t: world(t, missing)), ("item.toml", item), ("main.toml", main_toml)):
        open(os.path.join(dst, name), "w").write(fn(open(os.path.join(src, name)).read()))
    print(f"-> {dst}")


if __name__ == "__main__":
    main()
