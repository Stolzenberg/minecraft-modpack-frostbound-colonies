package de.frostbound.tweaks;

import net.neoforged.fml.common.Mod;

/**
 * Anpassungen für Frostbound Colonies. Die eigentliche Arbeit machen die Mixins:
 * Colder Primal Winter zeichnet Schneefall, Schneegeräusche und Nebel sonst dauerhaft,
 * unabhängig vom Wetter. Hier hängt beides am echten Regen-/Schneelevel der Welt (nur Client).
 * Der Hearth von Cold Sweat heizt seinen Raum selbst statt nur über den Rauchabzug (Server und Client).
 */
@Mod(FrostboundTweaks.MOD_ID)
public final class FrostboundTweaks {
    public static final String MOD_ID = "frostbound_tweaks";
}
