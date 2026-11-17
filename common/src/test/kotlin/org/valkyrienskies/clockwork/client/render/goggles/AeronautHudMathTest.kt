package org.valkyrienskies.clockwork.client.render.goggles

import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AeronautHudMathTest {
    @Test
    fun `rotated scaled ships can be targeted without reaching their blocks`() {
        val inverse = Matrix4d().translation(20.0, 0.0, -40.0).rotateY(.7).scale(2.0).invert()
        val origin = inverse.transformPosition(Vector3d())
        val direction = inverse.transformDirection(Vector3d(20.0, 0.0, -40.0).normalize())
        val entry = AeronautHudMath.rayEntry(origin, direction, Vector3d(-2.0), Vector3d(2.0), 128.0)
        assertNotNull(entry)
        assertTrue(entry!! in 35.0..45.0)
        assertNull(AeronautHudMath.rayEntry(origin, direction, Vector3d(-2.0), Vector3d(2.0), 10.0))
    }

    @Test
    fun `parallel rays and rays from inside the hull are handled`() {
        val direction = Vector3d(0.0, 0.0, -1.0)
        assertEquals(0.0, AeronautHudMath.rayEntry(Vector3d(), direction, Vector3d(-2.0), Vector3d(2.0), 128.0))
        assertNull(AeronautHudMath.rayEntry(Vector3d(4.0, 0.0, 0.0), direction, Vector3d(-2.0), Vector3d(2.0), 128.0))
    }

    @Test
    fun `world markers project to GUI coordinates and reject points behind the camera`() {
        val projection = Matrix4f().perspective(Math.toRadians(70.0).toFloat(), 2f, .05f, 512f)
        val center = AeronautHudMath.project(Vector3d(0.0, 0.0, -20.0), projection, 640, 320)!!
        assertEquals(320f, center.x, .001f)
        assertEquals(160f, center.y, .001f)
        val upperRight = AeronautHudMath.project(Vector3d(2.0, 2.0, -20.0), projection, 640, 320)!!
        assertTrue(upperRight.x > center.x && upperRight.y < center.y)
        assertNull(AeronautHudMath.project(Vector3d(0.0, 0.0, 20.0), projection, 640, 320))
        assertNull(AeronautHudMath.project(Vector3d(Double.NaN, 0.0, 0.0), projection, 640, 320))
    }

    @Test
    fun `support pulse keeps moving when the contour starts at a different corner`() {
        val rim = AeronautSupportRim()
        val outline = listOf(Vector3d(0.0, 0.0, 0.0), Vector3d(8.0, 0.0, 0.0),
            Vector3d(8.0, 0.0, 2.0), Vector3d(0.0, 0.0, 2.0))
        val now = 900_000_000L
        rim.update(outline, 0L) { _, _ -> true }
        val head = rim.headPosition(now)!!
        assertEquals(5.0, head.x(), 1e-8)
        rim.update(outline.drop(1) + outline.first(), now) { _, _ -> true }
        assertTrue(head.distance(rim.headPosition(now)!!) < 1e-8)
        val later = rim.headPosition(now * 2)!!
        assertTrue(later.distance(Vector3d(8.0, 0.0, 2.0)) < 1e-8)
        rim.update(emptyList(), now * 2) { _, _ -> true }
        assertNull(rim.headPosition(now * 2))
    }

    @Test
    fun `small footprint changes lerp and a blocked sweep uses the latest visible outline`() {
        val rim = AeronautSupportRim()
        val first = listOf(Vector3d(0.0, 0.0, 0.0), Vector3d(6.0, 0.0, 0.0),
            Vector3d(6.0, 0.0, 3.0), Vector3d(0.0, 0.0, 3.0))
        val next = first.map { Vector3d(it).add(0.6, 0.0, 0.0) }.toMutableList()
        next.add(1, Vector3d(3.6, 0.0, 0.0))
        val now = 300_000_000L
        rim.update(first, 0L) { _, _ -> true }
        rim.update(next, now) { _, _ -> true }
        assertEquals(0.0, rim.outlineAt(now).minOf { it.x() }, 1e-8)
        assertEquals(0.3, rim.outlineAt(now + 100_000_000L).minOf { it.x() }, 1e-8)
        assertEquals(next, rim.outlineAt(now + 200_000_000L))
        rim.update(next, now + 120_000_000L) { _, _ -> false }
        assertEquals(next, rim.outlineAt(now + 120_000_000L))
    }
}
