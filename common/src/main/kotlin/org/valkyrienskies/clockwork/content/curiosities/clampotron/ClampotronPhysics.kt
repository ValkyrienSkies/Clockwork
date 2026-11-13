package org.valkyrienskies.clockwork.content.curiosities.clampotron

import org.joml.Matrix3dc
import org.joml.Vector3d
import kotlin.math.abs

internal object ClampotronPhysics {
    data class Body(val inverseMass: Double, val inverseInertia: Matrix3dc, val offset: Vector3d)
    data class Wrench(val force: Vector3d, val torque: Vector3d)

    fun response(body: Body, force: Vector3d, torque: Vector3d): Wrench {
        val angular = body.inverseInertia.transform(Vector3d(body.offset).cross(force).add(torque), Vector3d())
        return Wrench(Vector3d(force).mul(body.inverseMass).add(Vector3d(angular).cross(body.offset)), angular)
    }

    fun solve(a: Body, b: Body, displacement: Vector3d, velocity: Vector3d,
              angularError: Vector3d, angularVelocity: Vector3d, supportedMass: Double,
              gravity: Double, delta: Double): Wrench {
        val empty = Wrench(Vector3d(), Vector3d())
        if (!displacement.isFinite || !velocity.isFinite || !angularError.isFinite || !angularVelocity.isFinite ||
            !supportedMass.isFinite() || supportedMass <= 0 || !gravity.isFinite() || !delta.isFinite()) return empty
        val step = delta.coerceIn(0.001, 0.05)
        val inverseMass = Array(6) { DoubleArray(6) }
        for (column in 0..5) {
            val force = Vector3d()
            val torque = Vector3d()
            if (column < 3) force.setComponent(column, 1.0) else torque.setComponent(column - 3, 1.0)
            val left = response(a, force, torque)
            val right = response(b, force, torque)
            val linear = left.force.add(right.force)
            val angular = left.torque.add(right.torque)
            for (row in 0..5) inverseMass[row][column] = if (row < 3) linear[row] else angular[row - 3]
        }
        val error = Vector3d(displacement)
        if (error.lengthSquared() > 16.0) error.normalize(4.0)
        val matrix = Array(6) { DoubleArray(7) }
        val supportForce = Vector3d(0.0, supportedMass * gravity, 0.0)
        val supportTorque = Vector3d(a.offset).cross(supportForce).negate()
        val support = doubleArrayOf(supportForce.x, supportForce.y, supportForce.z, supportTorque.x, supportTorque.y, supportTorque.z)
        for (row in 0..5) {
            val mass = if (row < 3) supportedMass else 1.0 / inverseMass[row][row].coerceAtLeast(1e-12)
            val stiffness = mass * if (row < 3) 64.0 else 40.0
            val damping = mass * if (row < 3) 16.0 else 13.0
            val implicit = step * (damping + stiffness * step)
            for (column in 0..5) matrix[row][column] = implicit * inverseMass[row][column] + if (row == column) 1.0 else 0.0
            matrix[row][6] = stiffness * (if (row < 3) error[row] else angularError[row - 3]) -
                (damping + stiffness * step) * (if (row < 3) velocity[row] else angularVelocity[row - 3]) +
                (0..5).sumOf { matrix[row][it] * support[it] }
        }
        for (column in 0..5) {
            val pivot = (column..5).maxBy { abs(matrix[it][column]) }
            val swap = matrix[column]; matrix[column] = matrix[pivot]; matrix[pivot] = swap
            val divisor = matrix[column][column]
            if (!divisor.isFinite() || abs(divisor) < 1e-12) return empty
            for (entry in column..6) matrix[column][entry] /= divisor
            for (row in 0..5) {
                if (row == column) continue
                val factor = matrix[row][column]
                for (entry in column..6) matrix[row][entry] -= factor * matrix[column][entry]
            }
        }
        val force = Vector3d(matrix[0][6], matrix[1][6], matrix[2][6])
        val torque = Vector3d(matrix[3][6], matrix[4][6], matrix[5][6])
        return if (force.isFinite && torque.isFinite) Wrench(force, torque) else empty
    }
}
