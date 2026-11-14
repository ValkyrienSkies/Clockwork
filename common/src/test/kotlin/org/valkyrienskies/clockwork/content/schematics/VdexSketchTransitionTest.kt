package org.valkyrienskies.clockwork.content.schematics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.schematics.VdexSketchTransition.Phase

class VdexSketchTransitionTest {
    @Test
    fun `drawing waits for geometry and does not restart on the same selection`() {
        val transition = VdexSketchTransition()
        transition.update("first", false, 0)
        assertEquals(Phase.WAITING, transition.phase)
        assertNull(transition.current)
        transition.update("first", true, 100)
        assertEquals(Phase.DRAWING, transition.phase)
        assertEquals(0f, transition.draw(100))
        transition.update("first", true, 1550)
        assertEquals(Phase.READY, transition.phase)
        transition.update("first", true, 2000)
        assertEquals(1f, transition.draw(2000))
    }

    @Test
    fun `rapid selections finish erasing before drawing the latest`() {
        val transition = VdexSketchTransition()
        transition.update("first", true, 0)
        transition.update("second", true, 2000)
        assertEquals(Phase.ERASING, transition.phase)
        assertEquals("first", transition.current)
        transition.update("third", true, 2300)
        assertEquals("first", transition.current)
        transition.update("third", true, 2650)
        assertEquals(Phase.DRAWING, transition.phase)
        assertEquals("third", transition.current)
        assertEquals(0f, transition.draw(2650))
    }

    @Test
    fun `interrupted drawing erases only its visible portion and waits for next mesh`() {
        val transition = VdexSketchTransition()
        transition.update("first", true, 0)
        transition.update("second", false, 725)
        assertEquals(.5f, transition.draw(1000))
        transition.update("second", false, 1375)
        assertEquals(Phase.WAITING, transition.phase)
        assertNull(transition.current)
        transition.update("second", true, 2000)
        assertEquals(Phase.DRAWING, transition.phase)
        assertEquals("second", transition.current)
    }

    @Test
    fun `clearing the selection erases to an empty page`() {
        val transition = VdexSketchTransition()
        transition.update("first", true, 0)
        transition.update("", false, 1500)
        assertEquals(Phase.ERASING, transition.phase)
        transition.update("", false, 2150)
        assertEquals(Phase.WAITING, transition.phase)
        assertNull(transition.current)
        assertEquals(0f, transition.draw(2150))
    }
}
