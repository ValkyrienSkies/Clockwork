package org.valkyrienskies.clockwork.content.curiosities.spring

import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpringShapeTest {
    @Test fun mountingSegmentsFollowEveryBlockFace() {
        val a = Vec3(1.0, 2.0, 3.0)
        val b = Vec3(5.0, 6.0, 7.0)
        for (faceA in Direction.values()) for (faceB in Direction.values()) {
            val na = Vec3.atLowerCornerOf(faceA.normal)
            val nb = Vec3.atLowerCornerOf(faceB.normal)
            val points = SpringShape.points(a, b, na, nb)
            assertEquals(a, points.first())
            assertEquals(b, points.last())
            assertEquals(1.0, points[1].subtract(a).normalize().dot(na), 1e-9)
            assertEquals(1.0, points[points.lastIndex - 1].subtract(b).normalize().dot(nb), 1e-9)
            assertTrue(points.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
        }
    }

    @Test fun textureUsesTwelvePixelsAcrossAndAlongAtRest() {
        assertEquals(0.75, SpringShape.WIDTH)
        assertEquals(SpringShape.WIDTH, SpringShape.REPEAT_LENGTH)
    }

    @Test fun recoilMovesBothBrokenTipsFromCenterToMounts() {
        assertEquals(0.5, SpringShape.recoilReach(0.0))
        assertEquals(0.0, SpringShape.recoilReach(1.0))
        assertTrue(SpringShape.recoilReach(0.25) < SpringShape.recoilReach(0.0))
        assertTrue(SpringShape.recoilReach(0.75) < SpringShape.recoilReach(0.25))
        for (i in 0..100) assertTrue(SpringShape.recoilReach(i / 100.0) in 0.0..0.5)
    }
}
