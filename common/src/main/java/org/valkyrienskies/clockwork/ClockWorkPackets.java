package org.valkyrienskies.clockwork;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatDrivingPacket;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.UpdateSeatRulesPacket;
import org.valkyrienskies.clockwork.content.physicalities.wing.BlockEntityColorPacket;
import org.valkyrienskies.clockwork.content.propulsion.afterblazer.AfterblazerStatusPacket;
import org.valkyrienskies.clockwork.platform.SharedValues;
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket;
import org.valkyrienskies.clockwork.platform.api.network.CWPacket;
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket;

import java.util.function.Function;

public enum ClockWorkPackets {

    // Client to Server
    UPDATE_SEAT_RULES(UpdateSeatRulesPacket.class, UpdateSeatRulesPacket::new),
    SEQUENCER_SEAT_DRIVING(SequencedSeatDrivingPacket.class, SequencedSeatDrivingPacket::new),

    // Server to Client

    COLORBLOCKENTITY(BlockEntityColorPacket.class, BlockEntityColorPacket::new),

    AFTERBLAZERSTATUS(AfterblazerStatusPacket.class, AfterblazerStatusPacket::new)
    ;

    <T extends CWPacket> ClockWorkPackets(Class<T> type, Function<FriendlyByteBuf, T> factory) {
        SharedValues.getPacketChannel().registerPacket(type, factory);
    }

    // Force the class to load
    public static void init() {
    }

    public static void sendToNear(Level world, BlockPos pos, int range, S2CCWPacket message) {
        SharedValues.getPacketChannel().sendToNear(world, pos, range, message);
    }

    public static void sendToServer(C2SCWPacket packet) {
        SharedValues.getPacketChannel().sendToServer(packet);
    }

    public static void sendToClientsTracking(S2CCWPacket packet, Entity entity) {
        SharedValues.getPacketChannel().sendToClientsTracking(packet, entity);
    }

    public static void sendToClientsTrackingAndSelf(S2CCWPacket packet, ServerPlayer player) {
        SharedValues.getPacketChannel().sendToClientsTrackingAndSelf(packet, player);
    }
}