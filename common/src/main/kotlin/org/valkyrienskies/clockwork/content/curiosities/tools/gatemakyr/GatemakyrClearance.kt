package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import org.joml.Intersectiond
import org.joml.Vector3d
import org.joml.primitives.AABBd
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipsIntersecting
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft

internal object GatemakyrClearance {
    private val xAxis = Vector3d(1.0, 0.0, 0.0)
    private val yAxis = Vector3d(0.0, 1.0, 0.0)
    private val zAxis = Vector3d(0.0, 0.0, 1.0)

    fun open(level: Level, target: GatemakyrTarget): Boolean {
        val frame = target.frame(level) ?: return false
        val diagonal = frame.u.scale(target.width * 0.5 - 0.0001)
            .add(frame.v.scale(target.height * 0.5 - 0.0001)).add(frame.normal.scale(1.0 / 64))
        val bounds = AABB(frame.position.subtract(diagonal), frame.position.add(diagonal))
        if (!level.hasChunksAt(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ)) ||
            level.getBlockCollisions(null, bounds).iterator().hasNext()) return false
        val center = frame.position.toJOML()
        val halfSize = Vector3d(bounds.xsize, bounds.ysize, bounds.zsize).mul(0.5)
        for (ship in level.getShipsIntersecting(bounds)) {
            val transform = if (ship is ClientShip) ship.renderTransform else ship.transform
            val local = AABBd(bounds.toJOML()).transform(transform.worldToShip).toMinecraft()
            val matrix = transform.shipToWorld
            val x = matrix.transformDirection(Vector3d(xAxis))
            val y = matrix.transformDirection(Vector3d(yAxis))
            val z = matrix.transformDirection(Vector3d(zAxis))
            val scale = Vector3d(x.length(), y.length(), z.length()).mul(0.5)
            x.normalize()
            y.normalize()
            z.normalize()
            for (shape in level.getBlockCollisions(null, local)) for (box in shape.toAabbs()) {
                val blockCenter = matrix.transformPosition(box.center.toJOML())
                val blockHalfSize = Vector3d(box.xsize, box.ysize, box.zsize).mul(scale)
                if (Intersectiond.testObOb(center, xAxis, yAxis, zAxis, halfSize,
                        blockCenter, x, y, z, blockHalfSize)) return false
            }
        }
        return true
    }
}
