package org.valkyrienskies.clockwork.util

import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import kotlin.math.abs
import kotlin.math.sqrt

//https://stackoverflow.com/questions/1171849/finding-quaternion-representing-the-rotation-from-one-vector-to-another
/**
 * aligns [right] of quaternion to [localDir]
 */
fun getHingeRotation(localDir: Vector3dc, right: Vector3dc = Vector3d(1.0, 0.0, 0.0)): Quaterniond {
//    if ((localDir - right).length() < 1e-5) { return Quaterniond() }

    val v1l = right.length()
    val v2l = localDir.length()

    val a = right.cross(localDir, Vector3d())

    val k = sqrt(v1l * v1l * v2l * v2l)
    val kCosTheta = right.dot(localDir)

    if (abs(kCosTheta / k + 1.0) < 1e-5) {
        val ort = right.orthogonalize(right, Vector3d())
        return Quaterniond(ort.x, ort.y, ort.z, 0.0).normalize()
    }

    return Quaterniond(a.x, a.y, a.z, k + kCosTheta).normalize()
}