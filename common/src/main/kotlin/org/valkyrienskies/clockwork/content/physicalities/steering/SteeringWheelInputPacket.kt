package org.valkyrienskies.clockwork.content.physicalities.steering

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class SteeringWheelInputPacket(
    private val pos: BlockPos, private val direction: Int, private val throttle: Int = 0,
    private val braking: Boolean = false, private val throttleAdjustment: Int = 0,
) : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this(
        buffer.readBlockPos(), buffer.readByte().toInt(), buffer.readByte().toInt(),
        buffer.isReadable && buffer.readBoolean(), if (buffer.isReadable) buffer.readByte().toInt() else 0
    )

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        buffer.writeByte(direction)
        buffer.writeByte(throttle)
        buffer.writeBoolean(braking)
        buffer.writeByte(throttleAdjustment)
    }
    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val level = context.sender.level()
            if (direction in -1..2 && throttle in -1..1 && throttleAdjustment in -1..1 && level.hasChunkAt(pos))
                (level.getBlockEntity(pos) as? SteeringWheelBlockEntity)
                    ?.control(context.sender, direction, throttle, braking, throttleAdjustment)
        }
        context.setPacketHandled(true)
    }
}
