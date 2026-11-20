package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface GateServerPositionAccessor {
    @Accessor("awaitingPositionFromClient")
    Vec3 awaitingGateCorrection();
}
