package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrBlockInteraction;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinGateUse {
    @Shadow public ServerPlayer player;

    @WrapMethod(method = "handleUseItemOn")
    private void clockwork$useGateTarget(ServerboundUseItemOnPacket packet, Operation<Void> original) {
        GatemakyrBlockInteraction.use(player, packet.getHitResult(), () -> original.call(packet));
    }
}
