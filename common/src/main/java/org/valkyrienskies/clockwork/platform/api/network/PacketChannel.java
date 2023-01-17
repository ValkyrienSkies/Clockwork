package org.valkyrienskies.clockwork.platform.api.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public interface PacketChannel {
    void sendToNear(Level world, BlockPos pos, int range, S2CCWPacket message);

    void sendToServer(C2SCWPacket packet);

    void sendToClientsTracking(S2CCWPacket packet, Entity entity);

    void sendToClientsTrackingAndSelf(S2CCWPacket packet, ServerPlayer player);
}
