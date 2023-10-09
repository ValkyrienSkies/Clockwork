package org.valkyrienskies.clockwork.content.contraptions.phys.altmeter

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class UpdateAltMeterPacket : C2SCWPacket {
    private val triggerHeight: Double
    private val pos: BlockPos

    constructor(buffer: FriendlyByteBuf) {
        triggerHeight = buffer.readDouble()
        pos = buffer.readBlockPos()
    }

    constructor(newHeight: Double, newPos: BlockPos) {
        triggerHeight = newHeight
        pos = newPos
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val be = context.sender.level.getBlockEntity(pos) as AltMeterBlockEntity?
            if (be != null && be.canPlayerUse(context.sender)) {
                be.triggerHeight = triggerHeight
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeDouble(triggerHeight)
        buffer.writeBlockPos(pos)
    }
}
