package org.valkyrienskies.clockwork.mixin.content.wanderwand;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkTransfer;
import org.valkyrienskies.core.api.ships.ServerShip;

@Mixin(targets = "org.valkyrienskies.mod.util.RelocationUtilKt", remap = false)
public class MixinLinkRelocation {
    @Inject(target = @Desc(value = "relocateBlock", args = {LevelChunk.class, BlockPos.class, LevelChunk.class,
            BlockPos.class, boolean.class, ServerShip.class, Rotation.class}), at = @At("RETURN"))
    private static void clockwork$moveLinks(LevelChunk fromChunk, BlockPos from, LevelChunk toChunk, BlockPos to,
            boolean update, ServerShip ship, Rotation rotation, CallbackInfo ci) {
        WandLinkTransfer.moveBlock(fromChunk.getLevel(), from, to, rotation);
    }
}
