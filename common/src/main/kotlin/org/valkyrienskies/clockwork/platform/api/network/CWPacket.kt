package org.valkyrienskies.clockwork.platform.api.network

import net.minecraft.network.FriendlyByteBuf

interface CWPacket {
    fun write(buffer: FriendlyByteBuf)
}
