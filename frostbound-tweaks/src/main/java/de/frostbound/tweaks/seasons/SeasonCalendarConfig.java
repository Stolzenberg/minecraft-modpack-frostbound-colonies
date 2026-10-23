package de.frostbound.tweaks.seasons;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.time.DateTimeException;
import java.time.ZoneId;

/** config/frostbound_tweaks-common.toml: Kalender-Jahreszeiten an/aus und Zeitzone. */
public final class SeasonCalendarConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.ConfigValue<String> TIME_ZONE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("seasons");
        ENABLED = b.comment("Serene-Seasons-Jahreszeit aus dem echten Datum setzen (März bis Mai Frühling, Juni bis August",
                        "Sommer, September bis November Herbst, Dezember bis Februar Winter; jeder Monat ist eine Teilsaison).",
                        "Aus: Serene Seasons zählt selbst weiter, /season set bleibt dann bestehen.")
                .define("calendar_seasons", true);
        TIME_ZONE = b.comment("Zeitzone für das Datum, z. B. Europe/Berlin")
                .define("time_zone", "Europe/Berlin", o -> o instanceof String s && valid(s));
        b.pop();
        SPEC = b.build();
    }

    private SeasonCalendarConfig() {
    }

    private static boolean valid(String id) {
        try {
            ZoneId.of(id);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }
}
