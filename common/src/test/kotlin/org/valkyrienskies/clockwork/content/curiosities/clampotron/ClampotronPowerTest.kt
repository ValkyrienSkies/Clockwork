package org.valkyrienskies.clockwork.content.curiosities.clampotron

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ClampotronPowerTest {
    @Test fun `brief overload recovers but sustained overload breaks the grip`() {
        var ticks = 0
        repeat(30) { ticks = ClampotronPower.overloadTicks(ticks, 1.3f) }
        assertFalse(ClampotronPower.shouldSnap(ticks, 0.0))
        repeat(15) { ticks = ClampotronPower.overloadTicks(ticks, 1.1f) }
        assertEquals(0, ticks)
        repeat(44) { ticks = ClampotronPower.overloadTicks(ticks, 1.3f) }
        assertFalse(ClampotronPower.shouldSnap(ticks, 0.0))
        ticks = ClampotronPower.overloadTicks(ticks, 1.3f)
        assertTrue(ClampotronPower.shouldSnap(ticks, 0.0))
        assertFalse(ClampotronPower.shouldSnap(0, 20.0))
        assertTrue(ClampotronPower.shouldSnap(0, 25.0))
    }

    @Test fun `warning starts halfway to snap strain and saturates at the threshold`() {
        assertEquals(0f, ClampotronPower.warning(0.6f), 0.0001f)
        assertEquals(0.5f, ClampotronPower.warning(0.9f), 0.0001f)
        assertEquals(1f, ClampotronPower.warning(1.2f), 0.0001f)
        assertEquals(1f, ClampotronPower.warning(3f), 0.0001f)
    }

    @Test fun `overstress disables field even with nonzero theoretical speed`() {
        for (rpm in listOf(-256.0, -64.0, 64.0, 256.0)) {
            assertTrue(ClampotronPower.running(rpm, false))
            assertFalse(ClampotronPower.running(rpm, true))
        }
        for (rpm in listOf(0.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertFalse(ClampotronPower.running(rpm, false))
    }

    @Test fun `mass increases stress while rpm increases available force in either direction`() {
        assertEquals(1.0, ClampotronPower.stress(0.0, 64000.0))
        assertEquals(5.0, ClampotronPower.stress(256000.0, 64000.0))
        assertEquals(17.0, ClampotronPower.stress(1024000.0, 64000.0))
        assertEquals(10240000.0, ClampotronPower.force(64.0, 160000.0))
        assertEquals(ClampotronPower.force(128.0, 160000.0), ClampotronPower.force(-128.0, 160000.0))
    }
}
