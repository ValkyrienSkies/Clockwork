package org.valkyrienskies.clockwork;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.UpdateAltMeterPacket;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluper.BluperGluePacket;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluper.BluperHelperPacket;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatDrivingPacket;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.UpdateSeatRulesPacket;
import org.valkyrienskies.clockwork.content.physicalities.wing.BlockEntityColorPacket;
import org.valkyrienskies.clockwork.platform.SharedValues;
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket;
import org.valkyrienskies.clockwork.platform.api.network.CWPacket;
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket;
import org.valkyrienskies.clockwork.util.blocktype.SyncableStoragePacket;

import java.util.function.Function;

public enum ClockworkPackets {

    // Client to Server
    UPDATE_SEAT_RULES(UpdateSeatRulesPacket.class, UpdateSeatRulesPacket::new),
    SEQUENCER_SEAT_DRIVING(SequencedSeatDrivingPacket.class, SequencedSeatDrivingPacket::new),


    // Server to Client

    COLORBLOCKENTITY(BlockEntityColorPacket.class, BlockEntityColorPacket::new),
    SYNCABLESTORAGE(SyncableStoragePacket.class, SyncableStoragePacket::new),


    //Bluper
    BLURPLESELECTOR(BluperGluePacket.class, BluperGluePacket::new),
    BLUPER_HELPER(BluperHelperPacket.class, BluperHelperPacket::new),


    UPDATE_ALT_METER(UpdateAltMeterPacket.class, UpdateAltMeterPacket::new),

    ;

    <T extends CWPacket> ClockworkPackets(final Class<T> type, final Function<FriendlyByteBuf, T> factory) {
        SharedValues.INSTANCE.getPacketChannel().registerPacket(type, factory);
    }

    // Force the class to load
    public static void init() {
    }

    public static void sendToNear(final Level world, final BlockPos pos, final int range, final S2CCWPacket message) {
        SharedValues.INSTANCE.getPacketChannel().sendToNear(world, pos, range, message);
    }

    public static void sendToServer(final C2SCWPacket packet) {
        SharedValues.INSTANCE.getPacketChannel().sendToServer(packet);
    }

    public static void sendToClientsTracking(final S2CCWPacket packet, final Entity entity) {
        SharedValues.INSTANCE.getPacketChannel().sendToClientsTracking(packet, entity);
    }

    public static void sendToClientsTrackingAndSelf(final S2CCWPacket packet, final ServerPlayer player) {
        SharedValues.INSTANCE.getPacketChannel().sendToClientsTrackingAndSelf(packet, player);
    }
}