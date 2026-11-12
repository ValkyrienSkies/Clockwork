package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

data class VdexPlacement(val position: Vec3, val yaw: Float) {
    fun world(point: Vec3): Vec3 = point.yRot(Math.toRadians(yaw.toDouble()).toFloat()).add(position)
    fun local(point: Vec3): Vec3 = point.subtract(position).yRot(-Math.toRadians(yaw.toDouble()).toFloat())
    fun rotate(bounds: AABB, degrees: Float): VdexPlacement {
        val next = copy(yaw = (yaw + degrees) % 360f)
        return next.copy(position = position.add(world(bounds.center).subtract(next.world(bounds.center))))
    }

    fun face(bounds: AABB, start: Vec3, end: Vec3): Direction? {
        val hit = bounds.clip(local(start), local(end)).orElse(null) ?: return null
        return listOf(Direction.WEST to abs(hit.x - bounds.minX), Direction.EAST to abs(hit.x - bounds.maxX),
            Direction.DOWN to abs(hit.y - bounds.minY), Direction.UP to abs(hit.y - bounds.maxY),
            Direction.NORTH to abs(hit.z - bounds.minZ), Direction.SOUTH to abs(hit.z - bounds.maxZ)).minBy { it.second }.first
    }

    companion object {
        fun at(bounds: AABB, target: Vec3, yaw: Float): VdexPlacement {
            val offset = Vec3(bounds.center.x, bounds.minY, bounds.center.z).yRot(Math.toRadians(yaw.toDouble()).toFloat())
            return VdexPlacement(target.subtract(offset), yaw)
        }
    }
}
