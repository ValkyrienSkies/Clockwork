package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GatemakyrSurfaceFitTest {
    @Test fun largeWallRespectsSelectedSize() {
        for (size in listOf(1, 3, 8, 32)) {
            val fit = GatemakyrSurfaceFit.fit(size) { _, _ -> true }!!
            assertEquals(size, fit.width)
            assertEquals(size, fit.height)
            assertTrue(0 in fit.u until fit.u + fit.width)
            assertTrue(0 in fit.v until fit.v + fit.height)
        }
    }
    @Test fun surfaceEdgesConstrainBothDimensions() {
        val fit = GatemakyrSurfaceFit.fit(32) { u, v -> u in -2..4 && v in -1..2 }!!
        assertEquals(GatemakyrSurfaceFit.Rectangle(-2, -1, 7, 4), fit)
    }
    @Test fun missingSupportPreventsSpreadingAcrossGap() {
        val supported = { u: Int, v: Int -> u in -3..3 && v in -3..3 && !(u == 1 && v == 0) }
        val fit = GatemakyrSurfaceFit.fit(7, 7, supported)!!
        assertEquals(28, fit.width * fit.height)
        for (u in fit.u until fit.u + fit.width) for (v in fit.v until fit.v + fit.height) assertTrue(supported(u, v))
        assertNull(GatemakyrSurfaceFit.fit(32) { _, _ -> false })
    }
    @Test fun openingExpandsFromImpactInsideFinalBounds() {
        val first = GatemakyrRiftShape.sample(7, 4, 2.0, -0.75, 0f, true)
        assertTrue(first.minX <= 2 && first.maxX >= 2)
        assertTrue(first.maxX - first.minX < 0.2)
        var previous = first
        for (age in 1..100) {
            val bounds = GatemakyrRiftShape.sample(7, 4, 2.0, -0.75, age.toFloat(), true)
            assertTrue(bounds.minX >= -3.5 && bounds.maxX <= 3.5 && bounds.minY >= -2 && bounds.maxY <= 2)
            assertTrue(bounds.minX <= previous.minX && bounds.maxX >= previous.maxX)
            previous = bounds
        }
        assertEquals(GatemakyrRiftShape.Bounds(-3.5, -2.0, 3.5, 2.0), previous)
        assertEquals(previous, GatemakyrRiftShape.sample(7, 4, 2.0, -0.75, 0f, false))
    }
}
