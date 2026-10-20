package org.valkyrienskies.clockwork.content.forces

import org.joml.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import kotlin.math.*

class PropellerHoverAndCruiseTest {
    private val density = 1.225
    private val gravity = 10.0
    private val gain = 960.0
    private val step = 1.0 / 60.0
    private val trim = Math.toRadians(4.0)
    private val com = Vector3d(.5, .5, 1.5)

    @Test
    fun `stationary lift keeps its four degree trim and RPM squared scaling`() {
        for (radius in listOf(1, 3, 5)) for (rpm in listOf(16.0, 32.0, 64.0, 128.0)) {
            val props = pair(rpm, radius)
            val loads = props.map { prepare(it, 0.0) }
            val radiusSquaredSum = (1..radius).sumOf { it.toDouble().pow(2) } * 8.0
            val expected = .5 * density * gain * (rpm * PI / 30).pow(2) * radiusSquaredSum * 2 * PI * trim
            assertEquals(expected, loads.sumOf { it.force.z() }, expected * 1e-10)
            props.forEach { assertEquals(trim, it.currentBladePitch, 1e-12) }
        }
    }

    @Test
    fun `settled sails oppose both climb and sink near hover across sizes and RPM`() {
        for (radius in listOf(1, 3, 5)) for (rpm in listOf(16.0, 32.0, 64.0, 128.0)) {
            val mass = hoverMass(rpm, radius)
            val stationary = settledThrust(rpm, radius, 0.0, mass)
            for (speed in listOf(.05, .25)) {
                val climbing = settledThrust(rpm, radius, speed, mass)
                val sinking = settledThrust(rpm, radius, -speed, mass)
                assertTrue(climbing < stationary && sinking > stationary,
                    "Wrong damping: radius=$radius RPM=$rpm sink=$sinking hover=$stationary climb=$climbing")
            }
        }
    }

    @Test
    fun `hover recovers from vertical kicks and pitch errors at constant RPM under gravity`() {
        // Rotate the same propulsion model upright; this exercises the full implicit solve.
        val upright = Matrix4d().rotateX(-PI / 2)
        for (radius in listOf(1, 3, 5)) for (rpm in listOf(32.0, 64.0, 128.0))
            for ((kick, pitchError) in listOf(-.5 to 0.0, .5 to 0.0, 0.0 to -1.0, 0.0 to 1.0)) {
            val mass = hoverMass(rpm, radius)
            val props = pair(rpm, radius)
            props.forEach { it.currentBladePitch += Math.toRadians(pitchError) }
            var speed = kick
            var displacement = 0.0
            repeat(1800) {
                val force = thrust(props, speed, mass, transform = upright)
                speed += (force / mass - gravity) * step
                displacement += speed * step
                // Damping arrests motion; it does not return to a commanded altitude.
                assertTrue(abs(speed) < 2.0 && abs(displacement) < 5.0,
                    "Hover escaped: radius=$radius RPM=$rpm kick=$kick pitchError=$pitchError v=$speed y=$displacement")
            }
            assertTrue(abs(speed) < .01,
                "Hover did not settle: radius=$radius RPM=$rpm kick=$kick pitchError=$pitchError v=$speed")
        }
    }

    @Test
    fun `hover supports load changes with a small steady climb or sink instead of runaway`() {
        val mass = hoverMass(64.0, 3)
        for (loadFactor in listOf(.95, 1.05)) {
            val props = pair(64.0, 3)
            var speed = 0.0
            var lastAcceleration = 0.0
            repeat(1800) {
                lastAcceleration = thrust(props, speed, mass * loadFactor) / (mass * loadFactor) - gravity
                speed += lastAcceleration * step
                assertTrue(abs(speed) < 1.0, "Five percent load change caused runaway vertical motion: $speed")
            }
            assertTrue(speed * (1.0 - loadFactor) > 0.0)
            assertTrue(abs(lastAcceleration) < .001, "Vertical speed should approach a steady value")
        }
    }

    @Test
    fun `plane sails retain cruise thrust well beyond fixed pitch advance speeds`() {
        for (radius in listOf(1, 3, 5)) for (rpm in listOf(32.0, 64.0, 128.0)) {
            val mass = hoverMass(rpm, radius)
            val staticThrust = settledThrust(rpm, radius, 0.0, mass)
            val tipSpeed = rpm * PI / 30 * radius
            for (advance in listOf(.1, .25, .5, 1.0, 2.0)) {
                val props = pair(rpm, radius)
                repeat(400) { props.forEach { prepare(it, tipSpeed * advance) } }
                val force = thrust(props, tipSpeed * advance, mass)
                assertTrue(force > .3 * staticThrust && force < staticThrust,
                    "Cruise thrust should stay useful without a movement boost: r=$radius RPM=$rpm advance=$advance ratio=${force/staticThrust}")
                if (advance >= 1) assertTrue(props.first().currentBladePitch > Math.toRadians(30.0),
                    "Sail pitch must keep adapting beyond the old cap for forward flight")
            }
        }
    }

    @Test
    fun `plane accelerates from rest and recovers cruise after speed disturbances`() {
        // Longitudinal flight with quadratic hull drag; wings are assumed to support weight.
        for (rpm in listOf(32.0, 64.0, 128.0)) {
            val radius = 3
            val mass = hoverMass(rpm, radius)
            val cruiseSpeed = rpm * PI / 30 * radius
            val dragCoefficient = settledThrust(rpm, radius, cruiseSpeed, mass) / cruiseSpeed.pow(2)
            for (initialSpeed in listOf(0.0, cruiseSpeed * .8, cruiseSpeed * 1.2)) {
                val props = pair(rpm, radius)
                if (initialSpeed > 0) repeat(400) { props.forEach { prepare(it, initialSpeed) } }
                var speed = initialSpeed
                repeat(1800) {
                    val force = thrust(props, speed, mass) - dragCoefficient * speed * abs(speed)
                    speed += force / mass * step
                    assertTrue(speed.isFinite() && speed in 0.0..cruiseSpeed * 1.25,
                        "Unstable cruise at RPM=$rpm: $speed")
                }
                assertEquals(cruiseSpeed, speed, cruiseSpeed * .002,
                    "Plane should reach the same cruise speed from rest, below, or above it")
            }
        }
    }

    @Test
    fun `same radius and mixed radius rotors retain smooth thrust as pitch adapts`() {
        for (radius in listOf(1, 3, 5)) {
            val mass = hoverMass(64.0, radius)
            val tipSpeed = 64.0 * PI / 30 * radius
            var previous = settledThrust(64.0, radius, -.1 * tipSpeed, mass)
            val staticThrust = settledThrust(64.0, radius, 0.0, mass)
            for (i in -9..200) {
                val next = settledThrust(64.0, radius, i * .01 * tipSpeed, mass)
                assertTrue(abs(next - previous) < .06 * staticThrust, "Pitch adaptation introduced a thrust cliff")
                previous = next
            }
        }
    }

    private fun pair(rpm: Double, radius: Int) = listOf(false, true).map { inverted ->
        PropData(Vector3i(if (inverted) -radius - 1 else radius + 1, 0, 0), Vector3d(0.0, 0.0, 1.0),
            0.0, rpm * .3 * if (inverted) -1 else 1,
            (1..radius).flatMap { r -> listOf(Vector3i(r,0,0), Vector3i(-r,0,0), Vector3i(0,r,0), Vector3i(0,-r,0)) },
            inverted, true, true, emptyList())
    }

    private fun prepare(prop: PropData, speed: Double, transform: Matrix4dc = Matrix4d()): PropellerAerodynamics.PreparedRotor {
        val flow = PropellerAerodynamics.Flow(transform, com,
            transform.transformDirection(Vector3d(0.0, 0.0, speed)), Vector3d(), Vector3d()) { density }
        return PropellerAerodynamics.prepare(prop, flow, 0.0, gain, 0.0, 0.0)
    }

    private fun thrust(props: List<PropData>, speed: Double, mass: Double, transform: Matrix4dc = Matrix4d()): Double {
        val loads = props.map { prepare(it, speed, transform) }
        val force = PropellerImpulseSolver.solve(loads, mass, Matrix3d().scale(mass * 16)).first
        return force.dot(transform.transformDirection(Vector3d(0.0, 0.0, 1.0)))
    }

    private fun settledThrust(rpm: Double, radius: Int, speed: Double, mass: Double): Double {
        val props = pair(rpm, radius)
        repeat(400) { props.forEach { prepare(it, speed) } }
        return thrust(props, speed, mass)
    }

    private fun hoverMass(rpm: Double, radius: Int): Double {
        val props = pair(rpm, radius)
        val raw = props.sumOf { prepare(it, 0.0).force.z() }
        var low = raw / gravity * .5
        var high = raw / gravity * 1.5
        repeat(50) {
            val mass = (low + high) / 2
            if (thrust(props, 0.0, mass) > mass * gravity) low = mass else high = mass
        }
        return (low + high) / 2
    }
}
