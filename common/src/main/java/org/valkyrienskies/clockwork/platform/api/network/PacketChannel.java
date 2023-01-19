package org.valkyrienskies.clockwork.platform.api.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.function.Function;

public interface PacketChannel {

    <T extends CWPacket> void registerPacket(Class<T> clazz, Function<FriendlyByteBuf, T> decode);

    void sendToNear(Level world, BlockPos pos, int range, S2CCWPacket message);

    void sendToServer(C2SCWPacket packet);

    void sendToClientsTracking(S2CCWPacket packet, Entity entity);

    void sendToClientsTrackingAndSelf(S2CCWPacket packet, ServerPlayer player);
}
