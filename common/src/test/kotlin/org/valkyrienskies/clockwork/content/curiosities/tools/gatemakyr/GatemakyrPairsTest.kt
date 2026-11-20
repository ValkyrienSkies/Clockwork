package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class GatemakyrPairsTest {
    @Test fun bothCarriersKeepTheirLocalBasesAndWaitForSurfaceSpread() {
        val a = GatemakyrTarget(Vec3(0.0, 0.0, 0.0), Direction.UP, 3000000000L, true, 32, 32, Vec3(15.5, 0.0, 0.0))
        val b = a.copy(face = Direction.EAST, shipId = 4000000000L)
        val first = GatemakyrPairs.Mouth(a, 40)
        val second = GatemakyrPairs.Mouth(b, 50)
        val owner = UUID.randomUUID()
        val data = GatemakyrPairs()
        data.pairs[owner] = first to second
        val pair = GatemakyrPairs.load(data.save(CompoundTag())).pairs.getValue(owner)
        assertEquals(first to second, pair)
        assertEquals(0.0, pair.first.target.localFrame().u.distanceTo(Vec3(1.0, 0.0, 0.0)), 1e-12)
        assertEquals(Vec3(0.0, 1.0, 0.0), pair.second.target.localFrame().v)
        assertEquals(145L, pair.first.readyAt())
    }

    @Test fun reloadPreservesOwnerSideOrderAndOpeningTimes() {
        val owner = UUID.randomUUID()
        val a = GatemakyrTarget(Vec3(0.5, 180.0, 0.5), Direction.SOUTH, width = 9, height = 6)
        val b = GatemakyrTarget(Vec3(-28669952.5, 128.004, 12290045.5), Direction.UP, 1L shl 38, true, 9, 6,
            Vec3(-28669954.0, 128.004, 12290044.2))
        val data = GatemakyrPairs()
        data.pairs[owner] = GatemakyrPairs.Mouth(a, 60) to GatemakyrPairs.Mouth(b, 40)
        assertEquals(data.pairs, GatemakyrPairs.load(data.save(CompoundTag())).pairs)
    }
}
