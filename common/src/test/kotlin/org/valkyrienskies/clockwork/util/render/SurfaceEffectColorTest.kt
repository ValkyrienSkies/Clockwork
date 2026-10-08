package org.valkyrienskies.clockwork.util.render

import org.joml.Vector4f
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SurfaceEffectColorTest {
    @Test
    fun `grey textures preserve cutout alpha and decode NativeImage channel order`() {
        assertEquals(0x80363636.toInt(), SurfaceEffectColor.greyPixel(0x800000FF.toInt()))
        assertEquals(0xFFB6B6B6.toInt(), SurfaceEffectColor.greyPixel(0xFF00FF00.toInt()))
        assertEquals(0x40121212, SurfaceEffectColor.greyPixel(0x40FF0000))
        assertEquals(0, SurfaceEffectColor.greyPixel(0x00123456) ushr 24)
    }

    @Test
    fun `bright launches saturate rather than wrapping byte colors`() {
        val color = Vector4f()
        for (mode in 0..4) for (age in 0..100) for (distance in -20..20) {
            SurfaceEffectColor.shade(mode, distance * 0.5f, age.toFloat(), 0.765f, 0.627f, 0.89f, 1f, color)
            assertTrue(listOf(color.x, color.y, color.z, color.w).all { it.isFinite() && it in 0f..1f })
            if (mode != 1) assertTrue(color.z >= color.x && color.x >= color.y)
        }
        val grab = shade(0, 1f, 0f)
        val launch = shade(2, 1f, 0f)
        assertTrue(launch.w > grab.w)
        assertEquals(1f, launch.z)
    }

    @Test
    fun `frozen biome tints lose color and completed fades remain invisible`() {
        val color = Vector4f()
        for (time in 0..100) {
            SurfaceEffectColor.shade(1, 2f, time.toFloat(), 0.1f, 0.8f, 0.2f, 0.88f, color)
            assertEquals(color.x, color.y)
            assertEquals(color.x, color.z)
            assertTrue(color.w > 0.8f)
        }
        for (mode in 0..4) {
            SurfaceEffectColor.shade(mode, 0f, 10f, 1f, 1f, 1f, 0f, color)
            assertEquals(0f, color.w)
        }
    }

    @Test
    fun `selection waves remain smooth across block boundaries and deselect stays red`() {
        val a = shade(3, 0.999f, 5f)
        val b = shade(3, 1.001f, 5f)
        assertEquals(a.w, b.w, 0.001f)
        val red = SurfaceEffectColor.shade(3, 1f, 5f, 1f, 0.2f, 0.3f, 0.65f, Vector4f())
        assertTrue(red.x > red.z * 2)
        assertEquals(a.w * 0.65f, red.w, 0.001f)
    }

    @Test
    fun `weld is a moving front with a short wake and no overlay ahead of it`() {
        assertTrue(shade(4, 0f, 0f).w > 0.9f)
        assertTrue(shade(4, -2f, 0f).w > 0f)
        assertEquals(0f, shade(4, -3f, 0f).w)
        assertEquals(0f, shade(4, 2f, 0f).w)
    }

    private fun shade(mode: Int, distance: Float, time: Float) =
        SurfaceEffectColor.shade(mode, distance, time, 0.765f, 0.627f, 0.89f, 1f, Vector4f())
}
