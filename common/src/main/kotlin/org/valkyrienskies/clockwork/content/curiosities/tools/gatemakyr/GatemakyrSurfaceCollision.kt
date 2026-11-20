package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Vector3d
import org.joml.Matrix4d
import org.valkyrienskies.mod.common.entity.ShipMountingEntity
import org.valkyrienskies.mod.common.entity.VSPhysicsEntity
import java.util.UUID
import kotlin.math.max

object GatemakyrSurfaceCollision {
    private data class Opening(val target: GatemakyrTarget, val side: Double)
    private val checkingExit = ThreadLocal<Opening>()
    private data class Departure(val opening: Opening, val anchor: Vec3)
    private val departure = ThreadLocal<Departure>()

    internal fun eligible(entity: Entity, remotePlayer: Boolean = false) = entity.isAlive && !entity.isSpectator &&
        (!entity.noPhysics || remotePlayer) &&
        !entity.isPassenger && entity !is VSPhysicsEntity && entity !is ShipMountingEntity

    internal fun ridingBounds(entity: Entity): AABB =
        entity.indirectPassengers.fold(entity.boundingBox) { box, rider -> box.minmax(rider.boundingBox) }

    private fun anchor(entity: Entity) = if (entity is Player) entity.eyePosition else entity.boundingBox.center

    private fun opening(target: GatemakyrTarget, side: Double, entity: Entity, query: AABB): AABB {
        val n = Vec3.atLowerCornerOf(target.face.normal)
        val v = if (target.face.axis.isVertical) Vec3(0.0, 0.0, -1.0) else Vec3(0.0, 1.0, 0.0)
        val diagonal = v.scale(target.height * 0.5).add(v.cross(n).scale(target.width * 0.5))
        val sweep = max(entity.deltaMovement.length(), 2.0 * GatemakyrPassage.radius(query, n))
        val depth = max(entity.bbHeight, entity.bbWidth) + sweep + 0.125
        return AABB(target.position.subtract(diagonal), target.position.add(diagonal)).expandTowards(n.scale(-depth * side))
    }

    private fun clearAt(entity: Entity, bounds: AABB, exit: Opening): Boolean {
        val box = bounds.deflate(1e-7)
        val level = entity.level()
        if (!level.hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ)) ||
            !level.worldBorder.isWithinBounds(box)) return false
        checkingExit.set(exit)
        return try { level.noCollision(entity, box) } finally { checkingExit.remove() }
    }

    internal data class Arrival(val contact: Vec3, val end: Vec3)

    private fun exitOffset(entity: Entity, box: AABB, gate: GatemakyrPlayerGate, exit: Opening): Vec3? {
        if (entity is Player && clearAt(entity, box, exit)) return Vec3.ZERO
        val shift = GatemakyrPassage.inset(box, gate.passage.exit, gate.destination) ?: return null
        return shift.takeIf { clearAt(entity, box.move(it), exit) }
    }

    internal fun exitAnchor(entity: Entity, contact: Vec3, end: Vec3, offset: Vec3, gate: GatemakyrPlayerGate): Arrival? {
        val level = entity.level()
        val mapped = ridingBounds(entity).move(contact.add(offset).subtract(entity.position()))
        val reverse = !gate.source.surface && !gate.destination.surface &&
            end.subtract(contact).dot(gate.passage.exit.normal) < 0
        val exit = Opening(gate.destination, if (reverse) -1.0 else 1.0)
        val shift = exitOffset(entity, mapped, gate, exit) ?: return null
        val start = contact.add(shift)
        val box = mapped.move(shift)
        val motion = end.subtract(contact)
        val swept = box.expandTowards(motion)
        if (!level.hasChunksAt(BlockPos.containing(swept.minX, swept.minY, swept.minZ),
                BlockPos.containing(swept.maxX, swept.maxY, swept.maxZ))) return Arrival(start, start)
        departure.set(Departure(exit, start))
        return try {
            Arrival(start, start.add(Entity.collideBoundingBox(entity, motion, box, level, level.getEntityCollisions(entity, swept))))
        } finally {
            departure.remove()
        }
    }

    @JvmStatic
    fun eyeInOpening(entity: Entity): Boolean {
        if (!eligible(entity)) return false
        val eye = entity.eyePosition
        val probe = AABB.ofSize(eye, entity.bbWidth * 0.8, 0.1, entity.bbWidth * 0.8)
        val level = entity.level()
        val gates = gates(entity)
        for (gate in gates.values) {
            val side = approachSide(entity, gate) ?: continue
            val local = if (gate.source.shipId < 0) probe else {
                val transform = gate.source.mountedTransform(level) ?: continue
                val min = Vector3d()
                val max = Vector3d()
                transform.invert(Matrix4d()).transformAab(probe.minX, probe.minY, probe.minZ, probe.maxX, probe.maxY, probe.maxZ, min, max)
                AABB(min.x, min.y, min.z, max.x, max.y, max.z)
            }
            if (!opening(gate.source, side, entity, local).intersects(local) || !canPass(entity, gate, side)) continue
            if (!level.collidesWithSuffocatingBlock(entity, probe) &&
                (local === probe || !level.collidesWithSuffocatingBlock(entity, local))) return true
        }
        return false
    }

    private fun gates(entity: Entity): Map<Pair<UUID, Int>, GatemakyrPlayerGate> {
        val level = entity.level()
        return when {
            level is ServerLevel -> if (level.server.isSameThread) GatemakyrServer.playerGates(level) else emptyMap()
            else -> GatemakyrClient.rayGates(level)
        }
    }

    private fun approachSide(entity: Entity, gate: GatemakyrPlayerGate): Double? {
        val origin = departure.get()
        val point = origin?.anchor ?: anchor(entity)
        val frame = gate.passage.entrance
        val normal = frame.normal.normalize()
        val distance = point.subtract(frame.position).dot(normal)
        val previous = point.add(entity.xo - entity.x, entity.yo - entity.y, entity.zo - entity.z)
        val before = if (origin == null) previous.subtract(frame.position).dot(normal) else distance
        if (!gate.source.surface && !gate.destination.surface) return if (before < 0) -1.0 else 1.0
        return if (distance < -0.06 && before < 0.0) null else 1.0
    }

    private fun canPass(entity: Entity, gate: GatemakyrPlayerGate, side: Double): Boolean {
        val point = departure.get()?.anchor ?: anchor(entity)
        val group = ridingBounds(entity)
        if (entity !is Player && !gate.admits(group, point.add(group.center.subtract(entity.boundingBox.center)))) return false
        val box = group.move(gate.exitContact(point).subtract(anchor(entity)))
        return exitOffset(entity, box, gate, Opening(gate.destination, side)) != null
    }

    @JvmStatic
    fun clipBacking(shape: VoxelShape, x: Int, y: Int, z: Int, openings: List<AABB>): VoxelShape {
        if (openings.isEmpty() || shape.isEmpty) return shape
        var clipped = shape
        for (hole in openings) {
            val local = hole.move(-x.toDouble(), -y.toDouble(), -z.toDouble())
            if (local.intersects(clipped.bounds())) {
                clipped = Shapes.joinUnoptimized(clipped, Shapes.create(local), BooleanOp.ONLY_FIRST)
                if (clipped.isEmpty) break
            }
        }
        return clipped
    }

    @JvmStatic
    fun openings(entity: Entity, query: AABB): List<AABB> {
        if (!eligible(entity)) return emptyList()
        checkingExit.get()?.let { return listOf(opening(it.target, it.side, entity, query)) }
        val gates = gates(entity)
        if (gates.isEmpty()) return emptyList()
        val exit = departure.get()?.opening
        var result: MutableList<AABB>? = null
        for (gate in gates.values) {
            val leaving = gate.source == exit?.target
            val side = if (leaving) exit!!.side else approachSide(entity, gate) ?: continue
            val hole = opening(gate.source, side, entity, query)
            if (!hole.intersects(query) || (!leaving && !canPass(entity, gate, side))) continue
            if (result == null) result = mutableListOf()
            result.add(hole)
        }
        return result ?: emptyList()
    }
}
