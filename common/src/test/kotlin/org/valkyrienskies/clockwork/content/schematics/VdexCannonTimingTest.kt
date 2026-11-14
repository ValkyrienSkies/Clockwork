package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VdexCannonTimingTest {
    @Test
    fun `shots overlap at the configured cadence and can arrive out of order`() {
        val saved = CompoundTag()
        val flights = VdexCannonBuild.Flights(saved)
        val launches = ArrayList<Int>()
        val arrivals = ArrayList<Pair<Int, Int>>()
        for (tick in 0..35) {
            flights.tick { arrivals.add(it to tick); true }
            if (flights.canFire() && launches.size < 2) {
                launches.add(tick)
                flights.launch(if (launches.size == 1) 30 else 8, 2, ListTag())
            }
            if (tick == 3) {
                assertTrue(flights.pending)
                assertEquals(2, saved.getInt("Cursor"))
                assertEquals(0, flights.delivered)
            }
        }
        assertEquals(listOf(0, 3), launches)
        assertEquals(listOf(1 to 11, 0 to 30), arrivals)
        assertEquals(2, flights.delivered)
        assertFalse(flights.pending)
    }

    @Test
    fun `reload retains cadence flights and refunds only undelivered paid shots once`() {
        val saved = CompoundTag()
        var flights = VdexCannonBuild.Flights(saved)
        val paid = ListTag().apply { add(CompoundTag().apply { putString("id", "minecraft:stone"); putByte("Count", 1) }) }
        flights.launch(1, 0, paid)
        flights.tick { true }
        flights.launch(10, 4, paid)
        flights.launch(12, 4, ListTag())
        assertFalse(flights.canFire())
        val reloaded = saved.copy()
        flights = VdexCannonBuild.Flights(reloaded)
        assertEquals(3, reloaded.getInt("Cursor"))
        assertEquals(1, flights.delivered)
        repeat(3) { assertFalse(flights.canFire()) }
        assertTrue(flights.canFire())
        assertFalse(flights.hasPendingBefore(1))
        assertTrue(flights.hasPendingBefore(2))
        assertEquals(1, flights.takeRefunds().size)
        assertTrue(flights.takeRefunds().isEmpty())
        assertFalse(flights.pending)
    }

    @Test
    fun `legacy paid flight migrates and blocked delivery remains pending without repaying`() {
        val saved = CompoundTag().apply {
            putInt("Cursor", 5); putBoolean("Paid", true); putBoolean("Consumed", true); putInt("Flight", 2)
            put("PaidItems", ListTag().apply { add(CompoundTag().apply { putString("id", "minecraft:stone") }) })
        }
        var flights = VdexCannonBuild.Flights(saved)
        assertEquals(6, saved.getInt("Cursor"))
        assertEquals(5, flights.delivered)
        assertFalse(saved.contains("Paid"))
        flights = VdexCannonBuild.Flights(saved.copy())
        assertTrue(flights.tick { error("Shot landed early") })
        assertFalse(flights.tick { false })
        assertTrue(flights.pending)
        assertEquals(5, flights.delivered)
        assertTrue(flights.tick { assertEquals(5, it); true })
        assertEquals(6, flights.delivered)
        assertFalse(flights.pending)
        assertTrue(flights.takeRefunds().isEmpty())
    }
}
