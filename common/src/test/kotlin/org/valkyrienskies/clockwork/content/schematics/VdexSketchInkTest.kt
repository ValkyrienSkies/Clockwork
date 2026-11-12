package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VdexSketchInkTest {
    @Test
    fun `stylised connections retain their anchors including vertical connections`() {
        val a = Vec3(2.0, 4.0, -3.0)
        for (b in listOf(Vec3(2.0, 10.0, -3.0), Vec3(-5.0, 3.0, 2.0))) {
            for (kind in VdexSketchInk.Kind.values()) {
                val drawing = VdexSketchInk.connection(kind, a, b, a.distanceTo(b) * 1.3)
                val path = drawing.strokes.first().points
                assertTrue(path.first().distanceTo(a) < 1e-7, kind.name)
                assertTrue(path.last().distanceTo(b) < 1e-7, kind.name)
                assertEquals(listOf(a, b), drawing.symbols.filter { it.toward == null }.map { it.anchor })
                assertTrue(drawing.strokes.flatMap { it.points }.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
            }
        }
    }

    @Test
    fun `slack ropes retain sag instead of becoming straight diagram links`() {
        val drawing = VdexSketchInk.connection(VdexSketchInk.Kind.ROPE, Vec3.ZERO, Vec3(4.0, 0.0, 0.0), 7.0)
        assertTrue(drawing.strokes.first().points.minOf { it.y } < -.5)
    }

    @Test
    fun `invalid anchors and extreme lengths cannot produce unbounded drawings`() {
        for (kind in VdexSketchInk.Kind.values()) {
            assertTrue(VdexSketchInk.connection(kind, Vec3.ZERO, Vec3.ZERO, 0.0).strokes.isEmpty())
            assertTrue(VdexSketchInk.connection(kind, Vec3.ZERO, Vec3(Double.NaN, 0.0, 0.0), 1.0).strokes.isEmpty())
            val drawing = VdexSketchInk.connection(kind, Vec3.ZERO, Vec3(10000.0, 0.0, 0.0), 10000.0)
            assertTrue(drawing.strokes.sumOf { it.points.size } < 600)
        }
    }
}
