package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropCreateData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropUpdateData
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.core.internal.ships.VsiPhysShip
import org.valkyrienskies.mod.common.util.toJOMLD
import java.lang.Math
import java.util.concurrent.ConcurrentLinkedQueue
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

    var dimensionId: DimensionId = "minecraft:dimension:minecraft:overworld"

    var ticksSinceLastUpdate = 0

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        if (applierUpdateData.isNotEmpty()) ticksSinceLastUpdate = 0
        super.physTick(physShip, physLevel)

        // Propeller Thrust
        for (physData in appliers.values) {
            if(physData.active) {
                val (force, torque) = if (physData.brass) computeForce(physShip.transform, physData, (physShip).velocity, physShip.omega, physShip, physLevel)
                else computeBladeForce(physShip, physData, physLevel)

                if (force.isFinite && torque.isFinite) {
                    if (physData.brass) {
                        physShip.applyWorldForceToBodyPos(force)
                        physShip.applyWorldTorque(torque)
                    } else {
                        physShip.applyWorldForceToModelPos(force, Vector3d(physData.position).add(0.5, 0.5, 0.5))
                        physShip.applyWorldTorque(torque)
                    }

                }
            }
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
        physShip: PhysShip,
        physLevel: PhysLevel
    ): Pair<Vector3dc, Vector3dc> {
        val estAngle = (physProp.bearingAngle + (physProp.bearingSpeed / 3.0 * ticksSinceLastUpdate.toDouble())) % 360.0

        val internal = physShip as VsiPhysShip
        val dragController = internal.dragController
        val wind = dragController?.getWindVector() ?: Vector3d()

        val omegaSign = sign(physProp.bearingSpeed.toDouble()).let { if (it == 0.0) 1.0 else it }
        val bearingVector: Vector3dc = Vector3d(physProp.position).add(0.5, 0.5, 0.5)
        val referencePropAxis = if (physProp.bearingAxisRot != null) {
            physProp.bearingAxisRot!!
        } else {
            physProp.bearingAxis!!
        }
        val axis: Vector3dc = referencePropAxis.mul(omegaSign, Vector3d()).normalize()
        val rotation: Quaterniondc = Quaterniond(AxisAngle4d(Math.toRadians(estAngle), axis))
        val angVel: Vector3dc = axis.mul(physProp.bearingSpeed.absoluteValue, Vector3d())
        val furthestTip = Vector3d()
        val worldAxis = physShip.transform.shipToWorld.transformDirection(axis, Vector3d()).normalize(Vector3d())
        val axialVelocity = physShip.velocity.dot(worldAxis)
        val pretendPitch = 12.0
        val netForce = Vector3d()
        val netTorque = Vector3d()
        val tiltQuat = physProp.bearingTiltQuat ?: Quaterniond()

        val eps = 1e-6

        for (pos in physProp.sailPositions!!) {
            val sailVector: Vector3dc = Vector3d(pos).add(bearingVector)
            val diff: Vector3dc = Vector3d(pos)
            val offsetFalloff = Vector3d(referencePropAxis).dot(diff)
            if (offsetFalloff > 4.0) {
                continue
            }
            val rotatedDiffShip: Vector3dc = tiltQuat.transform(rotation.transform(diff, Vector3d()), Vector3d())

            val rotatedPosShip = rotatedDiffShip.add(bearingVector, Vector3d())

            val sailPosWorld: Vector3dc = physTransform.shipToWorld.transformPosition(rotatedPosShip, Vector3d())

            val rWorld = Vector3d(sailPosWorld).sub(physTransform.positionInWorld, Vector3d())

            val vShipAtSailWorld = Vector3d(vel.add(wind, Vector3d())).add(Vector3d(omega).cross(rWorld, Vector3d()), Vector3d())

            val vPropAtBladeShip = Vector3d(angVel).cross(rotatedDiffShip, Vector3d())
            val vPropAtBladeWorld = physTransform.shipToWorld.transformDirection(vPropAtBladeShip, Vector3d())

            val vRelWorld = Vector3d(vShipAtSailWorld).add(vPropAtBladeWorld).negate()

            if (rotatedDiffShip.length() > furthestTip.length()) {
                furthestTip.set(rotatedDiffShip)
            }
//            val inflowAngle = atan2(axialVelocity / sailVel.length())

            // decompose relative wind into axial + tangential (around prop axis)
            val vAxial = vRelWorld.dot(worldAxis)                 // signed
            val vAxialVec = Vector3d(worldAxis).mul(vAxial)

            val vTanVec = Vector3d(vRelWorld).sub(vAxialVec)
            val vTan = vTanVec.length()

            // inflow angle phi: atan2(V_axial, V_tan)
            val inflowAngle = atan2(vAxial, max(vTan, eps))

            val optimalAngleOfAttack = Math.toRadians(4.0)
            val optimalPitch = inflowAngle + optimalAngleOfAttack

            val minPitch = Math.toRadians(-5.0)
            val maxPitch = Math.toRadians(30.0)
            val pitch = if (physProp.brass) {
                Mth.lerp(0.05f, physProp.currentBladePitch.toFloat(), optimalPitch.toFloat()).toDouble()
            } else {
                Math.toRadians(12.0)
            }.coerceIn(minPitch, maxPitch)

            physProp.currentBladePitch = pitch
            val angleOfAttack = physProp.currentBladePitch - inflowAngle
            //val thrustCoefficient = (angleOfAttack * cos(inflowAngle)) - (0.1 * sin(inflowAngle))

            val vUseful = max(abs(vAxial), 1.0)

            ///todo make this based off SU consumption or something
            val maxSailPowerWatts = 10000.0 // 10 kW

            //val q = 0.5 * physLevel.aerodynamicUtils.getAirDensityForY(sailPosWorld.y(), dimensionId) * ((axialVelocity).pow(2.0) + (sailVel.length()).pow(2.0))

            val rho = physLevel.aerodynamicUtils.getAirDensityForY(sailPosWorld.y(), dimensionId)
            val v2 = vRelWorld.lengthSquared()

            val liftCoefficient = 2.0 * Math.PI * angleOfAttack
            val dragCoefficient = 0.01 + 0.01 * liftCoefficient.pow(2.0)

            //val dA = 1.0 * r
            val q = 0.5 * rho * v2
            val dLift = q * liftCoefficient
            val dDrag = q * dragCoefficient

            val vtMin = 0.5 // m/s-ish (tune). Below this: fade thrust to 0.
            val vtFade = 2.0 // how quickly it ramps in

            val spinFactor = ((abs(physProp.bearingSpeed) - vtMin) / vtFade).coerceIn(0.0, 1.0)
            // use a smoothstep to avoid a sharp corner
            val sf = spinFactor * spinFactor * (3.0 - 2.0 * spinFactor)

            val dThrust = (dLift * cos(-inflowAngle) - dDrag * sin(-inflowAngle)) * sf

            val thrustCap = maxSailPowerWatts / vUseful
            val dThrustCapped = dThrust.coerceIn(-thrustCap, thrustCap)


            val force = worldAxis.mul(dThrustCapped, Vector3d())
            //            Vector3d force2 = force.mul(physProp.bearingSpeed, new Vector3d());
            val torque = rWorld.cross(force.mul(omegaSign, Vector3d()), Vector3d())

            force.mul(10.0) //ClockworkConfig.SERVER.forceMulPerSailInPropeller)

            if (offsetFalloff > 0.0001) force.div(offsetFalloff)
            if (offsetFalloff > 0.0001) torque.div(offsetFalloff)
            if (force.isFinite) netForce.add(force.mul(omegaSign, Vector3d()))
            if (torque.isFinite) netTorque.add(torque)
        }

        //netTorque.add(conserveMomentum(physShip, physProp, furthestTip, angVel))
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

    private fun computeBladeForce(physShip: PhysShip, physProp: PropData, physLevel: PhysLevel): Pair<Vector3dc, Vector3dc> {
        val estAngle = (physProp.bearingAngle + (physProp.bearingSpeed / 3.0 * ticksSinceLastUpdate.toDouble())) % 360.0
        val blades = physProp.blades
        val bladeCount = blades.size
        val angleBetweenBlades = 2 * Math.PI / bladeCount

        val internal = physShip as VsiPhysShip
        val dragController = internal.dragController
        val wind = dragController?.getWindVector() ?: Vector3d()

        val airDensityAtY = physLevel.aerodynamicUtils.getAirDensityForY(physShip.transform.positionInWorld.y(), dimensionId)
        val airTemperatureAtY = physLevel.aerodynamicUtils.getAirTemperatureForY(physShip.transform.positionInWorld.y(), dimensionId)

        val clockwiseAxis: Vector3dc = if (physProp.bearingAxis == Direction.UP.normal.toJOMLD()) {
            Direction.NORTH.normal.toJOMLD()
        } else {
            Direction.UP.normal.toJOMLD()
        }

        val omegaSign = sign(physProp.bearingSpeed.toDouble()).let { if (it == 0.0) 1.0 else it }
        // If you flip worldAxis for inverted, also flip the “rotation sense” for the aero model:


        val referencePropAxis = if (physProp.bearingAxisRot != null) {
            physProp.bearingAxisRot!!
        } else {
            physProp.bearingAxis!!
        }

        val worldAxis = physShip.transform.shipToWorld.transformDirection(referencePropAxis, Vector3d()).normalize(Vector3d())
//        if (physProp.inverted) {
//            worldAxis.mul(-1.0).normalize()
//        }
        val totalVelocityAtProp = physShip.velocity.add(wind, Vector3d()).add(physShip.angularVelocity.cross(Vector3d(physProp.position!!).sub(physShip.centerOfMass, Vector3d()).add(0.5, 0.5, 0.5), Vector3d()), Vector3d())
        val velocityTowardsPropellerDir = totalVelocityAtProp.dot(worldAxis)
        if (velocityTowardsPropellerDir.isNaN() || velocityTowardsPropellerDir.isInfinite()) {
            return Vector3d() to Vector3d()
        }
        val induced = 0.5

        val netForce = Vector3d()
        val netTorque = Vector3d()

        for (i in blades.indices) {
            val blade = blades[i]
            val bladeAngle = Math.toRadians(estAngle + (angleBetweenBlades * i.toDouble()))
            val bladePitch = -Math.toRadians(blade.angle) ///* rotationSense
            val bladeWidth = if (blade.wide) 0.375 else 0.25
            val r = blade.length / 2.0
            val rotatedDist = clockwiseAxis.mul(r, Vector3d()).rotateAxis(bladeAngle, referencePropAxis.x(), referencePropAxis.y(), referencePropAxis.z(), Vector3d())

            val rotationalVelocity = physProp.bearingSpeed.absoluteValue * r

            val absVt = abs(rotationalVelocity)
            if (absVt < 1e-4) continue

            val vtMin = 0.5 // m/s-ish (tune). Below this: fade thrust to 0.
            val vtFade = 2.0 // how quickly it ramps in

            val spinFactor = ((abs(rotationalVelocity) - vtMin) / vtFade).coerceIn(0.0, 1.0)
            // use a smoothstep to avoid a sharp corner
            val sf = spinFactor * spinFactor * (3.0 - 2.0 * spinFactor)

            val phi = atan2(velocityTowardsPropellerDir, rotationalVelocity)
            var angleOfAttack = bladePitch - phi

            val liftCoefficient = 2.0 * Math.PI * angleOfAttack
            val dragCoefficient = 0.01 + 0.01 * liftCoefficient.pow(2.0)

            val vA = (-velocityTowardsPropellerDir) + induced

            val effectiveVelocity = sqrt(vA * vA + rotationalVelocity * rotationalVelocity)

            val dA = bladeWidth * r
            val q = 0.5 * airDensityAtY * effectiveVelocity.pow(2.0)
            val dLift = q * dA * liftCoefficient
            val dDrag = q * dA * dragCoefficient

            val dThrust = (dLift * cos(phi) - dDrag * sin(phi)) * sf

            val vUseful = max(abs(velocityTowardsPropellerDir), 1.0)

            ///todo make this based off SU consumption or something
            val maxBladePowerWatts = 5000.0 * r // 10 kW

            val thrustCap = maxBladePowerWatts / vUseful
            val dThrustCapped = dThrust.coerceIn(-thrustCap, thrustCap)

            val force = worldAxis.mul(dThrustCapped * 10, Vector3d()).mul(omegaSign, Vector3d())
            //val torque = rotatedDist.cross(force, Vector3d())

            netForce.add(force)
            netTorque.add(Vector3d())
        }

        return netForce to netTorque
    }

    private fun setDimension(dimID: DimensionId) {
        dimensionId = dimID
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): PropellerController? {
            if (ship.getAttachment(PropellerController::class.java) == null) {
                val controller = PropellerController()
                controller.setDimension(ship.chunkClaimDimension)
                ship.setAttachment(controller)
            }
            return ship.getAttachment(PropellerController::class.java)
        }
    }
}
