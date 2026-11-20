package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.Shapes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GatemakyrSurfaceCollisionTest {
    @Test fun openingClipsOnlyTheCoveredPartOfEachBackingBlock() {
        val opening = listOf(AABB(-8.0, 63.0, 3.0, -6.5, 65.0, 4.0))
        val block = Shapes.block()
        assertTrue(GatemakyrSurfaceCollision.clipBacking(block, -8, 64, 3, opening).isEmpty)
        val edge = GatemakyrSurfaceCollision.clipBacking(block, -7, 64, 3, opening)
        assertEquals(listOf(AABB(0.5, 0.0, 0.0, 1.0, 1.0, 1.0)), edge.toAabbs())
        assertSame(block, GatemakyrSurfaceCollision.clipBacking(block, -6, 64, 3, opening))
    }
}
