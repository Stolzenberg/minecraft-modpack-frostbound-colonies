package de.frostbound.tweaks;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * Client-Anpassungen für Frostbound Colonies. Die eigentliche Arbeit machen die Mixins:
 * Colder Primal Winter zeichnet Schneefall, Schneegeräusche und Nebel sonst dauerhaft,
 * unabhängig vom Wetter. Hier hängt beides am echten Regen-/Schneelevel der Welt.
 */
@Mod(value = FrostboundTweaks.MOD_ID, dist = Dist.CLIENT)
public final class FrostboundTweaks {
    public static final String MOD_ID = "frostbound_tweaks";
}
