package org.valkyrienskies.clockwork.mixin.content.lubricant;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.joml.Vector3ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantMaterials;
import org.valkyrienskies.core.internal.world.chunks.VsiTerrainUpdate;

@Mixin(targets = "org.valkyrienskies.mod.common.VSGameUtilsKt", remap = false)
public class MixinTerrainUpdate {
    @Inject(target = @Desc(value = "toDenseVoxelUpdate", args = {LevelChunkSection.class, Vector3ic.class, Level.class},
        ret = VsiTerrainUpdate.class), at = @At("HEAD"), cancellable = true)
    private static void clockwork$coatedTerrain(LevelChunkSection section, Vector3ic chunk, Level level,
            CallbackInfoReturnable<VsiTerrainUpdate> cir) {
        VsiTerrainUpdate update = LubricantMaterials.terrain(section, chunk, level);
        if (update != null) cir.setReturnValue(update);
    }
}
