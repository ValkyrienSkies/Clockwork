package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GravitronFreezeAnimationTest {
    @Test
    fun `frozen fields do not expire with time`() {
        val field = GravitronFreezeAnimation(100)
        assertFalse(field.finished(100 + 72000))
        assertFalse(field.thawing)
        assertEquals(40f, field.freezeAge(100 + 72000))
    }

    @Test
    fun `thaw retains partial freeze coverage and duplicate events do not restart it`() {
        val field = GravitronFreezeAnimation(100)
        val visibleAge = field.freezeAge(107)
        field.thaw(107)
        assertEquals(visibleAge, field.freezeAge(107))
        assertEquals(visibleAge, field.freezeAge(112))
        field.thaw(112)
        assertEquals(5f, field.thawAge(112))
        assertFalse(field.finished(134))
        assertTrue(field.finished(135))
    }

    @Test
    fun `late tracking snapshots restore full coverage without replaying the freeze`() {
        val field = GravitronFreezeAnimation(1000000000L, 40f)
        assertEquals(40f, field.freezeAge(1000000000L, 0.25f))
        field.thaw(1000000000L)
        assertEquals(0.25f, field.thawAge(1000000000L, 0.25f))
        assertEquals(40f, field.freezeAge(1000000000L, 0.25f))
    }

    @Test
    fun `saved ship freeze retains its ship-space anchor and start time`() {
        val mapper = ObjectMapper()
        val saved = GravitronFrozenShip().also {
            it.x = 12345678.5; it.y = 72.5; it.z = -9876543.5; it.frozenAt = 1234567890L
        }
        val restored = mapper.readValue(mapper.writeValueAsString(saved), GravitronFrozenShip::class.java)
        assertEquals(saved.x, restored.x)
        assertEquals(saved.y, restored.y)
        assertEquals(saved.z, restored.z)
        assertEquals(saved.frozenAt, restored.frozenAt)
    }
}
