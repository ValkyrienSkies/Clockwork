package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class WandJumpPacket() : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this()
    override fun write(buffer: FriendlyByteBuf) {}
    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork { WanderwandServer.jump(context.sender) }
        context.setPacketHandled(true)
    }
}
