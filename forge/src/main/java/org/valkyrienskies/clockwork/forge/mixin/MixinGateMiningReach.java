package org.valkyrienskies.clockwork.forge.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrBlockInteraction;

@Mixin(ServerPlayerGameMode.class)
public abstract class MixinGateMiningReach {
    @ModifyExpressionValue(method = "handleBlockBreakAction", at = {
        @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;canReachRaw(Lnet/minecraft/core/BlockPos;D)Z", remap = false),
        @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;canReach(Lnet/minecraft/core/BlockPos;D)Z", remap = false)
    })
    private boolean clockwork$gateReach(boolean original) {
        return original || GatemakyrBlockInteraction.reached();
    }
}
