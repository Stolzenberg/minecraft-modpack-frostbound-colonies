package de.frostbound.tweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Primal Winter ersetzt renderSnowAndRain/tickRain per HEAD-Inject und zeichnet bzw. vertont
 * Schneefall in Winter-Dimensionen immer. @WrapMethod umschließt die Methode samt dieser Injects:
 * Ohne Niederschlag (Regenlevel 0) läuft gar nichts. Vanilla macht in dem Fall ohnehin nichts.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    @Nullable
    private ClientLevel level;

    @WrapMethod(method = "renderSnowAndRain")
    private void frostbound$snowOnlyWhilePrecipitating(LightTexture lightTexture, float partialTick, double camX, double camY, double camZ, Operation<Void> original) {
        if (level != null && level.getRainLevel(partialTick) <= 0.0F) {
            return;
        }
        original.call(lightTexture, partialTick, camX, camY, camZ);
    }

    @WrapMethod(method = "tickRain")
    private void frostbound$soundsOnlyWhilePrecipitating(Camera camera, Operation<Void> original) {
        if (level != null && level.getRainLevel(1.0F) <= 0.0F) {
            return;
        }
        original.call(camera);
    }
}
