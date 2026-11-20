package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrPlayerCrossing;

@Mixin(ClientPacketListener.class)
public abstract class MixinGatePlayerPosition {
    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void clockwork$finishTraversal(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        GatemakyrPlayerCrossing.clear();
    }
}
