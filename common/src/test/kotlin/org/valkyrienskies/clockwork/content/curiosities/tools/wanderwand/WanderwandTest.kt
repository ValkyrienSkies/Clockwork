package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import org.joml.primitives.AABBi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.util.AABBHelper.subtractWithAABB

class WanderwandTest {
    @Test fun `glue frames meet at the clicked faces with opposing normals for every face pair`() {
        val ra = org.joml.Quaterniond().rotateXYZ(0.6, -0.3, 1.2)
        val rb = org.joml.Quaterniond().rotateXYZ(-0.2, 0.7, -1.5)
        for (a in Direction.values()) for (b in Direction.values()) {
            val anchorA = WandAnchor(BlockPos(-20000000, 7, 20000000), a, 1)
            val anchorB = WandAnchor(BlockPos(20000100, -16, 20000300), b, 2)
            val (pa, pb) = WandLinkPhysics.gluePoses(anchorA, anchorB, ra, rb)
            assertEquals(anchorA.pos.x + 0.5 + a.stepX * 0.5, pa.pos.x(), 1e-8)
            assertEquals(anchorA.pos.y + 0.5 + a.stepY * 0.5, pa.pos.y(), 1e-8)
            assertEquals(anchorB.pos.z + 0.5 + b.stepZ * 0.5, pb.pos.z(), 1e-8)
            val alignedA = org.joml.Quaterniond(rb).mul(pb.rot).mul(org.joml.Quaterniond(pa.rot).invert())
                .transform(org.joml.Vector3d(a.stepX.toDouble(), a.stepY.toDouble(), a.stepZ.toDouble()))
            val oppositeB = rb.transform(org.joml.Vector3d(-b.stepX.toDouble(), -b.stepY.toDouble(), -b.stepZ.toDouble()))
            assertTrue(alignedA.distance(oppositeB) < 1e-8)
        }
    }

    @Test fun `glue has a bounded increasing force ramp and no rigid locked degrees of freedom`() {
        val pose = org.valkyrienskies.core.internal.joints.VSJointPose(org.joml.Vector3d(), org.joml.Quaterniond())
        var previous = -1f
        for (tick in 0..80) {
            val joint = WandLinkPhysics.glueJoint(1, 2, pose, pose, 1000.0, tick)
            assertTrue(joint.motions!!.values.all { it == org.valkyrienskies.core.internal.joints.VSD6Joint.D6Motion.FREE })
            val drive = joint.drives!![org.valkyrienskies.core.internal.joints.VSD6Joint.D6Drive.X]!!
            assertTrue(drive.driveForceLimit >= previous)
            assertTrue(drive.driveForceLimit <= 24000f)
            assertTrue(drive.driveForceLimit < joint.maxForceTorque!!.maxForce)
            previous = drive.driveForceLimit
        }
        assertEquals(0.0, WandLinkPhysics.ramp(0)); assertEquals(1.0, WandLinkPhysics.ramp(60))
    }

    @Test fun `ordinary rope load and glue settling survive but excessive tensile force breaks them`() {
        assertFalse(WandLinkPhysics.overloaded(true, 0.02, 0.2, 100))
        assertFalse(WandLinkPhysics.overloaded(true, 0.8, 0.0, 100))
        assertFalse(WandLinkPhysics.overloaded(true, 0.0, 10.0, 100))
        assertTrue(WandLinkPhysics.overloaded(true, 2.0, 0.0, 100))
        assertTrue(WandLinkPhysics.overloaded(true, 0.0, 30.0, 100))
        assertFalse(WandLinkPhysics.overloaded(false, 20.0, 0.0, 30))
        assertFalse(WandLinkPhysics.overloaded(false, 0.08, 0.5, 100))
        assertFalse(WandLinkPhysics.overloaded(false, 2.0, 0.0, 100))
        assertFalse(WandLinkPhysics.overloaded(true, -5.0, 100.0, 100))
    }

    @Test fun `glue survives a pull that breaks rope but has a firm stretch limit`() {
        assertTrue(WandLinkPhysics.overloaded(true, 0.0, 30.0, 100))
        assertFalse(WandLinkPhysics.overloaded(false, 0.0, 30.0, 100))
        assertFalse(WandLinkPhysics.overloaded(false, 5.0, 20.0, 100))
        assertFalse(WandLinkPhysics.overloaded(false, 6.0, 0.0, 100))
        assertTrue(WandLinkPhysics.overloaded(false, 6.01, 0.0, 100))
        assertTrue(WandLinkPhysics.overloaded(false, 0.0, 110.0, 100))
        val pose = org.valkyrienskies.core.internal.joints.VSJointPose(org.joml.Vector3d(), org.joml.Quaterniond())
        val joint = WandLinkPhysics.glueJoint(1, 2, pose, pose, 1000.0, 100)
        assertTrue(joint.maxForceTorque!!.maxForce > 1000 * WandLinkPhysics.ROPE_BREAK_ACCELERATION)
    }

    @Test fun `held bind tolerates swinging and reeling but snaps on a hard yank`() {
        val p = Vec3(5.0, 0.0, 0.0)
        assertFalse(WandLinkPhysics.grappleOverloaded(p, Vec3(0.0, 0.0, 1.0), Vec3.ZERO, Vec3.ZERO, 5.0))
        assertFalse(WandLinkPhysics.grappleOverloaded(p, Vec3(0.0, 0.0, 2.0), Vec3.ZERO, Vec3.ZERO, 5.0))
        assertFalse(WandLinkPhysics.grappleOverloaded(p, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, 4.25))
        assertFalse(WandLinkPhysics.grappleOverloaded(p, Vec3(1.0, 0.0, 0.0), Vec3.ZERO, Vec3.ZERO, 5.0))
        assertTrue(WandLinkPhysics.grappleOverloaded(p, Vec3(2.0, 0.0, 0.0), Vec3.ZERO, Vec3.ZERO, 5.0))
        assertFalse(WandLinkPhysics.grappleOverloaded(p, Vec3(1.0, 0.0, 0.0), Vec3.ZERO, Vec3(1.0, 0.0, 0.0), 5.0))
        assertFalse(WandLinkPhysics.grappleOverloaded(p, Vec3(4.0, 0.0, 0.0), Vec3.ZERO, Vec3.ZERO, 10.0))
    }

    @Test fun `crystal phases are bounded rotate forward and remain continuous through world day wrap`() {
        for (axis in 0..2) for (tick in 0L..24002L) {
            val a = WandCrystalMotion.angle(tick, 0f, axis)
            val b = WandCrystalMotion.angle(tick, 0.5f, axis)
            val c = WandCrystalMotion.angle(tick + 1, 0f, axis)
            assertTrue(a >= 0 && a < 360)
            assertTrue((b - a + 360) % 360 in 0.05f..4f)
            assertTrue((c - b + 360) % 360 in 0.05f..4f)
        }
        assertEquals(WandCrystalMotion.lift(0, 0f), WandCrystalMotion.lift(24000, 0f), 1e-6f)
    }

    @Test fun `cast attacks promptly and settles within twelve ticks`() {
        val animation = WanderwandAnimation(); animation.accept("attach_end", 0)
        assertTrue(animation.sample(2, 0f).charge > animation.sample(6, 0f).charge)
        assertEquals(WanderwandAnimation.Pose(), animation.sample(12, 0f))
    }

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
    @Test fun `swing input accelerates along the arc without increasing rope tension`() {
        val position = Vec3(3.0, -4.0, 0.0)
        val initial = Vec3(0.1, 0.075, 0.2)
        val accelerated = WandRopePhysics.accelerateSwing(position, initial, Vec3.ZERO, Vec3.ZERO, Vec3(1.0, 0.0, 0.0))
        val extra = accelerated.subtract(initial)
        assertTrue(extra.x > 0)
        assertTrue(extra.y > 0)
        assertEquals(0.0, extra.dot(position.normalize()), 1e-8)
        assertEquals(initial.z, accelerated.z, 1e-8)
        assertEquals(initial, WandRopePhysics.accelerateSwing(position, initial, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO))
    }
    @Test fun `swing assist is bounded relative to the anchor and can brake existing momentum`() {
        val position = Vec3(0.0, -5.0, 0.0)
        val anchorVelocity = Vec3(0.5, 0.0, 0.0)
        var velocity = anchorVelocity
        repeat(200) {
            velocity = WandRopePhysics.accelerateSwing(position, velocity, Vec3.ZERO, anchorVelocity, Vec3(1.0, 0.0, 0.0))
        }
        assertTrue(velocity.x > anchorVelocity.x + 0.5)
        assertTrue(velocity.x <= anchorVelocity.x + 1.2 + 1e-8)
        val fast = Vec3(3.0, 0.0, 0.0)
        assertEquals(fast, WandRopePhysics.accelerateSwing(position, fast, Vec3.ZERO, anchorVelocity, Vec3(1.0, 0.0, 0.0)))
        assertTrue(WandRopePhysics.accelerateSwing(position, fast, Vec3.ZERO, anchorVelocity, Vec3(-1.0, 0.0, 0.0)).x < fast.x)
    }
    @Test fun `diagonal swing input is normalized and radial input cannot pull through the anchor`() {
        val position = Vec3(0.0, -5.0, 0.0)
        val straight = WandRopePhysics.accelerateSwing(position, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, Vec3(1.0, 0.0, 0.0))
        val diagonal = WandRopePhysics.accelerateSwing(position, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, Vec3(1.0, 0.0, 1.0))
        assertEquals(straight.length(), diagonal.length(), 1e-8)
        assertEquals(Vec3.ZERO, WandRopePhysics.accelerateSwing(Vec3(5.0, 0.0, 0.0), Vec3.ZERO,
            Vec3.ZERO, Vec3.ZERO, Vec3(1.0, 0.0, 0.0)))
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
