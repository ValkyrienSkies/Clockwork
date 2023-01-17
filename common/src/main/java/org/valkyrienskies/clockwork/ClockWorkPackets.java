package org.valkyrienskies.clockwork;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueEffectPacket;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueRemovalPacket;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueSelectionPacket;

import java.util.function.Function;

import com.simibubi.create.foundation.networking.SimplePacketBase.NetworkDirection;
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket;
import org.valkyrienskies.clockwork.platform.api.network.CWPacket;
import org.valkyrienskies.clockwork.platform.api.network.PacketChannel;
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket;

import static com.simibubi.create.foundation.networking.SimplePacketBase.NetworkDirection.PLAY_TO_CLIENT;
import static com.simibubi.create.foundation.networking.SimplePacketBase.NetworkDirection.PLAY_TO_SERVER;

public enum ClockWorkPackets {

    // Client to Server
    BLUPERGLUE_IN_AREA(BluperGlueSelectionPacket.class, BluperGlueSelectionPacket::new, PLAY_TO_SERVER),
    BLUPERGLUE_REMOVED(BluperGlueRemovalPacket.class, BluperGlueRemovalPacket::new, PLAY_TO_SERVER),

    // Server to Client
    BLUPERGLUE_EFFECT(BluperGlueEffectPacket.class, BluperGlueEffectPacket::new, PLAY_TO_CLIENT),
    ;


    // versioning
    public static final ResourceLocation CHANNEL_NAME = ClockWorkMod.asResource("main");
    public static final int NETWORK_VERSION = 1;
    public static final String NETWORK_VERSION_STR = String.valueOf(NETWORK_VERSION);

    <T extends CWPacket> ClockWorkPackets(Class<T> type, Function<FriendlyByteBuf, T> factory,
                                          NetworkDirection direction) {
        // TODO load packets
    }

    public static void registerPackets() {

    }

    public static void sendToNear(Level world, BlockPos pos, int range, S2CCWPacket message) {

    }

    public static void sendToServer(C2SCWPacket packet) {

    }

    public static void sendToClientsTracking(S2CCWPacket packet, Entity entity) {

    }

    public static void sendToClientsTrackingAndSelf(S2CCWPacket packet, ServerPlayer player) {

    }
}