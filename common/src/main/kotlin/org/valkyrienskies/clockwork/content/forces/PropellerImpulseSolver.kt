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
        maxForce: Double = 0.0, maxTorque: Double = 0.0
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
        for (rotor in rotors) {
            for (i in 0..2) {
                system[i][6] += STEP * rotor.force.get(i)
                system[i + 3][6] += STEP * rotor.torque.get(i)
            }
            for (section in rotor.sections) for (column in 0..5) {
                pointVelocity.zero().setComponent(column % 3, 1.0)
                if (column >= 3) pointVelocity.cross(section.lever)
                section.response.transform(pointVelocity, sectionForce)
                section.lever.cross(sectionForce, sectionTorque)
                for (row in 0..2) {
                    system[row][column] -= STEP * sectionForce.get(row)
                    system[row + 3][column] -= STEP * sectionTorque.get(row)
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
            val force = Vector3d(rotor.force)
            val torque = Vector3d(rotor.torque)
            for (section in rotor.sections) {
                deltaOmega.cross(section.lever, pointVelocity).add(deltaVelocity)
                section.response.transform(pointVelocity, sectionForce)
                force.add(sectionForce)
                torque.add(section.lever.cross(sectionForce, sectionTorque))
            }
            if (maxForce > 0.0 && force.length() > maxForce) fraction = min(fraction, maxForce / force.length())
            if (maxTorque > 0.0 && torque.length() > maxTorque) fraction = min(fraction, maxTorque / torque.length())
        }
        val force = deltaVelocity.mul(mass * fraction / STEP)
        val torque = inertiaWorld.transform(deltaOmega, Vector3d()).mul(fraction / STEP)
        return if (force.isFinite && torque.isFinite) force to torque else zero()
    }
}
