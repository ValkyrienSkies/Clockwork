package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import java.util.UUID

class GatemakyrEffectPacket(val dimension: ResourceLocation, val owner: UUID, val kind: Kind, val side: Int,
                          val startedAt: Long, val duration: Int = 0, val origin: Vec3 = Vec3.ZERO,
                          val target: GatemakyrTarget? = null, val partner: GatemakyrTarget? = null, val partnerStartedAt: Long = 0,
                          val crossings: List<Pair<Vec3, Vec3>> = emptyList()) : S2CCWPacket {
    enum class Kind { CHARGE, CANCEL, SHOT, RIFT, CLOSE, CLEAR, IMPACT }
    override var player: Player? = null
    constructor(buffer: FriendlyByteBuf) : this(buffer.readResourceLocation(), buffer.readUUID(), buffer.readEnum(Kind::class.java),
        buffer.readVarInt(), buffer.readLong(), buffer.readVarInt(), Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
        if (buffer.readBoolean()) GatemakyrTarget.read(buffer) else null,
        if (buffer.readBoolean()) GatemakyrTarget.read(buffer) else null, buffer.readVarLong(),
        List(buffer.readVarInt().also { require(it in 0..8) }) { buffer.point() to buffer.point() })

    fun flightLength(start: Vec3, end: Vec3): Double {
        var from = start
        var length = 0.0
        for ((entrance, exit) in crossings) {
            length += from.distanceTo(entrance)
            from = exit
        }
        return length + from.distanceTo(end)
    }

    internal fun flightSegments(end: Vec3, startProgress: Double, endProgress: Double, visit: (Int, Vec3, Vec3) -> Boolean) {
        val length = flightLength(origin, end)
        val start = length * startProgress.coerceIn(0.0, 1.0)
        val finish = length * endProgress.coerceIn(0.0, 1.0)
        var distance = 0.0
        var from = origin
        for (i in 0..crossings.size) {
            val to = crossings.getOrNull(i)?.first ?: end
            val segment = from.distanceTo(to)
            val lo = maxOf(0.0, start - distance)
            val hi = minOf(segment, finish - distance)
            if (hi > lo && !visit(i, from.lerp(to, lo / segment), from.lerp(to, hi / segment))) return
            distance += segment
            if (i == crossings.size || distance >= finish) return
            from = crossings[i].second
        }
    }

    fun flightPoint(start: Vec3, end: Vec3, progress: Double): Vec3 {
        if (crossings.isEmpty()) return start.lerp(end, progress)
        var remaining = flightLength(start, end) * progress
        var from = start
        for ((entrance, exit) in crossings) {
            val length = from.distanceTo(entrance)
            if (remaining < length) return from.lerp(entrance, remaining / length)
            remaining -= length
            from = exit
        }
        return from.lerp(end, (remaining / from.distanceTo(end).coerceAtLeast(1e-9)).coerceIn(0.0, 1.0))
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeResourceLocation(dimension); buffer.writeUUID(owner); buffer.writeEnum(kind); buffer.writeVarInt(side)
        buffer.writeLong(startedAt); buffer.writeVarInt(duration)
        buffer.writeDouble(origin.x); buffer.writeDouble(origin.y); buffer.writeDouble(origin.z)
        buffer.writeBoolean(target != null); target?.write(buffer)
        buffer.writeBoolean(partner != null); partner?.write(buffer); buffer.writeVarLong(partnerStartedAt)
        buffer.writeVarInt(crossings.size)
        for ((entrance, exit) in crossings) for (point in listOf(entrance, exit)) {
            buffer.writeDouble(point.x)
            buffer.writeDouble(point.y)
            buffer.writeDouble(point.z)
        }
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { GatemakyrClient.accept(this) }
        context.setPacketHandled(true)
    }
}

private fun FriendlyByteBuf.point() = Vec3(readDouble(), readDouble(), readDouble())
