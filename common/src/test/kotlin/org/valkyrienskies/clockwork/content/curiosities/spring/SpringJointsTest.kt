package org.valkyrienskies.clockwork.content.curiosities.spring

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkPhysics
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.core.internal.joints.VSD6Joint
import org.valkyrienskies.core.internal.joints.VSJointPose
import java.util.UUID

class SpringJointsTest {
    @Test fun reloadReconstructsBothSessionJoints() {
        val pose = VSJointPose(Vector3d(), Quaterniond())
        val spring = WandLinks.Link(UUID.randomUUID(), WandAnchor(BlockPos.ZERO, Direction.UP),
            WandAnchor(BlockPos(0, 5, 0), Direction.DOWN, 42), true, 4.0, pose, pose, true, true)
        spring.settings = SpringSettings(2.0, 0.5, 0.25)
        spring.appliedLength = 3.5
        spring.jointId = 5
        spring.alignmentId = 6
        val rope = spring.copy(id = UUID.randomUUID(), spring = false)
        val saved = CompoundTag().also { it.put("links", ListTag().also { list -> list.add(spring.save()); list.add(rope.save()) }) }
        val restored = WandLinks.load(saved).links
        assertTrue(restored[spring.id]!!.spring)
        assertEquals(spring.settings, restored[spring.id]!!.settings)
        assertEquals(spring.appliedLength, restored[spring.id]!!.appliedLength)
        assertFalse(restored[rope.id]!!.spring)
        assertNull(restored[spring.id]!!.jointId)
        assertNull(restored[spring.id]!!.alignmentId)
    }

    @Test fun attachmentFramesMeasureTwistRelativeToPlacementOnRotatedShips() {
        val a = WandAnchor(BlockPos.ZERO, Direction.EAST, 1)
        val b = WandAnchor(BlockPos.ZERO, Direction.WEST, 2)
        val rotationA = Quaterniond().rotationXYZ(0.3, 0.4, 0.5)
        for (bend in listOf(0.0, 0.7)) {
            val rotationB = Quaterniond(rotationA).rotateY(bend).rotateX(Math.PI / 2)
            val (poseA, poseB) = WandLinkPhysics.gluePoses(a, b, rotationA, rotationB)
            val frameA = Quaterniond(rotationA).mul(poseA.rot)
            val frameB = Quaterniond(rotationB).mul(poseB.rot)
            assertEquals(0.0, SpringJoints.twistAngle(frameA, frameB), 1e-9)
            assertEquals(Math.PI / 3, SpringJoints.twistAngle(frameA, frameB.rotateX(Math.PI / 3)), 1e-9)
        }
    }

    @Test fun largeShipsKeepMassProportionalSpringResponse() {
        val pose = VSJointPose(Vector3d(), Quaterniond())
        val mass = SpringJoints.effectiveMass(2_000_000.0, 2_000_000.0)
        assertEquals(1_000_000.0, mass)
        assertEquals(2_000_000.0, SpringJoints.effectiveMass(null, 2_000_000.0))
        val small = SpringJoints.create(1, 2, pose, pose, 3.0, 1000.0).first
        val large = SpringJoints.create(1, 2, pose, pose, 3.0, mass).first
        assertEquals(small.stiffness / 1000.0, large.stiffness / mass)
        assertEquals(small.damping!! / 1000.0, large.damping!! / mass)
    }

    @Test fun aSmallFixedAnchorDoesNotWeakenTheSpringSupportingAHeavyLoad() {
        val heavy = 100_000.0
        assertEquals(heavy, SpringJoints.effectiveMass(100.0, heavy, fixedA = true))
        assertEquals(heavy, SpringJoints.effectiveMass(heavy, 100.0, fixedB = true))
        assertTrue(SpringJoints.effectiveMass(100.0, heavy) < heavy / 100)
    }

    @Test fun alignmentResistsTwistWithoutLockingMotion() {
        val pose = VSJointPose(Vector3d(), Quaterniond())
        val (spring, alignment) = SpringJoints.create(1, 2, pose, pose, 3.0, 500.0)
        assertEquals(3f, spring.restLength)
        assertTrue(spring.stiffness > 0f)
        assertTrue(spring.damping!! > 0f)
        assertEquals(setOf(VSD6Joint.D6Drive.TWIST), alignment.drives!!.keys)
        val twist = alignment.drives!![VSD6Joint.D6Drive.TWIST]!!
        assertTrue(twist.driveStiffness > 0f)
        assertTrue(twist.driveDamping > 0f)
        assertTrue(twist.driveForceLimit > 0f)
        assertTrue(alignment.motions!!.values.all { it == VSD6Joint.D6Motion.FREE })
        assertFalse(SpringJoints.overloaded(1.0, -100.0))
        assertFalse(SpringJoints.overloaded(-1.0, 100.0))
        assertTrue(SpringJoints.overloaded(1.0, 320.0))
        assertTrue(SpringJoints.overloaded(-1.0, -320.0))
    }
}
