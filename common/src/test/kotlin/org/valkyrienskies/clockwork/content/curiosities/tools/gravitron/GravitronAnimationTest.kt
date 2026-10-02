package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.abs

class GravitronAnimationTest {
    @Test
    fun `sampling extra render passes does not change motion`() {
        val animation = GravitronAnimation()
        animation.accept(GravitronAction.LAUNCH, 200)
        val expected = animation.sample(204, 0.35f)
        repeat(1000) { animation.sample(204, it / 1000f) }
        assertEquals(expected, animation.sample(204, 0.35f))
    }

    @Test
    fun `hold heartbeats update load without replaying grab`() {
        val animation = GravitronAnimation()
        animation.accept(GravitronAction.GRAB, 100, 0.4f)
        val before = animation.sample(110, 0f)
        animation.accept(GravitronAction.HOLD, 110, 0.95f)
        assertEquals(100L, animation.startedAt)
        assertEquals(before.opening, animation.sample(110, 0f).opening)
        assertTrue(animation.sample(110, 0f).strain > 0.7f)
        assertTrue(animation.holding)
    }

    @Test
    fun `late tracking snapshots open the prongs without a grab kick`() {
        val animation = GravitronAnimation()
        animation.accept(GravitronAction.HOLD, 900, 0.5f)
        assertEquals(22f, animation.sample(910, 0f).opening, 0.001f)
        assertEquals(0f, animation.sample(901, 0f).recoil)
    }

    @Test
    fun `idle heartbeat preserves an in-flight launch or overload`() {
        for (action in listOf(GravitronAction.LAUNCH, GravitronAction.FREEZE, GravitronAction.UNFREEZE, GravitronAction.OVERLOAD)) {
            val animation = GravitronAnimation()
            animation.accept(action, 10, 1.2f)
            val expected = animation.sample(15, 0f)
            animation.accept(GravitronAction.IDLE, 15)
            assertEquals(action, animation.action)
            assertEquals(expected, animation.sample(15, 0f))
        }
    }

    @Test
    fun `release transitions continuously from held pose and settles`() {
        val animation = GravitronAnimation()
        animation.accept(GravitronAction.GRAB, 0, 0.75f)
        val held = animation.sample(30, 0f)
        animation.accept(GravitronAction.RELEASE, 30)
        assertFalse(animation.holding)
        assertEquals(held.opening, animation.sample(30, 0f).opening)
        assertEquals(held.dial, animation.sample(30, 0f).dial)
        assertEquals(0f, animation.sample(50, 0f).opening)
        assertEquals(10f, animation.sample(50, 0f).dial)
    }

    @Test
    fun `all one-shot actions settle and remain finite`() {
        for (action in GravitronAction.entries.filter { it.duration > 0 && it != GravitronAction.GRAB }) {
            val animation = GravitronAnimation()
            animation.accept(action, 0, Float.NaN)
            for (frame in 0..500) {
                val pose = animation.sample(0, frame / 10f)
                assertTrue(listOf(pose.opening, pose.recoil, pose.lowering, pose.roll, pose.energy, pose.strain, pose.dial).all { it.isFinite() }, action.name)
                assertTrue(abs(pose.opening) < 100f)
            }
            val settled = animation.sample(100, 0f)
            assertEquals(0f, settled.recoil, 0.001f)
            assertEquals(0f, settled.opening, 0.001f)
            assertEquals(0f, settled.lowering, 0.001f)
            assertEquals(0f, settled.energy, 0.001f)
        }
    }

    @Test
    fun `launch has more kick than freezing and unfreeze draws inward`() {
        fun pose(action: GravitronAction) = GravitronAnimation().also { it.accept(action, 0) }.sample(6, 0f)
        assertTrue(pose(GravitronAction.LAUNCH).recoil > pose(GravitronAction.FREEZE).recoil * 3)
        assertTrue(pose(GravitronAction.UNFREEZE).recoil < 0f)
        assertTrue(pose(GravitronAction.LAUNCH).opening > pose(GravitronAction.FREEZE).opening)
    }

    @Test
    fun `animation clocks retain sub-tick precision in old worlds`() {
        val recent = GravitronAnimation().also { it.accept(GravitronAction.LAUNCH, 0) }
        val old = GravitronAnimation().also { it.accept(GravitronAction.LAUNCH, 1000000000L) }
        assertEquals(recent.sample(4, 0.25f), old.sample(1000000004L, 0.25f))
    }

    @Test
    fun `two players never share recoil or dial state`() {
        val first = GravitronAnimation().also { it.accept(GravitronAction.LAUNCH, 0) }
        val second = GravitronAnimation().also { it.accept(GravitronAction.HOLD, 0, 0.2f) }
        first.accept(GravitronAction.OVERLOAD, 10, 1.4f)
        assertTrue(second.holding)
        assertEquals(0f, second.sample(15, 0f).recoil)
        assertEquals(78f, second.sample(15, 0f).dial, 0.001f)
    }
}
