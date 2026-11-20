package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector4f
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GatemakyrClipTest {
    private val lens = Matrix4f().perspective(1.3f, 1.75f, 0.05f, 160f)

    @Test fun closeExitBehindNearPlaneKeepsPlayerLens() {
        assertEquals(lens, GatemakyrClip.projection(lens, Matrix4f(), Vec3(0.0, 0.0, -0.001), Vec3(0.0, 0.0, -1.0)))
    }

    @Test fun windowPlaneClassifiesFragmentsUsingThePlayerDepthRange() {
        val view = Matrix4f().rotateX(0.18f).rotateY(-0.27f)
        val center = Vec3(1.0, -0.2, -12.0)
        val normal = Vec3(0.6, 0.0, -0.8)
        val plane = GatemakyrClip.windowPlane(lens, view, center, normal, 854, 480)
        val transform = Matrix4f(lens).mul(view)
        for (offset in listOf(-1.0, 0.0, 1.0)) {
            val surface = center.add(0.8 * offset, 0.3 * offset, 0.6 * offset)
            for (distance in listOf(-0.02, 0.02)) {
                val p = transform.transform(Vector4f(surface.add(normal.scale(distance)).toVector3f(), 1f))
                val window = Vector4f((p.x / p.w + 1f) * 427f, (p.y / p.w + 1f) * 240f, (p.z / p.w + 1f) * 0.5f, 1f)
                assertTrue(window.z > 0.56f)
                assertTrue(plane.dot(window) * distance > 0.0)
            }
        }
    }

    @Test fun tiltedExitClipsAtItsSurfaceWithoutChangingScreenCoordinates() {
        val normal = Vec3(0.6, 0.0, -0.8)
        val center = Vec3(0.2, 0.0, -0.8)
        val clipped = GatemakyrClip.projection(lens, Matrix4f(), center, normal)
        for (x in listOf(-0.3, 0.0, 0.3)) {
            val surface = center.add(x * 0.8, 0.0, x * 0.6)
            for (depth in listOf(-0.01, 0.0, 0.01)) {
                val point = Vector4f(surface.add(normal.scale(depth)).toVector3f(), 1f)
                val v = clipped.transform(Vector4f(point))
                val original = lens.transform(point)
                assertEquals(original.x / original.w, v.x / v.w, 0.00001f)
                assertEquals(original.y / original.w, v.y / v.w, 0.00001f)
                if (depth == 0.0) assertEquals(-v.w, v.z, 0.00001f)
                else assertTrue((v.z + v.w) * depth > 0.0)
            }
        }
    }
}
