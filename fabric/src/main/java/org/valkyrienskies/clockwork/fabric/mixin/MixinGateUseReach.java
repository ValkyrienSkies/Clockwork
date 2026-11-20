package org.valkyrienskies.clockwork.fabric.mixin;

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

    @ModifyExpressionValue(method = "handleInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
    private double clockwork$portalEntityReach(double original, ServerboundInteractPacket packet) {
        return GatemakyrEntityInteraction.reached(player, packet) ? 0.0 : original;
    }

    @ModifyExpressionValue(method = "handleUseItemOn", at = {
        @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"),
        @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;distanceToSqr(DDD)D")
    })
    private double clockwork$gateReach(double original) {
        return GatemakyrBlockInteraction.distanceSquared(original);
    }
}
