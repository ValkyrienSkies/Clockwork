package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringSettings
import org.valkyrienskies.core.internal.joints.VSJointPose
import java.util.UUID

class WandLinkTransferTest {
    @Test fun thinkablesSaveAndPasteWithNewEndpointsAndNoStalePulseOrJoint() {
        val a = WandAnchor(BlockPos(10000, 82, -20000), Direction.DOWN, 41)
        val b = WandAnchor(BlockPos(30000, 78, -40000), Direction.UP, 42)
        fun pose(anchor: WandAnchor) = anchor.local().let { VSJointPose(Vector3d(it.x, it.y, it.z), Quaterniond()) }
        val link = WandLinks.Link(UUID.randomUUID(), a, b, true, 7.25, pose(a), pose(b), physical = true, thinkable = true)
        link.jointId = 90
        link.pulseStart = 120
        link.pulseDuration = 15
        val data = WandLinks().also { it.links[link.id] = link }
        val loaded = WandLinks.load(data.save(net.minecraft.nbt.CompoundTag())).links.getValue(link.id)
        assertTrue(loaded.thinkable && loaded.physical && loaded.rope && !loaded.spring)
        assertEquals(a, loaded.a)
        assertEquals(b, loaded.b)
        assertEquals(7.25, loaded.length)
        assertNull(loaded.jointId)
        assertEquals(-1L, loaded.pulseStart)
        val saved = WandLinkSchematics.encode(listOf(loaded), mapOf(41L to a.pos.below(2), 42L to b.pos.below(3)))
        val pasted = WandLinkSchematics.decode(saved, mapOf(
            41L to (71L to BlockPos(90000, 100, 10000)), 42L to (72L to BlockPos(120000, 100, 30000)))).single()
        assertTrue(pasted.thinkable && pasted.physical && pasted.rope && !pasted.spring)
        assertNotEquals(link.id, pasted.id)
        assertEquals(71L, pasted.a.shipId)
        assertEquals(72L, pasted.b.shipId)
        assertEquals(BlockPos(90000, 102, 10000), pasted.a.pos)
        assertEquals(BlockPos(120000, 103, 30000), pasted.b.pos)
        assertEquals(a.face, pasted.a.face)
        assertEquals(b.face, pasted.b.face)
        assertEquals(7.25, pasted.length)
        assertNull(pasted.jointId)
    }

    @Test fun everyGridRotationCarriesTheJointFrameWithItsBlockFace() {
        val from = WandAnchor(BlockPos(100, 80, -200), Direction.NORTH, 4)
        val originalRotation = Quaterniond().rotationXYZ(0.2, 0.4, 0.6)
        val point = from.local()
        val pose = VSJointPose(Vector3d(point.x, point.y, point.z), originalRotation)
        for (grid in WandGridRotation.all) {
            fun axis(face: Direction) = grid.face(face).normal.let { Vector3d(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()) }
            val rotation = Quaterniond().setFromNormalized(Matrix3d(axis(Direction.EAST), axis(Direction.UP), axis(Direction.SOUTH)))
            val to = WandAnchor(BlockPos(-230, 120, 500), grid.face(from.face), 7)
            val moved = WandLinkTransfer.pose(pose, from, WandLinkTransfer.Destination(to, rotation))
            val expected = to.local()
            assertEquals(0.0, moved.pos.distance(Vector3d(expected.x, expected.y, expected.z)), 1e-8)
            assertEquals(1.0, kotlin.math.abs(Quaterniond(rotation).mul(originalRotation).dot(moved.rot)), 1e-9)
        }
        assertEquals(Vector3d(point.x, point.y, point.z), pose.pos)
        assertEquals(originalRotation, pose.rot)
    }

    @Test fun schematicCopiesRemapBodiesFramesAndSettingsWithoutSharingRuntimeIds() {
        val a = WandAnchor(BlockPos(10000, 82, -20000), Direction.DOWN, 41, winch = true)
        val b = WandAnchor(BlockPos(30000, 78, -40000), Direction.UP, 42)
        fun pose(anchor: WandAnchor) = anchor.local().let { VSJointPose(Vector3d(it.x, it.y, it.z), Quaterniond().rotationX(0.3)) }
        for ((rope, physical, spring) in listOf(Triple(true, false, false), Triple(false, false, false),
            Triple(true, true, false), Triple(true, true, true))) {
            val original = WandLinks.Link(UUID.randomUUID(), a, b, rope, 6.75, pose(a), pose(b), physical, spring)
            original.settings = SpringSettings(12.0, 3.0, 8.0)
            original.appliedLength = 5.5
            original.jointId = 90; original.alignmentId = 91; original.pending = true
            val origins = mapOf(41L to a.pos.below(2), 42L to b.pos.below(3))
            val targets = mapOf(41L to (71L to BlockPos(90000, 100, 10000)), 42L to (72L to BlockPos(120000, 100, 30000)))
            val saved = WandLinkSchematics.encode(listOf(original), origins)
            val first = WandLinkSchematics.decode(saved, targets).single()
            val second = WandLinkSchematics.decode(saved, targets).single()
            assertNotEquals(original.id, first.id); assertNotEquals(first.id, second.id)
            assertEquals(71L, first.a.shipId); assertEquals(72L, first.b.shipId)
            assertEquals(targets.getValue(41).second.above(2), first.a.pos)
            assertEquals(targets.getValue(42).second.above(3), first.b.pos)
            assertEquals(a.face, first.a.face); assertTrue(first.a.winch)
            assertEquals(rope, first.rope); assertEquals(physical, first.physical); assertEquals(spring, first.spring)
            assertEquals(6.75, first.length)
            if (spring) { assertEquals(original.settings, first.settings); assertEquals(5.5, first.appliedLength) }
            assertEquals(1.0, kotlin.math.abs(Quaterniond(original.pose0.rot).dot(first.pose0.rot)), 1e-9)
            val local = first.a.local()
            assertEquals(0.0, first.pose0.pos.distance(Vector3d(local.x, local.y, local.z)), 1e-8)
            assertNull(first.jointId); assertNull(first.alignmentId); assertFalse(first.pending)
            assertEquals(a, original.a); assertEquals(90, original.jointId)
        }
    }

    @Test fun schematicCopiesKeepSameBodySpringsButOmitOutsideAnchors() {
        val a = WandAnchor(BlockPos(100, 80, 100), Direction.UP, 4)
        val b = a.copy(pos = a.pos.east(3))
        val pose = VSJointPose(Vector3d(), Quaterniond())
        val spring = WandLinks.Link(UUID.randomUUID(), a, b, true, 3.0, pose, pose, physical = true, spring = true)
        val external = spring.copy(id = UUID.randomUUID(), b = b.copy(shipId = -1))
        val data = WandLinkSchematics.encode(listOf(spring, external), mapOf(4L to a.pos))
        val restored = WandLinkSchematics.decode(data, mapOf(4L to (8L to BlockPos(300, 80, 300)))).single()
        assertTrue(restored.spring); assertEquals(restored.a.shipId, restored.b.shipId)
        assertTrue(WandLinkSchematics.decode(data, emptyMap()).isEmpty())
        data.putInt("version", 99)
        assertTrue(WandLinkSchematics.decode(data, mapOf(4L to (8L to BlockPos.ZERO))).isEmpty())
    }
}
