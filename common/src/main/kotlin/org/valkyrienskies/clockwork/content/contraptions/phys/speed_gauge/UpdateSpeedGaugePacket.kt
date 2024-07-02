package org.valkyrienskies.clockwork.content.contraptions.phys.speed_gauge

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class UpdateSpeedGaugePacket : C2SCWPacket {
    private val triggerSpeed: Double
    private val moreThan: Boolean
    private val pos: BlockPos

    constructor(buffer: FriendlyByteBuf) {
        triggerSpeed = buffer.readDouble()
        moreThan = buffer.readBoolean()
        pos = buffer.readBlockPos()
    }

    constructor(newHeight: Double, newThan: Boolean, newPos: BlockPos) {
        triggerSpeed = newHeight
        moreThan = newThan
        pos = newPos
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val be = context.sender.level.getBlockEntity(pos) as SpeedGaugeBlockEntity?
            if (be != null && be.canPlayerUse(context.sender)) {
                be.triggerSpeed = triggerSpeed
                be.moreThan = moreThan
                be.notifyUpdate()
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeDouble(triggerSpeed)
        buffer.writeBoolean(moreThan)
        buffer.writeBlockPos(pos)
    }
}
