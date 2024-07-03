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
            }
        )
        propellorUpdatePhysData.clear()

        // Propeller Thrust
        val netForce = Vector3d()
        val netTorque = Vector3d()
        for (physData in propellorPhysData.values) {
            if(!physData.overStressed) {
                val forceTorque = computeForce(
                    physShip.transform, physData, (physShip).velocity, physShip.omega, physShip
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
        physShip: PhysShip
    ): Pair<Vector3dc, Vector3dc> {
        val modifiedSpeed: Double = physProp.bearingSpeed * 1.5 //* 1.25, A little bit easier to generate force //TODO config?
        val bearingVector: Vector3dc = Vector3d(physProp.bearingPos).add(0.5, 0.5, 0.5)
        val axis: Vector3dc = physProp.bearingAxis!!.mul(sign(modifiedSpeed), Vector3d())
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
            val force = physTransform.shipToWorldRotation.transform(axis.mul(sailVel.length(), Vector3d()))
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
            val airPress = airPressure(sailPosWorld)
            force.mul(airPress * factor)
            torque.mul(airPress * factor)
            netForce.add(force)
            netTorque.add(torque)
        }

//        netTorque.add(conserveMomentum(physShip, physProp, furthestTip, angVel));
        if (physProp.inverted) {
            netForce.mul(-1.0)
        }
        //        System.out.println(netTorque);
        return Pair<Vector3dc, Vector3dc>(netForce, netTorque)
    }

    private fun conserveMomentum(
        physShip: PhysShip,
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
            angularVelocityPropeller.mul(physShip.momentOfInertia, Vector3d())

        // Add to convert from momentum relative to wheel into relative to ship
        val centerOfMassInShip = physShip.transform.positionInShip
        val r: Vector3dc =
            Vector3d(centerOfMassInShip.add(physProp.bearingPos, Vector3d())).sub(physShip.transform.positionInShip)
                .rotate(physShip.transform.shipToWorldRotation)
        val momentumModifier: Vector3dc = Vector3d(physShip.omega).cross(r).mul(physShip.mass)
        val angularMomentumRelShip: Vector3dc = Vector3d(angularMomentumRelProp).add(momentumModifier)
        val prevAngularMomentumRelShip: Vector3dc = Vector3d(prevAngMomentumRelProp).add(momentumModifier)
        val torque: Vector3dc = Vector3d(prevAngularMomentumRelShip).sub(angularMomentumRelShip).div(1 / 60.0)
        physProp.prevAngularMomentum = angularMomentumRelProp
        return torque
    }

    private fun airPressure(pos: Vector3dc): Double {
        val offset = Math.exp(-(320.0 - 64.0) / 192.0)
        val height = pos.y()
        val airPress = (Math.exp(-(height - 64.0) / 192) - offset) / (1.0 - offset)
        return if (java.lang.Double.isFinite(airPress)) {
            Mth.clamp(airPress, 0.0, 1.0)
        } else {
            0.0
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
