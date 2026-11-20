package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class GatemakyrAudioTest {
    private val right = Vec3(1.0, 0.0, 0.0)
    private val up = Vec3(0.0, 1.0, 0.0)
    private val front = Vec3(0.0, 0.0, 1.0)
    private val near = GatemakyrTarget.Frame(Vec3.ZERO, right, up, front, Vec3.ZERO)
    private val far = near.copy(position = Vec3(100.0, 0.0, 0.0))
    private val passage = GatemakyrPassage(near, far)

    @Test fun straightPathCountsBothSides() {
        val listener = Vec3(0.0, 0.0, 3.0)
        val source = Vec3(100.0, 0.0, 2.0)
        assertEquals(Vec3(0.0, 0.0, -2.0), GatemakyrAudio.apparentSource(passage, 2, 2, true, listener, source, 5.0))
        assertNull(GatemakyrAudio.apparentSource(passage, 2, 2, true, listener, source, 4.99))
    }

    @Test fun offAxisPathSoundsFromEdgeAndUsesBothLegsForVolume() {
        val listener = Vec3(3.0, 0.0, 2.0)
        val source = Vec3(97.0, 0.0, 2.0)
        val point = requireNotNull(GatemakyrAudio.apparentSource(passage, 2, 2, true, listener, source, 6.0))
        val edge = Vec3(1.0, 0.0, 0.0)
        assertEquals(sqrt(8.0) * 2, listener.distanceTo(point), 1e-8)
        assertEquals(0.0, point.subtract(listener).normalize().distanceTo(edge.subtract(listener).normalize()), 1e-8)
    }

    @Test fun onlyFreestandingPairsPassSoundBetweenBackFaces() {
        val listener = Vec3(0.0, 0.0, -3.0)
        val source = Vec3(100.0, 0.0, -2.0)
        assertNull(GatemakyrAudio.apparentSource(passage, 2, 2, true, listener, source, 5.0))
        assertEquals(Vec3(0.0, 0.0, 2.0), GatemakyrAudio.apparentSource(passage, 2, 2, false, listener, source, 5.0))
    }
}
