package de.frostbound.tweaks.mixin;

import de.frostbound.tweaks.DefaultResourcePacks;
import net.minecraft.client.Options;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mitgelieferte Resource Packs einmalig einschalten (siehe DefaultResourcePacks). loadSelectedResourcePacks
 * läuft beim Start, nachdem options.txt gelesen und die Packs gefunden wurden. Nach dem Speichern wird vermerkt.
 */
@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow
    public List<String> resourcePacks;

    @Inject(method = "loadSelectedResourcePacks", at = @At("HEAD"))
    private void frostbound$defaultResourcePacks(PackRepository repository, CallbackInfo ci) {
        DefaultResourcePacks.apply(resourcePacks, repository);
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void frostbound$rememberDefaultResourcePacks(CallbackInfo ci) {
        DefaultResourcePacks.saved();
    }
}
