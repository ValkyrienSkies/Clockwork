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
}
