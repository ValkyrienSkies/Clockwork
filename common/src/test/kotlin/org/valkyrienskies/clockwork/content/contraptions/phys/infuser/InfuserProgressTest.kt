package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class InfuserProgressTest {
    @Test fun `first use belongs to each player and survives a world save`() {
        val alice = UUID.randomUUID()
        val bob = UUID.randomUUID()
        val progress = InfuserProgress()
        assertTrue(progress.activate(alice))
        assertTrue(progress.isDirty)
        assertFalse(progress.activate(alice))
        val reloaded = InfuserProgress.load(progress.save(CompoundTag()))
        assertFalse(reloaded.activate(alice))
        assertTrue(reloaded.activate(bob))
        assertFalse(reloaded.activate(bob))
        assertTrue(InfuserProgress().activate(alice), "A different world has its own first activation")
    }

    @Test fun `region relocation metadata survives synchronization without any block lists`() {
        val region = InfuserRegion(net.minecraft.core.BlockPos(12, 65, -7), 42L,
            net.minecraft.core.BlockPos(1000000, 0, 2000000))
        val tag = region.save()
        assertEquals(region, InfuserRegion.load(tag))
        assertFalse(tag.contains("Blocks"))
    }
}
