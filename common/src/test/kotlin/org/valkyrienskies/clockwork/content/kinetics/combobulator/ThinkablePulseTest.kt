package org.valkyrienskies.clockwork.content.kinetics.combobulator

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ThinkablePulseTest {
    @Test fun serverTimestampDoesNotDetermineClientAnimationAge() {
        for (serverStart in listOf(100L, 900000L)) {
            val pulse = ThinkablePulse.receive(null, serverStart, 0, 12, 50)
            assertEquals(0.0, pulse.progress(50))
            assertEquals(0.5, pulse.progress(56))
            assertEquals(1.0, pulse.progress(62))
        }
    }

    @Test fun snapshotsDoNotRestartTheSamePulse() {
        val pulse = ThinkablePulse.receive(null, 4000, 2, 20, 100)
        assertEquals(0.1, pulse.progress(100))
        val refreshed = ThinkablePulse.receive(pulse, 4000, 5, 20, 106)
        assertSame(pulse, refreshed)
        assertEquals(0.4, refreshed.progress(106))
        val next = ThinkablePulse.receive(refreshed, 4200, 0, 20, 300)
        assertEquals(0.0, next.progress(300))
    }

    @Test fun expiredAndCancelledPulsesDoNotReplay() {
        val expired = ThinkablePulse.receive(null, 1000, 50, 12, 80)
        assertTrue(expired.progress(80) > 1.0)
        assertEquals(-1.0, ThinkablePulse.receive(expired, -1, -1, 0, 81).progress(81))
        assertEquals(-1.0, ThinkablePulse.receive(null, 1000, -1, 12, 81).progress(81))
    }
}
