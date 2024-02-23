package org.valkyrienskies.clockwork.util

import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import kotlin.math.sqrt
import kotlin.math.tan


object MathUtil {

    /**
     * Rotate a vector using a quaternion.
     *
     * @param vector The vector to rotate.
     * @param quaternion The quaternion representing the rotation.
     * @return The rotated vector.
     */
    fun rotateVecWithQuat(vector: Vec3, quaternion: Quaternionf): Vec3 {
        val vecQuat = Quaternionf(vector.x.toFloat(), vector.y.toFloat(), vector.z.toFloat(), 0.0f)
        val quatCopy = Quaternionf(quaternion)
        vecQuat.mul(quatCopy)
        quatCopy.conjugate()
        quatCopy.mul(vecQuat)
        return Vec3(quatCopy.x().toDouble(), quatCopy.y().toDouble(), quatCopy.z().toDouble())
    }

    /**
     * Reverse rotation of a vector using a quaternion.
     *
     * @param vector The vector to reverse rotate.
     * @param quaternion The quaternion representing the rotation to reverse.
     * @return The reverse rotated vector.
     */
    fun reverseRotateVecWithQuat(vector: Vec3, quaternion: Quaternionf): Vec3 {
        val vecQuat = Quaternionf(vector.x.toFloat(), vector.y.toFloat(), vector.z.toFloat(), 0.0f)
        val quatCopy = Quaternionf(quaternion)
        quatCopy.conjugate()
        vecQuat.mul(quatCopy)
        quatCopy.conjugate()
        quatCopy.mul(vecQuat)
        return Vec3(quatCopy.x().toDouble(), quatCopy.y().toDouble(), quatCopy.z().toDouble())
    }

    /**
     * Clamp a vector into a cone defined by an axis and angle.
     *
     * @param vector The vector to clamp.
     * @param coneAxis The axis of the cone.
     * @param coneAngle The angle of the cone.
     * @return The clamped vector.
     */
    fun clampVecIntoCone(vector: Vec3, coneAxis: Vec3, coneAngle: Double): Vec3 {
        val dotVV = vector.dot(vector)
        val dotVN = vector.dot(coneAxis)
        val dotNN = coneAxis.dot(coneAxis)
        val discriminant = dotNN * dotVV * 1.005 - dotVN * dotVN

        val offsetDistance = (-dotVN + sqrt(discriminant) / tan(coneAngle)) / dotNN
        if (offsetDistance < 0) return vector

        return vector.add(coneAxis.scale(offsetDistance)).normalize()
    }

    /**
     * Calculate quaternion representing rotation from one vector to another.
     *
     * @param startVec The start vector.
     * @param endVec The end vector.
     * @return The quaternion representing the rotation.
     */
    fun quatFromVecRot(startVec: Vec3, endVec: Vec3): Quaternionf {
        val crossProduct = startVec.cross(endVec)
        val cross = Vec3(crossProduct.x, crossProduct.y, crossProduct.z)
        val quaternion = Quaternionf(cross.x().toFloat(), cross.y().toFloat(), cross.z().toFloat(), 1.0f + startVec.dot(endVec).toFloat())
        quaternion.normalize()
        return quaternion
    }
}