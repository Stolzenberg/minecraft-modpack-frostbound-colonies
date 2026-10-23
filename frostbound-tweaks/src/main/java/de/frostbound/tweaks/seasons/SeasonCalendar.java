package de.frostbound.tweaks.seasons;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import sereneseasons.init.ModConfig;
import sereneseasons.season.SeasonHandler;
import sereneseasons.season.SeasonSavedData;
import sereneseasons.season.SeasonTime;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Serene Seasons folgt dem Kalender: Jeder Monat ist eine Teilsaison, März = früher Frühling, Dezember = früher Winter.
 * Serene Seasons zählt selbst über die Tageszeit weiter (bei passender sub_season_duration etwa im Takt der echten Zeit),
 * Schlafen, Lags und Ausfallzeiten verschieben das aber. Deshalb wird der Zyklus alle zehn Sekunden aufs Datum gesetzt.
 * Wird nur geladen, wenn Serene Seasons installiert ist (Klassenzugriffe).
 */
public final class SeasonCalendar {
    private static final int INTERVAL = 200;

    private SeasonCalendar() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, SeasonCalendar::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % INTERVAL != 0) {
            return;
        }
        if (!SeasonCalendarConfig.ENABLED.get() || !ModConfig.seasons.isDimensionWhitelisted(level.dimension())) {
            return;
        }
        int target = cycleTicks(ZonedDateTime.now(ZoneId.of(SeasonCalendarConfig.TIME_ZONE.get())));
        SeasonSavedData data = SeasonHandler.getSeasonSavedData(level);
        if (data.seasonCycleTicks != target) {
            data.seasonCycleTicks = target;
            data.setDirty();
            SeasonHandler.sendSeasonUpdate(level);
        }
    }

    /** Position im Jahreszyklus von Serene Seasons (0 = Beginn des frühen Frühlings) für ein Datum. */
    static int cycleTicks(ZonedDateTime now) {
        int subSeason = Math.floorMod(now.getMonthValue() - 3, 12);
        double intoMonth = (now.getDayOfMonth() - 1 + now.toLocalTime().toSecondOfDay() / 86400.0)
                / now.toLocalDate().lengthOfMonth();
        long ticks = Math.round((subSeason + intoMonth) * SeasonTime.ZERO.getSubSeasonDuration());
        return (int) Math.min(ticks, SeasonTime.ZERO.getCycleDuration() - 1L);
    }
}
