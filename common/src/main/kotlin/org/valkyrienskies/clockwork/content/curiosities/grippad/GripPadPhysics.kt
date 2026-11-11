package org.valkyrienskies.clockwork.content.curiosities.grippad

import org.joml.Vector3d
import org.joml.Vector3dc

internal object GripPadPhysics {
    const val CONTACT_RANGE = 0.08
    const val RELEASE_RANGE = 0.14
    const val MAX_SLIP = 0.35

    fun tangent(vector: Vector3dc, normal: Vector3dc) = Vector3d(vector).fma(-vector.dot(normal), normal)

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
