package org.valkyrienskies.clockwork.util

import net.minecraft.core.Direction
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.div
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.times

//fun getHingeRotation(localDirection: Direction): Quaterniond {
//    val localDir = when(localDirection) {
//        Direction.DOWN  -> Vector3d( 0.0, -1.0,  0.0)
//        Direction.UP    -> Vector3d( 0.0,  1.0,  0.0)
//        Direction.NORTH -> Vector3d(-1.0,  0.0,  0.0)
//        Direction.SOUTH -> Vector3d( 1.0,  0.0,  0.0)
//        Direction.WEST  -> Vector3d( 0.0,  0.0, -1.0)
//        Direction.EAST  -> Vector3d( 0.0,  0.0,  1.0)
//    }
//
//    val right = Vector3d(0.0, 0.0, 1.0)
//
//    if ((localDir - right).length() < 1e-5) { return Quaterniond() }
//
//    val v1l = right.length()
//    val v2l = localDir.length()
//
//    val a = right.cross(localDir)
//
//    val k = sqrt(v1l * v1l * v2l * v2l)
//    val kCosTheta = right.dot(localDir)
//
//    if (abs(kCosTheta / k + 1.0) < 1e-5) {
//        val ort = right.orthogonalize(right)
//        return Quaterniond(ort.x, ort.y, ort.z, 0.0).normalize()
//    }
//
//    return Quaterniond(a.x, a.y, a.z, k + kCosTheta).normalize()
//}

fun getHingeRotation(localDirection: Direction): Quaterniond {
    val rotationQuaternion: Quaterniond = when (localDirection) {
        Direction.UP -> {
            Quaterniond()
        }
        Direction.DOWN -> {
            Quaterniond(AxisAngle4d(Math.PI, Vector3d(1.0, 0.0, 0.0)))
        }
        Direction.NORTH -> {
            Quaterniond(AxisAngle4d(Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                Quaterniond(
                    AxisAngle4d(
                        Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)
                    )
                )
            ).normalize()
        }
        Direction.EAST -> {
            Quaterniond(AxisAngle4d(0.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                Quaterniond(
                    AxisAngle4d(
                        Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)
                    )
                )
            ).normalize()
        }
        Direction.SOUTH -> {
            Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0))).normalize()
        }
        Direction.WEST -> {
            Quaterniond(AxisAngle4d(1.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                Quaterniond(
                    AxisAngle4d(
                        Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)
                    )
                )
            ).normalize()
        }
    }

    val hingeOrientation: Quaterniond = rotationQuaternion.mul(
        Quaterniond(AxisAngle4d(Math.toRadians(90.0), 0.0, 0.0, 1.0)),
        Quaterniond()
    ).normalize()

    return hingeOrientation
}