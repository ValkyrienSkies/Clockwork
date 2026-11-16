package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity.Companion.thinkableAngleStep

class PhysBearingAngleTest {
    @Test fun targetsBeyondHalfATurnWrapInsteadOfClamping() {
        for ((target, expected) in listOf(181.0 to -179f, 270.0 to -90f, 359.5 to -.5f,
            360.0 to 0f, 450.0 to 90f, 720.0 to 0f, -181.0 to 179f, -270.0 to 90f,
            -360.0 to 0f, -450.0 to -90f)) {
            assertEquals(expected, thinkableAngleStep(0f, target, 360f), 1e-5f, "target $target")
        }
    }

    @Test fun movementAcrossAngleBoundariesUsesTheShortestPathAndSpeedLimit() {
        assertEquals(2f, thinkableAngleStep(179f, 181.0, 10f))
        assertEquals(-2f, thinkableAngleStep(-179f, 179.0, 10f))
        assertEquals(2f, thinkableAngleStep(359f, 1.0, 10f))
        assertEquals(-2f, thinkableAngleStep(1f, 359.0, 10f))
        assertEquals(-3f, thinkableAngleStep(0f, 270.0, 3f))
        assertEquals(3f, thinkableAngleStep(0f, -270.0, 3f))
        assertEquals(0f, thinkableAngleStep(0f, 270.0, 0f), 1e-5f)
        var angle = 179f
        repeat(8) { angle += thinkableAngleStep(angle, 190.0, 2f) }
        assertEquals(190f, angle)
    }

    @Test fun equivalentTurnsHoldPositionAndHalfTurnDirectionIsPreserved() {
        assertEquals(0f, thinkableAngleStep(-90f, 270.0, 10f))
        assertEquals(0f, thinkableAngleStep(360f, 0.0, 10f))
        assertEquals(0f, thinkableAngleStep(721f, 1.0, 10f))
        assertEquals(0f, thinkableAngleStep(45f, null, 10f))
        assertEquals(180f, thinkableAngleStep(0f, 180.0, 360f))
        assertEquals(-180f, thinkableAngleStep(0f, -180.0, 360f))
    }

    @Test fun largeFiniteTargetsWrapBeforeFloatConversion() {
        for (target in listOf(Double.MAX_VALUE, -Double.MAX_VALUE)) {
            val step = thinkableAngleStep(0f, target, 5f)
            assertTrue(step.isFinite())
            assertTrue(step in -5f..5f)
        }
    }
}
