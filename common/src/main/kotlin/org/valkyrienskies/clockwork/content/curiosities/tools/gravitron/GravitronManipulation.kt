package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector2dc
import org.joml.Vector3d
import org.joml.Vector3dc

object GravitronManipulation {
    const val MIN_DISTANCE = 2.0
    const val CREATIVE_MAX_DISTANCE = 1000.0
    const val DISTANCE_STEP = 1.0
    const val MAX_ROTATION_STEP = 45.0

    fun view(pitch: Double, yaw: Double): Quaterniond =
        Quaterniond().rotateY(Math.toRadians(-yaw)).rotateX(Math.toRadians(pitch))

    fun rotate(base: Quaterniondc, reference: Vector2dc, yaw: Double, pitch: Double): Quaterniond {
        if (!yaw.isFinite() || !pitch.isFinite()) return Quaterniond(base)
        val frame = view(reference.x(), reference.y())
        val drag = view(pitch.coerceIn(-MAX_ROTATION_STEP, MAX_ROTATION_STEP), yaw.coerceIn(-MAX_ROTATION_STEP, MAX_ROTATION_STEP))
        return Quaterniond(frame).mul(drag).mul(frame.invert(Quaterniond())).mul(base).normalize()
    }

    fun distance(current: Double, scroll: Double, maximum: Double): Double {
        if (!current.isFinite() || !scroll.isFinite()) return current
        return (current - scroll.coerceIn(-8.0, 8.0) * DISTANCE_STEP)
            .coerceIn(MIN_DISTANCE, maximum.coerceAtLeast(MIN_DISTANCE))
    }

    fun pointVelocity(linear: Vector3dc, angular: Vector3dc, offset: Vector3dc): Vector3d =
        Vector3d(angular).cross(offset).add(linear)

    fun acceleration(error: Vector3dc, velocity: Vector3dc, delta: Double, maximum: Double): Vector3d {
        if (!error.isFinite || !velocity.isFinite || !delta.isFinite()) return Vector3d()
        val step = delta.coerceIn(.001, .05)
        val denominator = 1.0 + 20.0 * step + 160.0 * step * step
        val correction = Vector3d(error)
        if (correction.lengthSquared() > 16.0) correction.normalize(4.0)
        val result = correction.mul(160.0).fma(-(20.0 + 160.0 * step), velocity).div(denominator)
        if (result.lengthSquared() > maximum * maximum) result.normalize(maximum)
        return result
    }

    fun supportedGain(mass: Double, supportedMass: Double, rigidMass: Double, delta: Double): Double {
        if (!mass.isFinite() || mass <= 0.0 || !supportedMass.isFinite() || !rigidMass.isFinite() || !delta.isFinite()) return 1.0
        val ratio = (supportedMass / mass).coerceAtLeast(1.0)
        val elasticRatio = (supportedMass / rigidMass.coerceAtLeast(mass)).coerceAtLeast(1.0)
        val step = delta.coerceIn(0.001, 0.05)
        return (ratio / (1.0 + (20.0 * step + 160.0 * step * step) * (elasticRatio - 1.0))).coerceAtLeast(1.0)
    }
}
