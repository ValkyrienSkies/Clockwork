package org.valkyrienskies.clockwork.content.curiosities.magnet

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.joml.Vector3d
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class MagneticFieldPacket(val dimension: ResourceLocation, val sources: List<Source>, val crowded: Boolean, val coresEnabled: Boolean) : S2CCWPacket {
    data class Source(val ship: Long, val position: BlockPos, val direction: Direction, val strength: Float,
                      val moment: Vector3d? = null) {
        fun normal(): Vector3d = moment?.let { Vector3d(it).normalize() }
            ?: Vector3d(direction.stepX.toDouble(), direction.stepY.toDouble(), direction.stepZ.toDouble())

        internal fun poles(): List<MagneticField.Pole> {
            val center = Vector3d(position.x + .5, position.y + .5, position.z + .5)
            return if (moment == null) MagneticField.poles(center, normal(), strength.toDouble())
            else MagneticField.corePoles(center, moment)
        }
    }
    override var player: Player? = null

    constructor(buffer: FriendlyByteBuf) : this(buffer.readResourceLocation(), readSources(buffer), buffer.readBoolean(), buffer.readBoolean())

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeResourceLocation(dimension)
        buffer.writeVarInt(sources.size)
        for (source in sources) {
            buffer.writeLong(source.ship)
            buffer.writeBlockPos(source.position)
            buffer.writeEnum(source.direction)
            buffer.writeFloat(source.strength)
            buffer.writeBoolean(source.moment != null)
            source.moment?.let { buffer.writeFloat(it.x.toFloat()); buffer.writeFloat(it.y.toFloat()); buffer.writeFloat(it.z.toFloat()) }
        }
        buffer.writeBoolean(crowded)
        buffer.writeBoolean(coresEnabled)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { MagneticFieldView.accept(this) }
        context.setPacketHandled(true)
    }

    companion object {
        const val MAX_SOURCES = 1024
        const val MAX_POLES = 4096
        private fun readSources(buffer: FriendlyByteBuf): List<Source> {
            val count = buffer.readVarInt()
            require(count in 0..MAX_SOURCES)
            return List(count) {
                val ship = buffer.readLong()
                val position = buffer.readBlockPos()
                val direction = buffer.readEnum(Direction::class.java)
                val strength = buffer.readFloat()
                val moment = if (buffer.readBoolean()) Vector3d(buffer.readFloat().toDouble(), buffer.readFloat().toDouble(), buffer.readFloat().toDouble()) else null
                require(strength.isFinite() && (moment == null || moment.isFinite && moment.lengthSquared() > 0.0))
                Source(ship, position, direction, strength, moment)
            }
        }
    }
}
