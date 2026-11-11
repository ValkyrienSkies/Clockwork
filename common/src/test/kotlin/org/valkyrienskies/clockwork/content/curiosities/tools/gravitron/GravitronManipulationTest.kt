package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import org.joml.Quaterniond
import org.joml.Vector2d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GravitronManipulationTest {
    @Test fun dragUsesTheViewAxesAndRetainsTheNewOrientationAfterRelease() {
        val reference = Vector2d(35.0, 120.0)
        val originalView = GravitronManipulation.view(reference.x, reference.y)
        val currentView = GravitronManipulation.view(-20.0, -65.0)
        val originalShip = Quaterniond().rotateXYZ(0.3, 0.7, -0.2)
        val editedShip = GravitronManipulation.rotate(originalShip, reference, 12.0, -8.0)
        val target = Quaterniond(currentView).mul(Quaterniond(originalView).invert()).mul(editedShip)
        val expected = Quaterniond(currentView).mul(GravitronManipulation.view(-8.0, 12.0))
            .mul(Quaterniond(originalView).invert()).mul(originalShip)
        assertEquals(1.0, kotlin.math.abs(target.dot(expected)), 1e-12)
        assertEquals(1.0, kotlin.math.abs(originalShip.dot(Quaterniond().rotateXYZ(0.3, 0.7, -0.2))), 1e-12)
        assertEquals(1.0, kotlin.math.abs(editedShip.dot(GravitronManipulation.rotate(editedShip, reference, 0.0, 0.0))), 1e-12)
    }

    @Test fun repeatedRotationStaysNormalizedWithoutEulerLimits() {
        var rotation = Quaterniond()
        repeat(10000) { rotation = GravitronManipulation.rotate(rotation, Vector2d(60.0, -140.0), 8.0, 3.0) }
        assertEquals(1.0, rotation.lengthSquared(), 1e-12)
        assertTrue(rotation.isFinite)
        assertEquals(rotation, GravitronManipulation.rotate(rotation, Vector2d(), Double.NaN, 0.0))
    }

    @Test fun distanceUsesScrollDirectionAndRespectsBothRangeLimits() {
        assertEquals(9.0, GravitronManipulation.distance(10.0, 1.0, 20.0))
        assertEquals(11.0, GravitronManipulation.distance(10.0, -1.0, 20.0))
        assertEquals(2.0, GravitronManipulation.distance(2.1, 1.0, 20.0))
        assertEquals(20.0, GravitronManipulation.distance(19.5, -1.0, 20.0))
        assertEquals(1000.0, GravitronManipulation.distance(999.5, -1.0, 1000.0))
        assertEquals(10.0, GravitronManipulation.distance(10.0, Double.NaN, 20.0))
        assertEquals(10.0, GravitronManipulation.distance(10.0, Double.POSITIVE_INFINITY, 20.0))
    }

    @Test fun rigidLoadsReceiveFullCompensationAndElasticLoadsHaveBoundedGain() {
        assertEquals(1.0, GravitronManipulation.supportedGain(1000.0, 1000.0, 1000.0, 1.0 / 60))
        assertEquals(100.0, GravitronManipulation.supportedGain(1000.0, 100000.0, 100000.0, 1.0 / 60))
        for (delta in listOf(1.0 / 60, 1.0 / 120, 1.0 / 30)) {
            val gain = GravitronManipulation.supportedGain(1000.0, 1e12, 1000.0, delta)
            assertTrue(gain > 1.0 && gain < 1.0 / (20.0 * delta + 160.0 * delta * delta))
        }
        assertEquals(1.0, GravitronManipulation.supportedGain(1000.0, 1e12, 1000.0, 0.05))
        assertEquals(1.0, GravitronManipulation.supportedGain(1000.0, 1000.0, 1000.0, 1.0 / 60))
        assertEquals(1.0, GravitronManipulation.supportedGain(1000.0, Double.NaN, 1000.0, 1.0 / 60))
    }
}
