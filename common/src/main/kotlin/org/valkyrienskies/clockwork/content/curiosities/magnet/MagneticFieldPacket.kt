package org.valkyrienskies.clockwork.content.curiosities.magnet

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class MagneticFieldPacket(val dimension: ResourceLocation, val sources: List<Source>, val crowded: Boolean) : S2CCWPacket {
    data class Source(val ship: Long, val position: BlockPos, val direction: Direction, val strength: Float)
    override var player: Player? = null

    constructor(buffer: FriendlyByteBuf) : this(buffer.readResourceLocation(), readSources(buffer), buffer.readBoolean())

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeResourceLocation(dimension)
        buffer.writeVarInt(sources.size)
        for (source in sources) {
            buffer.writeLong(source.ship)
            buffer.writeBlockPos(source.position)
            buffer.writeEnum(source.direction)
            buffer.writeFloat(source.strength)
        }
        buffer.writeBoolean(crowded)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { MagneticFieldView.accept(this) }
        context.setPacketHandled(true)
    }

    companion object {
        const val MAX_SOURCES = 512
        private fun readSources(buffer: FriendlyByteBuf): List<Source> {
            val count = buffer.readVarInt()
            require(count in 0..MAX_SOURCES)
            return List(count) { Source(buffer.readLong(), buffer.readBlockPos(), buffer.readEnum(Direction::class.java), buffer.readFloat()) }
        }
    }
}
