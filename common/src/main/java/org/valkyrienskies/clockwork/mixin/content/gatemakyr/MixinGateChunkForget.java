package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrChunkCache;

@Mixin(ClientPacketListener.class)
public abstract class MixinGateChunkForget {
    @Shadow private ClientLevel level;
    @Inject(method = "handleForgetLevelChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientChunkCache;drop(II)V"), cancellable = true)
    private void clockwork$retain(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        if (((GatemakyrChunkCache) level.getChunkSource()).keepsGateChunk(packet.getX(), packet.getZ())) ci.cancel();
    }
}
