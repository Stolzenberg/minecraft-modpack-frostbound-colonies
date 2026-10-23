package de.frostbound.tweaks;

import de.frostbound.tweaks.warmth.RoomHeat;
import de.frostbound.tweaks.warmth.RoomHeatConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * Anpassungen für Frostbound Colonies. Die eigentliche Arbeit machen die Mixins:
 * Colder Primal Winter zeichnet Schneefall, Schneegeräusche und Nebel sonst dauerhaft,
 * unabhängig vom Wetter. Hier hängt beides am echten Regen-/Schneelevel der Welt (nur Client).
 * Der Hearth von Cold Sweat heizt seinen Raum selbst statt nur über den Rauchabzug (Server und Client).
 * Mitgelieferte Resource Packs werden einmal eingeschaltet, siehe DefaultResourcePacks (nur Client).
 * Dazu die Raumwärme (Paket warmth): Wärmequellen heizen geschlossene Räume und speichern Wärme (Server).
 */
@Mod(FrostboundTweaks.MOD_ID)
public final class FrostboundTweaks {
    public static final String MOD_ID = "frostbound_tweaks";

    public FrostboundTweaks(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, RoomHeatConfig.SPEC);
        modBus.addListener(ModConfigEvent.Loading.class, e -> RoomHeatConfig.invalidate());
        modBus.addListener(ModConfigEvent.Reloading.class, e -> RoomHeatConfig.invalidate());
        // Die Raumwärme greift auf Cold-Sweat-Klassen zu und darf ohne Cold Sweat nicht geladen werden
        if (ModList.get().isLoaded("cold_sweat")) RoomHeat.init();
    }
}
