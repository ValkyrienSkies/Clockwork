package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.abs

class InfuserSequenceTest {
    @Test fun `skipping or loading after the climax still impacts once`() {
        for (first in listOf(true, false)) for (skip in listOf(false, true)) {
            val sequence = InfuserSequence(1000, first)
            if (skip) sequence.skip(1010)
            assertFalse(sequence.shouldImpact(1000L + sequence.impact - 1, false))
            assertTrue(sequence.shouldImpact(1000L + sequence.impact, false))
            assertTrue(sequence.shouldImpact(2000, false))
            assertFalse(sequence.shouldImpact(2000, true))
            val reloaded = sequence.copy()
            assertFalse(reloaded.shouldImpact(2000, true))
            assertEquals(sequence.impact, reloaded.impact)
        }
    }

    @Test fun `all viewers derive the same pose from the server timestamp`() {
        val server = InfuserSequence(12000, true)
        val lateViewer = server.copy()
        assertEquals(server.age(12120, 0.5f), lateViewer.age(12120, 0.5f))
        assertEquals(server.lift(server.age(12120, 0.5f)), lateViewer.lift(lateViewer.age(12120, 0.5f)))
        assertEquals(220, server.impact)
        assertEquals(80, InfuserSequence(12000, false).impact)
    }

    @Test fun `first activation connects regions successively and repeats connect together`() {
        val first = InfuserSequence(0, true)
        assertTrue(first.connection(0, 4) < first.connection(1, 4))
        assertTrue(first.connection(3, 4) < first.impact - 26)
        val repeat = InfuserSequence(0, false)
        assertEquals(repeat.connection(0, 4), repeat.connection(3, 4))
    }

    @Test fun `skip retains current core height and schedules an impact before settling`() {
        for (first in listOf(true, false)) for (tick in listOf(0, 10, 45, 75)) {
            val sequence = InfuserSequence(1000, first)
            val before = sequence.lift(tick.toFloat())
            assertTrue(sequence.skip(1000L + tick))
            assertEquals(before, sequence.lift(tick.toFloat()), 0.00001f)
            assertTrue(sequence.impact in tick..tick + 6)
            assertTrue(sequence.settled > sequence.impact)
            assertFalse(sequence.skip(1000L + tick + 1))
            assertEquals(0f, sequence.lift(sequence.settled.toFloat()), 0.00001f)
        }
    }

    @Test fun `skip at or after impact cannot restart assembly or change its time`() {
        val sequence = InfuserSequence(500, true)
        assertFalse(sequence.skip(720))
        assertFalse(sequence.skip(900))
        assertEquals(220, sequence.impact)
    }

    @Test fun `core settles while the terrain wave continues`() {
        for (first in listOf(true, false)) {
            val sequence = InfuserSequence(0, first)
            for (tick in 0..sequence.finished) assertTrue(sequence.lift(tick.toFloat()) in 0f..2.81f)
            assertEquals(0f, sequence.lift(sequence.finished.toFloat()))
            assertTrue(sequence.finished > sequence.settled + 40)
        }
    }

    @Test fun `crystal rotation wraps continuously and is independent of render calls`() {
        for (axis in 0..2) {
            val before = InfuserSequence.angle(23999, 0.999f, axis)
            val after = InfuserSequence.angle(24000, 0f, axis)
            val wrapped = ((after - before + 540f) % 360f) - 180f
            assertTrue(abs(wrapped) < 0.01f)
            assertEquals(after, InfuserSequence.angle(24000, 0f, axis))
        }
    }
}
