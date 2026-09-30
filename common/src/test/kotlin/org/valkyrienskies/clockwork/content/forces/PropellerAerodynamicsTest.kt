package org.valkyrienskies.clockwork.content.forces

import org.joml.Matrix4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.tan

class PropellerAerodynamicsTest {
    private val density = 1.225

    @Test
    fun `lift stalls and broadside drag dominates`() {
        val low = polar(4.0)
        val peak = polar(15.0)
        val stalled = polar(30.0)
        val broadside = polar(90.0)
        assertEquals(2.0 * PI * Math.toRadians(4.0), low.lift, 1e-12)
        assertTrue(peak.lift > low.lift && peak.lift > stalled.lift)
        assertTrue(stalled.drag > peak.drag)
        assertEquals(0.0, broadside.lift, 1e-12)
        assertEquals(2.01, broadside.drag, 1e-12)
    }

    @Test
    fun `polar is bounded symmetric and continuous through reverse flow`() {
        for (degrees in -720..720) {
            val value = polar(degrees.toDouble())
            val opposite = polar(-degrees.toDouble())
            assertTrue(value.lift.isFinite() && abs(value.lift) < 2.0)
            assertTrue(value.drag in 0.0..2.011)
            assertEquals(-value.lift, opposite.lift, 1e-12)
            assertEquals(value.drag, opposite.drag, 1e-12)
        }
        for (boundary in listOf(-180.0, -90.0, -30.0, -12.0, 0.0, 12.0, 30.0, 90.0, 180.0)) {
            val left = polar(boundary - 1e-6)
            val right = polar(boundary + 1e-6)
            assertEquals(left.lift, right.lift, 1e-6)
            assertEquals(left.drag, right.drag, 1e-6)
        }
    }

    @Test
    fun `section never adds energy to its relative motion`() {
        for (pitch in listOf(-90.0, -45.0, -4.0, 0.0, 4.0, 45.0, 90.0)) {
            val chord = Vector3d(0.0, 1.0, 0.0).rotateX(Math.toRadians(pitch))
            for (x in listOf(-5.0, 0.0, 5.0)) for (y in listOf(-10.0, 0.0, 10.0)) for (z in listOf(-8.0, 0.0, 8.0)) {
                val velocity = Vector3d(x, y, z)
                val force = PropellerAerodynamics.sectionForce(velocity, Vector3d(1.0, 0.0, 0.0), chord, density)
                assertTrue(force.isFinite)
                assertTrue(force.dot(velocity) <= 1e-9, "Section must dissipate relative kinetic energy")
                val reversed = PropellerAerodynamics.sectionForce(Vector3d(velocity).negate(), Vector3d(1.0, 0.0, 0.0), chord, density)
                assertVector(Vector3d(force).negate(), reversed)
            }
        }
    }

    @Test
    fun `spanwise wind produces skin drag without section lift`() {
        val force = PropellerAerodynamics.sectionForce(
            Vector3d(10.0, 0.0, 0.0), Vector3d(1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0), density
        )
        assertTrue(force.x < 0.0)
        assertEquals(0.0, force.y, 0.0)
        assertEquals(0.0, force.z, 0.0)
    }

    @Test
    fun `surface area uses both transformed edge lengths`() {
        val velocity = Vector3d(0.0, 10.0, 2.0)
        val force = PropellerAerodynamics.sectionForce(velocity, Vector3d(1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0), density)
        val scaled = PropellerAerodynamics.sectionForce(velocity, Vector3d(2.0, 0.0, 0.0), Vector3d(0.0, 3.0, 0.0), density)
        assertVector(force.mul(6.0), scaled)
    }

    @Test
    fun `span integral matches analytic thrust and shaft drag torque`() {
        val radius = 3.0
        val count = 4
        val prop = rotor(length = radius, count = count)
        val (force, torque) = solve(prop)
        val pressurePerRadiusSquared = 0.5 * density * (2.0 * PI).pow(2)
        val liftCoefficient = 2.0 * PI * Math.toRadians(4.0)
        val dragCoefficient = 0.01 + 0.01 * liftCoefficient.pow(2)
        val thrust = count * pressurePerRadiusSquared * 0.25 * liftCoefficient * radius.pow(3) / 3.0
        val shaftTorque = -count * pressurePerRadiusSquared * 0.25 * dragCoefficient * radius.pow(4) / 4.0
        assertVector(Vector3d(0.0, 0.0, thrust), force)
        assertVector(Vector3d(0.0, 0.0, shaftTorque), torque)
    }

    @Test
    fun `stalled rotor loses axial lift while broadside blades resist rotation`() {
        val peak = solve(rotor(pitch = -15.0))
        val stalled = solve(rotor(pitch = -45.0))
        val broadside = solve(rotor(pitch = -90.0))
        assertTrue(peak.first.z() > stalled.first.z())
        assertTrue(abs(broadside.first.z()) < 1e-8)
        assertTrue(broadside.second.z() < stalled.second.z())
    }

    @Test
    fun `span quadrature converges in mixed inflow and stall`() {
        val flow = flow(velocity = Vector3d(4.0, -2.0, 3.0), omega = Vector3d(0.4, -0.5, 0.2))
        for (pitch in listOf(-4.0, -20.0, -45.0)) {
            val ordinary = solve(rotor(pitch = pitch, length = 4.0), flow, segments = 8)
            val reference = solve(rotor(pitch = pitch, length = 4.0), flow, segments = 64)
            assertVector(reference.first, ordinary.first, 0.01)
            assertVector(reference.second, ordinary.second, 0.01)
        }
    }

    @Test
    fun `ship spin can cancel rotor spin at every blade section`() {
        for (sails in listOf(false, true)) {
            val prop = rotor(sails = sails)
            val (force, torque) = solve(prop, flow(omega = Vector3d(0.0, 0.0, -2.0 * PI)))
            assertVector(Vector3d(), force)
            assertVector(Vector3d(), torque)
        }
    }

    @Test
    fun `crosswind distinguishes advancing and retreating blades`() {
        val wind = flow(wind = Vector3d(2.0, 0.0, 0.0))
        val advancing = solve(rotor(count = 1), wind, angle = 0.0).first
        val retreating = solve(rotor(count = 1), wind, angle = 180.0).first
        assertTrue(abs(advancing.z() - retreating.z()) > 1.0)
    }

    @Test
    fun `ship pitch rate produces a distributed damping moment`() {
        val base = solve(rotor(length = 3.0)).second
        val pitching = solve(rotor(length = 3.0), flow(omega = Vector3d(0.8, 0.0, 0.0))).second
        assertTrue(abs(base.x()) < 1e-8)
        assertTrue(pitching.x() < -1.0)
    }

    @Test
    fun `a stopped rotor still resists translation through air`() {
        for (sails in listOf(false, true)) {
            val velocity = Vector3d(5.0, -2.0, 8.0)
            val result = solve(rotor(sails = sails, rpm = 0.0), flow(velocity = velocity))
            assertTrue(result.first.dot(velocity) < 0.0)
        }
    }

    @Test
    fun `density is sampled at the actual section heights`() {
        val heights = mutableListOf<Double>()
        val prop = rotor(length = 2.0, count = 1)
        solve(prop, flow().copy(densityAtHeight = { heights.add(it); density }))
        assertEquals(16, heights.size)
        assertTrue(heights.min() > -2.0 && heights.min() < -1.9)
        assertTrue(heights.max() > -0.1 && heights.max() < 0.0)
        val upward = rotor(axis = Vector3d(0.0, 1.0, 0.0))
        val result = solve(upward, flow().copy(densityAtHeight = { if (it > 0.9) density else 0.0 }))
        assertTrue(result.first.y() > 0.0, "Density at the hub one block above the COM must be used")
    }

    @Test
    fun `all six bearing facings have perpendicular evenly spaced blades`() {
        for (axis in listOf(
            Vector3d(1.0, 0.0, 0.0), Vector3d(-1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0),
            Vector3d(0.0, -1.0, 0.0), Vector3d(0.0, 0.0, 1.0), Vector3d(0.0, 0.0, -1.0)
        )) {
            val (force, torque) = solve(rotor(axis = axis))
            val spinAxis = Vector3d(abs(axis.x), abs(axis.y), abs(axis.z))
            assertTrue(force.dot(spinAxis) > 0.0 && torque.dot(spinAxis) < 0.0)
            assertVector(Vector3d(axis).mul(force.dot(axis)), force)
            assertVector(Vector3d(axis).mul(torque.dot(axis)), torque)
        }
    }

    @Test
    fun `negative facings follow the rendered positive-axis rotation`() {
        for (axis in listOf(Vector3d(0.0, 0.0, 1.0), Vector3d(0.0, 0.0, -1.0))) {
            // At 90 degrees, the first horizontal blade has rotated from -Y to +X.
            // It moves along +Y, so its zero-pitch drag must point along -Y for either facing.
            val (force, torque) = solve(rotor(axis = axis, pitch = 0.0, count = 1), angle = 90.0)
            assertTrue(force.y() < 0.0)
            assertEquals(0.0, force.x(), 1e-8)
            assertEquals(0.0, force.z(), 1e-8)
            assertTrue(torque.z() < 0.0)
        }
    }

    @Test
    fun `tilted rotors keep their force and torque axis through a revolution`() {
        val tilt = Quaterniond().rotateX(0.4).rotateY(-0.2)
        val axis = tilt.transform(Vector3d(0.0, 0.0, 1.0))
        for (sails in listOf(false, true)) {
            fun tilted() = rotor(sails = sails).apply { bearingTiltQuat = tilt; bearingAxisRot = axis }
            val initial = solve(tilted())
            assertVector(Vector3d(axis).mul(initial.first.dot(axis)), initial.first)
            for (angle in listOf(45.0, 90.0, 180.0, 270.0)) {
                val result = solve(tilted(), angle = angle)
                assertVector(initial.first, result.first)
                assertVector(initial.second, result.second)
            }
        }
    }

    @Test
    fun `contra rotating blades with mirrored pitch preserve thrust and cancel shaft torque`() {
        val tilt = Quaterniond().rotateX(0.3).rotateY(-0.2)
        for (axis in listOf(
            Vector3d(1.0, 0.0, 0.0), Vector3d(-1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0),
            Vector3d(0.0, -1.0, 0.0), Vector3d(0.0, 0.0, 1.0), Vector3d(0.0, 0.0, -1.0)
        )) for (rpm in listOf(-60.0, 60.0)) {
            val normal = rotor(axis = axis, rpm = rpm, pitch = -4.0).apply { bearingTiltQuat = tilt }
            val reversed = rotor(axis = axis, rpm = -rpm, pitch = 4.0).apply {
                bearingTiltQuat = tilt
                inverted = true
            }
            val normalResult = solve(normal, angle = 37.0)
            val reversedResult = solve(reversed, angle = -37.0)
            val shaft = tilt.transform(axis, Vector3d())
            assertVector(normalResult.first, reversedResult.first)
            assertTrue(abs(normalResult.second.dot(shaft)) > 1e-6)
            assertEquals(-normalResult.second.dot(shaft), reversedResult.second.dot(shaft), 1e-8)
        }
    }

    @Test
    fun `sail pitch update is independent of count and order`() {
        val prop = rotor(sails = true).apply { currentBladePitch = Math.toRadians(12.0) }
        val reversed = PropData(prop.position, prop.bearingAxis, 0.0, prop.bearingSpeed, prop.sailPositions!!.reversed(), false, true, true, emptyList())
        val duplicated = PropData(prop.position, prop.bearingAxis, 0.0, prop.bearingSpeed, prop.sailPositions!! + prop.sailPositions!!, false, true, true, emptyList())
        val flow = flow(velocity = Vector3d(2.0, 3.0, 4.0), omega = Vector3d(0.4, -0.3, 0.2))
        val a = solve(prop, flow)
        val b = solve(reversed, flow)
        val c = solve(duplicated, flow)
        assertEquals(prop.currentBladePitch, reversed.currentBladePitch, 1e-12)
        assertEquals(prop.currentBladePitch, duplicated.currentBladePitch, 1e-12)
        assertVector(a.first, b.first)
        assertVector(a.second, b.second)
        assertVector(Vector3d(a.first).mul(2.0), c.first)
        assertVector(Vector3d(a.second).mul(2.0), c.second)
    }

    @Test
    fun `sails keep useful thrust beyond the former collective limits`() {
        val staticThrust = solve(rotor(sails = true, rpm = 64.0)).first.z()
        for (speed in listOf(-12.0, -6.0, 6.0, 12.0)) {
            val prop = rotor(sails = true, rpm = 64.0)
            val result = settled(prop, flow(velocity = Vector3d(0.0, 0.0, speed)))
            assertTrue(result.first.z() > staticThrust,
                "Collective should follow inflow and preserve useful thrust at $speed m/s")
            if (speed > 0.0) assertTrue(prop.currentBladePitch > Math.toRadians(30.0))
            else assertTrue(prop.currentBladePitch < Math.toRadians(-5.0))
        }
    }

    @Test
    fun `settled sail force has no corner at the former pitch limits`() {
        val omega = 64.0 * 2.0 * PI / 60.0
        for (oldLimit in listOf(-5.0, 30.0)) {
            // All four sails are at radius one, so their former boundary speed is analytic.
            val boundarySpeed = omega * tan(Math.toRadians(oldLimit - 4.0))
            fun thrust(speed: Double) = settled(rotor(sails = true, rpm = 64.0),
                flow(velocity = Vector3d(0.0, 0.0, speed))).first.z()
            val left = thrust(boundarySpeed - 0.01)
            val center = thrust(boundarySpeed)
            val right = thrust(boundarySpeed + 0.01)
            assertTrue(abs(right - 2.0 * center + left) < abs(center) * 1e-4,
                "Thrust slope should remain smooth across the former $oldLimit degree limit")
        }
    }

    @Test
    fun `reversing RPM and axial flow mirrors sails beyond the old pitch range`() {
        for (speed in listOf(-12.0, -6.0, 6.0, 12.0)) {
            val positive = rotor(sails = true, rpm = 64.0)
            val negative = rotor(sails = true, rpm = -64.0)
            val a = settled(positive, flow(velocity = Vector3d(0.0, 0.0, speed)))
            val b = settled(negative, flow(velocity = Vector3d(0.0, 0.0, -speed)))
            assertVector(Vector3d(a.first).negate(), b.first)
            assertVector(Vector3d(a.second).negate(), b.second)
            assertEquals(positive.currentBladePitch, negative.currentBladePitch, 1e-10)
        }
    }

    @Test
    fun `fully adjusted sails stay passive when stopped and at extreme advance ratios`() {
        for (rpm in listOf(-64.0, -0.001, 0.0, 0.001, 64.0)) {
            for (speed in listOf(-1000.0, -12.0, 12.0, 1000.0)) {
                val velocity = Vector3d(0.0, 0.0, speed)
                val result = settled(rotor(sails = true, rpm = rpm), flow(velocity = velocity))
                assertTrue(result.first.isFinite && result.second.isFinite)
                val translationPower = result.first.dot(velocity)
                val shaftPower = result.second.z() * rpm * 2.0 * PI / 60.0
                assertTrue(translationPower + shaftPower <= 1e-8 * maxOf(1.0, abs(translationPower), abs(shaftPower)),
                    "The aerodynamic forces must dissipate total relative motion, including rotor spin")
                if (rpm == 0.0) assertTrue(translationPower < 0.0, "Stopped sails must resist translation")
            }
        }
    }

    @Test
    fun `limiting includes the full moment and preserves its relation to force`() {
        val prop = rotor(length = 3.0)
        val base = solve(prop)
        val limited = PropellerAerodynamics.compute(prop, flow(), 0.0, 1.0, base.first.length() / 4.0, base.second.length() / 7.0)
        assertVector(Vector3d(base.first).div(7.0), limited.first)
        assertVector(Vector3d(base.second).div(7.0), limited.second)
    }

    @Test
    fun `invalid and degenerate sections contribute no force`() {
        for (velocity in listOf(Vector3d(), Vector3d(Double.NaN, 0.0, 0.0))) {
            assertVector(Vector3d(), PropellerAerodynamics.sectionForce(velocity, Vector3d(1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0), density))
        }
        assertVector(Vector3d(), PropellerAerodynamics.sectionForce(Vector3d(10.0), Vector3d(), Vector3d(1.0), density))
        assertVector(Vector3d(), solve(rotor(length = 0.0)).first)
    }

    private fun polar(degrees: Double) = PropellerAerodynamics.coefficients(Math.toRadians(degrees))

    private fun rotor(
        sails: Boolean = false, pitch: Double = -4.0, length: Double = 1.0, count: Int = 4,
        rpm: Double = 60.0, axis: Vector3dc = Vector3d(0.0, 0.0, 1.0)
    ) = PropData(
        Vector3i(), axis, 0.0, rpm * 0.3,
        listOf(Vector3i(1, 0, 0), Vector3i(-1, 0, 0), Vector3i(0, 1, 0), Vector3i(0, -1, 0)),
        false, true, sails, List(count) { BladeData(false, pitch, length) }
    ).apply { currentBladePitch = Math.toRadians(4.0) }

    private fun flow(
        velocity: Vector3dc = Vector3d(), omega: Vector3dc = Vector3d(), wind: Vector3dc = Vector3d()
    ) = PropellerAerodynamics.Flow(
        Matrix4d().translation(-0.5, -0.5, -0.5), Vector3d(0.5, 0.5, 0.5), velocity, omega, wind
    ) { density }

    private fun solve(
        prop: PropData, flow: PropellerAerodynamics.Flow = flow(), angle: Double = 0.0, segments: Int = 8
    ) = PropellerAerodynamics.compute(prop, flow, angle, 1.0, 1e12, 1e12, segments)

    private fun settled(prop: PropData, flow: PropellerAerodynamics.Flow): Pair<Vector3dc, Vector3dc> {
        repeat(500) { solve(prop, flow) }
        return solve(prop, flow)
    }

    private fun assertVector(expected: Vector3dc, actual: Vector3dc, relativeTolerance: Double = 1e-8) {
        assertTrue(expected.distance(actual) <= relativeTolerance * maxOf(1.0, expected.length()), "Expected $expected, got $actual")
    }
}
