package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import com.mojang.blaze3d.vertex.VertexConsumer
import org.joml.Matrix4f
import org.joml.Vector4f
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GatemakyrEntitySliceTest {
    private class Capture : VertexConsumer {
        val vertices = mutableListOf<Pair<Double, Float>>()
        var x = 0.0
        var u = 0f
        override fun vertex(x: Double, y: Double, z: Double) = apply { this.x = x }
        override fun uv(u: Float, v: Float) = apply { this.u = u }
        override fun color(r: Int, g: Int, b: Int, a: Int) = this
        override fun normal(x: Float, y: Float, z: Float) = this
        override fun overlayCoords(u: Int, v: Int) = this
        override fun uv2(u: Int, v: Int) = this
        override fun defaultColor(r: Int, g: Int, b: Int, a: Int) {}
        override fun unsetDefaultColor() {}
        override fun endVertex() { vertices.add(x to u) }
    }

    private fun cut(vararg xs: Double, projectBack: Boolean = true): List<Pair<Double, Float>> {
        val capture = Capture()
        val buffer = GateSliceVertices(capture, Vector4f(1f, 0f, 0f, 0f), Matrix4f().translation(20f, 0f, 0f), xs.size, projectBack)
        for ((i, x) in xs.withIndex()) {
            buffer.vertex(x, if (i < 2) 0.0 else 1.0, 0.0).uv((0.5 + x / 2).toFloat(), 0f).endVertex()
        }
        return capture.vertices
    }

    @Test fun crossingQuadKeepsBothTexturedHalvesAtTheirOwnGate() {
        val vertices = cut(-1.0, 1.0, 1.0, -1.0)
        assertEquals(listOf(0.0, 1.0, 1.0, 0.0, 19.0, 20.0, 20.0, 19.0), vertices.map { it.first })
        assertEquals(listOf(0.5f, 1f, 1f, 0.5f, 0f, 0.5f, 0.5f, 0f), vertices.map { it.second })
    }

    @Test fun firstPersonViewCanKeepTheVisibleHalfWithoutDrawingItsOwnInterior() {
        assertEquals(listOf(0.0, 1.0, 1.0, 0.0),
            cut(-1.0, 1.0, 1.0, -1.0, projectBack = false).map { it.first })
        assertEquals(0, cut(-3.0, -2.0, -1.0, projectBack = false).size)
    }

    @Test fun facesOnThePlaneAreDrawnOnceAndRearFacesAreMappedWhole() {
        assertEquals(listOf(0.0, 0.0, 0.0), cut(0.0, 0.0, 0.0).map { it.first })
        assertEquals(listOf(17.0, 18.0, 19.0), cut(-3.0, -2.0, -1.0).map { it.first })
    }
}
