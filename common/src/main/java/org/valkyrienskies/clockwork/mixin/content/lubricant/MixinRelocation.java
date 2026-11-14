package org.valkyrienskies.clockwork.mixin.content.lubricant;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantData;

@Mixin(targets = "org.valkyrienskies.mod.util.RelocationUtilKt", remap = false)
public class MixinRelocation {
    @Inject(method = "updateBlock", at = @At("HEAD"))
    private static void clockwork$moveCoating(Level level, BlockPos from, BlockPos to, BlockState state, CallbackInfo ci) {
        LubricantData.moveBlock(level, from, to);
    }
}
