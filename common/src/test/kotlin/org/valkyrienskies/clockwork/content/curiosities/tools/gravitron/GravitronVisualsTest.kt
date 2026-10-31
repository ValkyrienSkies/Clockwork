package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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
        assertTrue(strength(GravitronAction.UNFREEZE, 8f, 10f) > 0.8f)
        assertTrue(strength(GravitronAction.UNFREEZE, 2f, 10f) > 0.8f)
        assertEquals(0f, strength(GravitronAction.UNFREEZE, 12f, 10f))
        assertTrue(strength(GravitronAction.UNFREEZE, 0f, 20f) > 0.8f)
        assertEquals(0f, strength(GravitronAction.UNFREEZE, 10f, 20f))
    }

    @Test
    fun `grab has the former launch strength and launch covers a wider crest`() {
        for (radius in 1..5) {
            val distance = radius.toFloat()
            assertTrue(strength(GravitronAction.HOLD, distance, distance / 0.22f) > 0.95f)
            assertTrue(strength(GravitronAction.LAUNCH, distance + 1f, distance / 0.8f) >
                strength(GravitronAction.HOLD, distance + 1f, distance / 0.22f))
        }
    }

    @Test
    fun `frozen coverage persists until thawed and thaw begins without a visual jump`() {
        for (distance in 0..32) {
            val d = distance / 2f
            assertEquals(strength(GravitronAction.FREEZE, d, 40f), strength(GravitronAction.FREEZE, d, 72000f))
            for (freezeAge in listOf(0f, 3f, 12f, 40f)) {
                assertEquals(strength(GravitronAction.FREEZE, d, freezeAge),
                    GravitronVisuals.surfaceStrength(GravitronAction.UNFREEZE, d, 0f, freezeAge), 0.00001f)
            }
        }
    }

    @Test
    fun `surface effects stay bounded and one shots completely fade`() {
        for (action in GravitronAction.entries) {
            for (age in 0..130) for (distance in 0..20) {
                val value = strength(action, distance.toFloat(), age.toFloat())
                assertTrue(value.isFinite() && value in 0f..1f)
                if (distance >= GravitronVisuals.SURFACE_RADIUS) assertEquals(0f, value)
                if (age == 130 && action !in listOf(GravitronAction.GRAB, GravitronAction.HOLD, GravitronAction.FREEZE))
                    assertEquals(0f, value)
            }
        }
    }

    @Test
    fun `core advances at steady speed across action changes`() {
        val animation = GravitronAnimation()
        val before = GravitronVisuals.coreAngle(4000f)
        animation.accept(GravitronAction.LAUNCH, 4000)
        assertNotEquals(animation.sample(4000, 0f).energy, animation.sample(4001, 0f).energy)
        assertEquals(2f, (GravitronVisuals.coreAngle(4001f) - before + 360f) % 360f, 0.001f)
    }

    private fun strength(action: GravitronAction, distance: Float, age: Float) =
        GravitronVisuals.surfaceStrength(action, distance, age)
}
