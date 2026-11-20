package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GatemakyrRaycastTest {
    private val center = Vec3(8.0, 20.004, 5.0)
    private val target = GatemakyrTarget(center, Direction.UP, surface = true, width = 4, height = 2)
    private val frame = GatemakyrTarget.Frame(center, Vec3(1.5, 0.0, 0.0), Vec3(0.0, 0.0, -1.5),
        Vec3(0.0, 1.5, 0.0), center)

    @Test fun intersectionUsesTheContactPointAndScaledRectangle() {
        val delta = Vec3(-2.0, -4.0, 0.0)
        assertEquals(0.5, GatemakyrRaycast.intersection(frame, target, true, center.add(3.8, 2.0, 1.49), delta)!!)
        assertNull(GatemakyrRaycast.intersection(frame, target, true, center.add(3.8, 2.0, 1.51), delta))
        assertNull(GatemakyrRaycast.intersection(frame, target, true, center.add(4.1, 2.0, 0.0), delta))
    }

    @Test fun finiteSegmentsRespectTheBackingAndDoNotReenterAtTheirOrigin() {
        val up = Vec3(0.0, 2.0, 0.0)
        assertNull(GatemakyrRaycast.intersection(frame, target, true, center.subtract(up), up))
        assertEquals(1.0, GatemakyrRaycast.intersection(frame, target, false, center.subtract(up), up)!!)
        assertNull(GatemakyrRaycast.intersection(frame, target, false, center, up))
        assertNull(GatemakyrRaycast.intersection(frame, target, false, center.subtract(up.scale(2.0)), up))
        assertNull(GatemakyrRaycast.intersection(frame, target, false, center, Vec3(1.0, 0.0, 0.0)))
    }
}
