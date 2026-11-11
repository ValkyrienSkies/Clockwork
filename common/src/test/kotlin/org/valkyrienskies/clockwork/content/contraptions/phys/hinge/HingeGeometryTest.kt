package org.valkyrienskies.clockwork.content.contraptions.phys.hinge

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.core.internal.joints.VSD6Joint
import org.valkyrienskies.core.internal.joints.VSJointPose
import java.util.UUID

class HingeGeometryTest {
    @Test fun everyMountingOrientationHasAPerpendicularSignedPinAxis() {
        val frames = mutableSetOf<Pair<Direction, Direction>>()
        for (facing in Direction.values()) for (roll in 0..3) {
            val q = HingeGeometry.orientation(facing, roll)
            val normal = q.transform(Vector3d(0.0, 1.0, 0.0))
            val axis = q.transform(Vector3d(1.0, 0.0, 0.0))
            assertEquals(facing.stepX.toDouble(), normal.x, 1e-9)
            assertEquals(facing.stepY.toDouble(), normal.y, 1e-9)
            assertEquals(facing.stepZ.toDouble(), normal.z, 1e-9)
            assertEquals(0.0, normal.dot(axis), 1e-9)
            frames.add(facing to Direction.getNearest(axis.x, axis.y, axis.z))
            assertEquals(35.0, HingeGeometry.angle(q, Quaterniond(q).rotateX(Math.toRadians(35.0))), 1e-8)
            assertEquals(-65.0, HingeGeometry.angle(q, Quaterniond(q).rotateX(Math.toRadians(-65.0))), 1e-8)
        }
        assertEquals(24, frames.size)
    }

    @Test fun limitsRestrictOnlyTwistAndPermitMeasurableStrain() {
        val pose = VSJointPose(Vector3d(), Quaterniond())
        val joint = HingeGeometry.joint(1, 2, pose, pose, -35, 115)
        for (axis in VSD6Joint.D6Axis.values()) assertEquals(
            if (axis == VSD6Joint.D6Axis.TWIST) VSD6Joint.D6Motion.LIMITED else VSD6Joint.D6Motion.LOCKED, joint.motions!![axis])
        assertEquals(Math.toRadians(-35.0), joint.twistLimit!!.lowerLimit.toDouble(), 1e-6)
        assertEquals(Math.toRadians(115.0), joint.twistLimit!!.upperLimit.toDouble(), 1e-6)
        assertEquals(HingeGeometry.COMPLIANCE, joint.compliance)
        assertFalse(joint.shouldBeSerialized)
        val locked = HingeGeometry.joint(null, 2, pose, pose, 0, 0)
        assertTrue(locked.motions!!.values.all { it == VSD6Joint.D6Motion.LOCKED })
        assertNull(locked.twistLimit)
    }

    @Test fun savedHingesKeepTheirLimitsAndBrokenStateWithoutReusingRuntimeJointIds() {
        val data = HingeLinks()
        val link = HingeLinks.Link(UUID.randomUUID(), WandAnchor(BlockPos(1, 2, 3), Direction.UP),
            WandAnchor(BlockPos(100, 20, 300), Direction.UP, 55), -30, 120, false)
        link.jointId = 400; link.pending = true
        data.links[link.id] = link
        val loaded = HingeLinks.load(data.save(CompoundTag())).links.getValue(link.id)
        assertEquals(link.a, loaded.a); assertEquals(link.b, loaded.b)
        assertEquals(-30, loaded.lower); assertEquals(120, loaded.upper); assertFalse(loaded.connected)
        assertNull(loaded.jointId); assertFalse(loaded.pending)
    }
}
