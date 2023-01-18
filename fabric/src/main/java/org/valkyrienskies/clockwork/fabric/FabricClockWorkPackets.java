package org.valkyrienskies.clockwork.fabric;

import me.pepperbell.simplenetworking.SimpleChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.valkyrienskies.clockwork.ClockWorkPackets;
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket;
import org.valkyrienskies.clockwork.platform.api.network.CWPacket;
import org.valkyrienskies.clockwork.platform.api.network.PacketChannel;
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket;

import java.util.function.Function;

public class FabricClockWorkPackets implements PacketChannel {
    public static FabricClockWorkPackets INSTANCE = new FabricClockWorkPackets();

    @Override
    public <T extends CWPacket> void registerPacket(Class<T> clazz, Function<FriendlyByteBuf, T> decode) {

    }

    @Override
    public void sendToNear(Level world, BlockPos pos, int range, S2CCWPacket message) {

    }

    @Override
    public void sendToServer(C2SCWPacket packet) {

    }

    @Override
    public void sendToClientsTracking(S2CCWPacket packet, Entity entity) {

    }

    @Override
    public void sendToClientsTrackingAndSelf(S2CCWPacket packet, ServerPlayer player) {

    }
}
