package org.valkyrienskies.clockwork.content.physicalities.steering

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class SteeringWheelInputPacket(private val pos: BlockPos, private val direction: Int) : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this(buffer.readBlockPos(), buffer.readByte().toInt())
    override fun write(buffer: FriendlyByteBuf) { buffer.writeBlockPos(pos); buffer.writeByte(direction) }
    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val level = context.sender.level()
            if (direction in -1..2 && level.hasChunkAt(pos))
                (level.getBlockEntity(pos) as? SteeringWheelBlockEntity)?.control(context.sender, direction)
        }
        context.setPacketHandled(true)
    }
}
