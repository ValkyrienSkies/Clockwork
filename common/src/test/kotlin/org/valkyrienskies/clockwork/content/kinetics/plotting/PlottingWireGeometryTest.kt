package org.valkyrienskies.clockwork.content.kinetics.plotting

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.hypot

class PlottingWireGeometryTest {
    private val directions = listOf(1 to 0, 0 to 1, -1 to 0, 0 to -1)

    @Test fun rotatedCurvesKeepTheirEndpointsAndWidth() {
        for (start in directions) for (end in directions) {
            val curve = PlottingWireRenderer.Curve(160.0, -96.0, start, end, 1.0)
            assertEquals(0.0, curve.x.first(), .000001)
            assertEquals(0.0, curve.y.first(), .000001)
            assertEquals(160.0, curve.x.last(), .000001)
            assertEquals(-96.0, curve.y.last(), .000001)
            assertTrue(curve.lengths.last() >= hypot(160.0, -96.0))
            for (i in curve.x.indices) {
                assertEquals(1.0, hypot(curve.normalX[i], curve.normalY[i]), .000001)
                if (i > 0) assertTrue(curve.lengths[i] >= curve.lengths[i - 1])
            }
        }
    }

    @Test fun cacheAcceptsPanRoundingButRejectsShapeChanges() {
        val curve = PlottingWireRenderer.Curve(160.0, -96.0, 1 to 0, -1 to 0, 1.0)
        assertTrue(curve.matches(160.0 + 1e-10, -96.0 - 1e-10, 1 to 0, -1 to 0, 1.0))
        assertFalse(curve.matches(161.0, -96.0, 1 to 0, -1 to 0, 1.0))
        assertFalse(curve.matches(160.0, -96.0, 0 to 1, -1 to 0, 1.0))
        assertFalse(curve.matches(160.0, -96.0, 1 to 0, 0 to -1, 1.0))
        assertFalse(curve.matches(160.0, -96.0, 1 to 0, -1 to 0, .5))
    }

    @Test fun overviewUsesFewerSegmentsAndKeepsScaledEndpoints() {
        val full = PlottingWireRenderer.Curve(320.0, 128.0, 1 to 0, -1 to 0, 1.0)
        val overview = PlottingWireRenderer.Curve(32.0, 12.8, 1 to 0, -1 to 0, .1)
        assertTrue(overview.x.size < full.x.size)
        assertEquals(full.x.last() * .1, overview.x.last(), .000001)
        assertEquals(full.y.last() * .1, overview.y.last(), .000001)
    }

    @Test fun coincidentEndpointsHaveFiniteGeometry() {
        for (start in directions) for (end in directions) {
            val curve = PlottingWireRenderer.Curve(0.0, 0.0, start, end, .1)
            for (values in listOf(curve.x, curve.y, curve.normalX, curve.normalY, curve.lengths))
                assertTrue(values.all { it.isFinite() })
        }
    }
}
