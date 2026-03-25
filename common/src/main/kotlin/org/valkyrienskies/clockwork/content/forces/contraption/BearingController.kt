package org.valkyrienskies.clockwork.content.forces.contraption

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import org.joml.Matrix3dc
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingData
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingUpdateData
import org.valkyrienskies.clockwork.util.minus
import org.valkyrienskies.clockwork.util.times
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.api.world.PhysLevel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class BearingController : ShipPhysicsListener {
    val bearingData = HashMap<Int, PhysBearingData>()

    @JsonIgnore
    private val bearingUpdateData = ConcurrentHashMap<Int, PhysBearingUpdateData>()

    @JsonIgnore
    private val createdBearings = ConcurrentLinkedQueue<Pair<Int, PhysBearingData>>()
    private val removedBearings = ConcurrentLinkedQueue<Int>()
    private var nextBearingID = 0

    //attachment from subship moves iteslf
    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        while (!createdBearings.isEmpty()) { createdBearings.remove().also { (id, data) -> bearingData[id] = data } }
        while (!removedBearings.isEmpty()) { bearingData.remove(removedBearings.remove()) }
        bearingUpdateData.forEach { (id: Int, data: PhysBearingUpdateData) ->
            val physData = bearingData[id] ?: return@forEach
            physData.bearingAngle = data.bearingAngle
            physData.angularSpeed = data.bearingRPM
            physData.angleFollowing = data.locked
        }
        bearingUpdateData.clear()
        for (data in bearingData.values) {
            if (data.angleFollowing) continue
            val physShipBearingIsOnId = data.mainShipId
            if (physShipBearingIsOnId == PhysBearingBlockEntity.NO_SHIPTRAPTION_ID) {
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

    private fun computeRotationalForce(
        data: PhysBearingData,
        physShip: PhysShip,
        otherPhysShip: PhysShip?
    ): Vector3dc {
        val prevRPM = data.angularSpeed
        val prevAngle = data.bearingAngle
        data.actualAngle = getAngle(data.bearingAxis!!, physShip.transform, otherPhysShip?.transform)
        if (!java.lang.Double.isFinite(data.actualAngle) || !java.lang.Double.isFinite(data.bearingAngle) || !java.lang.Float.isFinite(data.angularSpeed)) {
            return Vector3d()
        }
        if (data.aligning) {
            data.angularSpeed = abs(prevRPM) * if (data.actualAngle > 0) -1 else 1
            data.bearingAngle = 0.0
            data.angleFollowing = true
        }
        val torque = if (data.angleFollowing) {
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
        data: PhysBearingData,
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
        data: PhysBearingData,
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

        val maxTorque = ClockworkConfig.SERVER.angleFollowingMaxTorque
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

    fun addPhysBearing(data: PhysBearingData): Int {
        val id = nextBearingID++
        createdBearings.add(Pair(id, data))
        return id
    }

    fun removePhysBearing(id: Int) { removedBearings.add(id) }
    fun updatePhysBearing(id: Int, data: PhysBearingUpdateData) { bearingUpdateData[id] = data }

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
        fun getOrCreate(ship: LoadedServerShip): BearingController? {
            if (ship.getAttachment(BearingController::class.java) == null) {
                ship.setAttachment(BearingController())
            }
            return ship.getAttachment(BearingController::class.java)
        }

        inline fun <reified T> areQueuesEqual(left: Queue<T>, right: Queue<T>): Boolean {
            return left.toTypedArray().contentEquals(right.toTypedArray())
        }
    }
}
