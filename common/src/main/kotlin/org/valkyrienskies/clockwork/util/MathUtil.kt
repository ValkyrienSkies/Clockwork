package org.valkyrienskies.clockwork.util

import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import kotlin.math.sqrt
import kotlin.math.tan


object MathUtil {

    fun rotateQuat(pVec3: Vec3, quaternion: Quaternionf): Vec3 {
        val quatFromVec3 = Quaternionf(pVec3.x.toFloat(), pVec3.y.toFloat(), pVec3.z.toFloat(), 0.0f)
        val quatCopy = Quaternionf(quaternion)
        quatFromVec3.mul(quatCopy)
        quatCopy.conjugate()
        quatCopy.mul(quatFromVec3)
        return Vec3(quatCopy.x().toDouble(), quatCopy.y().toDouble(), quatCopy.z().toDouble())
    }


    fun rotateQuatReverse(pVec3: Vec3, quaternion: Quaternionf): Vec3 {
        val quatFromVec3 = Quaternionf(pVec3.x.toFloat(), pVec3.y.toFloat(), pVec3.z.toFloat(), 0.0f)
        val quatCopy = Quaternionf(quaternion)
        quatCopy.conjugate()
        quatFromVec3.mul(quatCopy)
        quatCopy.conjugate()
        quatCopy.mul(quatFromVec3)
        return Vec3(quatCopy.x().toDouble(), quatCopy.y().toDouble(), quatCopy.z().toDouble())
    }

    fun clampIntoCone(v: Vec3, coneAxis: Vec3, coneAngle: Double): Vec3 {
        val vv: Double = v.dot(v)
        val vn: Double = v.dot(coneAxis)
        val nn: Double = coneAxis.dot(coneAxis)
        val disc = nn * vv * 1.005 - vn * vn

        val offsetDistance = (-vn + sqrt(disc) / tan(coneAngle)) / nn
        if (offsetDistance < 0) return v

        return v.add(coneAxis.scale(offsetDistance)).normalize()
    }

    fun getQuaternionFromVectorRotation(start: Vec3, end: Vec3): Quaternionf {
        val vec = start.cross(end)
        val cross = Vec3(vec.x, vec.y, vec.z)
        val quaternion =
            Quaternionf(cross.x().toFloat(), cross.y().toFloat(), cross.z().toFloat(), 1.0f + start.dot(end).toFloat())
        quaternion.normalize()
        return quaternion
    }
}