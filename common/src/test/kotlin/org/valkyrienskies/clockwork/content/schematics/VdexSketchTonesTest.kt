package org.valkyrienskies.clockwork.content.schematics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class VdexSketchTonesTest {
    private fun pixels(vararg tones: Int): ByteBuffer = ByteBuffer.allocate(tones.size * 4).also { buffer ->
        tones.forEach { buffer.putInt(it) }
        buffer.flip()
    }

    @Test
    fun `paper and isolated outliers do not dominate the tone range`() {
        val tones = IntArray(100) { 100 + it % 50 }
        val measured = VdexSketchTones.measure(pixels(*tones))
        val padded = VdexSketchTones.measure(pixels(*IntArray(2000), 51, *tones, 255))
        assertEquals(measured.x, padded.x, .015f)
        assertEquals(measured.y, padded.y, .015f)
    }

    @Test
    fun `flat and empty schematics have a finite bounded range`() {
        for (alpha in listOf(0, 51, 128, 255)) {
            val range = VdexSketchTones.measure(pixels(*IntArray(100) { alpha }))
            assertTrue(range.x >= 0f && range.y <= 1f)
            assertTrue(range.y - range.x >= .249f)
        }
        val full = VdexSketchTones.measure(pixels(*IntArray(100) { if (it % 2 == 0) 51 else 255 }))
        assertEquals(0f, full.x)
        assertEquals(1f, full.y)
    }

    @Test
    fun `dark and bright materials receive comparable tonal contrast`() {
        val dark = VdexSketchTones.measure(pixels(*IntArray(100) { 70 + it % 50 }))
        val bright = VdexSketchTones.measure(pixels(*IntArray(100) { 170 + it % 50 }))
        assertEquals(dark.y - dark.x, bright.y - bright.x, .001f)
        assertEquals(100f / 204f, bright.x - dark.x, .001f)
    }
}
