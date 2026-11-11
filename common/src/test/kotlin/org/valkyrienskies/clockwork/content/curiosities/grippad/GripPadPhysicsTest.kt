package org.valkyrienskies.clockwork.content.curiosities.grippad

import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GripPadPhysicsTest {
    @Test fun gripNeverAppliesSuctionOrResistsLifting() {
        for (normal in listOf(Vector3d(0.0, 1.0, 0.0), Vector3d(1.0, 0.0, 0.0), Vector3d(1.0, 1.0, 1.0).normalize())) {
            val force = GripPadPhysics.force(Vector3d(0.2, 0.3, -0.1), Vector3d(4.0, 2.0, -3.0), normal, 5000.0, 0.002, 1, 1.0 / 60)
            assertEquals(0.0, force.dot(normal), 1e-9)
            assertEquals(0.0, GripPadPhysics.force(Vector3d(normal).mul(3.0), Vector3d(normal).mul(8.0), normal, 5000.0, 0.002, 1, 1.0 / 60).length(), 1e-9)
        }
    }

    @Test fun multiplePadsShareTheForceAndDampingBudget() {
        val normal = Vector3d(0.0, 1.0, 0.0)
        val error = Vector3d(0.1, 0.0, 0.2)
        val velocity = Vector3d(1.0, 0.0, 2.0)
        val single = GripPadPhysics.force(error, velocity, normal, 5000.0, 0.002, 1, 1.0 / 60)
        for (pads in 2..16) {
            val shared = GripPadPhysics.force(error, velocity, normal, 5000.0, 0.002, pads, 1.0 / 60).mul(pads.toDouble())
            assertEquals(0.0, shared.distance(single), 1e-8)
        }
    }

    @Test fun dampingDoesNotReverseVelocityEvenWithHeavyConnectedLoads() {
        for (load in listOf(100.0, 5000.0, 1e8)) for (delta in listOf(1.0 / 120, 1.0 / 60, 0.05)) {
            val velocity = Vector3d(20.0, 0.0, 0.0)
            val force = GripPadPhysics.force(Vector3d(), velocity, Vector3d(0.0, 1.0, 0.0), load, 0.01, 1, delta)
            val next = Vector3d(velocity).fma(delta * 0.01, force)
            assertTrue(next.x in 0.0..20.0)
            assertTrue(force.length() <= load * 35.0 + 1e-8)
        }
    }

    @Test fun matchingSurfaceVelocityNeedsNoCorrection() {
        assertEquals(Vector3d(), GripPadPhysics.force(Vector3d(), Vector3d(), Vector3d(0.0, 1.0, 0.0), 5000.0, 0.001, 4, 1.0 / 60))
    }
}
