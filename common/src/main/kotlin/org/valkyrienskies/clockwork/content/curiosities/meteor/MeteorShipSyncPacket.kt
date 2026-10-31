package org.valkyrienskies.clockwork.content.curiosities.meteor

import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class MeteorShipSyncPacket(
    private val shipId: Long,
    private val active: Boolean
) : S2CCWPacket {
    override var player: Player? = null

    constructor(buffer: FriendlyByteBuf) : this(buffer.readLong(), buffer.readBoolean())

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null) {
                MeteorRenderer.setMeteorState(shipId, active)
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeLong(shipId)
        buffer.writeBoolean(active)
    }
}
