package org.valkyrienskies.clockwork.content.forces

import org.joml.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData

class PropellerSpinTorqueTest {
    private val com = Vector3d(.5, .5, 1.5)

    @Test
    fun `offset unbalanced rotor retains its full hub moment including axial component`() {
        for (sails in listOf(false, true)) for (speed in listOf(0.0, 20.0)) {
            val flow = flow(Vector3d(0.0, 0.0, speed))
            val centered = prepare(rotor(sails = sails, balanced = false), flow, enabled = false)
            val hubOffset = Vector3d(2.0, -3.0, 1.0)
            val offset = prepare(rotor(sails = sails, balanced = false, position = Vector3i(2, -3, 1)), flow, enabled = false)
            val hubMoment = hubOffset.cross(centered.force, Vector3d())
            assertTrue(kotlin.math.abs(hubMoment.z) > 1e-6, "Fixture must have an axial hub moment")
            assertTrue(centered.torque.length() > 1e-6, "Unbalanced thrust must retain a non-axial moment")
            assertEquals(0.0, centered.torque.z(), 1e-8)
            assertVector(centered.force, offset.force)
            assertVector(Vector3d(centered.torque).add(hubMoment), offset.torque)
        }
    }

    @Test
    fun `default preparation is identical to explicitly enabled spin torque`() {
        for (sails in listOf(false, true)) {
            val flow = flow(Vector3d(4.0, -2.0, 6.0), Vector3d(.3, -.4, .5))
            val default = PropellerAerodynamics.prepare(rotor(sails = sails), flow, 37.0, 240.0, 0.0, 0.0)
            val enabled = prepare(rotor(sails = sails), flow, true)
            assertLoads(default, enabled)
        }
    }

    @Test
    fun `stationary balanced rotor loses only its raw shaft reaction`() {
        for (sails in listOf(false, true)) for (rpm in listOf(-128.0, 128.0)) {
            val enabled = prepare(rotor(rpm, sails), enabled = true)
            val disabled = prepare(rotor(rpm, sails), enabled = false)
            assertTrue(kotlin.math.abs(enabled.torque.z()) > 1e-6)
            assertTrue(enabled.force.length() > 1e-6)
            assertVector(enabled.force, disabled.force)
            assertVector(Vector3d(), disabled.torque)
            assertResponses(enabled, disabled)
        }
    }

    @Test
    fun `powered balanced rotor does not roll a translating ship with spin torque disabled`() {
        for (sails in listOf(false, true)) for (rpm in listOf(-128.0, 128.0)) {
            val flow = flow(Vector3d(0.0, 0.0, 20.0))
            val enabled = prepare(rotor(rpm, sails), flow, true)
            val disabled = prepare(rotor(rpm, sails), flow, false)
            assertTrue(kotlin.math.abs(enabled.torque.z()) > 1e-6)
            assertVector(enabled.force, disabled.force)
            assertVector(Vector3d(), disabled.torque)
        }
    }

    @Test
    fun `implicit acceleration does not restore disabled shaft torque`() {
        for (sails in listOf(false, true)) for (rpm in listOf(-128.0, 128.0))
            for (speed in listOf(0.0, 20.0)) {
            val disabled = prepare(rotor(rpm, sails), flow(Vector3d(0.0, 0.0, speed)), false)
            val result = PropellerImpulseSolver.solve(listOf(disabled), 1000.0, Matrix3d().scale(4000.0))
            assertTrue(result.first.length() > 1e-6, "Disabling shaft torque must retain thrust")
            assertVector(Vector3d(), result.second)
        }
    }

    @Test
    fun `powered rotor still damps ship spin with shaft reaction disabled`() {
        for (sails in listOf(false, true)) for (rpm in listOf(-128.0, 128.0))
            for (spin in listOf(-5.0, 5.0)) for (mass in listOf(1.0, 1000.0, 10000.0)) {
            val disabled = prepare(rotor(rpm, sails), flow(Vector3d(0.0, 0.0, 20.0), Vector3d(0.0, 0.0, spin)), false)
            assertTrue(disabled.torque.z() * spin < 0.0, "Passive torque must oppose ship spin")
            val inertia = mass * 4.0
            val result = PropellerImpulseSolver.solve(listOf(disabled), mass, Matrix3d().scale(inertia))
            val nextSpin = spin + result.second.z() * PropellerImpulseSolver.STEP / inertia
            assertTrue(nextSpin / spin in 0.0..<1.0, "Damping must not reverse or accelerate ship spin: $nextSpin")
        }
    }

    @Test
    fun `stopped rotors retain spin damping and passive full tick response with setting disabled`() {
        for (sails in listOf(false, true)) for (mass in listOf(1.0, 1000.0, 10000.0)) {
            val velocity = Vector3d(30.0, -20.0, 100.0)
            val omega = Vector3d(2.0, -3.0, 5.0)
            val flow = flow(velocity, omega)
            val enabled = prepare(rotor(0.0, sails, position = Vector3i(2, -1, 0)), flow, true)
            val disabled = prepare(rotor(0.0, sails, position = Vector3i(2, -1, 0)), flow, false)
            assertLoads(enabled, disabled)
            assertTrue(disabled.torque.length() > 1e-6)
            val inertia = Matrix3d().scaling(mass * 2, mass * 3, mass * 4)
            val result = PropellerImpulseSolver.solve(listOf(disabled), mass, inertia)
            val nextV = Vector3d(velocity).add(Vector3d(result.first).mul(PropellerImpulseSolver.STEP / mass))
            val nextW = Vector3d(omega).add(Matrix3d(inertia).invert().transform(result.second, Vector3d()).mul(PropellerImpulseSolver.STEP))
            fun energy(v: Vector3dc, w: Vector3dc) = mass * v.lengthSquared() + w.dot(inertia.transform(w, Vector3d()))
            assertTrue(energy(nextV, nextW) < energy(velocity, omega))
        }
    }

    @Test
    fun `mixed nonparallel rotors keep order independent and rotationally covariant impulses`() {
        val rotation = Quaterniond().rotateXYZ(.7, -.4, .2)
        val basis = Matrix3d().rotation(rotation)
        val inertia = Matrix3d().scaling(30000.0, 50000.0, 80000.0)
        val rotatedInertia = Matrix3d(basis).mul(inertia).mul(basis.transpose(Matrix3d()))
        val flow = flow(Vector3d(5.0, -10.0, 15.0), Vector3d(.4, -.5, .9))
        val rotatedFlow = flow.copy(modelToWorld = Matrix4d().rotate(rotation),
            velocityWorld = rotation.transform(flow.velocityWorld, Vector3d()),
            angularVelocityWorld = rotation.transform(flow.angularVelocityWorld, Vector3d()))
        fun loads(f: PropellerAerodynamics.Flow) = listOf(
            prepare(rotor(128.0, position = Vector3i(2, -1, 0)), f, false),
            prepare(rotor(-64.0, position = Vector3i(-2, 1, 0)).apply {
                bearingTiltQuat = Quaterniond().rotateY(.8)
            }, f, false),
            prepare(rotor(0.0, sails = true, position = Vector3i(0, 2, 1)), f, false)
        )
        val prepared = loads(flow)
        val result = PropellerImpulseSolver.solve(prepared, 10000.0, inertia)
        val reversed = PropellerImpulseSolver.solve(prepared.reversed(), 10000.0, inertia)
        assertVector(result.first, reversed.first)
        assertVector(result.second, reversed.second)
        val rotated = PropellerImpulseSolver.solve(loads(rotatedFlow), 10000.0, rotatedInertia)
        assertVector(rotation.transform(result.first, Vector3d()), rotated.first)
        assertVector(rotation.transform(result.second, Vector3d()), rotated.second)
    }

    @Test
    fun `torque cap is evaluated after cancelling commanded shaft torque`() {
        for (sails in listOf(false, true)) for (speed in listOf(0.0, 20.0)) {
            val flow = flow(Vector3d(0.0, 0.0, speed))
            val uncapped = prepare(rotor(sails = sails), flow, enabled = false)
            val enabled = prepare(rotor(sails = sails), flow, enabled = true)
            val cap = enabled.torque.length() * .1
            assertTrue(cap > 1e-6)
            val cappedDisabled = prepare(rotor(sails = sails), flow, enabled = false, maxTorque = cap)
            val cappedEnabled = prepare(rotor(sails = sails), flow, enabled = true, maxTorque = cap)
            assertLoads(uncapped, cappedDisabled)
            assertTrue(cappedEnabled.force.length() < uncapped.force.length() * .11)
            val integrated = PropellerImpulseSolver.solve(listOf(cappedDisabled), 1000.0, Matrix3d().scale(4000.0), maxTorque = cap)
            assertTrue(integrated.second.length() <= cap + 1e-8)
            val uncappedIntegrated = PropellerImpulseSolver.solve(listOf(uncapped), 1000.0, Matrix3d().scale(4000.0))
            assertVector(uncappedIntegrated.first, integrated.first)
            assertVector(uncappedIntegrated.second, integrated.second)
        }
    }

    private fun rotor(
        rpm: Double = 128.0, sails: Boolean = false, balanced: Boolean = true, position: Vector3i = Vector3i()
    ) = PropData(position, Vector3d(0.0, 0.0, 1.0), 0.0, rpm * .3,
        if (balanced) listOf(Vector3i(2, 0, 0), Vector3i(-2, 0, 0), Vector3i(0, 2, 0), Vector3i(0, -2, 0))
        else listOf(Vector3i(2, 0, 0)), false, true, sails,
        List(if (balanced) 4 else 1) { BladeData(true, -12.0, 3.5) })

    private fun flow(velocity: Vector3dc = Vector3d(), omega: Vector3dc = Vector3d()) =
        PropellerAerodynamics.Flow(Matrix4d(), com, velocity, omega, Vector3d()) { 1.225 }

    private fun prepare(
        prop: PropData, flow: PropellerAerodynamics.Flow = flow(), enabled: Boolean, maxTorque: Double = 0.0
    ) = PropellerAerodynamics.prepare(prop, flow, 37.0, 240.0, 0.0, maxTorque, spinTorqueEnabled = enabled)

    private fun assertLoads(a: PropellerAerodynamics.PreparedRotor, b: PropellerAerodynamics.PreparedRotor) {
        assertVector(a.force, b.force)
        assertVector(a.torque, b.torque)
        assertVector(a.axis, b.axis)
        assertResponses(a, b)
    }

    private fun assertResponses(a: PropellerAerodynamics.PreparedRotor, b: PropellerAerodynamics.PreparedRotor) {
        assertEquals(a.sections.size, b.sections.size)
        a.sections.zip(b.sections).forEach { (left, right) ->
            assertVector(left.lever, right.lever)
            for (column in 0..2) assertVector(left.response.getColumn(column, Vector3d()), right.response.getColumn(column, Vector3d()))
        }
    }

    private fun assertVector(expected: Vector3dc, actual: Vector3dc) {
        assertTrue(expected.distance(actual) <= 1e-8 * maxOf(1.0, expected.length()), "Expected $expected, got $actual")
    }
}
