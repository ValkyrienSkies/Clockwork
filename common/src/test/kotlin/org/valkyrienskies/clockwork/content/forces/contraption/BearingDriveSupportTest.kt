package org.valkyrienskies.clockwork.content.forces.contraption

import io.mockk.*
import org.joml.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.api_impl.config.ConfigPhysicsBackendType
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.core.internal.joints.*
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import java.util.EnumMap

class BearingDriveSupportTest {
    private val ships = HashMap<Long, PhysShip>()
    private val joints = HashMap<Int, VSJoint>()
    private val level = mockk<VsiPhysLevel> {
        every { getShipById(any()) } answers { ships[firstArg()] }
        every { getJointById(any()) } answers { joints[firstArg()] }
        every { getJointsFromShip(any()) } answers {
            val id = firstArg<Long>()
            joints.filterValues { it.shipId0 == id || it.shipId1 == id }.keys
        }
        every { updateJoint(any(), any()) } answers { joints[firstArg()] = secondArg(); true }
    }

    private fun ship(id: Long, mass: Double, position: Vector3d, rotation: Quaterniond = Quaterniond(), fixed: Boolean = false): PhysShip {
        val transform = mockk<ShipTransform> {
            every { shipToWorldRotation } returns rotation
            every { positionInWorld } returns position
            every { shipToWorld } returns Matrix4d().translationRotate(position, rotation)
        }
        return mockk<PhysShip>(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.mass } returns mass
            every { isStatic } returns fixed
            every { this@mockk.transform } returns transform
            every { momentOfInertia } returns Matrix3d().scale(mass / 6.0)
            every { angularVelocity } returns Vector3d()
        }.also { ships[id] = it }
    }

    private fun hinge(id: Int, parent: Long?, child: Long, anchor: Vector3d): VSRevoluteJoint {
        val poseA = parent?.let { Vector3d(anchor).sub(ships[it]!!.transform.positionInWorld) } ?: anchor
        val poseB = Vector3d(anchor).sub(ships[child]!!.transform.positionInWorld)
        return VSRevoluteJoint(parent, VSJointPose(poseA, Quaterniond()), child, VSJointPose(poseB, Quaterniond()))
            .also { joints[id] = it }
    }

    private fun inertia(id: Int) = BearingDriveSupport.effectiveInertia(level, id, joints[id]!!, Vector3d(1.0, 0.0, 0.0))

    @Test fun lightLinkIncludesPayloadAndUpstreamWorldAnchor() {
        ship(1, 1.0, Vector3d(0.0, 0.0, 0.5))
        ship(2, 100.0, Vector3d(0.0, 0.0, 1.5))
        hinge(10, null, 1, Vector3d())
        hinge(11, 1, 2, Vector3d(0.0, 0.0, 1.0))
        assertEquals(1.0 / 6.0 + 0.25 + 100.0 * (1.0 / 6.0 + 2.25), inertia(10), 1e-9)
        assertEquals(100.0 * (1.0 / 6.0 + 0.25), inertia(11), 1e-9)
        joints.remove(11)
        assertEquals(1.0 / 6.0 + 0.25, inertia(10), 1e-9)
    }

    @Test fun gimbalEdgesAndReversedEndpointsCarryTheSameLoad() {
        ship(1, 1.0, Vector3d(0.0, 0.0, 0.5))
        ship(2, 100.0, Vector3d(0.0, 0.0, 1.5))
        hinge(10, null, 1, Vector3d())
        val h = hinge(11, 1, 2, Vector3d(0.0, 0.0, 1.0))
        val before = inertia(10)
        val motions = EnumMap<VSD6Joint.D6Axis, VSD6Joint.D6Motion>(VSD6Joint.D6Axis::class.java)
        VSD6Joint.D6Axis.entries.forEach { motions[it] = VSD6Joint.D6Motion.LOCKED }
        motions[VSD6Joint.D6Axis.SWING1] = VSD6Joint.D6Motion.LIMITED
        motions[VSD6Joint.D6Axis.SWING2] = VSD6Joint.D6Motion.LIMITED
        joints[11] = VSD6Joint(h.shipId1, h.pose1, h.shipId0, h.pose0, motions = motions)
        assertEquals(before, inertia(10), 1e-9)
        assertEquals(100.0 * (1.0 / 6.0 + 0.25), inertia(11), 1e-9)
        motions[VSD6Joint.D6Axis.Z] = VSD6Joint.D6Motion.FREE
        assertEquals(1.0 / 6.0 + 0.25, inertia(10), 1e-9)
    }

    @Test fun floatingAssemblyUsesBothSidesWithoutDoubleCountingLoops() {
        ship(1, 2.0, Vector3d())
        ship(2, 4.0, Vector3d())
        ship(3, 8.0, Vector3d())
        hinge(10, 1, 2, Vector3d())
        hinge(11, 2, 3, Vector3d())
        assertEquals(1.0 / (6.0 / 2.0 + 6.0 / 12.0), inertia(10), 1e-9)
        hinge(12, 3, 1, Vector3d())
        assertEquals(1.0 / (6.0 / 2.0 + 6.0 / 4.0), inertia(10), 1e-9)
    }

    @Test fun angularInertiaUsesWorldAxisAndParallelAxisOffset() {
        val rotation = Quaterniond().rotateZ(Math.PI / 2.0)
        val body = ship(1, 3.0, Vector3d(0.0, 0.0, 2.0), rotation)
        every { body.momentOfInertia } returns Matrix3d().scaling(1.0, 2.0, 3.0)
        hinge(10, null, 1, Vector3d())
        assertEquals(14.0, inertia(10), 1e-9)
        assertEquals(0.0, BearingDriveSupport.effectiveInertia(level, 10, joints[10]!!, Vector3d()))
    }

    @Test fun physBearingCreatesImplicitDriveForWholeConnectedLoad() {
        val sub = ship(1, 1.0, Vector3d(0.0, 0.0, 0.5))
        ship(2, 100.0, Vector3d(0.0, 0.0, 1.5))
        val h = hinge(10, null, 1, Vector3d())
        hinge(11, 1, 2, Vector3d(0.0, 0.0, 1.0))
        h.shouldBeSerialized = true
        val data = BearingData(Vector3d(1.0, 0.0, 0.0), angularSpeed = 0.2f,
            subPos = Vector3d(h.pose1.pos), jointId = 10)
        val controller = BearingController()
        controller.bearingData[0] = data
        controller.physTick(sub, level)
        val drive = joints[10] as VSD6Joint
        assertTrue(drive.shouldBeSerialized)
        assertTrue(drive.drives!![VSD6Joint.D6Drive.TWIST]!!.driveDamping > 1000f)
        assertTrue(drive.driveVelocity!!.angular.x() < 0f)
        verify(exactly = 0) { sub.applyWorldTorque(any()) }
        data.angularSpeed = 0f
        controller.physTick(sub, level)
        assertEquals(0f, (joints[10] as VSD6Joint).drives!![VSD6Joint.D6Drive.TWIST]!!.driveDamping)
    }

    @Test fun failedJointUpdateDoesNotAlsoApplyExplicitTorque() {
        val sub = ship(1, 1.0, Vector3d())
        hinge(10, null, 1, Vector3d())
        every { level.updateJoint(any(), any()) } returns false
        val controller = BearingController()
        controller.bearingData[0] = BearingData(Vector3d(1.0, 0.0, 0.0), angularSpeed = 1f, jointId = 10)
        controller.physTick(sub, level)
        verify(exactly = 0) { sub.applyWorldTorque(any()) }
    }

    @Test fun lockedAndAligningModesKeepTheirTargetsGainsAndTorqueCap() {
        val sub = ship(1, 12.0, Vector3d())
        hinge(10, null, 1, Vector3d())
        val controller = BearingController()
        val data = BearingData(Vector3d(1.0, 0.0, 0.0), bearingAngle = 0.3, locked = true, jointId = 10)
        controller.bearingData[0] = data
        controller.physTick(sub, level)
        val joint = joints[10] as VSD6Joint
        val drive = joint.drives!![VSD6Joint.D6Drive.TWIST]!!
        assertEquals((2.0 * ClockworkConfig.SERVER.angleFollowingAngleErrorMultiplier).toFloat(), drive.driveStiffness)
        assertEquals((2.0 * ClockworkConfig.SERVER.angleFollowingOmegaErrorMultiplier).toFloat(), drive.driveDamping)
        val cap = ClockworkConfig.SERVER.angleFollowingMaxTorque
        assertEquals(if (cap > 0) cap.toFloat() else Float.POSITIVE_INFINITY, drive.driveForceLimit)
        val angle = 0.3 - Math.toRadians(ClockworkConfig.SERVER.angleFollowingAngleDeadbandDeg)
        assertEquals(1.0, kotlin.math.abs(joint.drivePosition!!.pose.rot.dot(Quaterniond().rotateX(angle))), 1e-12)
        data.aligning = true
        controller.physTick(sub, level)
        assertEquals(1.0, (joints[10] as VSD6Joint).drivePosition!!.pose.rot.w(), 1e-12)
        verify(exactly = 0) { sub.applyWorldTorque(any()) }
    }

    @Test fun staleJointIdIsRecoveredFromMatchingBearingAnchors() {
        val sub = ship(1, 1.0, Vector3d())
        ship(2, 2.0, Vector3d())
        hinge(10, null, 1, Vector3d())
        val unrelated = hinge(99, null, 2, Vector3d())
        val data = BearingData(Vector3d(1.0, 0.0, 0.0), angularSpeed = 1f, jointId = 99)
        val controller = BearingController()
        controller.bearingData[0] = data
        controller.physTick(sub, level)
        assertEquals(10, data.jointId)
        assertSame(unrelated, joints[99])
        assertTrue(joints[10] is VSD6Joint)
        verify(exactly = 0) { sub.applyWorldTorque(any()) }
    }

    @Test fun classicBackendKeepsExplicitBearingTorque() {
        val previousBackend = VSCoreConfig.SERVER.physics.physicsBackend
        try {
            VSCoreConfig.SERVER.physics.physicsBackend = ConfigPhysicsBackendType.KRUNCH_CLASSIC
            val sub = ship(1, 1.0, Vector3d())
            val original = hinge(10, null, 1, Vector3d())
            val controller = BearingController()
            controller.bearingData[0] = BearingData(Vector3d(1.0, 0.0, 0.0), angularSpeed = 1f, jointId = 10)
            controller.physTick(sub, level)
            assertSame(original, joints[10])
            verify(exactly = 1) { sub.applyWorldTorque(any()) }
        } finally {
            VSCoreConfig.SERVER.physics.physicsBackend = previousBackend
        }
    }

    @Test fun gimbalTargetMatchesRelativeJointFramesForEveryFacing() {
        val host = Quaterniond().rotateXYZ(0.3, -0.4, 0.2)
        val target = Quaterniond().rotateXYZ(-0.1, 0.15, 0.3)
        for (axis in listOf(Vector3d(1.0,0.0,0.0), Vector3d(0.0,1.0,0.0), Vector3d(0.0,0.0,1.0))) {
            for (sign in listOf(-1.0, 1.0)) {
                val frame = Quaterniond().rotationTo(Vector3d(1.0,0.0,0.0), Vector3d(axis).mul(sign))
                val joint = VSD6Joint(1, VSJointPose(Vector3d(), frame), 2, VSJointPose(Vector3d(), frame))
                val relative = BearingDriveSupport.gimbalTarget(joint, host, target)
                val predictedHostFrame = Quaterniond(target).mul(frame).mul(relative)
                assertEquals(1.0, kotlin.math.abs(predictedHostFrame.dot(Quaterniond(host).mul(frame))), 1e-12)
            }
        }
    }

    @Test fun clearingDriveRetainsJointAndSerialization() {
        val drives = EnumMap<VSD6Joint.D6Drive,VSD6Joint.D6JointDrive>(VSD6Joint.D6Drive::class.java)
        drives[VSD6Joint.D6Drive.SWING] = VSD6Joint.D6JointDrive(10f, 5f, 20f)
        val joint = VSD6Joint(1, VSJointPose(Vector3d(), Quaterniond()), null,
            VSJointPose(Vector3d(), Quaterniond()), drives = drives)
        joint.shouldBeSerialized = true
        joints[10] = joint
        assertTrue(BearingDriveSupport.clearDrive(level, 10))
        val cleared = joints[10] as VSD6Joint
        assertNull(cleared.drives)
        assertTrue(cleared.shouldBeSerialized)
        assertEquals(joint.pose0, cleared.pose0)
    }
}
