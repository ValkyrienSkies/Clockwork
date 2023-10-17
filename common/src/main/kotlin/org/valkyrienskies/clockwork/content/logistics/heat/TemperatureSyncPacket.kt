package org.valkyrienskies.clockwork.content.logistics.heat

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class TemperatureSyncPacket: S2CCWPacket {
    val pos: BlockPos
    val temperature: Double
    val id: Int

    constructor(pos: BlockPos, temperature: Double, id: Int) {
        this.pos = pos
        this.temperature = temperature
        this.id = id
    }

    constructor(buffer: FriendlyByteBuf) {
        this.pos = buffer.readBlockPos()
        this.temperature = buffer.readDouble()
        this.id = buffer.readInt()
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            println("TODO: Handle a TemperatureSyncPacket")
        }
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        buffer.writeDouble(temperature)
        buffer.writeInt(id)
    }
}

