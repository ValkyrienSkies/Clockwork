package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class WandRopeSwingTest {
    @Test
    fun `bottom of the arc has the same light drag as the sides`() {
        val bottom = WandRopePhysics.swingDrag(Vec3(0.0, -10.0, 0.0), Vec3(1.0, 0.0, 0.0), Vec3.ZERO, Vec3.ZERO)
        val side = WandRopePhysics.swingDrag(Vec3(-10.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Vec3.ZERO, Vec3.ZERO)
        assertEquals(bottom.length(), side.length(), 1e-10)
        assertTrue(bottom.length() > 0.99 && bottom.length() < 1.0)
        assertEquals(0.0, bottom.y, 1e-10)
        assertEquals(0.0, side.x, 1e-10)
    }

    @Test
    fun `unpowered pendulum carries through the bottom and climbs without gaining energy`() {
        val old = swing(false)
        val fixed = swing(true)
        assertTrue(fixed.bottomSpeed > old.bottomSpeed * 2)
        assertTrue(fixed.returnHeight in 3.0..5.0, "Return height: ${fixed.returnHeight}")
        assertTrue(fixed.maxStretch < 0.75, "Stretch: ${fixed.maxStretch}")
    }

    @Test
    fun `moving anchor keeps its carry velocity while only relative motion loses energy`() {
        val position = Vec3(3.0, -4.0, 0.0)
        val velocity = Vec3(0.8, 0.6, 0.3)
        val drift = Vec3(1.4, -0.2, 0.9)
        val still = WandRopePhysics.swingDrag(position, velocity, Vec3.ZERO, Vec3.ZERO)
        val moving = WandRopePhysics.swingDrag(position, velocity.add(drift), Vec3.ZERO, drift)
        assertTrue(moving.subtract(drift).distanceTo(still) < 1e-10)
        assertTrue(still.length() < velocity.length())
        assertEquals(drift, WandRopePhysics.swingDrag(position, drift, Vec3.ZERO, drift))
        assertEquals(velocity, WandRopePhysics.swingDrag(Vec3.ZERO, velocity, Vec3.ZERO, Vec3.ZERO))
    }

    @Test
    fun `slack rope is ineligible for swing drag`() {
        assertFalse(WandRopePhysics.isTaut(Vec3(2.0, 0.0, 0.0), Vec3.ZERO, 5.0))
        assertFalse(WandRopePhysics.isTaut(Vec3.ZERO, Vec3.ZERO, 2.0))
        assertTrue(WandRopePhysics.isTaut(Vec3(0.0, -5.0, 0.0), Vec3.ZERO, 5.0))
        assertTrue(WandRopePhysics.isTaut(Vec3(5.2, 0.0, 0.0), Vec3.ZERO, 5.0))
    }

    @Test
    fun `normal rope stretch does not cause stale server velocity packets`() {
        for (tick in 0L..100L) {
            assertFalse(WandRopePhysics.needsServerCorrection(Vec3(0.0, -10.6, 0.0), Vec3.ZERO, 10.0, tick, -100))
        }
        val position = Vec3(0.0, -12.0, 0.0)
        assertTrue(WandRopePhysics.needsServerCorrection(position, Vec3.ZERO, 10.0, 100, -100))
        for (tick in 101L..109L)
            assertFalse(WandRopePhysics.needsServerCorrection(position, Vec3.ZERO, 10.0, tick, 100))
        assertTrue(WandRopePhysics.needsServerCorrection(position, Vec3.ZERO, 10.0, 110, 100))
    }

    private data class Swing(val bottomSpeed: Double, val returnHeight: Double, val maxStretch: Double)

    private fun swing(reducedDrag: Boolean): Swing {
        val length = 10.0
        var position = Vec3(-length * sin(PI / 3), -length * cos(PI / 3), 0.0)
        var velocity = Vec3.ZERO
        var bottomSpeed = 0.0
        var returnHeight = 0.0
        var stretch = 0.0
        repeat(160) {
            val oldPosition = position
            // Vanilla travel moves, adds gravity, then applies friction. The rope runs afterward.
            position = position.add(velocity)
            velocity = velocity.add(0.0, -0.08, 0.0)
            velocity = if (reducedDrag && WandRopePhysics.isTaut(position, Vec3.ZERO, length))
                WandRopePhysics.swingDrag(position, velocity, Vec3.ZERO, Vec3.ZERO)
            else velocity.multiply(0.91, 0.98, 0.91)
            velocity = WandRopePhysics.constrain(position, velocity, Vec3.ZERO, Vec3.ZERO, length) ?: velocity
            if (bottomSpeed == 0.0 && oldPosition.x < 0 && position.x >= 0) bottomSpeed = velocity.length()
            if (bottomSpeed > 0 && position.x > 0) returnHeight = max(returnHeight, position.y + length)
            stretch = max(stretch, position.length() - length)
        }
        return Swing(bottomSpeed, returnHeight, stretch)
    }
}
