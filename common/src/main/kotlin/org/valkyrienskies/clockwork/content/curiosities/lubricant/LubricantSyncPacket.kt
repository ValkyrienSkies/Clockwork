package org.valkyrienskies.clockwork.content.curiosities.lubricant

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class LubricantSyncPacket(val dimension: ResourceLocation, val blocks: LongArray, val areas: LongArray) : S2CCWPacket {
    override var player: Player? = null
    constructor(buffer: FriendlyByteBuf) : this(buffer.readResourceLocation(), buffer.readLongArray(null, LubricantData.MAX_VISIBLE),
        buffer.readLongArray(null, LubricantData.MAX_VISIBLE))
    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeResourceLocation(dimension)
        buffer.writeLongArray(blocks)
        buffer.writeLongArray(areas)
    }
    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { if (blocks.size == areas.size) LubricantClient.accept(dimension, blocks, areas) }
        context.setPacketHandled(true)
    }
}
