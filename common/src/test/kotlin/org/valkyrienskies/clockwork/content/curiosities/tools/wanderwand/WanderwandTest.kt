package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import org.joml.primitives.AABBi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.util.AABBHelper.subtractWithAABB

class WanderwandTest {
    @Test fun `saved links retain both ship anchors and rest poses without retaining runtime IDs`() {
        val data = WandLinks()
        val a = WandAnchor(BlockPos(-25000120, -16, 23000010), Direction.EAST, 123)
        val b = WandAnchor(BlockPos(12, 70, -45), Direction.UP, -1)
        val poseA = org.valkyrienskies.core.internal.joints.VSJointPose(org.joml.Vector3d(-25000119.5, -15.5, 23000010.5), org.joml.Quaterniond().rotateXYZ(0.1, 0.4, 0.7))
        val poseB = org.valkyrienskies.core.internal.joints.VSJointPose(org.joml.Vector3d(12.5, 71.0, -44.5), org.joml.Quaterniond())
        for (rope in listOf(false, true)) {
            val link = WandLinks.Link(java.util.UUID.randomUUID(), a, b, rope, 18.75, poseA, poseB)
            link.jointId = 991; link.pending = true
            data.links[link.id] = link
        }
        val loaded = WandLinks.load(data.save(net.minecraft.nbt.CompoundTag()))
        assertEquals(2, loaded.links.size)
        for ((id, link) in loaded.links) {
            val original = data.links[id]!!
            assertEquals(original.a, link.a); assertEquals(original.b, link.b)
            assertEquals(original.rope, link.rope); assertEquals(18.75, link.length)
            assertEquals(original.pose0, link.pose0); assertEquals(original.pose1, link.pose1)
            assertNull(link.jointId); assertFalse(link.pending)
        }
    }
    @Test fun `selection max faces are exclusive`() {
        val b = AABBi(0, 0, 0, 2, 2, 2)
        assertTrue(WandSelectionMath.contains(b, 1, 1, 1))
        assertFalse(WandSelectionMath.contains(b, 2, 1, 1))
        assertFalse(WandSelectionMath.contains(b, 1, 2, 1))
        assertFalse(WandSelectionMath.contains(b, 1, 1, 2))
    }

    @Test fun `overlapping additions form a disjoint union`() {
        val boxes = WandSelectionMath.union(listOf(AABBi(0, 0, 0, 3, 3, 3)), AABBi(1, 1, 1, 4, 4, 4))
        assertEquals(46L, boxes.sumOf(WandSelectionMath::volume))
        for (x in 0..4) for (y in 0..4) for (z in 0..4)
            assertTrue(boxes.count { WandSelectionMath.contains(it, x, y, z) } <= 1)
    }

    @Test fun `subtract then add back preserves exact volume`() {
        val whole = AABBi(-5, -5, -5, 5, 5, 5)
        val hole = AABBi(-2, -2, -2, 2, 2, 2)
        val cut = whole.subtractWithAABB(hole)
        assertEquals(936L, cut.sumOf(WandSelectionMath::volume))
        val restored = WandSelectionMath.union(cut, hole)
        assertEquals(1000L, restored.sumOf(WandSelectionMath::volume))
    }

    @Test fun `repeated selections cannot duplicate volume`() {
        val box = AABBi(-10, 2, 7, 12, 6, 9)
        var selection = listOf<org.joml.primitives.AABBic>()
        repeat(20) { selection = WandSelectionMath.union(selection, box) }
        assertEquals(WandSelectionMath.volume(box), selection.sumOf(WandSelectionMath::volume))
    }
    @Test fun `legacy overlapping selections are normalized before editing`() {
        val boxes = WandSelectionMath.normalize(listOf(AABBi(0, 0, 0, 3, 3, 3), AABBi(1, 1, 1, 4, 4, 4), AABBi(0, 0, 0, 3, 3, 3)))
        assertEquals(46L, boxes.sumOf(WandSelectionMath::volume))
    }

    @Test fun `rope keeps tangential velocity but stops outward velocity`() {
        val v = WandRopePhysics.constrain(Vec3(5.0, 0.0, 0.0), Vec3(1.0, 0.0, 0.7), Vec3.ZERO, Vec3.ZERO, 5.0)!!
        assertEquals(0.0, v.x, 1e-8); assertEquals(0.7, v.z, 1e-8)
    }
    @Test fun `slack rope does not pull`() {
        assertNull(WandRopePhysics.constrain(Vec3(2.0, 0.0, 0.0), Vec3(1.0, 0.0, 0.0), Vec3.ZERO, Vec3.ZERO, 5.0))
    }
    @Test fun `rope allows inward motion and follows moving anchors`() {
        val inward = Vec3(-0.4, 0.2, 0.0)
        assertEquals(inward, WandRopePhysics.constrain(Vec3(5.0, 0.0, 0.0), inward, Vec3.ZERO, Vec3.ZERO, 5.0))
        val carried = WandRopePhysics.constrain(Vec3(5.0, 0.0, 0.0), Vec3(0.8, 0.0, 0.3), Vec3.ZERO, Vec3(0.5, 0.0, 0.0), 5.0)!!
        assertEquals(0.5, carried.x, 1e-8); assertEquals(0.3, carried.z, 1e-8)
    }
    @Test fun `reel correction is bounded`() {
        val v = WandRopePhysics.constrain(Vec3(100.0, 0.0, 0.0), Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, 2.0)!!
        assertEquals(-0.9, v.x, 1e-8)
        assertNull(WandRopePhysics.constrain(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, 2.0))
    }
    @Test fun `animation sampling is independent of render count`() {
        val a = WanderwandAnimation(); a.accept("weld_end", 100)
        val pose = a.sample(108, 0.5f)
        repeat(240) { assertEquals(pose, a.sample(108, 0.5f)) }
        assertEquals(WanderwandAnimation.Pose(), a.sample(200, 0f))
    }
    @Test fun `equip settles and energy extraction reverses the cast`() {
        val a = WanderwandAnimation(); a.accept("draw", 0)
        assertEquals(1f, a.sample(0, 0f).lower)
        assertEquals(0f, a.sample(16, 0f).lower)
        a.accept("bind_start", 0); assertTrue(a.sample(6, 0f).tilt > 0)
        a.accept("dismiss", 0); assertTrue(a.sample(6, 0f).tilt < 0)
    }
    @Test fun `every weld face pair has a flush lattice rotation`() {
        assertEquals(24, WandGridRotation.all.size)
        for (a in Direction.values()) for (b in Direction.values()) {
            val rotation = WandGridRotation.choose(a, b)
            assertEquals(b.opposite, rotation.face(a))
            val p = rotation.offset(BlockPos(2, -3, 5))
            assertEquals(38, p.x * p.x + p.y * p.y + p.z * p.z)
        }
    }
    @Test fun `lattice rotations never mirror a ship or merge blocks`() {
        for (r in WandGridRotation.all) {
            val x = r.vector(Vec3(1.0, 0.0, 0.0)); val y = r.vector(Vec3(0.0, 1.0, 0.0)); val z = r.vector(Vec3(0.0, 0.0, 1.0))
            assertEquals(1.0, x.cross(y).dot(z), 1e-6)
            val points = mutableSetOf<BlockPos>()
            for (a in -2..2) for (b in -2..2) for (c in -2..2) points.add(r.offset(BlockPos(a, b, c)))
            assertEquals(125, points.size)
        }
    }
    @Test fun `glue targeting respects range and direction`() {
        val a = Vec3(-2.0, 0.0, 5.0); val b = Vec3(2.0, 0.0, 5.0)
        assertTrue(WandLinkTargeting.intersects(Vec3.ZERO, Vec3(0.0, 0.0, 1.0), 15.0, a, b))
        assertFalse(WandLinkTargeting.intersects(Vec3.ZERO, Vec3(0.0, 0.0, 1.0), 3.0, a, b))
        assertFalse(WandLinkTargeting.intersects(Vec3.ZERO, Vec3(0.0, 0.0, -1.0), 15.0, a, b))
    }
}
