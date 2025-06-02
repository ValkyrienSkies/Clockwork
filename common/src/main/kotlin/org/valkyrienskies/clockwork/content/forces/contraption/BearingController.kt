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
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.sign

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class BearingController : ShipForcesInducer {
    val bearingData = HashMap<Int, PhysBearingData>()

    @JsonIgnore
    private val bearingUpdateData = ConcurrentHashMap<Int, PhysBearingUpdateData>()

    @JsonIgnore
    private val createdBearings = ConcurrentLinkedQueue<Pair<Int, PhysBearingData>>()
    private val removedBearings = ConcurrentLinkedQueue<Int>()
    private var nextBearingID = 0
    override fun applyForces(physShip: PhysShip) {
        // Do nothing, actual work is in applyForcesAndLookupPhysShips()
    }

    //attachment from subship moves iteslf
    override fun applyForcesAndLookupPhysShips(physShip: PhysShip, lookupPhysShip: (ShipId) -> PhysShip?) {
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
            if (!data.angleFollowing) {continue}
            val physShipBearingIsOnId = data.mainShipId
            if (physShipBearingIsOnId == PhysBearingBlockEntity.NO_SHIPTRAPTION_ID) {
                // Constraint connects to world
                val torque = computeRotationalForce(data, physShip, null)
                physShip.applyInvariantTorque(torque)
                continue
            }
            val physShipBearingIsOn = lookupPhysShip.invoke(physShipBearingIsOnId)
            if (physShipBearingIsOn == null) {
                val torque = computeRotationalForce(data, physShip, null)
                physShip.applyInvariantTorque(torque)
            } else {
                val torque = computeRotationalForce(data, physShip, physShipBearingIsOn)
                physShip.applyInvariantTorque(torque)
                physShipBearingIsOn.applyInvariantTorque(torque.mul(-1.0, Vector3d()))
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
        data.actualAngle = getAngle(data, physShip.transform, otherPhysShip?.transform)
        if (data.aligning) {
            data.angularSpeed = abs(prevRPM) * if (data.actualAngle > 0) -1 else 1
            data.bearingAngle = 0.0
            data.angleFollowing = true
        }
        val torque = computeLockedRotationalForce(data, physShip, otherPhysShip)
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

        // Only apply torque on the bearing axis
        // Proportional
        val perpendicularAxis = when {
            abs(data.bearingAxis.x()) == 1.0 -> Vector3d(0.0, 1.0, 0.0)
            abs(data.bearingAxis.y()) == 1.0 -> Vector3d(1.0, 0.0, 0.0)
            abs(data.bearingAxis.z()) == 1.0 -> Vector3d(0.0, 1.0, 0.0)
            else -> throw RuntimeException("incorrect data")
        }

        var bearingAxisAfterRot: Vector3dc = data.bearingAxis.rotate(subShip.transform.shipToWorldRotation, Vector3d())
        var perpAfterRot: Vector3dc = perpendicularAxis.rotate(subShip.transform.shipToWorldRotation, Vector3d())
        if (mainShip != null) {
            perpAfterRot = mainShip.transform.shipToWorldRotation.transformInverse(perpAfterRot, Vector3d())
            bearingAxisAfterRot = mainShip.transform.shipToWorldRotation.transformInverse(bearingAxisAfterRot, Vector3d())
        }

        // If we are more than 5 degrees out of alignment, then don't apply any torque
        if (bearingAxisAfterRot.angleCos(data.bearingAxis) < 0.9961947 && bearingAxisAfterRot.angleCos(data.bearingAxis) > -0.9961947) {
            return Vector3d()
        }
        val perpAfterRotInPlane: Vector3dc = perpAfterRot.sub(data.bearingAxis.mul(data.bearingAxis.dot(perpAfterRot), Vector3d()), Vector3d())
        val angleBTShipInRadians = perpAfterRotInPlane.angle(perpendicularAxis)
        val crossOfYourMother: Vector3dc = perpAfterRotInPlane.cross(perpendicularAxis, Vector3d())
        val angleWRespectToBearingAxis: Double = if (crossOfYourMother.lengthSquared() > 0.0) {
            angleBTShipInRadians * sign(crossOfYourMother.dot(data.bearingAxis)) * -1
            // bro what do you expect me to do :sus:
        } else {
            0.0
        }
        var angleErr = data.bearingAngle - angleWRespectToBearingAxis
        while (angleErr > Math.PI) {
            angleErr -= 2 * Math.PI
        }
        while (angleErr < -Math.PI) {
            angleErr += 2 * Math.PI
        }

        //rpm makes angleErr correct faster, but it should also correct if no rpm is present
        angleErr = angleErr * (abs(data.angularSpeed) + ClockworkConfig.SERVER.angleFollowingBaseAngleErrorMultiplier)

        // Derivative
        val relativeOmegaInPhysShip: Vector3dc = subShip.transform.worldToShip.transformDirection(actualRelativeOmega, Vector3d())
        val relativeOmegaInPhysShipParallelBearingAxis = data.bearingAxis.dot(relativeOmegaInPhysShip)
        val omegaErr = -relativeOmegaInPhysShipParallelBearingAxis

        val torque = angleErr * torqueMassMultiplier * ClockworkConfig.SERVER.angleFollowingAngleErrorMultiplier
                   + omegaErr * torqueMassMultiplier * ClockworkConfig.SERVER.angleFollowingOmegaErrorMultiplier
        return bearingAxisInGlobal.mul(torque, Vector3d())
    }

    fun addPhysBearing(data: PhysBearingData): Int {
        val id = nextBearingID++
        createdBearings.add(Pair(id, data))
        return id
    }

    fun removePhysBearing(id: Int) {
        removedBearings.add(id)
    }

    fun updatePhysBearing(id: Int, data: PhysBearingUpdateData) {
        bearingUpdateData[id] = data
    }

    override fun equals(other: Any?): Boolean {
        // self check
        return if (this === other) {
            true
        } else if (other !is BearingController) {
            false
        } else {
            (bearingData == other.bearingData && bearingUpdateData == other.bearingUpdateData && areQueuesEqual(
                createdBearings,
                other.createdBearings
            ) && areQueuesEqual(removedBearings, other.removedBearings) && nextBearingID == other.nextBearingID)
        }
    }

    //TODO stupid code duplication
    private fun getAngle(data: PhysBearingData, physShipTransform: ShipTransform, otherPhysShipTransform: ShipTransform?): Double {
        val bearingAxisInGlobal = Vector3d(data.bearingAxis)
        otherPhysShipTransform?.shipToWorldRotation?.transform(bearingAxisInGlobal)

        // Only apply torque on the bearing axis
        // Proportional
        val perpendicularAxis: Vector3dc = if (abs(data.bearingAxis!!.x()) == 1.0) {
            Vector3d(0.0, 1.0, 0.0)
        } else if (abs(data.bearingAxis.y()) == 1.0) {
            Vector3d(1.0, 0.0, 0.0)
        } else if (abs(data.bearingAxis.z()) == 1.0) {
            Vector3d(0.0, 1.0, 0.0)
        } else {
            throw RuntimeException("how the fuck did you mess this up g")
        }
        var perpAfterRot: Vector3dc = perpendicularAxis.rotate(physShipTransform.shipToWorldRotation, Vector3d())
        perpAfterRot = otherPhysShipTransform?.shipToWorldRotation?.transformInverse(perpAfterRot, Vector3d()) ?: perpAfterRot

        val perpAfterRotInPlane: Vector3dc = perpAfterRot.sub(data.bearingAxis.mul(data.bearingAxis.dot(perpAfterRot), Vector3d()), Vector3d())
        val angleBTShipInRadians = perpAfterRotInPlane.angle(perpendicularAxis)
        val crossOfYourMother: Vector3dc = perpAfterRotInPlane.cross(perpendicularAxis, Vector3d())
        val angleWRespectToBearingAxis: Double = if (crossOfYourMother.lengthSquared() > 1e-12) {
            angleBTShipInRadians * sign(crossOfYourMother.dot(data.bearingAxis)) * -1
            // bro what do you expect me to do :sus:
        } else {
            0.0
        }
        return angleWRespectToBearingAxis
    }

    //tolerance is in degrees
    fun canDisassemble(id: Int?, mainShip: ServerShip, otherShip: ServerShip?, tolerance: Int=5): Boolean {
        val item = bearingData[id] ?: return false
        if (abs(Math.toDegrees(getAngle(item, mainShip.transform, otherShip?.transform))) > tolerance) return false
        return true
    }

    fun setAligning(yn: Boolean, id: Int) {
        bearingData[id]?.aligning = yn
    }

    companion object {
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
