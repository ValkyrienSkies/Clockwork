package org.valkyrienskies.clockwork.content.curiosities.grippad

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Vector3d
import org.joml.Vector3dc
import kotlin.math.abs

internal object GripPadPhysics {
    const val CONTACT_RANGE = 0.08
    const val RELEASE_RANGE = 0.14
    const val MAX_SLIP = 0.35

    fun tangent(vector: Vector3dc, normal: Vector3dc) = Vector3d(vector).fma(-vector.dot(normal), normal)

    fun contactDistanceSquared(point: Vector3dc, pad: BlockPos, face: Direction): Double? {
        val x = point.x() - pad.x - 0.5
        val y = point.y() - pad.y - 0.5
        val z = point.z() - pad.z - 0.5
        if (abs(x) > (if (face.axis == Direction.Axis.X) RELEASE_RANGE else 0.500001) ||
            abs(y) > (if (face.axis == Direction.Axis.Y) RELEASE_RANGE else 0.500001) ||
            abs(z) > (if (face.axis == Direction.Axis.Z) RELEASE_RANGE else 0.500001)) return null
        return x * x + y * y + z * z
    }

    fun force(error: Vector3dc, velocity: Vector3dc, normal: Vector3dc, load: Double,
              inverseMass: Double, contacts: Int, delta: Double): Vector3d {
        if (!load.isFinite() || load <= 0 || !inverseMass.isFinite() || inverseMass <= 0 ||
            !delta.isFinite() || delta <= 0 || !error.isFinite || !velocity.isFinite) return Vector3d()
        val share = load / contacts.coerceAtLeast(1)
        val stiffness = share * 240.0
        val damping = share * 32.0
        val force = tangent(error, normal).mul(stiffness)
            .fma(-(damping + stiffness * delta), tangent(velocity, normal))
            .div(1.0 + inverseMass * contacts.coerceAtLeast(1) * delta * (damping + stiffness * delta))
        val maximum = share * 35.0
        if (force.lengthSquared() > maximum * maximum) force.normalize(maximum)
        return force
    }
}
