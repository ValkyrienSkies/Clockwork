package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrHit
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrServer
import java.util.UUID
import kotlin.math.abs
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrPassage
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrPlayerGate
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrRaycast

internal class GravitronGatePath(val gate: GatemakyrPlayerGate, val apparentAnchor: Vec3, val contact: Vec3, val bent: Boolean = false) {
    internal data class Reference(val owner: UUID, val side: Int, val entranceTime: Long,
                                  val exitTime: Long, val localAnchor: Vec3) {
        constructor(gate: GatemakyrPlayerGate, anchor: Vec3) : this(gate.packet.owner, gate.packet.side,
            gate.packet.startedAt, gate.partner.startedAt, anchor)
        fun matches(gate: GatemakyrPlayerGate) = gate.packet.owner == owner && gate.packet.side == side &&
            gate.packet.startedAt == entranceTime && gate.partner.startedAt == exitTime
    }

    companion object {
        private fun fraction(gate: GatemakyrPlayerGate, eye: Vec3, end: Vec3) = GatemakyrRaycast.intersection(
            gate.passage.entrance, gate.source, gate.source.surface || gate.destination.surface, eye, end.subtract(eye))

        fun unfolded(gate: GatemakyrPlayerGate, anchor: Vec3) =
            GatemakyrPassage(gate.passage.exit, gate.passage.entrance).point(anchor)

        fun atExit(gate: GatemakyrPlayerGate, reference: Vec3, anchor: Vec3) =
            gate.passage.point(reference).distanceToSqr(anchor) < reference.distanceToSqr(anchor)

        fun through(gate: GatemakyrPlayerGate, eye: Vec3, anchor: Vec3): GravitronGatePath {
            val apparent = unfolded(gate, anchor)
            val t = fraction(gate, eye, apparent)
            if (t != null) return GravitronGatePath(gate, apparent, eye.lerp(apparent, t))
            val frame = gate.passage.entrance
            val from = eye.subtract(frame.position)
            val to = apparent.subtract(frame.position)
            val a = abs(from.dot(frame.normal))
            val b = abs(to.dot(frame.normal))
            val point = from.lerp(to, if (a + b > 1e-8) a / (a + b) else 0.5)
            fun coordinate(axis: Vec3, size: Int) =
                (point.dot(axis) / axis.lengthSqr()).coerceIn(-size * 0.5, size * 0.5)
            val contact = frame.position.add(frame.u.scale(coordinate(frame.u, gate.source.width)))
                .add(frame.v.scale(coordinate(frame.v, gate.source.height)))
            return GravitronGatePath(gate, apparent, contact, true)
        }

        fun picked(player: Player, pos: BlockPos,
                   heldShip: Long, range: Double): List<GatemakyrPlayerGate> {
            val level = player.level() as? ServerLevel ?: return emptyList()
            val gates = GatemakyrServer.physicalGates(level)
            if (gates.isEmpty()) return emptyList()
            val eye = player.eyePosition
            val ray = ClipContext(eye, eye.add(player.lookAngle.scale(range)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)
            val hit = GatemakyrRaycast.clip(level, ray, false, null, false) ?: return emptyList()
            if (hit.type != HitResult.Type.BLOCK || hit.blockPos != pos) return emptyList()
            val picked = (hit as GatemakyrHit).gatePath?.gates.orEmpty()
            return picked.takeIf { route -> route.all { hop -> gates.any { it.matches(hop) } &&
                hop.source.shipId != heldShip && hop.destination.shipId != heldShip } }.orEmpty()
        }

        fun forward(route: List<GatemakyrPlayerGate>, point: Vec3) = route.fold(point) { p, gate -> gate.passage.point(p) }
        fun backward(route: List<GatemakyrPlayerGate>, point: Vec3) = route.asReversed().fold(point) { p, gate -> unfolded(gate, p) }

        fun depth(route: List<GatemakyrPlayerGate>, preferred: Int, reference: Vec3, anchor: Vec3): Int {
            val current = preferred.coerceIn(0, route.size)
            return listOf(current, current - 1, current + 1).filter { it in 0..route.size }
                .minBy { forward(route.take(it), reference).distanceToSqr(anchor) }
        }

        fun legs(route: List<GatemakyrPlayerGate>, eye: Vec3, anchor: Vec3): List<GravitronGatePath> {
            var from = eye
            var apparent = backward(route, anchor)
            return route.map { gate ->
                apparent = gate.passage.point(apparent)
                through(gate, from, apparent).also { from = gate.passage.point(it.contact) }
            }
        }

        fun aimed(gates: Collection<GatemakyrPlayerGate>, eye: Vec3, target: Vec3, heldShip: Long): GatemakyrPlayerGate? {
            var nearest = Double.POSITIVE_INFINITY
            var result: GatemakyrPlayerGate? = null
            for (gate in gates) {
                if (gate.source.shipId == heldShip || gate.destination.shipId == heldShip) continue
                val t = fraction(gate, eye, target) ?: continue
                if (t < nearest) {
                    nearest = t
                    result = gate
                }
            }
            return result
        }

        fun toAnchor(gates: Collection<GatemakyrPlayerGate>, eye: Vec3, anchor: Vec3, heldShip: Long): GravitronGatePath? {
            var distance = eye.distanceToSqr(anchor)
            var result: GravitronGatePath? = null
            for (gate in gates) {
                if (gate.source.shipId == heldShip || gate.destination.shipId == heldShip) continue
                val apparent = GatemakyrPassage(gate.passage.exit, gate.passage.entrance).point(anchor)
                val length = eye.distanceToSqr(apparent)
                if (length >= distance) continue
                val t = fraction(gate, eye, apparent) ?: continue
                distance = length
                result = GravitronGatePath(gate, apparent, eye.lerp(apparent, t))
            }
            return result
        }
    }
}
