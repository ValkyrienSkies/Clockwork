package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VdexPlacementTest {
    private val bounds = AABB(-8.0, -3.0, -1.0, 2.0, 5.0, 7.0)
    private fun same(a: Vec3, b: Vec3) { assertEquals(0.0, a.distanceTo(b), .00002) }

    @Test
    fun `deploy places the rotated bottom centre on the target`() {
        val target = Vec3(15.5, 72.0, -3.5)
        for (yaw in listOf(0f, 90f, 180f, -90f, 37f)) {
            val placement = VdexPlacement.at(bounds, target, yaw)
            same(target, placement.world(Vec3(bounds.center.x, bounds.minY, bounds.center.z)))
        }
    }

    @Test
    fun `rotation keeps off-centre connected assemblies in place`() {
        val placement = VdexPlacement(Vec3(4.0, 60.0, -10.0), 0f)
        var rotated = placement
        repeat(4) {
            rotated = rotated.rotate(bounds, -90f)
            same(placement.world(bounds.center), rotated.world(bounds.center))
        }
        same(placement.position, rotated.position)
        same(placement.world(Vec3(1.0, 2.0, 3.0)), rotated.world(Vec3(1.0, 2.0, 3.0)))
    }

    @Test
    fun `face selection follows rotation instead of the world bounding box`() {
        val placement = VdexPlacement(Vec3(10.0, 64.0, -3.0), 90f)
        assertEquals(Direction.WEST, placement.face(bounds, placement.world(Vec3(-12.0, 0.0, 0.0)), placement.world(Vec3(0.0, 0.0, 0.0))))
        assertEquals(Direction.UP, placement.face(bounds, placement.world(Vec3(0.0, 9.0, 0.0)), placement.world(Vec3(0.0, 0.0, 0.0))))
        assertNull(placement.face(bounds, placement.world(Vec3(9.0, 12.0, 0.0)), placement.world(Vec3(12.0, 12.0, 0.0))))
    }
}
