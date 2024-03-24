package org.valkyrienskies.clockwork.content.logistics.heat

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class TemperatureSyncPacket: S2CCWPacket {
    val pos: BlockPos
    val temperature: Double
    val id: int

    constructor(shipId: Long, pocketId: Int) {
        this.ship = shipId
        this.id = pocketId
    }

    constructor(buffer: FriendlyByteBuf) {
        this.ship = buffer.readLong()
        this.id = buffer.readInt()
    }
    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            ClientAirPocketStorage.deleteAirPocket(ship, id)
        }
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeLong(ship)
        buffer.writeInt(id)
    }
}