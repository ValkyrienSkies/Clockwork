package org.valkyrienskies.clockwork.forge;

import net.minecraft.network.FriendlyByteBuf;
import org.valkyrienskies.clockwork.ClockworkPackets;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.GravitronGrabPacket;
import org.valkyrienskies.clockwork.platform.SharedValues;
import org.valkyrienskies.clockwork.platform.api.network.CWPacket;

import java.util.function.Function;

public enum ForgeClockworkPackets  {
    GRAVITRON_GRAB_PACKET(GravitronGrabPacket.class, GravitronGrabPacket::new);

    <T extends CWPacket> ForgeClockworkPackets(final Class<T> type, final Function<FriendlyByteBuf, T> factory) {
        SharedValues.getPacketChannel().registerPacket(type, factory);
    }

    public static void init() {
    }
}
