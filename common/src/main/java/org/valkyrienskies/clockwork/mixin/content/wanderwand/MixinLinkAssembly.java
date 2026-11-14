package org.valkyrienskies.clockwork.mixin.content.wanderwand;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkTransfer;
import org.valkyrienskies.mod.common.assembly.StructureMetadataRelocator;

@Mixin(value = StructureMetadataRelocator.class, remap = false)
public class MixinLinkAssembly {
    @Inject(method = "relocateStructureMetadata", at = @At("HEAD"))
    private static void clockwork$moveLinks(ServerLevel level, Set<BlockPos> blocks, BlockPos min,
            BlockPos max, BlockPos destination, CallbackInfo ci) {
        WandLinkTransfer.moveRegion(level, blocks, min, destination);
    }
}
