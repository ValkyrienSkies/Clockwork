package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import net.minecraft.util.Mth
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropCreateData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropUpdateData
import org.valkyrienskies.clockwork.util.AerodynamicUtils
import org.valkyrienskies.clockwork.util.AerodynamicUtils.getAirDensityForY
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
import kotlin.collections.HashMap
import kotlin.math.*

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PropellerController(
    override val appliers: HashMap<Int, PropData> = HashMap(),
    override val applierUpdateData: ConcurrentLinkedQueue<Pair<Int, PropUpdateData>> = ConcurrentLinkedQueue(),
    override val createdAppliers: ConcurrentLinkedQueue<Pair<Int, PropCreateData>> = ConcurrentLinkedQueue(),
    override val removedAppliers: ConcurrentLinkedQueue<Int> = ConcurrentLinkedQueue(),
    override var nextApplierID: Int = 0
) : MultiInstanceForceApplier<PropUpdateData, PropData, PropCreateData> {

    var ticksSinceLastUpdate = 0

    override fun applyForces(physShip: PhysShip) {
        if (applierUpdateData.isNotEmpty()) ticksSinceLastUpdate = 0
        super.applyForces(physShip)

        // Propeller Thrust
        val netForce = Vector3d()
        val netTorque = Vector3d()
        for (physData in appliers.values) {
            if(physData.active) {
                val (force, torque) = if (physData.brass) {
                    computeForce(
                        physShip.transform, physData, (physShip).velocity, physShip.omega, physShip
                    )
                } else {
                    computeBladeForce(physShip, physData)
                }
                netForce.add(force)
                netTorque.add(torque)
            }
        }
        if (netForce.isFinite && netTorque.isFinite) {
            physShip.applyInvariantForce(netForce)
            physShip.applyInvariantTorque(netTorque)
        }
        // Propeller Pushing
        ticksSinceLastUpdate++
    }

    //todo: redo this entire piece of shit
    private fun computeForce(
        physTransform: ShipTransform,
        physProp: PropData,
        vel: Vector3dc,
        omega: Vector3dc,
        physShip: PhysShip
    ): Pair<Vector3dc, Vector3dc> {
        val estAngle = (physProp.bearingAngle + (physProp.bearingSpeed / 3.0 * ticksSinceLastUpdate.toDouble())) % 360.0

        val modifiedSpeed: Double = (physProp.bearingSpeed * 10.0 / 3.0) * if (physProp.inverted) -1.0 else 1.0 //* 1.25, A little bit easier to generate force //TODO config?
        val bearingVector: Vector3dc = Vector3d(physProp.position).add(0.5, 0.5, 0.5)
        val axis: Vector3dc = physProp.bearingAxis!!.mul(sign(modifiedSpeed), Vector3d()).normalize()
        val rotation: Quaterniondc = Quaterniond(AxisAngle4d(Math.toRadians(estAngle), axis))
        val angVel: Vector3dc = axis.mul(modifiedSpeed / 60.0 * (2.0 * Math.PI), Vector3d())
        val furthestTip = Vector3d()
        val worldAxis = physShip.transform.shipToWorld.transformDirection(axis, Vector3d()).normalize(Vector3d())
        val axialVelocity = physShip.velocity.dot(worldAxis)
        val pretendPitch = 12.0
        val netForce = Vector3d()
        val netTorque = Vector3d()

        for (pos in physProp.sailPositions!!) {
            val sailVector: Vector3dc = Vector3d(pos).add(bearingVector)
            val diff: Vector3dc = Vector3d(pos)
            val offsetFalloff = Vector3d(physProp.bearingAxis).dot(diff)
            if (offsetFalloff > 4.0) {
                continue
            }
            val rotatedDiff: Vector3dc = rotation.transform(diff, Vector3d())
            val sailVel: Vector3dc = rotatedDiff.cross(angVel, Vector3d())
            val sailPosWorld: Vector3dc = physTransform.shipToWorld.transformPosition(sailVector, Vector3d())
            val sailPosRelShip: Vector3dc = sailVector.sub(physTransform.positionInShip, Vector3d())
            if (rotatedDiff.length() > furthestTip.length()) {
                furthestTip.set(rotatedDiff)
            }
            val inflowAngle = atan(axialVelocity / sailVel.length())
            val optimalAngleOfAttack = 4.0
            val optimalPitch = inflowAngle + optimalAngleOfAttack
            physProp.currentBladePitch = if (physProp.brass) {
                Mth.lerp(0.05, physProp.currentBladePitch, optimalPitch)
            } else {
                pretendPitch
            }
            val angleOfAttack = physProp.currentBladePitch - inflowAngle
            val thrustCoefficient = (angleOfAttack * cos(inflowAngle)) - (0.1 * sin(inflowAngle))

            val q = 0.5 * getAirDensityForY(sailPosWorld.y(), 563.0) * ((axialVelocity).pow(2.0) + (sailVel.length()).pow(2.0))

            val thrust = q * thrustCoefficient

            val force = worldAxis.mul(thrust, Vector3d())
            //            Vector3d force2 = force.mul(physProp.bearingSpeed, new Vector3d());
            val torque = sailPosRelShip.cross(force, Vector3d())

            force.mul(5000.0)

            if (offsetFalloff > 0.0001) force.div(offsetFalloff)
            if (offsetFalloff > 0.0001) torque.div(offsetFalloff)
            if (force.isFinite) netForce.add(force)
            if (torque.isFinite) netTorque.add(torque)
        }

        netTorque.add(conserveMomentum(physShip, physProp, furthestTip, angVel))
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
        if (angVel.length().absoluteValue < 0.0001) {
            //return to epically dodge numerical instability at extremely small speeds
            return Vector3d()
        }
        val propAxis: Vector3dc = Vector3d(physProp.bearingAxis)
        val propSpeed: Double = physProp.bearingSpeed

        // 1/2 * Mass * (Outer Wheel Radius^2 + Total Wheel Radius^2)

        // negative to fix dir
        val rotVel = angVel.mul(-1.0, Vector3d())
        val angularVelocityPropeller: Vector3dc = Vector3d(propAxis).mul(rotVel)
        val angularMomentumRelProp: Vector3dc =
            angularVelocityPropeller.mul(physShip.momentOfInertia, Vector3d())

        // Add to convert from momentum relative to wheel into relative to ship
        val centerOfMassInShip = physShip.transform.positionInShip
        val r: Vector3dc =
            Vector3d(centerOfMassInShip.add(Vector3d(physProp.position), Vector3d())).sub(physShip.transform.positionInShip)
                .rotate(physShip.transform.shipToWorldRotation)
        val momentumModifier: Vector3dc = Vector3d(physShip.omega).cross(r).mul(physShip.mass)
        val angularMomentumRelShip: Vector3dc = Vector3d(angularMomentumRelProp).add(momentumModifier)
        val prevAngularMomentumRelShip: Vector3dc = Vector3d(prevAngMomentumRelProp).add(momentumModifier)
        val torque: Vector3dc = Vector3d(prevAngularMomentumRelShip).sub(angularMomentumRelShip).div(60.0)
        physProp.prevAngularMomentum = angularMomentumRelProp
        if (!torque.isFinite || torque.length().isNaN()) {
            return Vector3d()
        }
        return torque
    }

    private fun computeBladeForce(physShip: PhysShip, physProp: PropData): Pair<Vector3dc, Vector3dc> {
        val estAngle = (physProp.bearingAngle + (physProp.bearingSpeed / 3.0 * ticksSinceLastUpdate.toDouble())) % 360.0
        val blades = physProp.blades
        val bladeCount = blades.size
        val angleBetweenBlades = 2 * Math.PI / bladeCount

        val airDensityAtY = AerodynamicUtils.getAirDensityForY(physShip.transform.positionInWorld.y(), 563.0)
        val airTemperatureAtY = AerodynamicUtils.getAirTemperatureForY(physShip.transform.positionInWorld.y(), 563.0)

        val velocityTowardsPropellerDir = physShip.velocity.dot(physShip.transform.shipToWorld.transformDirection(physProp.bearingAxis!!, Vector3d()).normalize())

        val netForce = Vector3d(physProp.bearingAxis).normalize()
        val netTorque = Vector3d(physProp.bearingAxis).normalize()

        var overallThrust = 0.0
        var overallTorque = 0.0

        for (i in blades.indices) {
            val blade = blades[i]
            val bladeAngle = Math.toRadians(estAngle) + (angleBetweenBlades * i.toDouble())
            val bladePitch = Math.toRadians(blade.angle)
            val bladeWidth = if (blade.wide) 0.375 else 0.25
            val bladeLength = blade.length

            val steps = ClockworkConfig.SERVER.bladeIntegrationSteps

            val dr = bladeLength / steps
            var bladeThrust = 0.0
            var bladeTorque = 0.0

            for (step in 0 until steps.toInt()) {
                val r = step.toDouble() * dr

                val effectiveVelocity = sqrt(velocityTowardsPropellerDir.pow(2.0) + (physProp.bearingSpeed * r).pow(2.0))

                val angleOfAttack = bladePitch - atan(velocityTowardsPropellerDir / (physProp.bearingSpeed * r))

                val liftCoefficient = 2.0 * Math.PI * angleOfAttack
                val dragCoefficient = 0.01 * liftCoefficient

                val dLift = 0.5 * airDensityAtY * effectiveVelocity.pow(2.0) * bladeWidth * dr * liftCoefficient
                val dDrag = 0.5 * airDensityAtY * effectiveVelocity.pow(2.0) * bladeWidth * dr * dragCoefficient

                val dThrust = dLift * Math.cos(bladeAngle) - dDrag * Math.sin(bladeAngle)
                val dTorque = r * (dLift * Math.sin(bladeAngle) + dDrag * Math.cos(bladeAngle))

                bladeThrust += dThrust
                bladeTorque += dTorque
            }
            overallThrust += bladeThrust
            overallTorque += bladeTorque
        }

        netForce.mul(overallThrust)
        netTorque.mul(overallTorque)

        return netForce to netTorque
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

    companion object {
        fun getOrCreate(ship: ServerShip): PropellerController? {
            if (ship.getAttachment(PropellerController::class.java) == null) {
                ship.saveAttachment(PropellerController::class.java, PropellerController())
            }
            return ship.getAttachment(PropellerController::class.java)
        }
    }
}
