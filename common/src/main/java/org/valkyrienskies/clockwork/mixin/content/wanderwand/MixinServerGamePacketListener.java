package org.valkyrienskies.clockwork.mixin.content.wanderwand;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandServer;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListener {
    @Shadow public ServerPlayer player;
    @Shadow private int aboveGroundTickCount;

    @Inject(method = "tick", at = @At("HEAD"))
    private void clockwork$allowRopeSuspension(CallbackInfo ci) {
        // A server-confirmed rope can legitimately suspend a non-flying player indefinitely.
        if (WanderwandServer.isGrappling(player)) aboveGroundTickCount = 0;
    }
}
