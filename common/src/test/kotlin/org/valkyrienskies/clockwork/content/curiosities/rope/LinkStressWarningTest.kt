package org.valkyrienskies.clockwork.content.curiosities.rope

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringJoints
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringSettings

class LinkStressWarningTest {
    @Test fun warningVolumeRisesFromQuietToFullAtBreakingLoad() {
        assertEquals(0f, LinkStressWarning.volume(0.49f))
        assertTrue(LinkStressWarning.volume(LinkStressWarning.START) <= 0.05f)
        assertTrue(LinkStressWarning.volume(0.9f) > LinkStressWarning.volume(0.7f))
        assertEquals(1f, LinkStressWarning.volume(1f))
        assertEquals(1f, LinkStressWarning.volume(2f))
        assertEquals(0f, LinkStressWarning.volume(Float.NaN))
        assertTrue(LinkStressWarning.RELEASE < LinkStressWarning.START)
    }

    @Test fun slackRopeNeverStrainsWhileSpringCompressionCanWarn() {
        val previousStrength = SpringSettings(stiffness = 4.0, damping = 2.0)
        assertEquals(0.0, RopeKnotPhysics.stressRatio(-1.0, 1000.0))
        assertEquals(0.75, RopeKnotPhysics.stressRatio(6.0, 0.0))
        assertEquals(0.1640625, SpringJoints.stressRatio(13.5, 0.0))
        assertEquals(0.65625, SpringJoints.stressRatio(13.5, 0.0, previousStrength))
        assertEquals(0.65625, SpringJoints.stressRatio(-13.5, 0.0, previousStrength))
        assertFalse(RopeKnotPhysics.overloaded(6.0, 0.0))
        assertFalse(SpringJoints.overloaded(13.5, 0.0, previousStrength))
        assertTrue(RopeKnotPhysics.overloaded(8.1, 0.0))
        assertTrue(SpringJoints.overloaded(20.6, 0.0, previousStrength))
    }
}
