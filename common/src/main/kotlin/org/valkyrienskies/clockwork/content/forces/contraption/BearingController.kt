package org.valkyrienskies.clockwork.content.forces.contraption

import com.fasterxml.jackson.annotation.JsonAutoDetect
import net.minecraft.core.BlockPos
import org.joml.Matrix3dc
import org.joml.Quaterniondc
import org.joml.Quaterniond
import org.joml.Vector3f
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.util.minus
import org.valkyrienskies.clockwork.util.times
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.core.internal.joints.VSD6Joint
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint
import org.valkyrienskies.core.internal.joints.VSJointPose
import java.lang.Float
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

data class BearingData(
    val bearingAxis: Vector3dc? = null,
    var bearingAngle: Double = 0.0,
    var angularSpeed: kotlin.Float = 0f,
    var locked: Boolean = false,
    var kinematic: Boolean = false,
    var aligning: Boolean = false,

    var mainShipId: Long = -1,
    var mainPos: Vector3d = Vector3d(),
    var subPos: Vector3d = Vector3d(),
    var actualAngle: Double = 0.0,
    var lastLockedTorque: Double = 0.0,
    var jointId: Int = -1,
)

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class BearingController : ShipPhysicsListener {
    val bearingData = HashMap<Long, BearingData>()


    //attachment from subship moves iteslf
    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        for (data in bearingData.values) {
            // Locked bearings use a fixed joint with a target rotation, they don't need any torque
            if (data.kinematic) continue

            if (updateDrive(data, physShip, physLevel)) continue

            val physShipBearingIsOnId = data.mainShipId
            if (physShipBearingIsOnId == -1L) {
                // Constraint connects to world
                val torque = computeRotationalForce(data, physShip, null)
                physShip.applyWorldTorque(torque)
                continue
            }
            val physShipBearingIsOn = physLevel.getShipById(physShipBearingIsOnId)
            if (physShipBearingIsOn == null) {
                val torque = computeRotationalForce(data, physShip, null)
                physShip.applyWorldTorque(torque)
            } else {
                val torque = computeRotationalForce(data, physShip, physShipBearingIsOn)
                physShip.applyWorldTorque(torque)
                physShipBearingIsOn.applyWorldTorque(torque.mul(-1.0, Vector3d()))
            }
        }
    }

    private fun updateDrive(data: BearingData, subShip: PhysShip, level: PhysLevel): Boolean {
        val physics = level as? VsiPhysLevel ?: return false
        val axis = data.bearingAxis ?: return false
        val mainShip = physics.getShipById(data.mainShipId)
        val current = physics.getJointById(data.jointId)?.takeIf {
            it.shipId1 == subShip.id && it.shipId0 == mainShip?.id &&
                it.pose0.pos.distanceSquared(data.mainPos) < 1e-12 &&
                it.pose1.pos.distanceSquared(data.subPos) < 1e-12
        } ?: physics.getJointsFromShip(subShip.id)
            .firstNotNullOfOrNull { id ->
                physics.getJointById(id)?.takeIf {
                    it.shipId1 == subShip.id && it.shipId0 == mainShip?.id &&
                        it.pose0.pos.distanceSquared(data.mainPos) < 1e-12 &&
                        it.pose1.pos.distanceSquared(data.subPos) < 1e-12
                }?.also { data.jointId = id }
            } ?: return false
        if (current.shipId1 != subShip.id || current.shipId0 != mainShip?.id) return false
        if (current !is VSRevoluteJoint && current !is VSD6Joint) return false
        if (!BearingDriveSupport.supportsAngularDrives())
            return !BearingDriveSupport.clearDrive(physics, data.jointId)
        val locked = data.locked || data.aligning
        if (locked && ClockworkConfig.SERVER.angleFollowingMaxTorqueStep > 0.0)
            return !BearingDriveSupport.clearDrive(physics, data.jointId)
        val worldAxis = Vector3d(axis)
        mainShip?.transform?.shipToWorldRotation?.transform(worldAxis)
        val inertia = BearingDriveSupport.effectiveInertia(physics, data.jointId, current, worldAxis)
        if (inertia <= 0.0) return !BearingDriveSupport.clearDrive(physics, data.jointId)
        data.actualAngle = getAngle(axis, subShip.transform, mainShip?.transform)
        if (!data.actualAngle.isFinite() || !data.bearingAngle.isFinite() || !data.angularSpeed.isFinite()) {
            BearingDriveSupport.clearDrive(physics, data.jointId)
            return true
        }
        val stiffness: Double
        val damping: Double
        val cap: Double
        var targetAngle = 0.0
        var targetVelocity = 0.0
        if (locked) {
            val error = shortestAngleDeltaRadians(if (data.aligning) 0.0 else data.bearingAngle, data.actualAngle)
            val deadband = Math.toRadians(ClockworkConfig.SERVER.angleFollowingAngleDeadbandDeg)
            targetAngle = data.actualAngle + sign(error) * max(0.0, abs(error) - deadband)
            stiffness = inertia * ClockworkConfig.SERVER.angleFollowingAngleErrorMultiplier
            damping = inertia * ClockworkConfig.SERVER.angleFollowingOmegaErrorMultiplier
            cap = ClockworkConfig.SERVER.angleFollowingMaxTorque.toDouble()
        } else {
            stiffness = 0.0
            val resistance = if (abs(data.angularSpeed) > 0.001)
                ClockworkConfig.SERVER.unlockedModeRotationResistanceMultiplier else 0.0
            if (resistance <= 0.0 && abs(data.angularSpeed) > 0.001)
                return !BearingDriveSupport.clearDrive(physics, data.jointId)
            damping = inertia * ClockworkConfig.SERVER.unlockedModeOmegaErrorMultiplier * resistance
            targetVelocity = if (resistance > 0.0) -data.angularSpeed / resistance else 0.0
            val configured = ClockworkConfig.SERVER.unlockedModeMaxTorque
            cap = if (configured > 0.0) max(configured,
                inertia * ClockworkConfig.SERVER.unlockedModeMinAngularAcceleration) else 0.0
        }
        val motions = EnumMap<VSD6Joint.D6Axis, VSD6Joint.D6Motion>(VSD6Joint.D6Axis::class.java)
        VSD6Joint.D6Axis.entries.forEach { motions[it] = VSD6Joint.D6Motion.LOCKED }
        motions[VSD6Joint.D6Axis.TWIST] = VSD6Joint.D6Motion.FREE
        val drives = EnumMap<VSD6Joint.D6Drive, VSD6Joint.D6JointDrive>(VSD6Joint.D6Drive::class.java)
        drives[VSD6Joint.D6Drive.TWIST] = VSD6Joint.D6JointDrive(
            BearingDriveSupport.finiteGain(stiffness), BearingDriveSupport.finiteGain(damping),
            if (cap > 0.0) BearingDriveSupport.finiteGain(cap) else Float.POSITIVE_INFINITY)
        val updated = VSD6Joint(current.shipId0, current.pose0, current.shipId1, current.pose1,
            current.maxForceTorque, current.compliance, motions = motions, drives = drives,
            drivePosition = VSD6Joint.DrivePosition(VSJointPose(Vector3d(), Quaterniond().rotateX(targetAngle))),
            driveVelocity = VSD6Joint.DriveVelocity(Vector3f(), Vector3f(targetVelocity.toFloat(), 0f, 0f)))
        updated.shouldBeSerialized = current.shouldBeSerialized
        if (current != updated) physics.updateJoint(data.jointId, updated)
        return true
    }

    private fun computeRotationalForce(
        data: BearingData,
        physShip: PhysShip,
        otherPhysShip: PhysShip?
    ): Vector3dc {
        val prevRPM = data.angularSpeed
        val prevAngle = data.bearingAngle
        data.actualAngle = getAngle(data.bearingAxis!!, physShip.transform, otherPhysShip?.transform)
        if (!java.lang.Double.isFinite(data.actualAngle) || !java.lang.Double.isFinite(data.bearingAngle) || !Float.isFinite(data.angularSpeed)) {
            return Vector3d()
        }
        if (data.aligning) {
            data.angularSpeed = abs(prevRPM) * if (data.actualAngle > 0) -1 else 1
            data.bearingAngle = 0.0
        }
        val torque = if (data.aligning || data.locked) {
            computeLockedRotationalForce(data, physShip, otherPhysShip)
        } else {
            computeUnlockedRotationalForce(data, physShip, otherPhysShip)
        }

        data.angularSpeed = prevRPM
        data.bearingAngle = prevAngle
        return torque
    }

    private fun getAngularInertia(physShip: PhysShip, localPos: Vector3dc, axisGlobal: Vector3dc): Double {
        val globalPos: Vector3dc = physShip.transform.shipToWorld.transformPosition(localPos, Vector3d())
        val offset: Vector3dc = globalPos.sub(physShip.transform.positionInWorld, Vector3d())
        return getAngularInertia(
            physShip.momentOfInertia,
            physShip.transform.shipToWorldRotation,
            physShip.mass,
            offset,
            axisGlobal
        )
    }

    private fun getAngularInertia(
        inertiaTensorLocal: Matrix3dc,
        rotation: Quaterniondc,
        mass: Double,
        offsetGlobal: Vector3dc,
        axisGlobal: Vector3dc
    ): Double {
        val offsetPerpToAxis: Vector3dc =
            offsetGlobal.sub(axisGlobal.mul(axisGlobal.dot(offsetGlobal), Vector3d()), Vector3d())
        val axisLocal: Vector3dc = rotation.transformInverse(axisGlobal, Vector3d())
        return inertiaTensorLocal.transform(axisLocal, Vector3d())
            .dot(axisLocal) + offsetPerpToAxis.lengthSquared() * mass
    }

    private fun parallelOperator(left: Double, right: Double): Double {
        return 1.0 / (1.0 / left + 1.0 / right)
    }

    private fun computeUnlockedRotationalForce(
        data: BearingData,
        subShip: PhysShip,
        mainShip: PhysShip?
    ): Vector3dc {
        if (data.bearingAxis == null) {
            return Vector3d()
        }
        val bearingAxisInGlobal = Vector3d(data.bearingAxis)
        mainShip?.transform?.shipToWorldRotation?.transform(bearingAxisInGlobal)
        val idealRelativeOmega = bearingAxisInGlobal.mul(-data.angularSpeed.toDouble(), Vector3d())
        val actualRelativeOmega = if (!subShip.isStatic) {
            Vector3d(subShip.angularVelocity)
        } else {
            // TODO: What about static bodies that are moving?
            Vector3d()
        }
        val torqueMassMultiplier: Double
        if (!subShip.isStatic) {
            if (mainShip != null && !mainShip.isStatic) {
                val physShipInertia  = getAngularInertia(subShip,  data.subPos,  bearingAxisInGlobal)
                val otherShipInertia = getAngularInertia(mainShip, data.mainPos, bearingAxisInGlobal)
                torqueMassMultiplier = parallelOperator(physShipInertia, otherShipInertia)
                // Sub mainShip angularVel from actualRelativeOmega if mainShip is not static
                // TODO: What about static bodies that are moving?
                actualRelativeOmega.sub(mainShip.angularVelocity)
            } else {
                torqueMassMultiplier = getAngularInertia(subShip, data.subPos, bearingAxisInGlobal)
            }
        } else if (mainShip != null && !mainShip.isStatic) {
            // Set it to be the inertia of otherPhysShip
            torqueMassMultiplier = getAngularInertia(mainShip, data.mainPos, bearingAxisInGlobal)
            // Sub mainShip angularVel from actualRelativeOmega if mainShip is not static
            // TODO: What about static bodies that are moving?
            actualRelativeOmega.sub(mainShip.angularVelocity)
        } else {
            return Vector3d()
        }
        var bearingAxisAfterRot: Vector3dc = data.bearingAxis.rotate(subShip.transform.shipToWorldRotation, Vector3d())
        if (mainShip != null) {
            bearingAxisAfterRot = mainShip.transform.shipToWorldRotation.transformInverse(bearingAxisAfterRot, Vector3d())
        }

        // If we are more than 5 degrees out of alignment, then don't apply any torque
        if (bearingAxisAfterRot.angleCos(data.bearingAxis) < 0.9961947 && bearingAxisAfterRot.angleCos(data.bearingAxis) > -0.9961947) {
            return Vector3d()
        }
        val angularVelError = idealRelativeOmega - actualRelativeOmega * if (abs(data.angularSpeed) > 0.001) ClockworkConfig.SERVER.unlockedModeRotationResistanceMultiplier else 0.0
        val angularVelErrorAlongBearingAxis: Vector3dc = bearingAxisInGlobal.mul(bearingAxisInGlobal.dot(angularVelError), Vector3d())
        // Only apply torque on the bearing axis
        val torque = angularVelErrorAlongBearingAxis.mul(
            torqueMassMultiplier * ClockworkConfig.SERVER.unlockedModeOmegaErrorMultiplier,
            Vector3d()
        )
        if (!torque.isFinite()) {
            return Vector3d()
        }
        val configuredMaxTorque = ClockworkConfig.SERVER.unlockedModeMaxTorque
        val inertiaScaledMinTorque = torqueMassMultiplier * ClockworkConfig.SERVER.unlockedModeMinAngularAcceleration
        val effectiveMaxTorque = if (configuredMaxTorque > 0.0) max(configuredMaxTorque, inertiaScaledMinTorque) else 0.0

        if (effectiveMaxTorque > 0.0 && torque.lengthSquared() > effectiveMaxTorque * effectiveMaxTorque) {
            torque.normalize(effectiveMaxTorque)
        }
        return torque
    }

    private fun computeLockedRotationalForce(
        data: BearingData,
        subShip: PhysShip,
        mainShip: PhysShip?
    ): Vector3dc {
        if (data.bearingAxis == null) {
            return Vector3d()
        }
        val bearingAxisInGlobal = Vector3d(data.bearingAxis)
        mainShip?.transform?.shipToWorldRotation?.transform(bearingAxisInGlobal)
        val actualRelativeOmega = if (!subShip.isStatic) {
            Vector3d(subShip.angularVelocity)
        } else {
            Vector3d()
        }
        val torqueMassMultiplier: Double
        if (!subShip.isStatic) {
            if (mainShip != null && !mainShip.isStatic) {
                val physShipInertia = getAngularInertia(subShip, data.subPos, bearingAxisInGlobal)
                val otherShipInertia = getAngularInertia(mainShip, data.mainPos, bearingAxisInGlobal)
                torqueMassMultiplier = parallelOperator(physShipInertia, otherShipInertia)
                actualRelativeOmega.sub(mainShip.angularVelocity)
            } else {
                torqueMassMultiplier = getAngularInertia(subShip, data.subPos, bearingAxisInGlobal)
            }
        } else if (mainShip != null && !mainShip.isStatic) {
            torqueMassMultiplier = getAngularInertia(mainShip, data.mainPos, bearingAxisInGlobal)
            actualRelativeOmega.sub(mainShip.angularVelocity)
        } else {
            return Vector3d()
        }

        val rawAngleErr = shortestAngleDeltaRadians(data.bearingAngle, data.actualAngle)
        val deadbandRad = Math.toRadians(ClockworkConfig.SERVER.angleFollowingAngleDeadbandDeg)
        val angleErr = when {
            rawAngleErr > deadbandRad -> rawAngleErr - deadbandRad
            rawAngleErr < -deadbandRad -> rawAngleErr + deadbandRad
            else -> 0.0
        }
        val omegaErr = -bearingAxisInGlobal.dot(actualRelativeOmega)
        var torque = (
                angleErr * torqueMassMultiplier * ClockworkConfig.SERVER.angleFollowingAngleErrorMultiplier +
                        omegaErr * torqueMassMultiplier * ClockworkConfig.SERVER.angleFollowingOmegaErrorMultiplier
                )
        if (!java.lang.Double.isFinite(torque)) {
            return Vector3d()
        }

        val maxTorque = ClockworkConfig.SERVER.angleFollowingMaxTorque.toDouble()
        if (maxTorque > 0.0) {
            torque = torque.coerceIn(-maxTorque, maxTorque)
        }

        val maxTorqueStep = ClockworkConfig.SERVER.angleFollowingMaxTorqueStep
        if (maxTorqueStep > 0.0) {
            val minTorque = data.lastLockedTorque - maxTorqueStep
            val maxTorqueForStep = data.lastLockedTorque + maxTorqueStep
            torque = torque.coerceIn(minTorque, maxTorqueForStep)
        }
        data.lastLockedTorque = torque

        return bearingAxisInGlobal.mul(torque, Vector3d())
    }

    private fun Vector3dc.isFinite(): Boolean {
        return java.lang.Double.isFinite(x()) && java.lang.Double.isFinite(y()) && java.lang.Double.isFinite(z())
    }

    private fun shortestAngleDeltaRadians(target: Double, actual: Double): Double {
        var delta = target - actual
        while (delta > Math.PI) {
            delta -= Math.PI * 2.0
        }
        while (delta < -Math.PI) {
            delta += Math.PI * 2.0
        }
        return delta
    }

    fun setData(bearingKey: BlockPos, data: BearingData) {
        bearingData[bearingKey.asLong()] = data
    }

    fun removeData(bearingKey: BlockPos) {
        bearingData.remove(bearingKey.asLong())
    }

    companion object {
        @JvmStatic
        fun getAngle(bearingAxis: Vector3dc, physShipTransform: ShipTransform, otherPhysShipTransform: ShipTransform?): Double {
            val bearingAxisInGlobal = bearingAxis.get(Vector3d())
            otherPhysShipTransform?.shipToWorldRotation?.transform(bearingAxisInGlobal)

            // Only apply torque on the bearing axis
            // Proportional
            val perpendicularAxis = when {
                abs(bearingAxis.x()) == 1.0 -> Vector3d(0.0, 1.0, 0.0)
                abs(bearingAxis.y()) == 1.0 -> Vector3d(1.0, 0.0, 0.0)
                abs(bearingAxis.z()) == 1.0 -> Vector3d(0.0, 1.0, 0.0)
                else -> throw RuntimeException("incorrect data")
            }
            var perpAfterRot: Vector3dc = perpendicularAxis.rotate(physShipTransform.shipToWorldRotation, Vector3d())
            perpAfterRot = otherPhysShipTransform?.shipToWorldRotation?.transformInverse(perpAfterRot, Vector3d()) ?: perpAfterRot

            val perpAfterRotInPlane: Vector3dc = perpAfterRot.sub(bearingAxis.mul(bearingAxis.dot(perpAfterRot), Vector3d()), Vector3d())
            val angleBTShipInRadians = perpAfterRotInPlane.angle(perpendicularAxis)
            val crossOfYourMother: Vector3dc = perpAfterRotInPlane.cross(perpendicularAxis, Vector3d())
            val angleWRespectToBearingAxis: Double = if (crossOfYourMother.lengthSquared() > 1e-12) {
                angleBTShipInRadians * sign(crossOfYourMother.dot(bearingAxis)) * -1
                // bro what do you expect me to do :sus:
            } else {
                0.0
            }
            return angleWRespectToBearingAxis
        }

        @OptIn(VsBeta::class)
        fun getOrCreate(ship: LoadedServerShip): BearingController {
            if (ship.getAttachment(BearingController::class.java) == null) {
                ship.setAttachment(BearingController())
            }
            return ship.getAttachment(BearingController::class.java)!!
        }

        inline fun <reified T> areQueuesEqual(left: Queue<T>, right: Queue<T>): Boolean {
            return left.toTypedArray().contentEquals(right.toTypedArray())
        }
    }
}
