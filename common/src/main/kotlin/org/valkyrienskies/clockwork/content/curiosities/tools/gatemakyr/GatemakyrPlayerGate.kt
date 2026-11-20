package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider
import java.util.UUID
import kotlin.math.abs

internal class GatemakyrPlayerGate(val packet: GatemakyrEffectPacket, val partner: GatemakyrEffectPacket,
                                   val passage: GatemakyrPassage) {
    val source = requireNotNull(packet.target)
    val destination = requireNotNull(partner.target)

    fun matches(other: GatemakyrPlayerGate) = packet.owner == other.packet.owner && packet.side == other.packet.side &&
        packet.startedAt == other.packet.startedAt &&
        partner.startedAt == other.partner.startedAt && source == other.source && destination == other.destination

    fun admits(box: AABB, center: Vec3): Boolean {
        val inset = GatemakyrPassage.inset(box.move(center.subtract(box.center)), passage.entrance, source)
        return inset != null && inset.lengthSqr() <= 1e-6
    }

    fun refreshSupport(player: Player) {
        val drag = (player as IEntityDraggingInformationProvider).draggingInformation
        val ship = drag.lastShipStoodOn
        if (ship != null) {
            if (GatemakyrShipPortals.retainDeck(player, ship)) {
                val motion = drag.addedMovementLastTick
                val carried = passage.direction(Vec3(motion.x(), motion.y(), motion.z()))
                drag.addedMovementLastTick = Vector3d(carried.x, carried.y, carried.z)
                player.setOnGround(true)
                return
            }
            drag.lastShipStoodOn = null
            drag.addedMovementLastTick = Vector3d()
            drag.addedYawRotLastTick = 0.0
        }
        player.setOnGround(supportedAfterCrossing(player))
    }

    private fun supportedAfterCrossing(player: Player): Boolean {
        if (!player.onGround() || passage.direction(Vec3(0.0, 1.0, 0.0)).y < 1.0 - 1e-6) return false
        val box = player.boundingBox
        val feet = AABB(box.minX + 1e-7, box.minY - 1e-5, box.minZ + 1e-7, box.maxX - 1e-7, box.minY, box.maxZ - 1e-7)
        return !player.level().noCollision(player, feet)
    }

    fun exitContact(point: Vec3): Vec3 {
        val frame = passage.entrance
        val normal = frame.normal.normalize()
        return passage.point(point.subtract(normal.scale(point.subtract(frame.position).dot(normal))))
    }

    fun crossingPoint(previous: GatemakyrPlayerGate, from: Vec3, to: Vec3, fraction: Double): Vec3 {
        val a = previous.passage.entrance
        val b = passage.entrance
        val start = from.subtract(a.position)
        val end = to.subtract(b.position)
        fun component(old: Vec3, now: Vec3) = (1 - fraction) * start.dot(old) / old.lengthSqr() +
            fraction * end.dot(now) / now.lengthSqr()
        return b.position.add(b.u.scale(component(a.u, b.u))).add(b.v.scale(component(a.v, b.v)))
    }

    fun crossing(previous: GatemakyrPlayerGate, from: Vec3, to: Vec3): Double? {
        val a = previous.passage.entrance
        val b = passage.entrance
        val z0 = from.subtract(a.position).dot(a.normal.normalize())
        val z1 = to.subtract(b.position).dot(b.normal.normalize())
        if (z0 == 0.0 || z0 * z1 > 0.0 || (z0 < 0.0 && (source.surface || destination.surface))) return null
        val fraction = z0 / (z0 - z1)
        val start = from.subtract(a.position)
        val end = to.subtract(b.position)
        fun coordinate(oldAxis: Vec3, axis: Vec3): Double {
            val before = start.dot(oldAxis) / oldAxis.lengthSqr()
            return before + fraction * (end.dot(axis) / axis.lengthSqr() - before)
        }
        if (abs(coordinate(a.u, b.u)) > source.width * 0.5 ||
            abs(coordinate(a.v, b.v)) > source.height * 0.5) return null
        return fraction
    }

    companion object {
        fun eligible(player: Player) = GatemakyrSurfaceCollision.eligible(player)

        fun <T> snapshot(level: Level, rifts: Map<Pair<UUID, Int>, T>, render: Boolean, packetOf: (T) -> GatemakyrEffectPacket): Map<Pair<UUID, Int>, GatemakyrPlayerGate> {
            if (rifts.size < 2) return emptyMap()
            val result = HashMap<Pair<UUID, Int>, GatemakyrPlayerGate>()
            for ((key, value) in rifts) {
                val packet = packetOf(value)
                val partner = rifts[key.first to (1 - key.second)]?.let(packetOf) ?: continue
                val source = packet.target?.frame(level, render) ?: continue
                val exit = partner.target?.frame(level, render) ?: continue
                result[key] = GatemakyrPlayerGate(packet, partner, GatemakyrPassage(source, exit))
            }
            return result
        }
    }
}
