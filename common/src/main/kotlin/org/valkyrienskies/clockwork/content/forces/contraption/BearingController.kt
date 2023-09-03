package org.valkyrienskies.clockwork.content.forces.contraption

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import org.joml.Matrix3dc
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingCreateData
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingData
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingUpdateData
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.api.ShipForcesInducer
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.jvm.functions.Function1

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class BearingController : ShipForcesInducer {
    val bearingData = HashMap<Int, PhysBearingData>()

    @JsonIgnore
    private val bearingUpdateData = ConcurrentHashMap<Int, PhysBearingUpdateData>()

    @JsonIgnore
    private val createdBearings = ConcurrentLinkedQueue<Pair<Int, PhysBearingCreateData>>()
    private val removedBearings = ConcurrentLinkedQueue<Int>()
    private var nextBearingID = 0
    override fun applyForces(physShip: PhysShip) {
        // Do nothing, actual work is in applyForcesAndLookupPhysShips()
    }

    override fun applyForcesAndLookupPhysShips(physShip: PhysShip, lookupPhysShip: (ShipId) -> PhysShip?) {
        while (!createdBearings.isEmpty()) {
            val createData = createdBearings.remove()
            bearingData[createData.component1()] = PhysBearingData(
                createData.component2().bearingPos,
                createData.component2().bearingAxis,
                createData.component2().bearingAngle,
                createData.component2().bearingRPM,
                createData.component2().locked,
                createData.component2().shiptraptionID,
                createData.component2().constraint,
                createData.component2().hingeConstraint,
                createData.component2().posDampConstraint,
                createData.component2().rotDampConstraint
            )
        }
        while (!removedBearings.isEmpty()) {
            bearingData.remove(removedBearings.remove() as Int)
        }
        bearingUpdateData.forEach { (id: Int, data: PhysBearingUpdateData) ->
            val physData = bearingData[id] ?: return@forEach
            physData.bearingAngle = data.bearingAngle
            physData.bearingRPM = data.bearingRPM
            physData.locked = data.locked
        }
        bearingUpdateData.clear()
        for (data in bearingData.values) {
            if (data.angleConstraint == null) {
                val physShipBearingIsOnId = data.hingeConstraint!!.shipId1
                if (physShipBearingIsOnId == PhysBearingBlockEntity.NO_SHIPTRAPTION_ID) {
                    // Constraint connects to world
                    val torque = computeRotationalForce(data, physShip as PhysShipImpl, null)
                    physShip.applyInvariantTorque(torque)
                } else {
                    val physShipBearingIsOn = lookupPhysShip.invoke(physShipBearingIsOnId)
                    if (physShipBearingIsOn == null) {
                        val torque = computeRotationalForce(data, physShip as PhysShipImpl, null)
                        physShip.applyInvariantTorque(torque)
                    } else {
                        val torque =
                            computeRotationalForce(data, physShip as PhysShipImpl, physShipBearingIsOn as PhysShipImpl?)
                        physShip.applyInvariantTorque(torque)
                        physShipBearingIsOn.applyInvariantTorque(torque.mul(-1.0, Vector3d()))
                    }
                }
            }
        }
    }

    //    private Vector3dc computeRotationalForce(PhysBearingData data, PhysShipImpl physShip) {
    //        double mass = physShip.getInertia().getShipMass();
    //
    //        Vector3dc actualOmega = physShip.getPoseVel().getOmega();
    //        Vector3d idealOmega;
    //        if (data.bearingAxis != null) {
    //            idealOmega = data.bearingAxis.mul(data.bearingRPM, new Vector3d()).mul((2*Math.PI)/60);
    //        } else {
    //            idealOmega = new Vector3d();
    //        }
    //
    //
    //        Vector3dc torque = idealOmega.sub(actualOmega, new Vector3d()).mul(mass * 10);
    //
    //        return torque;
    //    }
    private fun computeRotationalForce(
        data: PhysBearingData,
        physShip: PhysShipImpl,
        otherPhysShip: PhysShipImpl?
    ): Vector3dc {
        val torque: Vector3dc
        torque = if (data.locked) {
            computeLockedRotationalForce(data, physShip, otherPhysShip)
        } else {
            computeUnlockedRotationalForce(data, physShip, otherPhysShip)
        }
        return torque
    }

    private fun getAngularInertia(physShip: PhysShipImpl, localPos: Vector3dc, axisGlobal: Vector3dc): Double {
        val globalPos: Vector3dc = physShip.transform.shipToWorld.transformPosition(localPos, Vector3d())
        val offset: Vector3dc = globalPos.sub(physShip.poseVel.pos, Vector3d())
        return getAngularInertia(
            physShip.inertia.momentOfInertiaTensor,
            physShip.transform.shipToWorldRotation,
            physShip.inertia.shipMass,
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
        physShip: PhysShipImpl,
        otherPhysShip: PhysShipImpl?
    ): Vector3dc {
        if (data.bearingAxis == null) {
            return Vector3d()
        }
        val bearingAxisInGlobal = Vector3d(data.bearingAxis)
        otherPhysShip?.transform?.shipToWorldRotation?.transform(bearingAxisInGlobal)
        val idealRelativeOmega = bearingAxisInGlobal.mul(data.bearingRPM.toDouble(), Vector3d()).mul(2 * Math.PI / 60)
        val actualRelativeOmega: Vector3d
        actualRelativeOmega = if (!physShip.isStatic) {
            Vector3d(physShip.poseVel.omega)
        } else {
            // TODO: What about static bodies that are moving?
            Vector3d()
        }
        val torqueMassMultiplier: Double
        if (!physShip.isStatic) {
            if (otherPhysShip != null && !otherPhysShip.isStatic) {
                val physShipInertia =
                    getAngularInertia(physShip, data.attachConstraint!!.localPos0, bearingAxisInGlobal)
                val otherShipInertia =
                    getAngularInertia(otherPhysShip, data.attachConstraint!!.localPos1, bearingAxisInGlobal)
                torqueMassMultiplier = parallelOperator(physShipInertia, otherShipInertia)
                // Sub otherPhysShip angularVel from actualRelativeOmega if otherPhysShip is not static
                // TODO: What about static bodies that are moving?
                actualRelativeOmega.sub(otherPhysShip.poseVel.omega)
            } else {
                torqueMassMultiplier =
                    getAngularInertia(physShip, data.attachConstraint!!.localPos0, bearingAxisInGlobal)
            }
        } else if (otherPhysShip != null && !otherPhysShip.isStatic) {
            // Set it to be the inertia of otherPhysShip
            torqueMassMultiplier =
                getAngularInertia(otherPhysShip, data.attachConstraint!!.localPos1, bearingAxisInGlobal)
            // Sub otherPhysShip angularVel from actualRelativeOmega if otherPhysShip is not static
            // TODO: What about static bodies that are moving?
            actualRelativeOmega.sub(otherPhysShip.poseVel.omega)
        } else {
            return Vector3d()
        }
        var bearingAxisAfterRot: Vector3dc = data.bearingAxis.rotate(physShip.poseVel.rot, Vector3d())
        if (otherPhysShip != null) {
            bearingAxisAfterRot = otherPhysShip.poseVel.rot.transformInverse(bearingAxisAfterRot, Vector3d())
        }

        // If we are more than 5 degrees out of alignment, then don't apply any torque
        if (bearingAxisAfterRot.angleCos(data.bearingAxis) < 0.9961947 && bearingAxisAfterRot.angleCos(data.bearingAxis) > -0.9961947) {
            return Vector3d()
        }
        val angularVelError: Vector3dc = idealRelativeOmega.sub(actualRelativeOmega, Vector3d())
        val angularVelErrorAlongBearingAxis: Vector3dc =
            bearingAxisInGlobal.mul(bearingAxisInGlobal.dot(angularVelError), Vector3d())
        // Only apply torque on the bearing axis
        return angularVelErrorAlongBearingAxis.mul(torqueMassMultiplier * 10.0, Vector3d())
    }

    private fun computeLockedRotationalForce(
        data: PhysBearingData,
        physShip: PhysShipImpl,
        otherPhysShip: PhysShipImpl?
    ): Vector3dc {
        if (data.bearingAxis == null) {
            return Vector3d()
        }
        val bearingAxisInGlobal = Vector3d(data.bearingAxis)
        otherPhysShip?.transform?.shipToWorldRotation?.transform(bearingAxisInGlobal)
        val actualRelativeOmega: Vector3d
        actualRelativeOmega = if (!physShip.isStatic) {
            Vector3d(physShip.poseVel.omega)
        } else {
            // TODO: What about static bodies that are moving?
            Vector3d()
        }
        val torqueMassMultiplier: Double
        if (!physShip.isStatic) {
            if (otherPhysShip != null && !otherPhysShip.isStatic) {
                val physShipInertia =
                    getAngularInertia(physShip, data.attachConstraint!!.localPos0, bearingAxisInGlobal)
                val otherShipInertia =
                    getAngularInertia(otherPhysShip, data.attachConstraint!!.localPos1, bearingAxisInGlobal)
                torqueMassMultiplier = parallelOperator(physShipInertia, otherShipInertia)
                // Sub otherPhysShip angularVel from actualRelativeOmega if otherPhysShip is not static
                // TODO: What about static bodies that are moving?
                actualRelativeOmega.sub(otherPhysShip.poseVel.omega)
            } else {
                torqueMassMultiplier =
                    getAngularInertia(physShip, data.attachConstraint!!.localPos0, bearingAxisInGlobal)
            }
        } else if (otherPhysShip != null && !otherPhysShip.isStatic) {
            // Set it to be the inertia of otherPhysShip
            torqueMassMultiplier =
                getAngularInertia(otherPhysShip, data.attachConstraint!!.localPos1, bearingAxisInGlobal)
            // Sub otherPhysShip angularVel from actualRelativeOmega if otherPhysShip is not static
            // TODO: What about static bodies that are moving?
            actualRelativeOmega.sub(otherPhysShip.poseVel.omega)
        } else {
            return Vector3d()
        }


        // Only apply torque on the bearing axis


        //Proportional
        val perpendicularAxis: Vector3dc
        perpendicularAxis = if (Math.abs(data.bearingAxis.x()) == 1.0) {
            Vector3d(0.0, 1.0, 0.0)
        } else if (Math.abs(data.bearingAxis.y()) == 1.0) {
            Vector3d(1.0, 0.0, 0.0)
        } else if (Math.abs(data.bearingAxis.z()) == 1.0) {
            Vector3d(0.0, 1.0, 0.0)
        } else {
            throw RuntimeException("how the fuck did you mess this up g")
        }
        var bearingAxisAfterRot: Vector3dc = data.bearingAxis.rotate(physShip.poseVel.rot, Vector3d())
        var perpAfterRot: Vector3dc = perpendicularAxis.rotate(physShip.poseVel.rot, Vector3d())
        if (otherPhysShip != null) {
            perpAfterRot = otherPhysShip.poseVel.rot.transformInverse(perpAfterRot, Vector3d())
            bearingAxisAfterRot = otherPhysShip.poseVel.rot.transformInverse(bearingAxisAfterRot, Vector3d())
        }

        // If we are more than 5 degrees out of alignment, then don't apply any torque
        if (bearingAxisAfterRot.angleCos(data.bearingAxis) < 0.9961947 && bearingAxisAfterRot.angleCos(data.bearingAxis) > -0.9961947) {
            return Vector3d()
        }
        val perpAfterRotInPlane: Vector3dc =
            perpAfterRot.sub(data.bearingAxis.mul(data.bearingAxis.dot(perpAfterRot), Vector3d()), Vector3d())
        val angleBTShipInRadians = perpAfterRotInPlane.angle(perpendicularAxis)
        val crossOfYourMother: Vector3dc = perpAfterRotInPlane.cross(perpendicularAxis, Vector3d())
        val angleWRespectToBearingAxis: Double
        angleWRespectToBearingAxis = if (crossOfYourMother.lengthSquared() > 1e-12) {
            angleBTShipInRadians * Math.signum(crossOfYourMother.dot(data.bearingAxis)) * -1
            // bro what do you expect me to do :sus:
        } else {
            0.0
        }
        var angleErr = Math.toRadians(data.bearingAngle) - angleWRespectToBearingAxis
        while (angleErr > Math.PI) {
            angleErr -= 2 * Math.PI
        }
        while (angleErr < -Math.PI) {
            angleErr += 2 * Math.PI
        }

        //Derivative
        val relativeOmegaInPhysShip: Vector3dc =
            physShip.transform.worldToShip.transformDirection(actualRelativeOmega, Vector3d())
        val relativeOmegaInPhysShipParallelBearingAxis = data.bearingAxis.dot(relativeOmegaInPhysShip)
        val omegaErr = data.bearingRPM * (2 * Math.PI / 60) - relativeOmegaInPhysShipParallelBearingAxis
        val torque = angleErr * torqueMassMultiplier * 50.0 + omegaErr * torqueMassMultiplier * 50.0

//        return angularVelErrorAlongBearingAxis.mul(torqueMassMultiplier * 10.0, new Vector3d());
        return bearingAxisInGlobal.mul(torque, Vector3d())
    }

    fun addPhysBearing(data: PhysBearingCreateData): Int {
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
            (bearingData == other.bearingData && bearingUpdateData == other.bearingUpdateData
                    && areQueuesEqual(createdBearings, other.createdBearings)
                    && areQueuesEqual(removedBearings, other.removedBearings)
                    && nextBearingID == other.nextBearingID)
        }
    }

    fun canDisassemble(): Boolean {
        return false
    }

    fun setAligning(yn: Boolean, id: Int) {
        bearingData[id]!!.setAligning(yn)
    }

    companion object {
        fun getOrCreate(ship: ServerShip): BearingController? {
            if (ship.getAttachment(BearingController::class.java) == null) {
                ship.saveAttachment(BearingController::class.java, BearingController())
            }
            return ship.getAttachment(BearingController::class.java)
        }

        inline fun <reified T> areQueuesEqual(left: Queue<T>, right: Queue<T>): Boolean {
            return left.toTypedArray().contentEquals(right.toTypedArray())
        }
    }
}