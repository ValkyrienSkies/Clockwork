package org.valkyrienskies.clockwork;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.UpdateSeatRulesPacket;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueEffectPacket;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueRemovalPacket;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueSelectionPacket;

import java.util.function.Function;

import org.valkyrienskies.clockwork.platform.SharedValues;
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket;
import org.valkyrienskies.clockwork.platform.api.network.CWPacket;
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket;

public enum ClockWorkPackets {

    // Client to Server
    BLUPERGLUE_IN_AREA(BluperGlueSelectionPacket.class, BluperGlueSelectionPacket::new),
    BLUPERGLUE_REMOVED(BluperGlueRemovalPacket.class, BluperGlueRemovalPacket::new),
    UPDATE_SEAT_RULES(UpdateSeatRulesPacket.class, UpdateSeatRulesPacket::new),

    // Server to Client
    BLUPERGLUE_EFFECT(BluperGlueEffectPacket.class, BluperGlueEffectPacket::new),
    ;

    // Force the class to load
    public static void init() {}

    <T extends CWPacket> ClockWorkPackets(Class<T> type, Function<FriendlyByteBuf, T> factory) {
        SharedValues.getPacketChannel().registerPacket(type, factory);
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