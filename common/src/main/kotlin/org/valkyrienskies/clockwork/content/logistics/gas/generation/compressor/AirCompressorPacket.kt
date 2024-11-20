package org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class AirCompressorPacket: S2CCWPacket {


    private val isOn: Boolean
    private val pos: BlockPos



    constructor(buffer: FriendlyByteBuf) {
        isOn = buffer.readBoolean()
        pos = buffer.readBlockPos()

    }

    constructor(newIsOn: Boolean, newPos: BlockPos) {
        isOn = newIsOn
        pos = newPos
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {

            val be = Minecraft.getInstance().level?.getBlockEntity(pos) as AirCompressorBlockEntity
            be.isOn = isOn

        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBoolean(isOn)
        buffer.writeBlockPos(pos)

    }
}