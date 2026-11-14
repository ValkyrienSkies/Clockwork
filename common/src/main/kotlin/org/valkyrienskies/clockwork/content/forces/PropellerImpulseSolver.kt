package org.valkyrienskies.clockwork.content.forces

import org.joml.Matrix3dc
import org.joml.Vector3d
import org.joml.Vector3dc
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

internal object PropellerImpulseSolver {
    const val STEP = 1.0 / 60.0

    fun solve(
        rotors: List<PropellerAerodynamics.PreparedRotor>, mass: Double, inertiaWorld: Matrix3dc,
        maxForce: Double = 0.0, maxTorque: Double = 0.0,
        reportForce: ((Int, Vector3dc) -> Unit)? = null
    ): Pair<Vector3dc, Vector3dc> {
        fun zero() = Vector3d() to Vector3d()
        if (rotors.isEmpty() || !mass.isFinite() || mass <= 0.0 || !inertiaWorld.isFinite ||
            inertiaWorld.m00() <= 0.0 || inertiaWorld.m11() <= 0.0 || inertiaWorld.m22() <= 0.0 ||
            inertiaWorld.m00() * inertiaWorld.m11() - inertiaWorld.m01() * inertiaWorld.m10() <= 0.0 ||
            inertiaWorld.determinant() <= 0.0) return zero()

        val scale = doubleArrayOf(sqrt(mass), sqrt(mass), sqrt(mass),
            sqrt(inertiaWorld.m00()), sqrt(inertiaWorld.m11()), sqrt(inertiaWorld.m22()))
        val system = Array(6) { DoubleArray(7) }
        for (i in 0..2) system[i][i] = mass
        for (row in 0..2) for (column in 0..2) system[row + 3][column + 3] = inertiaWorld.get(column, row)

        val pointVelocity = Vector3d()
        val sectionForce = Vector3d()
        val sectionTorque = Vector3d()
        val responseForce = Vector3d()
        val responseTorque = Vector3d()
        fun responseTo(rotor: PropellerAerodynamics.PreparedRotor, velocity: Vector3dc, omega: Vector3dc) {
            responseForce.zero()
            responseTorque.zero()
            for (section in rotor.sections) {
                omega.cross(section.lever, pointVelocity).add(velocity)
                section.response.transform(pointVelocity, sectionForce)
                responseForce.add(sectionForce)
                responseTorque.add(section.lever.cross(sectionForce, sectionTorque))
            }
            // Apply the same per-hub shaft policy to the response as to the initial load.
            // Otherwise implicit translation can reintroduce roll even from a zero-torque load.
            rotor.shaftDamping?.apply(responseForce, responseTorque, omega, responseTorque)
        }
        val linearBasis = Vector3d()
        val angularBasis = Vector3d()
        for (rotor in rotors) {
            for (i in 0..2) {
                system[i][6] += STEP * rotor.force.get(i)
                system[i + 3][6] += STEP * rotor.torque.get(i)
            }
            for (column in 0..5) {
                linearBasis.zero()
                angularBasis.zero()
                if (column < 3) linearBasis.setComponent(column, 1.0)
                else angularBasis.setComponent(column - 3, 1.0)
                responseTo(rotor, linearBasis, angularBasis)
                for (row in 0..2) {
                    system[row][column] -= STEP * responseForce.get(row)
                    system[row + 3][column] -= STEP * responseTorque.get(row)
                }
            }
        }
        for (row in 0..5) {
            for (column in 0..5) system[row][column] /= scale[row] * scale[column]
            system[row][6] /= scale[row]
            if (system[row].any { !it.isFinite() }) return zero()
        }

        for (column in 0..5) {
            val pivotRow = (column..5).maxBy { abs(system[it][column]) }
            val swap = system[column]
            system[column] = system[pivotRow]
            system[pivotRow] = swap
            val pivot = system[column][column]
            if (!pivot.isFinite() || abs(pivot) < 1e-12) return zero()
            for (row in column + 1..5) {
                val factor = system[row][column] / pivot
                for (j in column + 1..6) system[row][j] -= factor * system[column][j]
            }
        }
        val solution = DoubleArray(6)
        for (row in 5 downTo 0) {
            var value = system[row][6]
            for (column in row + 1..5) value -= system[row][column] * solution[column]
            solution[row] = value / system[row][row]
        }
        for (i in 0..5) solution[i] /= scale[i]
        if (solution.any { !it.isFinite() }) return zero()
        val deltaVelocity = Vector3d(solution[0], solution[1], solution[2])
        val deltaOmega = Vector3d(solution[3], solution[4], solution[5])

        var fraction = 1.0
        if (maxForce > 0.0 || maxTorque > 0.0) for (rotor in rotors) {
            responseTo(rotor, deltaVelocity, deltaOmega)
            val force = responseForce.add(rotor.force)
            val torque = responseTorque.add(rotor.torque)
            if (maxForce > 0.0 && force.length() > maxForce) fraction = min(fraction, maxForce / force.length())
            if (maxTorque > 0.0 && torque.length() > maxTorque) fraction = min(fraction, maxTorque / torque.length())
        }
        if (reportForce != null) rotors.forEachIndexed { index, rotor ->
            val applied = Vector3d(rotor.force)
            for (section in rotor.sections) {
                deltaOmega.cross(section.lever, pointVelocity).add(deltaVelocity)
                applied.add(section.response.transform(pointVelocity, sectionForce))
            }
            reportForce(index, applied.mul(fraction))
        }
        val force = deltaVelocity.mul(mass * fraction / STEP)
        val torque = inertiaWorld.transform(deltaOmega, Vector3d()).mul(fraction / STEP)
        return if (force.isFinite && torque.isFinite) force to torque else zero()
    }
}
