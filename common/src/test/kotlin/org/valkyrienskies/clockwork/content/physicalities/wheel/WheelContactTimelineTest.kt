package org.valkyrienskies.clockwork.content.physicalities.wheel

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WheelContactTimelineTest {
    private fun contact(depth: Float) = listOf(TireContact(-1, Vector3d(0.0, 1.0, 0.0), depth))

    @Test
    fun bunchedPacketsInterpolateAtTheirServerTicks() {
        val timeline = WheelContactTimeline()
        timeline.add(100, contact(.1f))
        timeline.add(102, contact(.3f))
        timeline.add(104, contact(.2f))
        assertFalse(timeline.add(102, contact(.9f)))
        assertEquals(.15f, timeline.sample(100.5).single().compression, .00001f)
        assertEquals(.25f, timeline.sample(103.0).single().compression, .00001f)
    }

    @Test
    fun compressionFadesAfterSilenceAndDoesNotBridgeTrackingGaps() {
        val timeline = WheelContactTimeline()
        timeline.add(100, contact(.4f))
        assertEquals(.4f, timeline.sample(102.0).single().compression)
        assertEquals(.35f, timeline.sample(102.5).single().compression, .00001f)
        assertTrue(timeline.sample(106.0).isEmpty())
        timeline.add(120, contact(.2f))
        assertTrue(timeline.sample(118.0).isEmpty())
        assertEquals(.1f, timeline.sample(119.0).single().compression)
        timeline.add(122, emptyList())
        assertEquals(.1f, timeline.sample(121.0).single().compression)
    }

    @Test
    fun readsTimestampedAndLegacyContactPackets() {
        val dimension = ResourceLocation("minecraft", "overworld")
        for (tick in listOf(321L, null)) {
            val buffer = FriendlyByteBuf(Unpooled.buffer())
            try {
                val state = TireState(42, contact(.5f), .25f)
                WheelContactPacket(dimension, listOf(state), emptyList(), tick).write(buffer)
                val decoded = WheelContactPacket(buffer)
                assertEquals(listOf(state), decoded.wheels)
                assertEquals(tick, decoded.sampleTick)
                assertEquals(0, buffer.readableBytes())
            } finally {
                buffer.release()
            }
        }
    }
}
