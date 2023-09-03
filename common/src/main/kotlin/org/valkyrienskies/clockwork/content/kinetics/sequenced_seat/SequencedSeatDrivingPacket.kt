package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat

import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class SequencedSeatDrivingPacket : C2SCWPacket {
    private val seatId: Int
    private val pressedKeys: Set<InputKey>

    constructor(seatId: Int, pressedKeys: Set<InputKey>) {
        this.seatId = seatId
        this.pressedKeys = pressedKeys
    }

    constructor(buffer: FriendlyByteBuf) {
        seatId = buffer.readInt()
        pressedKeys = InputKey.fromInt(buffer.readInt())
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val level = context.sender.level
            val entity = level.getEntity(seatId)
            if (entity is SequencedSeatEntity && context.sender == entity.firstPassenger) {
                val be =
                    level.getBlockEntity(entity.blockPosition())
                (be as? SequencedSeatBlockEntity)?.updateInput(pressedKeys)
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeInt(seatId)
        buffer.writeInt(InputKey.asInt(pressedKeys))
    }
}