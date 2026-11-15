package org.valkyrienskies.clockwork.content.physicalities.wheel

import org.joml.Vector3d
import org.joml.Vector3dc
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.hypot

class WheelStepAssistTest {
    private data class Box(val min: Vector3d, val max: Vector3d)
    private val settings = WheelStepAssist.Settings(1.05, 0.35, 40000.0)

    private fun probe(boxes: List<Box>, velocity: Vector3d = Vector3d()) =
        { start: Vector3dc, direction: Vector3dc, length: Double ->
            boxes.mapNotNull { box ->
                var near = 0.0
                var far = length
                var normal = Vector3d(direction).negate()
                for (axis in 0..2) {
                    val d = direction.get(axis)
                    if (abs(d) < 1e-12) {
                        if (start.get(axis) < box.min.get(axis) || start.get(axis) > box.max.get(axis)) far = -1.0
                    } else {
                        val a = (box.min.get(axis) - start.get(axis)) / d
                        val b = (box.max.get(axis) - start.get(axis)) / d
                        val entry = minOf(a, b)
                        if (entry > near) {
                            near = entry
                            normal = Vector3d().setComponent(axis, if (d > 0.0) -1.0 else 1.0)
                        }
                        far = minOf(far, maxOf(a, b))
                    }
                }
                if (near <= far && far >= 0.0) WheelStepAssist.Hit(99L, near, normal, velocity) else null
            }.minByOrNull { it.distance }
        }

    private fun terrain(height: Double = 1.0, reverse: Boolean = false, narrow: Boolean = false) = listOf(
        Box(Vector3d(-100.0, -2.0, -100.0), Vector3d(100.0, 0.0, 100.0)),
        Box(Vector3d(if (narrow) -0.1 else -10.0, 0.0, if (reverse) -10.0 else 1.2),
            Vector3d(if (narrow) 0.1 else 10.0, height, if (reverse) -1.2 else 10.0)))

    private fun calculate(boxes: List<Box> = terrain(), radius: Double = 1.5, height: Double = radius - 0.1,
                          velocity: Vector3d = Vector3d(0.0, 0.0, 2.0), surfaceVelocity: Vector3d = Vector3d(),
                          rpm: Double? = 256.0, load: Double = 120000.0, torque: Double = 256000.0,
                          config: WheelStepAssist.Settings = settings): WheelStepAssist.Push? {
        val center = Vector3d(0.0, height, 0.0)
        val axle = Vector3d(1.0, 0.0, 0.0)
        val step = WheelStepAssist.probe(center, axle, Vector3d(0.0, 1.0, 0.0), velocity, radius, .4, rpm,
            config, probe(boxes, surfaceVelocity)) ?: return null
        return WheelStepAssist.force(center, axle, Vector3d(velocity).sub(surfaceVelocity), radius, rpm, torque, load, config, step)
    }

    @Test fun `assist lifts without kicking backwards in both driving directions`() {
        for (direction in listOf(-1.0, 1.0)) {
            val velocity = Vector3d(0.0, 0.0, direction * 2.0)
            val result = calculate(terrain(reverse = direction < 0), velocity = velocity, rpm = direction * 256.0)!!
            assertTrue(result.force.y > 0.0)
            assertEquals(0.0, result.force.z, 1e-9)
            assertTrue(result.force.length() <= settings.maximumForce)
            assertTrue(result.force.dot(velocity) <= 0.0)
        }
    }

    @Test fun `assist preserves loaded tire compression when approaching a ledge`() {
        val center = Vector3d(0.0, .8, 0.0)
        val step = WheelStepAssist.probe(center, Vector3d(1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0),
            Vector3d(0.0, 0.0, 1.0), 1.136, .4, 16.0, settings, probe(terrain()))!!
        assertEquals(.8, step.rideHeight, 1e-9)
    }

    @Test fun `flat ground tall walls narrow tops and ceilings receive no assist`() {
        assertNull(calculate(terrain().take(1)))
        assertNull(calculate(terrain(height = 3.0)))
        assertNull(calculate(terrain(narrow = true)))
        assertNull(calculate(terrain() + Box(Vector3d(-10.0, 2.5, -10.0), Vector3d(10.0, 3.0, 10.0))))
        assertNull(calculate(radius = 0.5))
    }

    @Test fun `ledge can support the center and one shoulder without covering the entire tire`() {
        val ground = terrain().first()
        val edge = Box(Vector3d(-.1, 0.0, 1.2), Vector3d(10.0, 1.0, 10.0))
        assertNotNull(calculate(listOf(ground, edge)))
        assertNull(calculate(terrain(narrow = true)))
    }

    @Test fun `uneven ledge tops do not disable support`() {
        val ground = terrain().first()
        val low = Box(Vector3d(-10.0, 0.0, 1.2), Vector3d(.1, .5, 10.0))
        val high = Box(Vector3d(.1, 0.0, 1.2), Vector3d(10.0, 1.0, 10.0))
        assertNotNull(calculate(listOf(ground, low, high)))
    }

    @Test fun `tilted tire samples ledge heights in the gravity frame`() {
        for (tilt in listOf(-.45, -.2, .2, .45)) {
            val axle = Vector3d(1.0, tilt, 0.0).normalize()
            val step = WheelStepAssist.probe(Vector3d(0.0, 1.4, 0.0), axle, Vector3d(0.0, 1.0, 0.0),
                Vector3d(0.0, 0.0, 1.0), 1.5, .8, 64.0, settings, probe(terrain()))
            assertNotNull(step)
            assertEquals(1.0, step!!.height, 1e-9)
        }
    }

    @Test fun `tilting does not count vertical climbing as sideways travel`() {
        val step = WheelStepAssist.Step(99L, Vector3d(0.0, 1.0, 0.0), Vector3d(0.0, 0.0, 1.0),
            Vector3d(0.0, 1.0, 0.0), 1.0, 3.0, 1.4)
        val center = Vector3d(0.0, 2.4, -.5)
        val straight = WheelStepAssist.force(center, Vector3d(1.0, 0.0, 0.0), Vector3d(),
            1.5, 64.0, 64000.0, 120000.0, settings, step)
        assertNotNull(straight)
        val tilted = WheelStepAssist.force(center, Vector3d(1.0, .49, 0.0).normalize(), Vector3d(),
            1.5, 64.0, 64000.0, 120000.0, settings, step)
        assertEquals(straight, tilted)
    }

    @Test fun `airborne braking parked and already climbing wheels are not lifted`() {
        assertNull(calculate(load = 0.0))
        assertNull(calculate(height = 4.0))
        assertNull(calculate(rpm = 0.0))
        assertNull(calculate(velocity = Vector3d(), rpm = null))
        assertNull(calculate(velocity = Vector3d(0.0, 3.0, 2.0)))
        assertNull(calculate(velocity = Vector3d(0.0, -3.0, 2.0)))
    }

    @Test fun `moving terrain uses relative motion`() {
        val stationary = calculate()!!.force
        val moving = calculate(velocity = Vector3d(0.0, 0.0, 5.0), surfaceVelocity = Vector3d(0.0, 0.0, 3.0))!!.force
        assertEquals(stationary, moving)
        assertNull(calculate(velocity = Vector3d(0.0, 0.0, 3.0), surfaceVelocity = Vector3d(0.0, 0.0, 3.0), rpm = null))
    }

    @Test fun `strength torque and supported load bound the force`() {
        val full = calculate()!!.force.length()
        val stronger = calculate(load = 20000.0, config = settings.copy(maximumForce = 1e6))!!.force.length()
        val weaker = calculate(load = 20000.0, config = settings.copy(strength = .1, maximumForce = 1e6))!!.force.length()
        assertTrue(stronger > weaker)
        assertTrue(calculate(torque = 1000.0, load = 20000.0)!!.force.length() < stronger)
        assertTrue(calculate(load = 10000.0)!!.force.length() < full)
        val bounded = calculate(load = 1e9, torque = 1e9)!!.force.length()
        assertTrue(bounded <= settings.maximumForce + 1e-8)
        assertNull(calculate(config = settings.copy(strength = 0.0)))
        assertNull(calculate(config = settings.copy(height = Double.NaN)))
    }

    @Test fun `combined lift and actual traction stay within the force budget`() {
        for (load in listOf(0.0, 50000.0, 150000.0, 200000.0)) {
            val grip = WheelStepAssist.traction(1000.0, load, 2.0, 10000.0, 20000.0, 1.2, 1.0 / 60, 200000.0)
            assertTrue(hypot(load, grip) <= 200000.0 + 1e-8)
        }
    }

    @Test fun `loaded tire gets lift to clear the round envelope at the corner`() {
        val step = WheelStepAssist.Step(99L, Vector3d(0.0, 1.0, 0.0), Vector3d(0.0, 0.0, 1.0),
            Vector3d(0.0, 1.0, 0.0), 1.0, 2.5, .65)
        val result = WheelStepAssist.force(Vector3d(0.0, 1.5, -.5), Vector3d(1.0, 0.0, 0.0), Vector3d(),
            1.136, 16.0, 64000.0, 120000.0, settings.copy(maximumForce = 200000.0), step)!!
        assertTrue(result.force.y > 120000.0)
        assertEquals(0.0, result.force.z, 1e-9)
    }

    @Test fun `lift begins before tire hits the vertical face and stops above the ramp`() {
        val center = Vector3d(0.0, 1.4, -1.0)
        val axle = Vector3d(1.0, 0.0, 0.0)
        val up = Vector3d(0.0, 1.0, 0.0)
        val velocity = Vector3d(0.0, 0.0, 2.0)
        val step = WheelStepAssist.probe(center, axle, up, velocity, 1.5, .4, 32.0, settings, probe(terrain()))!!
        assertTrue(step.edge.z - center.z > 1.5)
        assertTrue(WheelStepAssist.force(center, axle, velocity, 1.5, 32.0, 256000.0, 40000.0, settings, step)!!.force.y > 0)
        assertNull(WheelStepAssist.force(Vector3d(center).add(0.0, 2.0, 0.0), axle, velocity,
            1.5, 32.0, 256000.0, 40000.0, settings, step))
    }

    @Test fun `virtual traction opposes slip and cannot reverse it in one tick`() {
        val mass = 900.0
        val inertia = 1700.0
        val radius = 1.5
        val dt = 1.0 / 60.0
        for (slip in listOf(-20.0, -.1, 0.0, .1, 20.0)) {
            val force = WheelStepAssist.traction(slip, 50000.0, .8, mass, inertia, radius, dt)
            assertTrue(force * slip <= 0.0)
            assertTrue(abs(force) <= 40000.0)
            val after = slip + force * dt * (1.0 / mass + radius * radius / inertia)
            assertTrue(abs(after) <= abs(slip) + 1e-10)
            assertTrue(after * slip >= -1e-10)
        }
        assertEquals(0.0, WheelStepAssist.traction(20.0, 50000.0, 0.0, mass, inertia, radius, dt), 1e-12)
        assertEquals(0.0, WheelStepAssist.traction(20.0, 0.0, .8, mass, inertia, radius, dt), 1e-12)
    }
}
