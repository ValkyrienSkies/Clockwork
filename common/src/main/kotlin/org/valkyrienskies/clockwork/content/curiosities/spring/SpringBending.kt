package org.valkyrienskies.clockwork.content.curiosities.spring

import org.joml.Vector3d
import org.joml.Vector3dc
import kotlin.math.atan2

internal object SpringBending {
    fun torque(normal: Vector3dc, direction: Vector3dc, fallbackAxis: Vector3dc,
               relativeAngularVelocity: Vector3dc, mass: Double, settings: SpringSettings): Vector3d {
        val axis = normal.cross(direction, Vector3d())
        val sine = axis.length()
        val cosine = normal.dot(direction).coerceIn(-1.0, 1.0)
        val angle = atan2(sine, cosine)
        if (sine > 1e-6) axis.div(sine)
        else if (cosine < 0) {
            axis.set(fallbackAxis).fma(-normal.dot(fallbackAxis), normal).normalize()
        } else axis.zero()
        val damping = Vector3d(relativeAngularVelocity).fma(-relativeAngularVelocity.dot(direction), direction)
        val torque = axis.mul(angle * mass * 14.0 * settings.stiffness)
            .fma(-mass * 5.25 * settings.damping, damping)
        torque.fma(-torque.dot(direction), direction)
        val limit = mass * 120.0
        if (torque.lengthSquared() > limit * limit) torque.normalize(limit)
        return torque
    }

    fun reactionForce(torqueA: Vector3dc, torqueB: Vector3dc, chord: Vector3dc): Vector3d =
        Vector3d(torqueA).add(torqueB).cross(chord).div(chord.lengthSquared())
}
