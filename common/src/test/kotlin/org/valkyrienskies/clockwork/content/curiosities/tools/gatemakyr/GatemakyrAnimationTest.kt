package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GatemakyrAnimationTest {
    @Test
    fun `charge heartbeats and extra render passes preserve the timeline`() {
        val animation = GatemakyrAnimation()
        animation.start(1, 100)
        val expected = animation.sample(120, 0.5f)
        repeat(100) { animation.sample(120, it / 100f) }
        animation.start(1, 110)
        assertEquals(100L, animation.startedAt)
        assertEquals(expected, animation.sample(120, 0.5f))
        assertEquals(1f, animation.sample(10000, 0f).charge)
    }

    @Test
    fun `cancelling a partial charge closes continuously without firing recoil`() {
        val animation = GatemakyrAnimation()
        animation.start(0, 0)
        val before = animation.sample(15, 0f)
        animation.release(15, false)
        assertEquals(before.opening, animation.sample(15, 0f).opening)
        assertFalse(animation.charging)
        assertEquals(0f, animation.sample(18, 0f).recoil)
        assertEquals(GatemakyrAnimation.Pose(0f, 0f, 0f, 0f), animation.sample(30, 0f))
    }

    @Test
    fun `firing expels energy and briefly opens the prongs further`() {
        val animation = GatemakyrAnimation()
        animation.start(0, 10)
        animation.release(50, true)
        val pose = animation.sample(53, 0f)
        assertTrue(pose.opening > 26f)
        assertTrue(pose.recoil > 0.5f)
        assertEquals(0f, pose.energy)
        animation.start(1, 80)
        assertEquals(1, animation.side)
        assertEquals(0f, animation.sample(80, 0f).charge)
    }
}
