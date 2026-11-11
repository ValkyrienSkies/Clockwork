package org.valkyrienskies.clockwork.mixin.content.lubricant;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantData;
import org.valkyrienskies.mod.common.assembly.StructureMetadataRelocator;

@Mixin(value = StructureMetadataRelocator.class, remap = false)
public class MixinStructureMetadataRelocator {
    @Inject(method = "relocateStructureMetadata", at = @At("HEAD"))
    private static void clockwork$moveCoatings(ServerLevel level, Set<BlockPos> blocks, BlockPos min,
            BlockPos max, BlockPos destination, CallbackInfo ci) {
        LubricantData.moveRegion(level, blocks, min, destination);
    }
}
