package de.frostbound.tweaks.mixin;

import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import com.momosoftworks.coldsweat.util.world.SpreadPath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cold Sweat lässt die warme Luft des Hearth nur oben aus dem Hearth-Kopf (technisch ein Rauchabzug) starten.
 * Ein Schornstein durchs Dach leitet sie damit komplett ins Freie, wo sie verworfen wird, und der Raum bleibt kalt.
 * Hier startet die Luft zusätzlich an den vier Seiten beider Hearth-Blöcke: Der Hearth heizt seinen Raum selbst,
 * der Rauchabzug oben ist nur noch Schornstein bzw. Lüftungsrohr zu anderen Gebäuden.
 * Die Seitenpfade werden laufend nachgesetzt, damit sie nach Umbauten am Raum wiederkommen.
 */
@Mixin(value = HearthBlockEntity.class, remap = false)
public abstract class HearthRadiantMixin {
    private static final Direction[] FROSTBOUND$SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    @Shadow List<SpreadPath> paths;
    @Shadow Map<BlockPos, SpreadPath> pathLookup;
    @Shadow int frozenPaths;
    @Shadow boolean spreading;
    @Shadow public int ticksExisted;

    @Shadow public abstract void addPath(SpreadPath path);

    // Direkt nachdem tick() "spreading" für den aktiven Hearth berechnet hat (Brennstoff da, Heizen/Kühlen an)
    @Inject(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER,
            target = "Lcom/momosoftworks/coldsweat/common/blockentity/HearthBlockEntity;spreading:Z"))
    private void frostbound$seedSidePaths(Level level, BlockPos pos, CallbackInfo ci) {
        // Boiler und Icebox erben vom Hearth, sind aber nur einen Block hoch und bleiben wie sie sind
        if (((Object) this).getClass() != HearthBlockEntity.class || this.ticksExisted % 10 != 0) return;

        BlockPos hearthPos = ((BlockEntity) (Object) this).getBlockPos();
        int before = this.paths.size();
        for (BlockPos part : new BlockPos[]{hearthPos, hearthPos.above()}) {
            for (Direction dir : FROSTBOUND$SIDES) {
                BlockPos seedPos = part.relative(dir);
                // Schon ein Pfad da; null heißt nur "von einem Nachbarn aus blockiert" (z. B. vom Hearth-Kopf seitlich)
                if (this.pathLookup.get(seedPos) != null) continue;
                // Unter freiem Himmel würde Cold Sweat den Pfad im nächsten Schritt wieder verwerfen
                if (WorldHelper.canSeeSky(level, seedPos.above(), 64)) continue;
                this.addPath(new SpreadPath(seedPos, dir));
            }
        }
        if (this.paths.size() != before) {
            this.spreading = this.frozenPaths < this.paths.size();
        }
    }
}
