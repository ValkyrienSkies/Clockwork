package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WandRopeSlideTest {
    @Test fun `rope picking follows the visible sag instead of the straight chord`() {
        val curve = WandRopeCurve(Vec3(-5.0, 4.0, 0.0), Vec3(5.0, 4.0, 0.0), 14.0)
        assertEquals(curve.a, curve.point(0.0))
        assertTrue(curve.b.distanceTo(curve.point(1.0)) < 1e-8)
        val middle = curve.point(0.5)
        assertTrue(middle.y < 2.0)
        val hit = curve.pick(middle.add(0.0, 0.0, -3.0), Vec3(0.0, 0.0, 1.0), 4.5)!!
        assertEquals(0.5, hit.parameter, 0.025)
        assertNull(curve.pick(Vec3(0.0, 4.0, -3.0), Vec3(0.0, 0.0, 1.0), 4.5))
    }

    @Test fun `rope behind a surface or beyond reach cannot be picked`() {
        val curve = WandRopeCurve(Vec3(-5.0, 0.0, 3.0), Vec3(5.0, 0.0, 3.0), 10.0)
        assertNotNull(curve.pick(Vec3.ZERO, Vec3(0.0, 0.0, 1.0), 4.5))
        assertNull(curve.pick(Vec3.ZERO, Vec3(0.0, 0.0, 1.0), 2.5))
        assertNull(curve.pick(Vec3.ZERO, Vec3(0.0, 0.0, -1.0), 4.5))
    }

    @Test fun `gravity accelerates downhill and does not depend on which end was created first`() {
        val a = Vec3(0.0, 20.0, 0.0); val b = Vec3(100.0, 0.0, 0.0)
        val forward = WandRopeCurve(a, b, a.distanceTo(b))
        val reverse = WandRopeCurve(b, a, a.distanceTo(b))
        var t = 0.1; var speed = 0.0
        repeat(100) {
            val step = WandRopeSlidePhysics.step(forward, t, speed)
            val reversed = WandRopeSlidePhysics.step(reverse, 1 - t, -speed)
            assertTrue(step.parameter > t)
            assertTrue(step.speed in 0.0..WandRopeSlidePhysics.MAX_SPEED)
            assertEquals(1 - step.parameter, reversed.parameter, 1e-8)
            assertEquals(-step.speed, reversed.speed, 1e-8)
            t = step.parameter; speed = step.speed
        }
        assertTrue(speed > 0.3)
    }

    @Test fun `flat rope can suspend a still rider and sag pulls toward its low point`() {
        val a = Vec3(0.0, 5.0, 0.0); val b = Vec3(10.0, 5.0, 0.0)
        val flat = WandRopeSlidePhysics.step(WandRopeCurve(a, b, 10.0), 0.5, 0.0)
        assertEquals(0.5, flat.parameter)
        assertEquals(0.0, flat.speed)
        val slack = WandRopeCurve(a, b, 14.0)
        assertTrue(WandRopeSlidePhysics.step(slack, 0.2, 0.0).speed > 0)
        assertTrue(WandRopeSlidePhysics.step(slack, 0.8, 0.0).speed < 0)
    }

    @Test fun `endpoints dismount and collapsed ropes do not produce invalid velocity`() {
        val curve = WandRopeCurve(Vec3(0.0, 5.0, 0.0), Vec3(1.0, 0.0, 0.0), 5.1)
        val step = WandRopeSlidePhysics.step(curve, 0.99, 0.7)
        assertTrue(step.finished)
        assertEquals(1.0, step.parameter)
        assertTrue(WandRopeSlidePhysics.step(WandRopeCurve(Vec3.ZERO, Vec3.ZERO, 0.0), 0.5, 0.0).finished)
    }

    @Test fun `moving anchors carry the path without changing slope acceleration`() {
        val a = Vec3(0.0, 20.0, 0.0); val b = Vec3(10.0, 10.0, 0.0)
        val drift = Vec3(0.4, 0.2, -0.3)
        val before = WandRopeCurve(a, b, 16.0)
        val after = WandRopeCurve(a.add(drift), b.add(drift), 16.0)
        assertTrue(after.point(0.3).subtract(before.point(0.3)).distanceTo(drift) < 1e-8)
        assertEquals(WandRopeSlidePhysics.step(before, 0.3, 0.2), WandRopeSlidePhysics.step(after, 0.3, 0.2))
    }

    @Test fun `sweep catches thin walls and ground before a body passes through them`() {
        val body = AABB(-0.3, 1.0, -0.3, 0.3, 2.8, 0.3)
        val wall = AABB(0.65, 0.0, -2.0, 0.7, 4.0, 2.0)
        assertFalse(WandRopeSlidePhysics.clearSweep(body, Vec3(1.5, 0.0, 0.0)) { !it.intersects(wall) })
        val floor = AABB(-10.0, -1.0, -10.0, 10.0, 0.0, 10.0)
        assertFalse(WandRopeSlidePhysics.clearSweep(body, Vec3(1.0, -1.5, 0.0)) { !it.intersects(floor) })
        assertTrue(WandRopeSlidePhysics.clearSweep(body, Vec3(1.0, -0.5, 0.0)) { !it.intersects(floor) })
    }

    @Test fun `clearance accounts for the head and shoulders as well as the feet`() {
        val body = AABB(-0.3, 1.0, -0.3, 0.3, 2.8, 0.3)
        val overhang = AABB(0.65, 2.6, -2.0, 1.0, 3.0, 2.0)
        assertFalse(WandRopeSlidePhysics.clearSweep(body, Vec3(1.0, 0.0, 0.0)) { !it.intersects(overhang) })
        assertTrue(WandRopeSlidePhysics.clearSweep(body, Vec3(-1.0, 0.0, 0.0)) { !it.intersects(overhang) })
        assertFalse(WandRopeSlidePhysics.clearSweep(body, Vec3(Double.NaN, 0.0, 0.0)) { true })
    }
}
