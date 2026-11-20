package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

interface GatemakyrChunkCache {
    fun setGateChunks(chunks: Set<Long>)
    fun keepsGateChunk(x: Int, z: Int): Boolean
}

class GatemakyrChunkWatchPacket(private val dimension: ResourceLocation, private val chunks: Set<Long>) : S2CCWPacket {
    override var player: Player? = null
    constructor(buf: FriendlyByteBuf) : this(buf.readResourceLocation(), buf.readLongArray(null, 200).toSet())
    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeResourceLocation(dimension)
        buffer.writeLongArray(chunks.toLongArray())
    }
    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            val level = Minecraft.getInstance().level ?: return@enqueueWork
            if (level.dimension().location() == dimension) (level.chunkSource as GatemakyrChunkCache).setGateChunks(chunks)
        }
        context.setPacketHandled(true)
    }
}
