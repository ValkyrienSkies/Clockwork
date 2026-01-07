package org.valkyrienskies.clockwork.content.curiosities.altmeter

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.PressurePlateBlock
import org.valkyrienskies.clockwork.content.curiosities.altmeter.AltMeterBlockEntity.AltMeterDirection
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class UpdateAltMeterPacket : C2SCWPacket {
    private val triggerHeight: Int
    private val triggerSensitivity: Int
    private val triggerDirection: AltMeterDirection
    private val pos: BlockPos

    constructor(buffer: FriendlyByteBuf) {
        triggerHeight = buffer.readInt()
        triggerSensitivity = buffer.readInt()
        triggerDirection = enumValueOf<AltMeterDirection>(buffer.readComponent().string)
        pos = buffer.readBlockPos()
    }

    constructor(newHeight: Int, newSensitivity: Int, newDirection: AltMeterDirection, newPos: BlockPos) {
        triggerHeight = newHeight
        triggerSensitivity = newSensitivity
        triggerDirection = newDirection
        pos = newPos
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val be = context.sender.level().getBlockEntity(pos) as AltMeterBlockEntity?
            if (be != null && be.canPlayerUse(context.sender)) {
                be.triggerHeight = triggerHeight
                be.triggerSensitivity = triggerSensitivity
                be.triggerDirection = triggerDirection
                be.notifyUpdate()
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeInt(triggerHeight)
        buffer.writeInt(triggerSensitivity)
        buffer.writeComponent(Component.literal(triggerDirection.name))
        buffer.writeBlockPos(pos)
    }
}
