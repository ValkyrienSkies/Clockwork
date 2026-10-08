package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import java.util.UUID

class WandRopeRidePacket(val link: UUID?) : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this(if (buffer.readBoolean()) buffer.readUUID() else null)
    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBoolean(link != null)
        link?.let { buffer.writeUUID(it) }
    }
    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork { WandRopeRiding.use(context.sender, link) }
        context.setPacketHandled(true)
    }
}
