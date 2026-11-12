package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.core.Direction
import net.minecraft.core.BlockPos
import org.joml.Matrix4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.mod.common.schematic.VdexShipEntry
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VdexPlacementTest {
    @Test
    fun `cannon target stays on the same block as the partial ship centre of mass changes`() {
        val source = VdexBlueprintData.Ship(
            VdexShipEntry(relativePos = Vector3d(4.0, 2.0, -1.0), relativeRot = Quaterniond().rotationZ(Math.PI / 2), scale = 1.5),
            BlockPos(4, 3, 2), emptyList(), emptyList(), Vector3d(.75, 1.5, .5))
        val paste = Vec3(30.0, 70.0, 20.0)
        val rotation = Quaterniond().rotationY(Math.PI / 2)
        val block = Vector3d(2.75, 1.5, .5)
        val target = source.worldPoint(Vector3d(block), paste, rotation)
        assertEquals(0.0, target.distance(Vector3d(29.0, 75.0, 16.0)), 1e-8)
        val corner = Vector3d(28000000.0, 64.0, -28000000.0)
        for (partialCenter in listOf(Vector3d(.5, .5, .5), Vector3d(2.0, 1.0, .75), source.pivot)) {
            val center = Vector3d(corner).add(partialCenter)
            val worldCenter = source.worldPoint(Vector3d(partialCenter), paste, rotation)
            val transform = Matrix4d().translation(worldCenter).rotate(Quaterniond(rotation).mul(source.entry.relativeRot))
                .scale(source.entry.scale).translate(Vector3d(center).negate())
            val placed = transform.transformPosition(Vector3d(corner).add(block))
            assertEquals(0.0, placed.distance(target), 1e-6)
        }
    }

    private val bounds = AABB(-8.0, -3.0, -1.0, 2.0, 5.0, 7.0)
    private fun same(a: Vec3, b: Vec3) { assertEquals(0.0, a.distanceTo(b), .00002) }

    @Test
    fun `deploy places the rotated bottom centre on the target`() {
        val target = Vec3(15.5, 72.0, -3.5)
        for (yaw in listOf(0f, 90f, 180f, -90f, 37f)) {
            val placement = VdexPlacement.at(bounds, target, yaw)
            same(target, placement.world(Vec3(bounds.center.x, bounds.minY, bounds.center.z)))
        }
    }

    @Test
    fun `rotation keeps off-centre connected assemblies in place`() {
        val placement = VdexPlacement(Vec3(4.0, 60.0, -10.0), 0f)
        var rotated = placement
        repeat(4) {
            rotated = rotated.rotate(bounds, -90f)
            same(placement.world(bounds.center), rotated.world(bounds.center))
        }
        same(placement.position, rotated.position)
        same(placement.world(Vec3(1.0, 2.0, 3.0)), rotated.world(Vec3(1.0, 2.0, 3.0)))
    }

    @Test
    fun `face selection follows rotation instead of the world bounding box`() {
        val placement = VdexPlacement(Vec3(10.0, 64.0, -3.0), 90f)
        assertEquals(Direction.WEST, placement.face(bounds, placement.world(Vec3(-12.0, 0.0, 0.0)), placement.world(Vec3(0.0, 0.0, 0.0))))
        assertEquals(Direction.UP, placement.face(bounds, placement.world(Vec3(0.0, 9.0, 0.0)), placement.world(Vec3(0.0, 0.0, 0.0))))
        assertNull(placement.face(bounds, placement.world(Vec3(9.0, 12.0, 0.0)), placement.world(Vec3(12.0, 12.0, 0.0))))
    }
}
