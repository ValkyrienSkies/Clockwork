package org.valkyrienskies.clockwork.content.physicalities.wheel

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.AABB
import org.joml.Matrix4d
import org.joml.Matrix4dc
import org.joml.Vector3d
import org.joml.primitives.AABBd
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.getShipsIntersecting
import org.valkyrienskies.mod.common.util.toMinecraft
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal object WheelPlacement {
    private const val SIDES = 32
    private val boxAxes = listOf(Vector3d(1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0), Vector3d(0.0, 0.0, 1.0))

    fun hasClearance(level: ServerLevel, pos: BlockPos, facing: Direction, size: WheelSize) =
        hasClearance(level, pos, facing, size.tire())

    fun hasClearance(level: ServerLevel, pos: BlockPos, facing: Direction, size: WheelTire): Boolean {
        val parent = level.getShipManagingPos(pos)
        val transform = parent?.transform?.shipToWorld?.let(::Matrix4d) ?: Matrix4d()
        transform.translate(pos.x + .5, pos.y + .5, pos.z + .5)
            .rotate(WheelSize.axleRotation(facing.stepX.toDouble(), facing.stepY.toDouble(), facing.stepZ.toDouble()))
        val worldVolume = Volume(transform, size)
        if (!worldVolume.clear(level, if (parent == null) pos else null)) return false
        for (ship in level.getShipsIntersecting(worldVolume.bounds)) {
            if (ship.id == parent?.id) continue
            if (!Volume(Matrix4d(ship.transform.worldToShip).mul(transform), size).clear(level)) return false
        }
        return true
    }

    private class Volume(transform: Matrix4dc, size: WheelTire) {
        private val center = transform.transformPosition(Vector3d())
        private val halfAxle = transform.transformDirection(Vector3d(size.width / 2, 0.0, 0.0))
        private val radial = List(SIDES) { i ->
            val angle = (i + .5) * 2 * PI / SIDES
            val radius = size.radius / cos(PI / SIDES)
            transform.transformDirection(Vector3d(0.0, cos(angle) * radius, sin(angle) * radius))
        }
        private val axes = buildList {
            addAll(boxAxes)
            add(Vector3d(halfAxle).normalize())
            for (i in 0 until SIDES / 2) {
                val angle = i * 2 * PI / SIDES
                add(transform.transformDirection(Vector3d(0.0, cos(angle), sin(angle))).normalize())
                val edge = Vector3d(radial[(i + 1) % SIDES]).sub(radial[i])
                for (axis in boxAxes) add(edge.cross(axis, Vector3d()))
            }
            for (axis in boxAxes) add(halfAxle.cross(axis, Vector3d()))
        }.filter { it.lengthSquared() > 1e-12 }.map { Vector3d(it).normalize() }
        private val extents = axes.map { axis -> abs(axis.dot(halfAxle)) + radial.maxOf { axis.dot(it) } }
        val bounds = AABBd().also { box ->
            for (point in radial) {
                box.union(Vector3d(center).add(point).add(halfAxle))
                box.union(Vector3d(center).add(point).sub(halfAxle))
            }
        }

        fun clear(level: ServerLevel, ignore: BlockPos? = null): Boolean {
            val ignored = ignore?.let { level.getBlockState(it).getCollisionShape(level, it)
                .move(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()).toAabbs() }.orEmpty()
            val query = bounds.toMinecraft()
            if (!level.hasChunksAt(BlockPos.containing(query.minX, query.minY, query.minZ),
                    BlockPos.containing(query.maxX, query.maxY, query.maxZ))) return false
            for (shape in level.getBlockCollisions(null, query)) {
                for (box in shape.toAabbs()) if (box !in ignored && intersects(box)) return false
            }
            return true
        }

        private fun intersects(box: AABB): Boolean {
            val offset = Vector3d((box.minX + box.maxX) / 2, (box.minY + box.maxY) / 2,
                (box.minZ + box.maxZ) / 2).sub(center)
            val half = Vector3d(box.xsize / 2, box.ysize / 2, box.zsize / 2)
            for ((i, axis) in axes.withIndex()) {
                val reach = abs(axis.x) * half.x + abs(axis.y) * half.y + abs(axis.z) * half.z
                if (abs(axis.dot(offset)) >= extents[i] + reach - .001) return false
            }
            return true
        }
    }
}
