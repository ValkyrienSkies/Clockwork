package org.valkyrienskies.clockwork.platform.api.network;

import net.minecraft.network.FriendlyByteBuf;

public interface CWPacket {

    void write(FriendlyByteBuf buffer);

}
