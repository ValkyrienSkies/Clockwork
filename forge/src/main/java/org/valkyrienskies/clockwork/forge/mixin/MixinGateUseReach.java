package org.valkyrienskies.clockwork.forge.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrEntityInteraction;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrBlockInteraction;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinGateUseReach {
    @Shadow public ServerPlayer player;

    @ModifyExpressionValue(method = "handleInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;canReachRaw(Lnet/minecraft/world/entity/Entity;D)Z", remap = false))
    private boolean clockwork$portalEntityReach(boolean original, ServerboundInteractPacket packet) {
        return original || GatemakyrEntityInteraction.reached(player, packet);
    }

    @ModifyExpressionValue(method = "handleUseItemOn", at = {
        @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;canReachRaw(Lnet/minecraft/core/BlockPos;D)Z", remap = false),
        @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;canReach(Lnet/minecraft/core/BlockPos;D)Z", remap = false)
    })
    private boolean clockwork$gateReach(boolean original) {
        return original || GatemakyrBlockInteraction.reached();
    }
}
