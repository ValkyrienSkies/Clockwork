package org.valkyrienskies.clockwork.content.curiosities.rope

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor

class RopeWinchTest {
    @Test fun `released rope pays out without retracting or exceeding maximum`() {
        assertEquals(20.0, RopeWinchBlockEntity.releasedLength(8.0, 20.0, 32.0))
        assertEquals(8.0, RopeWinchBlockEntity.releasedLength(8.0, 6.0, 32.0))
        assertEquals(32.0, RopeWinchBlockEntity.releasedLength(8.0, 40.0, 32.0))
        assertEquals(40.0, RopeWinchBlockEntity.releasedLength(40.0, 42.0, 32.0))
        assertEquals(8.0, RopeWinchBlockEntity.releasedLength(8.0, Double.NaN, 32.0))
    }

    @Test fun `speed direction and limits`() {
        assertEquals(8.125, RopeWinchBlockEntity.nextLength(8.0, 64.0, 32))
        assertEquals(7.875, RopeWinchBlockEntity.nextLength(8.0, -64.0, 32))
        assertEquals(32.0, RopeWinchBlockEntity.nextLength(32.0, 256.0, 32))
        assertEquals(0.5, RopeWinchBlockEntity.nextLength(0.6, -256.0, 32))
        assertEquals(8.0, RopeWinchBlockEntity.nextLength(8.0, 0.0, 32))
        assertEquals(8.0, RopeWinchBlockEntity.nextLength(8.0, Double.NaN, 32))
    }

    @Test fun `lowering maximum does not teleport the load`() {
        assertEquals(64.0, RopeWinchBlockEntity.nextLength(64.0, 128.0, 32))
        assertEquals(63.75, RopeWinchBlockEntity.nextLength(64.0, -128.0, 32))
    }

    @Test fun `winch outlet survives serialization on moving bodies`() {
        val anchor = WandAnchor(BlockPos(-20000000, 80, 20000000), Direction.DOWN, 42, true)
        assertEquals(anchor, WandAnchor.load(anchor.save()))
        assertEquals(79.995, anchor.local().y, 1e-8)
        val legacy = anchor.save().also { it.remove("winch") }
        assertFalse(WandAnchor.load(legacy).winch)
    }
}
