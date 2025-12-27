package org.valkyrienskies.clockwork.util

import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * PID controller for orientation using quaternion error.
 *
 * - Computes error quaternion: qErr = qTarget * inverse(qCurrent)
 * - Converts to axis-angle error vector: e = axis * angle (radians)
 * - Runs PID on e as a Vector3
 *
 * Output: Vector3f (typically an angular velocity command in rad/s, or torque command if you treat it that way)
 */
class PIDQuaternion(
    val p: Float = 1.0f,
    val i: Float = 0.1f,
    val d: Float = 0.05f,
    /** clamp for integral term (radians) to avoid windup */
    val integralLimit: Float = 1.0f
) {
    private val integral = Vector3f()
    private val lastError = Vector3f()

    val isIntegralReset: Boolean
        get() = integral.lengthSquared() == 0.0f

    fun resetIntegral() {
        integral.zero()
    }

    /**
     * @param target desired orientation
     * @param current current orientation
     * @param dtSeconds timestep in seconds (e.g. 1f/60f for physics tick)
     * @return control output vector (rad/s-ish)
     */
    fun control(
        target: Quaternionf,
        current: Quaternionf,
        dtSeconds: Float,
        Kp: Float = p,
        Ki: Float = i,
        Kd: Float = d
    ): Vector3f {
        // qErr = target * inverse(current)
        val qInv = Quaternionf(current).conjugate() // for unit quats, conjugate == inverse
        val qErr = Quaternionf(target).mul(qInv)

        // Ensure "shortest path": keep w >= 0 (q and -q represent same rotation)
        if (qErr.w < 0f) qErr.invert()

        // Convert quaternion error to axis-angle vector: axis * angle
        val errVec = quatToAxisAngleVector(qErr) // radians

        // P
        val proportional = Vector3f(errVec).mul(Kp)

        // I (with clamp)
        integral.fma(dtSeconds, errVec) // integral += errVec * dt
        clampMagnitude(integral, integralLimit)
        val integralTerm = Vector3f(integral).mul(Ki)

        // D
        val derivative = Vector3f(errVec).sub(lastError).div(dtSeconds.coerceAtLeast(1e-6f))
        val derivativeTerm = derivative.mul(Kd)

        lastError.set(errVec)

        return proportional.add(integralTerm).add(derivativeTerm)
    }

    /**
     * Converts a unit quaternion to axis*angle vector (radians).
     * For small angles, result approximates 2 * vector_part.
     */
    private fun quatToAxisAngleVector(q: Quaternionf): Vector3f {
        // q assumed normalized-ish. If not, normalize a copy:
        val qq = Quaternionf(q).normalize()

        val vx = qq.x
        val vy = qq.y
        val vz = qq.z
        val w = qq.w

        val sinHalf = sqrt(vx * vx + vy * vy + vz * vz)

        // angle in [0, pi] because we forced w >= 0 earlier
        val angle = 2f * atan2(sinHalf, w)

        if (sinHalf < 1e-6f || angle == 0f) {
            // small-angle approximation: axis*angle ≈ 2 * vector_part
            return Vector3f(vx, vy, vz).mul(2f)
        }

        val invSinHalf = 1f / sinHalf
        val axis = Vector3f(vx * invSinHalf, vy * invSinHalf, vz * invSinHalf)

        // axis * angle gives a nice 3D error vector
        return axis.mul(angle)
    }

    private fun clampMagnitude(v: Vector3f, maxMag: Float) {
        val len2 = v.lengthSquared()
        val max2 = maxMag * maxMag
        if (len2 > max2 && len2 > 0f) {
            v.mul(maxMag / sqrt(len2))
        }
    }
}
