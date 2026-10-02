package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class GravitronVisualsTest {
    @Test
    fun `launch wave moves away from the hit instead of lighting the entire ship`() {
        assertTrue(strength(GravitronAction.LAUNCH, 4f, 5f) > 0.9f)
        assertEquals(0f, strength(GravitronAction.LAUNCH, 8f, 5f))
        assertEquals(0f, strength(GravitronAction.LAUNCH, 4f, 10f))
        assertTrue(strength(GravitronAction.LAUNCH, 8f, 10f) > 0.9f)
    }

    @Test
    fun `freeze fills the reached area while unfreeze contracts toward the hit`() {
        assertTrue(strength(GravitronAction.FREEZE, 4f, 10f) > 0.8f)
        assertEquals(0f, strength(GravitronAction.FREEZE, 8f, 10f))
        assertTrue(strength(GravitronAction.FREEZE, 8f, 20f) > 0.8f)
        assertTrue(strength(GravitronAction.UNFREEZE, 10f, 10f) > 0.6f)
        assertEquals(0f, strength(GravitronAction.UNFREEZE, 2f, 10f))
        assertTrue(strength(GravitronAction.UNFREEZE, 2f, 20f) > 0.6f)
        assertEquals(0f, strength(GravitronAction.UNFREEZE, 10f, 20f))
    }

    @Test
    fun `surface effects stay bounded and one shots completely fade`() {
        for (action in GravitronAction.entries) {
            for (age in 0..130) for (distance in 0..20) {
                val value = strength(action, distance.toFloat(), age.toFloat())
                assertTrue(value.isFinite() && value in 0f..1f)
                if (distance >= GravitronVisuals.SURFACE_RADIUS) assertEquals(0f, value)
                if (age == 130 && action != GravitronAction.GRAB && action != GravitronAction.HOLD)
                    assertEquals(0f, value)
            }
        }
    }

    @Test
    fun `core advances at steady speed across action changes and fits in the mouth`() {
        val animation = GravitronAnimation()
        val before = GravitronVisuals.coreAngle(4000f)
        animation.accept(GravitronAction.LAUNCH, 4000)
        assertNotEquals(animation.sample(4000, 0f).energy, animation.sample(4001, 0f).energy)
        assertEquals(2f, (GravitronVisuals.coreAngle(4001f) - before + 360f) % 360f, 0.001f)
        // OVERLOAD_FX spans +/-5 px in XY and +/-1 px in Z about its center.
        assertTrue(sqrt(50f) * GravitronVisuals.CORE_SCALE < 1.4f)
        assertTrue(GravitronVisuals.EMITTER_Z + GravitronVisuals.CORE_SCALE < 1.14781f)
    }

    private fun strength(action: GravitronAction, distance: Float, age: Float) =
        GravitronVisuals.surfaceStrength(action, distance, age)
}
