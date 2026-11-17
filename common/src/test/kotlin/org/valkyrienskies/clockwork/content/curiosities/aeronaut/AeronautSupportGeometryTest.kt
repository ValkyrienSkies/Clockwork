package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AeronautSupportGeometryTest {
    @Test fun supportGuideDetectsTerrainBetweenItsEndpointRays() {
        val view = AeronautShipReadout.ViewingTriangle(Vec3(0.5, 2.0, 0.5), Vec3(0.5, 2.0, 8.0), Vec3(8.0, 2.0, 8.0))
        assertTrue(view.intersects(1.45, 1.9, 4.0, 1.55, 2.1, 4.05))
        assertFalse(view.intersects(1.45, 0.0, 4.0, 1.55, 1.0, 4.05))
        assertFalse(view.intersects(6.0, 1.0, 2.0, 7.0, 3.0, 3.0))
    }

    @Test fun drawsTheConvexFootprintAtTheSupportHeight() {
        val corners = listOf(Vector3d(1.0, 1.0, 2.0), Vector3d(5.0, 3.0, 2.0),
            Vector3d(5.0, 4.0, 6.0), Vector3d(1.0, 1.0, 6.0))
        val outline = AeronautSupportGeometry.outline(corners + listOf(corners[0], Vector3d(3.0, 2.0, 4.0)), 2.5)
        assertEquals(corners.map { Vector3d(it.x, 2.5, it.z) }.toSet(), outline.toSet())
        assertTrue(AeronautSupportGeometry.contains(outline, Vector3d(3.0, 10.0, 4.0)))
        assertTrue(AeronautSupportGeometry.contains(outline, Vector3d(1.0, 10.0, 4.0)))
        assertFalse(AeronautSupportGeometry.contains(outline, Vector3d(5.1, 10.0, 4.0)))
    }

    @Test fun lineContactHasNoSupportArea() {
        val contacts = (0..3).map { Vector3d(it.toDouble(), 0.0, it.toDouble()) }
        val outline = AeronautSupportGeometry.outline(contacts, 0.0)
        assertEquals(listOf(contacts.first(), contacts.last()), outline)
        assertFalse(AeronautSupportGeometry.contains(outline, contacts[1]))
        assertTrue(AeronautSupportGeometry.outline(emptyList(), 0.0).isEmpty())
    }

    @Test fun groupsOnlyConnectedCoplanarContactsAndWeightsTheirCenterByLoad() {
        fun cell(x: Int, load: Double, plane: Int = 0) = AeronautContactTelemetry.Cell(
            AeronautContactTelemetry.Face(BlockPos(x, 0, 0), Direction.DOWN, plane),
            Vector3d(x + 0.5, plane / 8.0, 0.5), load, load, emptyList())
        val contacts = listOf(cell(0, 30.0), cell(2, 10.0), cell(4, 20.0), cell(1, 5.0, 4))
        val joined = AeronautSupportGeometry.connectedFaces(contacts, 8) { it.block.x == 1 }
        assertEquals(setOf(5.0, 20.0, 40.0), joined.map { it.load }.toSet())
        assertEquals(Vector3d(1.0, 0.0, 0.5), joined.single { it.load == 40.0 }.center)
        assertEquals(4, AeronautSupportGeometry.connectedFaces(contacts, 0) { error("Face budget was exceeded") }.size)
    }
}
