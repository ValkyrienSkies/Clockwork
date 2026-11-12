package org.valkyrienskies.clockwork.mixin.content.schematics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.schematics.VdexConstruction;

@Mixin(ServerPlayerGameMode.class)
public class MixinConstructionMining {
    @Shadow protected ServerLevel level;
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void clockwork$holdBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (VdexConstruction.locked(level, pos)) cir.setReturnValue(false);
    }
}
