package org.valkyrienskies.clockwork.content.physicalities.wheel

import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class WheelWaterTest {
    @Test
    fun paddleDragDissipatesEnergyWithoutReversingMotion() {
        for (dt in listOf(1.0 / 120, 1.0 / 60, .05)) for (speed in listOf(-80.0, -2.0, 0.0, 2.0, 80.0)) {
            val velocity = Vector3d(3.0, speed, -4.0)
            val force = WheelInteractions.paddleForce(velocity, Vector3d(0.0, 1.0, 0.0), 500.0, .1, dt)
            assertTrue(force.dot(velocity) <= 0)
            val next = Vector3d(velocity).fma(dt * .1, force)
            assertTrue(next.lengthSquared() <= velocity.lengthSquared())
            assertTrue(next.y * speed >= 0)
        }
    }

    @Test
    fun halfSubmergedWheelPushesForwardAndLoadsTheAxle() {
        fun forces(spin: Double, submergedOnly: Boolean): Pair<Vector3d, Double> {
            val total = Vector3d()
            var torque = 0.0
            for (i in 0 until 8) {
                val point = Vector3d(0.0, cos((i + .5) * PI / 4), sin((i + .5) * PI / 4))
                if (submergedOnly && point.y > 0) continue
                val tangent = Vector3d(0.0, -point.z, point.y)
                val force = WheelInteractions.paddleForce(Vector3d(tangent).mul(spin), tangent, 100.0, .01, 1.0 / 60)
                total.add(force)
                torque += Vector3d(point).cross(force).x
            }
            return total to torque
        }
        val forward = forces(10.0, true)
        val reverse = forces(-10.0, true)
        assertTrue(forward.first.z > 0 && forward.second < 0)
        assertEquals(-forward.first.z, reverse.first.z, 1e-8)
        assertEquals(0.0, forces(10.0, false).first.length(), 1e-8)
        assertEquals(0.0, forces(0.0, true).first.length(), 1e-8)
    }

    @Test
    fun coastingPaddleSlowsWithoutAddingEnergy() {
        var speed = 15.0
        repeat(600) {
            val next = speed + WheelInteractions.waterDrag(speed, 300.0, .02, 1.0 / 60) * .02 / 60
            assertTrue(next in 0.0..speed)
            speed = next
        }
        assertTrue(speed < .1)
    }
}
