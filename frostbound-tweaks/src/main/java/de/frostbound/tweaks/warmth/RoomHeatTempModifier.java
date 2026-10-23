package de.frostbound.tweaks.warmth;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hebt die Umgebungstemperatur (Trait WORLD) um die Raumwärme an, höchstens bis zur Raumtemperatur-Grenze.
 * Steht nach Cold Sweats Blockwärme: Wer direkt am Ofen schon über der Grenze ist, bekommt nichts dazu.
 * Cold Sweat rechnet Modifier nur auf dem Server; der Client bekommt das Ergebnis synchronisiert.
 */
public class RoomHeatTempModifier extends TempModifier {
    @Override
    protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait) {
        if (!(entity instanceof ServerPlayer player)) return temp -> temp;
        double heat = Temperature.convert(RoomHeat.heatFor(player), Temperature.Units.F, Temperature.Units.MC, false);
        if (heat <= 0) return temp -> temp;
        double limit = Temperature.convert(RoomHeatConfig.ROOM_TEMP_LIMIT.get(), Temperature.Units.F, Temperature.Units.MC, true);
        return temp -> temp >= limit ? temp : Math.min(limit, temp + heat);
    }
}
