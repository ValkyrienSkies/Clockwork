package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrSounds;

@Mixin(ClientPacketListener.class)
public abstract class MixinGateSoundPackets {
    @WrapMethod(method = "handleSoundEvent")
    private void clockwork$playNetworkSound(ClientboundSoundPacket packet, Operation<Void> original) {
        GatemakyrSounds.fromNetwork(() -> original.call(packet));
    }

    @WrapMethod(method = "handleSoundEntityEvent")
    private void clockwork$playNetworkEntitySound(ClientboundSoundEntityPacket packet, Operation<Void> original) {
        GatemakyrSounds.fromNetwork(() -> original.call(packet));
    }
}
