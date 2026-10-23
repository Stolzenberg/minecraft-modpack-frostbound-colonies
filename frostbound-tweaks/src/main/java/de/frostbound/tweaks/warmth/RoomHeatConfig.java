package de.frostbound.tweaks.warmth;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

/**
 * Einstellungen der Raumwärme (config/frostbound_tweaks-common.toml) und die daraus gelesenen Wärmequellen.
 * Die Listen werden beim ersten Zugriff ausgewertet und nach einem Config- oder Tag-Reload neu gelesen.
 */
public final class RoomHeatConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ModConfigSpec SPEC;
    static final ModConfigSpec.BooleanValue ENABLED;
    static final ModConfigSpec.IntValue SCAN_INTERVAL;
    static final ModConfigSpec.IntValue MAX_VOLUME;
    static final ModConfigSpec.IntValue MIN_VOLUME;
    static final ModConfigSpec.IntValue LEAK_LIMIT;
    static final ModConfigSpec.DoubleValue MAX_HEAT;
    static final ModConfigSpec.DoubleValue ROOM_TEMP_LIMIT;
    static final ModConfigSpec.DoubleValue CHARGE_MINUTES;
    static final ModConfigSpec.DoubleValue STORAGE_MINUTES;
    static final ModConfigSpec.DoubleValue MASS_BONUS;
    static final ModConfigSpec.IntValue MASS_MAX;
    static final ModConfigSpec.ConfigValue<List<? extends String>> SOURCES;
    static final ModConfigSpec.ConfigValue<List<? extends String>> MASS_BLOCKS;

    private static final String CANDLELIGHT_STOVES = String.join(",", List.of(
            "bamboo", "basalt", "cobblestone", "deepslate", "end", "granite", "mud", "quartz", "red_nether_bricks",
            "sandstone", "stone_bricks").stream().map(s -> "candlelight:" + s + "_stove").toList());
    private static final String FIREPLACES = String.join(",", List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "crimson", "warped").stream()
            .map(s -> "manyideas_christmas:fireplace_" + s).toList());

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Raumwärme: Wärmequellen heizen den geschlossenen Raum, in dem sie stehen (braucht Cold Sweat).",
                "Temperaturen in °F (Cold Sweat rechnet intern mit 1 MC-Einheit = 45 °F).").push("raumwaerme");
        ENABLED = b.comment("Raumwärme an/aus").define("aktiv", true);
        SCAN_INTERVAL = b.comment("Höchstens so oft (Ticks) wird der Raum um einen stillstehenden Spieler neu vermessen")
                .defineInRange("messabstand", 40, 20, 1200);
        MAX_VOLUME = b.comment("Größere Räume (Luftblöcke) gelten als nicht beheizbar, z. B. Höhlen")
                .defineInRange("max_volumen", 2000, 50, 20000);
        MIN_VOLUME = b.comment("Kleinere Räume zählen mit diesem Volumen (sonst heizt eine Kerze eine Besenkammer auf)")
                .defineInRange("min_volumen", 30, 1, 1000);
        LEAK_LIMIT = b.comment("Luftblöcke mit Himmelssicht, die der Raum berührt (offene Tür = 2). Jedes Leck kostet",
                        "1/Grenze der Wärme, ab der Grenze ist der Raum offen und heizt nicht mehr")
                .defineInRange("leck_grenze", 16, 1, 1000);
        MAX_HEAT = b.comment("Höchste Raumwärme in °F").defineInRange("max_waerme", 60.0, 0.0, 500.0);
        ROOM_TEMP_LIMIT = b.comment("Über diese Umgebungstemperatur (°F, absolut) heizt die Raumwärme nicht hinaus",
                        "(68 °F = 20 °C; Wärme direkt am Ofen kann darüber liegen)")
                .defineInRange("raumtemperatur_grenze", 68.0, -100.0, 200.0);
        CHARGE_MINUTES = b.comment("Wärmespeicher: Zeitkonstante beim Aufheizen in Minuten (nach dieser Zeit 63 % geladen)")
                .defineInRange("aufheizen_minuten", 2.0, 0.05, 120.0);
        STORAGE_MINUTES = b.comment("Wärmespeicher: Zeitkonstante beim Abkühlen in Minuten (nach dieser Zeit noch 37 %)")
                .defineInRange("abkuehlen_minuten", 4.0, 0.05, 240.0);
        MASS_BONUS = b.comment("Jeder Speichermasse-Block direkt um die Quelle (3×3×3) verlängert das Abkühlen um diesen Anteil")
                .defineInRange("masse_bonus", 0.25, 0.0, 10.0);
        MASS_MAX = b.comment("Höchstens so viele Speichermasse-Blöcke zählen").defineInRange("masse_max", 12, 0, 26);
        SOURCES = b.comment("Wärmequellen: \"blöcke;leistung;speichert[;zustand]\"",
                        "  blöcke: IDs oder #Tags, durch Kommas getrennt",
                        "  leistung: °F × Luftblöcke (3000 heizt 75 Luftblöcke, eine 5×5-Hütte, um 40 °F)",
                        "  speichert: true = Wärmespeicher (lädt sich beim Brennen auf, kühlt danach langsam ab)",
                        "  zustand: z. B. \"blaze=kindled\" (mehrere mit Komma). Blöcke mit \"lit\" zählen nur brennend,",
                        "  Kerzen (\"candles\") und Kerzenkränze (\"lit_count\") je brennender Kerze")
                .defineListAllowEmpty("quellen", List.of(
                        "minecraft:furnace,minecraft:blast_furnace,minecraft:smoker;3000;true",
                        "farmersdelight:stove,farm_and_charm:stove,handcrafted:oven;2500;true",
                        CANDLELIGHT_STOVES + ";2500;true",
                        "minecraft:campfire;4000;true",
                        FIREPLACES + ";5000;true;y=0,z=1",
                        "cold_sweat:hearth_bottom;12000;true",
                        "create:blaze_burner;2500;false;blaze=kindled",
                        "create:blaze_burner;4000;false;blaze=seething",
                        "minecraft:torch,minecraft:wall_torch;150;false",
                        "minecraft:lantern;200;false",
                        "candlelight:lamp;200;false",
                        "#minecraft:candles,#minecraft:candle_cakes;50;false",
                        "manyideas_christmas:wreath_candle;50;false"),
                        () -> "", o -> o instanceof String);
        MASS_BLOCKS = b.comment("Speichermasse um Wärmespeicher (IDs oder #Tags): Ziegel und Stein halten die Wärme wie ein Kachelofen")
                .defineListAllowEmpty("speichermasse", List.of(
                        "minecraft:bricks", "minecraft:mud_bricks", "minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
                        "minecraft:cracked_stone_bricks", "minecraft:chiseled_stone_bricks", "minecraft:stone",
                        "minecraft:cobblestone", "minecraft:mossy_cobblestone", "minecraft:smooth_stone",
                        "minecraft:cobbled_deepslate", "minecraft:polished_deepslate", "minecraft:deepslate_bricks",
                        "minecraft:deepslate_tiles", "minecraft:nether_bricks", "minecraft:red_nether_bricks",
                        "minecraft:polished_blackstone_bricks", "#minecraft:terracotta", "minecraft:clay"),
                        () -> "", o -> o instanceof String);
        b.pop();
        SPEC = b.build();
    }

    private static List<Source> sources;
    private static List<Matcher> massMatchers;
    private static final Map<Block, List<Source>> SOURCE_CACHE = new IdentityHashMap<>();
    private static final Map<Block, Boolean> MASS_CACHE = new IdentityHashMap<>();

    private RoomHeatConfig() {}

    /** Nach Config- oder Tag-Reload: Listen beim nächsten Zugriff neu auswerten. */
    public static synchronized void invalidate() {
        sources = null;
        massMatchers = null;
        SOURCE_CACHE.clear();
        MASS_CACHE.clear();
    }

    /** Alle Quellen-Einträge, die zum Block passen (unabhängig vom Zustand); leer, wenn keiner passt. */
    static synchronized List<Source> sourcesFor(BlockState state) {
        if (sources == null) parse();
        return SOURCE_CACHE.computeIfAbsent(state.getBlock(), block -> {
            List<Source> out = new ArrayList<>(1);
            for (Source s : sources) if (s.matcher.matches(state)) out.add(s);
            return out.isEmpty() ? List.of() : out;
        });
    }

    /** Erster Wärmespeicher-Eintrag für den Block oder null. */
    static Source storageFor(BlockState state) {
        for (Source s : sourcesFor(state)) if (s.stores) return s;
        return null;
    }

    static synchronized boolean isMass(BlockState state) {
        if (massMatchers == null) parse();
        return MASS_CACHE.computeIfAbsent(state.getBlock(), block -> {
            for (Matcher m : massMatchers) if (m.matches(state)) return true;
            return false;
        });
    }

    private static void parse() {
        List<Source> parsed = new ArrayList<>();
        for (String line : SOURCES.get()) {
            String[] parts = line.split(";");
            try {
                Map<String, String> predicates = new HashMap<>();
                if (parts.length > 3) {
                    for (String p : parts[3].split(",")) {
                        String[] kv = p.trim().split("=");
                        predicates.put(kv[0].trim(), kv[1].trim());
                    }
                }
                parsed.add(new Source(Matcher.of(parts[0]), Double.parseDouble(parts[1].trim()),
                        Boolean.parseBoolean(parts[2].trim()), predicates));
            } catch (RuntimeException e) {
                LOGGER.warn("[frostbound_tweaks] Raumwärme: Quelle \"{}\" nicht lesbar: {}", line, e.toString());
            }
        }
        List<Matcher> mass = new ArrayList<>();
        for (String id : MASS_BLOCKS.get()) mass.add(Matcher.of(id));
        sources = parsed;
        massMatchers = mass;
    }

    /** Ein Eintrag aus "quellen". */
    static final class Source {
        final Matcher matcher;
        final double power;
        final boolean stores;
        final Map<String, String> predicates;

        Source(Matcher matcher, double power, boolean stores, Map<String, String> predicates) {
            this.matcher = matcher;
            this.power = power;
            this.stores = stores;
            this.predicates = predicates;
        }

        /** Brennt die Quelle gerade (Zustand passt, "lit" ist an, sofern vorhanden und nicht ausdrücklich gesetzt)? */
        boolean isActive(BlockState state) {
            for (Map.Entry<String, String> e : predicates.entrySet()) {
                Property<?> prop = state.getBlock().getStateDefinition().getProperty(e.getKey());
                if (prop == null || !valueName(state, prop).equals(e.getValue())) return false;
            }
            if (!predicates.containsKey("lit") && state.hasProperty(BlockStateProperties.LIT)
                    && !state.getValue(BlockStateProperties.LIT)) return false;
            return multiplier(state) > 0;
        }

        /** Anzahl brennender Kerzen bei Kerzen und Kerzenkränzen, sonst 1. */
        double multiplier(BlockState state) {
            if (state.hasProperty(BlockStateProperties.CANDLES)) return state.getValue(BlockStateProperties.CANDLES);
            Property<?> litCount = state.getBlock().getStateDefinition().getProperty("lit_count");
            if (litCount instanceof IntegerProperty ip) return state.getValue(ip);
            return 1;
        }

        private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> prop) {
            return prop.getName(state.getValue(prop));
        }
    }

    /** Block-IDs und #Tags, durch Kommas getrennt; unbekannte IDs (Mod fehlt) werden ignoriert. */
    static final class Matcher {
        final List<Block> blocks = new ArrayList<>();
        final List<TagKey<Block>> tags = new ArrayList<>();

        static Matcher of(String spec) {
            Matcher m = new Matcher();
            for (String raw : spec.split(",")) {
                String id = raw.trim();
                if (id.isEmpty()) continue;
                if (id.startsWith("#")) {
                    m.tags.add(TagKey.create(Registries.BLOCK, ResourceLocation.parse(id.substring(1))));
                } else {
                    BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).ifPresentOrElse(m.blocks::add,
                            () -> LOGGER.debug("[frostbound_tweaks] Raumwärme: Block {} gibt es nicht", id));
                }
            }
            return m;
        }

        boolean matches(BlockState state) {
            for (Block b : blocks) if (state.is(b)) return true;
            for (TagKey<Block> t : tags) if (state.is(t)) return true;
            return false;
        }
    }
}
