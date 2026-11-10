package org.valkyrienskies.clockwork.content.curiosities.spring

import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotEntity
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class SpringAdjustPacket(private val entity: Int, private val direction: Int) : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this(buffer.readVarInt(), buffer.readByte().toInt())
    override fun write(buffer: FriendlyByteBuf) { buffer.writeVarInt(entity); buffer.writeByte(direction) }
    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val player = context.sender
            val endpoint = player.level().getEntity(entity) as? RopeKnotEntity
            if (endpoint != null) SpringControls.adjust(player, endpoint, direction)
        }
        context.setPacketHandled(true)
    }
}
