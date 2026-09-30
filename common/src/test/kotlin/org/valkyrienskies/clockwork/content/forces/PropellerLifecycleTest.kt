package org.valkyrienskies.clockwork.content.forces

import org.joml.Vector3d
import org.joml.Vector3i
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropCreateData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropUpdateData

/** Exercises the real physics queues. Block-entity assembly itself needs a Minecraft runtime. */
class PropellerLifecycleTest {
    @Test
    fun `reassembly replaces the model geometry and collective pitch in both directions`() {
        for (initialSails in listOf(false, true)) {
            val controller = PropellerController()
            val oldId = controller.createApplier(snapshot(initialSails))
            controller.pollChanges()
            controller.appliers.getValue(oldId).currentBladePitch = 0.8

            controller.removeApplier(oldId)
            val replacement = snapshot(!initialSails)
            val newId = controller.createApplier(replacement)
            controller.pollChanges()

            assertNotEquals(oldId, newId)
            assertEquals(setOf(newId), controller.appliers.keys)
            val data = controller.appliers.getValue(newId)
            assertEquals(replacement.brass, data.brass)
            assertEquals(replacement.blades, data.blades)
            assertEquals(replacement.sailPositions, data.sailPositions)
            assertEquals(Math.toRadians(12.0), data.currentBladePitch, 0.0)
        }
    }

    @Test
    fun `disassembly and reassembly before queue polling leave only the new rotor`() {
        val controller = PropellerController()
        val oldId = controller.createApplier(snapshot(false))
        controller.removeApplier(oldId)
        val newId = controller.createApplier(snapshot(true))
        // An update already in transit belongs to the old applier, never its replacement.
        controller.updateApplier(oldId, PropUpdateData(99.0, 77.0, true, false, listOf(BladeData(true, 80.0, 10.0))))
        controller.pollChanges()

        assertEquals(setOf(newId), controller.appliers.keys)
        val data = controller.appliers.getValue(newId)
        assertTrue(data.brass && data.active && data.blades.isEmpty())
        assertEquals(18.0, data.bearingSpeed, 0.0)
        assertEquals(0.0, data.bearingAngle, 0.0)

        controller.removeApplier(newId)
        controller.pollChanges()
        assertTrue(controller.appliers.isEmpty())
    }

    private fun snapshot(sails: Boolean) = PropCreateData(
        Vector3i(), Vector3d(0.0, 0.0, 1.0), 0.0, 18.0,
        if (sails) listOf(Vector3i(2, 0, 0), Vector3i(-2, 0, 0)) else emptyList(),
        false, true, sails, if (sails) emptyList() else listOf(BladeData(false, 4.0, 3.0))
    )
}
