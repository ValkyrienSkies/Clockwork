package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrBlockInteraction;

@Mixin(ServerPlayerGameMode.class)
public abstract class MixinGateMining {
    @Shadow @Final protected ServerPlayer player;
    @Shadow private boolean isDestroyingBlock;
    @Shadow private boolean hasDelayedDestroy;
    @Shadow private BlockPos destroyPos;
    @Shadow private BlockPos delayedDestroyPos;

    @WrapMethod(method = "handleBlockBreakAction")
    private void clockwork$mineGateTarget(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face, int height, int sequence, Operation<Void> original) {
        GatemakyrBlockInteraction.mine(player, pos, face, action, () -> original.call(pos, action, face, height, sequence));
        GatemakyrBlockInteraction.retainMining(player, isDestroyingBlock ? destroyPos : null, hasDelayedDestroy ? delayedDestroyPos : null);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void clockwork$clearFinishedTargets(CallbackInfo ci) {
        GatemakyrBlockInteraction.retainMining(player, isDestroyingBlock ? destroyPos : null, hasDelayedDestroy ? delayedDestroyPos : null);
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void clockwork$validateFinalBreak(BlockPos pos, CallbackInfoReturnable<Boolean> result) {
        if (!GatemakyrBlockInteraction.canFinishMining(player, pos)) result.setReturnValue(false);
    }
}
