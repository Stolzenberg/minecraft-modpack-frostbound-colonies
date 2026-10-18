package de.frostbound.tweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Primal Winters Nebeldichte hängt nur vom Himmelslicht ab (Mth.clampedMap(Himmelslicht, 0, 15, 0, 1)).
 * Mit dem Regenlevel multipliziert, blendet der Nebel bei klarem Himmel über Primal Winters eigene
 * Glättung aus und bei Schneefall wieder ein.
 */
@Pseudo
@Mixin(targets = "com.alcatrazescapee.primalwinter.client.ClientEventHandler", remap = false)
public abstract class PrimalWinterFogMixin {
    @ModifyExpressionValue(method = "renderFogDensity", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clampedMap(FFFFF)F"))
    private static float frostbound$fogOnlyWhilePrecipitating(float original) {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? original : original * level.getRainLevel(1.0F);
    }
}
