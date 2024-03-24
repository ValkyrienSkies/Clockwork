package org.valkyrienskies.clockwork.content.logistics.heat

import net.minecraft.client.Minecraft
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
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level!!.getBlockEntity(
                    pos
                ) is IHeatable
            ) {
                val ce =
                    Minecraft.getInstance().level!!.getBlockEntity(pos) as IHeatable?
                ce?.temperature = this.temperature
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        buffer.writeDouble(temperature)
        buffer.writeInt(id)
    }
}

