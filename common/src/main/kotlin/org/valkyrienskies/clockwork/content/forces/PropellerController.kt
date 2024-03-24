package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import net.minecraft.util.Mth
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropCreateData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropUpdateData
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.core.impl.game.ships.ShipInertiaDataImpl
import java.lang.Math
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.BiConsumer
import kotlin.math.sign

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PropellerController : ShipForcesInducer {

    private val propellorPhysData: HashMap<Int, PropData> = HashMap<Int, PropData>()
    private val propellorUpdatePhysData: ConcurrentHashMap<Int, PropUpdateData> =
        ConcurrentHashMap<Int, PropUpdateData>()
    private val createdProps: ConcurrentLinkedQueue<Pair<Int, PropCreateData>> =
        ConcurrentLinkedQueue<Pair<Int, PropCreateData>>()
    private val removedProps = ConcurrentLinkedQueue<Int>()
    private var nextPropID = 0

    override fun applyForces(physShip: PhysShip) {
        while (!createdProps.isEmpty()) {
            val createData: Pair<Int, PropCreateData> = createdProps.remove()
            val propInertiaData = ShipInertiaDataImpl.newEmptyShipInertiaData()
            for (i in createData.component2().propellorPositions) {
                propInertiaData.onSetBlock(i.x(), i.y(), i.z(), 0.0, 100.0)
            }
            propellorPhysData[createData.component1()] = PropData(
                createData.component2().bearingPos,
                createData.component2().bearingAxis,
                createData.component2().bearingAngle,
                createData.component2().bearingSpeed,
                Quaterniond(),
                createData.component2().propellorPositions,
                createData.component2().inverted,
                createData.component2().overStressed
            )
        }
        while (!removedProps.isEmpty()) {
            propellorPhysData.remove(removedProps.remove() as Int)
        }
        propellorUpdatePhysData.forEach(
            BiConsumer<Int, PropUpdateData> forEach@{ id: Int, data: PropUpdateData ->
                val physData: PropData = propellorPhysData[id] ?: return@forEach
                physData.bearingAngle = data.rotationAngle
                physData.bearingSpeed = data.rotationSpeed
                physData.inverted = data.inverted
                physData.overStressed = data.overStressed
                physData.bearingRotation = data.bearingRotation
            }
        )
        propellorUpdatePhysData.clear()

        // Propeller Thrust
        val netForce = Vector3d()
        val netTorque = Vector3d()
        for (physData in propellorPhysData.values) {
            if (!physData.overStressed) {
                val forceTorque = computeForce(
                    physShip.transform,
                    physData,
                    (physShip as PhysShipImpl).poseVel.vel,
                    physShip.poseVel.omega,
                    physShip
                )
                netForce.add(forceTorque.component1())
                netTorque.add(forceTorque.component2())
            }
        }
        if (netForce.isFinite && netTorque.isFinite) {
            physShip.applyInvariantForce(netForce)
            physShip.applyInvariantTorque(netTorque)
        }
        // Propeller Pushing
    }

    private fun computeForce(
            physTransform: ShipTransform,
            physProp: PropData,
            vel: Vector3dc,
            omega: Vector3dc,
            physShip: PhysShipImpl
    ): Pair<Vector3dc, Vector3dc> {
        val modifiedSpeed: Double = physProp.bearingSpeed * 1.5 //* 1.25, A little bit easier to generate force //TODO config?
        val bearingVector: Vector3dc = Vector3d(physProp.bearingPos).add(0.5, 0.5, 0.5)
        val axis: Vector3dc = physProp.bearingAxis!!.mul(sign(modifiedSpeed), Vector3d()).rotate(physTransform.shipToWorldRotation)
        val rotation: Quaterniondc = Quaterniond(AxisAngle4d(Math.toRadians(physProp.bearingAngle), axis))
        val angVel: Vector3dc = axis.mul(modifiedSpeed / 60.0 * (2.0 * Math.PI), Vector3d())
        val furthestTip = Vector3d()
        val netForce = Vector3d()
        val netTorque = Vector3d()

        for (pos in physProp.propellorPositions!!) {
            val sailVector: Vector3dc = Vector3d(pos.x().toDouble(), pos.y().toDouble(), pos.z().toDouble())
                .add(bearingVector)
            val diff: Vector3dc = sailVector.sub(bearingVector, Vector3d())
            val rotatedDiff: Vector3dc = rotation.transform(diff, Vector3d())
            val sailVel: Vector3dc = rotatedDiff.cross(angVel, Vector3d())
            if (rotatedDiff.length() > furthestTip.length()) {
                furthestTip.set(rotatedDiff)
            }
            val force = axis.mul(sailVel.length(), Vector3d())
                .mul(5000.0, Vector3d())
            //            Vector3d force2 = force.mul(physProp.bearingSpeed, new Vector3d());
            val sailPosWorld: Vector3dc = physTransform.shipToWorld.transformPosition(sailVector, Vector3d())
            val sailPosRelShip: Vector3dc = sailPosWorld.sub(physTransform.positionInWorld, Vector3d())
            val torque = sailPosRelShip.cross(force, Vector3d())
            val sailPosRelCenterMass: Vector3dc = physTransform.shipToWorld.transformPosition(sailVector, Vector3d())
                .sub(physTransform.positionInWorld, Vector3d())
            val worldVelAtSail: Vector3dc = omega.cross(sailPosRelCenterMass, Vector3d()).add(vel, Vector3d())
            val exhaustVel = exhaustVelocity(rotatedDiff, angVel)
            var factor = 1.0 - Mth.clamp(axis.dot(worldVelAtSail) / exhaustVel, 0.0, 1.0)
            if (!java.lang.Double.isFinite(factor)) {
                factor = 1.0
            }
            val airPressFact = getAirPressureForY(sailPosWorld.y())
            var airPress = 0.0
            if (airPressFact > 0.0) {
                airPress = Mth.clamp(airPressFact / 22632.10, 0.0, 1.0)
            }

            force.mul(airPress * factor)
            torque.mul(airPress * factor)
            netForce.add(force)
            netTorque.add(torque)
        }

//        netTorque.add(conserveMomentum(physShip, physProp, furthestTip, angVel));
        netForce.rotate(physProp.bearingRotation)
        if (physProp.inverted) {
            netForce.mul(-1.0)
        }

        //        System.out.println(netTorque);
        return Pair<Vector3dc, Vector3dc>(netForce, netTorque)
    }

    private fun conserveMomentum(
            physShip: PhysShipImpl,
            physProp: PropData,
            furthestTip: Vector3dc,
            angVel: Vector3dc
    ): Vector3dc {
        var prevAngMomentumRelProp: Vector3dc = Vector3d()
        if (physProp.prevAngularMomentum != null) {
            prevAngMomentumRelProp = physProp.prevAngularMomentum!!
        }
        val propAxis: Vector3dc = Vector3d(physProp.bearingAxis)
        val propSpeed: Double = physProp.bearingSpeed

        // 1/2 * Mass * (Outer Wheel Radius^2 + Total Wheel Radius^2)

        // negative to fix dir
        val rotVel = propSpeed * (2 * Math.PI / 60) * -1
        val angularVelocityPropeller: Vector3dc = Vector3d(propAxis).mul(rotVel)
        val angularMomentumRelProp: Vector3dc =
            angularVelocityPropeller.mul(physShip.inertia.momentOfInertiaTensor, Vector3d())

        // Add to convert from momentum relative to wheel into relative to ship
        val centerOfMassInShip = physShip.transform.positionInShip
        val r: Vector3dc =
            Vector3d(centerOfMassInShip.add(physProp.bearingPos, Vector3d())).sub(physShip.transform.positionInShip)
                .rotate(physShip.transform.shipToWorldRotation)
        val momentumModifier: Vector3dc = Vector3d(physShip.poseVel.omega).cross(r).mul(physShip.inertia.shipMass)
        val angularMomentumRelShip: Vector3dc = Vector3d(angularMomentumRelProp).add(momentumModifier)
        val prevAngularMomentumRelShip: Vector3dc = Vector3d(prevAngMomentumRelProp).add(momentumModifier)
        val torque: Vector3dc = Vector3d(prevAngularMomentumRelShip).sub(angularMomentumRelShip).div(1 / 60.0)
        physProp.prevAngularMomentum = angularMomentumRelProp
        return torque
    }

    //todo add this to AerodynamicUtils once this is merged to Melting Point's branch
    fun getAirPressureForY(y: Double, maxHeight: Double = 563.0): Double {
        val worldScale = 11000.0 / (maxHeight - 63.0)

        val realAltitude = (y - 63.0) * worldScale

        val layer = when {
            realAltitude < 11000 -> 0
            realAltitude < 20000 -> 1
            realAltitude < 32000 -> 2
            realAltitude < 47000 -> 3
            realAltitude < 51000 -> 4
            realAltitude < 71000 -> 5
            else -> 6
        }

        val hb = when (layer) {
            0 -> 0.0
            1 -> 11000.0
            2 -> 20000.0
            3 -> 32000.0
            4 -> 47000.0
            5 -> 51000.0
            6 -> 71000.0
            else -> 0.0
        }

        val pb = when (layer) {
            0 -> 101325.00
            1 -> 22632.10
            2 -> 5747.89
            3 -> 868.02
            4 -> 110.91
            5 -> 66.94
            6 -> 3.96
            else -> 0.0
        }

        val Tb = when (layer) {
            0 -> 288.15
            1 -> 216.65
            2 -> 216.65
            3 -> 228.65
            4 -> 270.65
            5 -> 270.65
            6 -> 214.65
            else -> 0.0
        }

        val g0 = 9.80665 // grav accelerant

        val R = 8.3144598 // universal gas constant

        val M = 0.0289644 // molar mass of air

        val L = when (layer) {
            0 -> 0.0065
            1 -> 0.0
            2 -> -0.001
            3 -> -0.0028
            4 -> 0.0
            5 -> 0.0028
            6 -> 0.002
            else -> 0.0
        }

        return when (L != 0.0) {
            true -> pb * Math.pow(1.0 - (L / Tb) * (realAltitude - hb), ((g0 * M) / (R * L)))
            else -> pb * Math.exp((-g0 * M * (realAltitude - hb)) / (R * Tb))
        }


    }

    //todo replace this call with AerodynamicUtils once this is merged to Melting Point's branch
    fun getAirDensityForY(y: Double, maxHeight: Double): Double {
        val worldScale = 11000.0 / (maxHeight - 63.0)

        val realAltitude = (y - 63.0) * worldScale

        val layer = when {
            realAltitude < 11000 -> 0
            realAltitude < 20000 -> 1
            realAltitude < 32000 -> 2
            realAltitude < 47000 -> 3
            realAltitude < 51000 -> 4
            realAltitude < 71000 -> 5
            else -> 6
        }

        val hb = when (layer) {
            0 -> 0.0
            1 -> 11000.0
            2 -> 20000.0
            3 -> 32000.0
            4 -> 47000.0
            5 -> 51000.0
            6 -> 71000.0
            else -> 0.0
        }

        val pb = when (layer) {
            0 -> 1.225
            1 -> 0.36391
            2 -> 0.08803
            3 -> 0.01322
            4 -> 0.00143
            5 -> 0.00086
            6 -> 0.000064
            else -> 0.0
        }

        val Tb = when (layer) {
            0 -> 288.15
            1 -> 216.65
            2 -> 216.65
            3 -> 228.65
            4 -> 270.65
            5 -> 270.65
            6 -> 214.65
            else -> 0.0
        }

        val g0 = 9.80665

        val R = 8.3144598

        val M = 0.0289644

        val L = when (layer) {
            0 -> 0.0065
            1 -> 0.0
            2 -> -0.001
            3 -> -0.0028
            4 -> 0.0
            5 -> 0.0028
            6 -> 0.002
            else -> 0.0
        }

        return when (L != 0.0) {
            true -> pb * Math.pow((Tb - (realAltitude - hb) * L) / Tb, ((g0 * M) / (R * L) -1.0))
            else -> pb * Math.exp((-g0 * M * (realAltitude - hb)) / (R * Tb))
        }
    }

    private fun exhaustVelocity(posRelBearing: Vector3dc, omega: Vector3dc): Double {
        return Math.min(posRelBearing.cross(omega, Vector3d()).length() * 15, 40.0)
    }

    fun addPropeller(data: PropCreateData): Int {
        val id = nextPropID++
        createdProps.add(Pair<Int, PropCreateData>(id, data))
        return id
    }

    fun removePropeller(id: Int) {
        removedProps.add(id)
    }

    fun updatePropeller(id: Int, data: PropUpdateData) {
        propellorUpdatePhysData[id] = data
    }

    override fun equals(other: Any?): Boolean {
        // self check
        return if (this === other) {
            true
        } else if (other !is PropellerController) {
            false
        } else {
            (propellorPhysData == other.propellorPhysData && propellorUpdatePhysData == other.propellorUpdatePhysData && areQueuesEqual<Pair<Int, PropCreateData>>(
                createdProps,
                other.createdProps
            ) && areQueuesEqual<Int>(removedProps, other.removedProps) && nextPropID == other.nextPropID)
        }
    }

    companion object {
        fun getOrCreate(ship: ServerShip): PropellerController? {
            if (ship.getAttachment(PropellerController::class.java) == null) {
                ship.saveAttachment(PropellerController::class.java, PropellerController())
            }
            return ship.getAttachment(PropellerController::class.java)
        }

        inline fun <reified T> areQueuesEqual(left: Queue<T>, right: Queue<T>): Boolean {
            return left.toTypedArray().contentEquals(right.toTypedArray())
        }
    }
}
