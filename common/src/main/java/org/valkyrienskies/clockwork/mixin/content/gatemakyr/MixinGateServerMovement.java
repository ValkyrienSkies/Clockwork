package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrServer;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinGateServerMovement {
    @Shadow public ServerPlayer player;
    @Shadow private Vec3 awaitingPositionFromClient;

    @WrapMethod(method = "handleMovePlayer")
    private void clockwork$observeCrossing(ServerboundMovePlayerPacket packet, Operation<Void> move) {
        if (!player.serverLevel().getServer().isSameThread()) {
            move.call(packet);
            return;
        }
        Vec3 eye = player.getEyePosition();
        move.call(packet);
        if (awaitingPositionFromClient == null && !eye.equals(player.getEyePosition())) GatemakyrServer.observeMove(player, eye);
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void clockwork$forgetCrossing(CallbackInfo ci) {
        GatemakyrServer.forgetMove(player);
    }
}
