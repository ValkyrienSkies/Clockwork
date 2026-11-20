package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GatemakyrPairTest {
    @Test fun rectangularCapsApplyBeforeSearchingForSupport() {
        val fit = GatemakyrSurfaceFit.fit(3, 12) { u, v ->
            (u in -8..8 && v in -1..1) || (u in -1..1 && v in -8..2)
        }
        assertEquals(GatemakyrSurfaceFit.Rectangle(-1, -8, 3, 11), fit)
    }

    @Test fun shrinkingSurfaceKeepsWholeBlocksAndTheHitCell() {
        val wall = GatemakyrTarget(Vec3(4.5, 4.0, -0.004), Direction.NORTH, surface = true,
            width = 9, height = 8, impact = Vec3(8.7, 0.2, -0.004))
        val small = GatemakyrTarget(Vec3.ZERO, Direction.EAST, width = 2, height = 3)
        val resized = wall.fitTo(small)
        assertEquals(2, resized.width)
        assertEquals(3, resized.height)
        assertEquals(Vec3(8.0, 1.5, -0.004), resized.position)
        assertEquals(wall.impact, resized.impact)
        assertEquals(resized, resized.fitTo(wall))
        assertEquals(small, small.fitTo(resized))
    }

    @Test fun perpendicularMouthsMapGazeAndPositionBothWays() {
        val a = GatemakyrTarget.Frame(Vec3.ZERO, Vec3(-1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0),
            Vec3(0.0, 0.0, -1.0), Vec3.ZERO)
        val b = GatemakyrTarget.Frame(Vec3(14.0, 8.0, 1.0), Vec3(0.0, 0.0, 1.0), a.v,
            Vec3(-1.0, 0.0, 0.0), Vec3.ZERO)
        val passage = GatemakyrPassage(a, b)
        val p = Vec3(0.25, 1.0, 0.5)
        assertEquals(Vec3(13.5, 9.0, 1.25), passage.point(p))
        assertEquals(b.normal, passage.direction(a.normal.reverse()))
        assertEquals(p, GatemakyrPassage(b, a).point(passage.point(p)))
    }
}
