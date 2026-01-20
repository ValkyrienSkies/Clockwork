package org.valkyrienskies.clockwork.content.forces

import net.minecraft.core.Direction
import net.minecraft.util.Mth
import org.joml.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.util.AerodynamicUtils
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.core.impl.shadow.DL
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.vsCore
import java.lang.Math
import kotlin.math.*

class PropellerFormulaTest {

    val sailPositions = listOf(
        Vector3i(1, 0, 0),
        Vector3i(2, 0, 0),
        Vector3i(3, 0, 0),
        Vector3i(4, 0, 0),
        Vector3i(-1, 0, 0),
        Vector3i(-2, 0, 0),
        Vector3i(-3, 0, 0),
        Vector3i(-4, 0, 0),
        Vector3i(0, 1, 0),
        Vector3i(0, 2, 0),
        Vector3i(0, 3, 0),
        Vector3i(0, 4, 0),
        Vector3i(0, -1, 0),
        Vector3i(0, -2, 0),
        Vector3i(0, -3, 0),
        Vector3i(0, -4, 0),
    )

    val blades = listOf(
        BladeData(wide = false, angle = 15.0, length = 4.0),
        BladeData(wide = false, angle = 15.0, length = 4.0),
        BladeData(wide = false, angle = 15.0, length = 4.0),
        BladeData(wide = false, angle = 15.0, length = 4.0),
    )

    val representativeOmega = 256.0 * 2.0 * 3.0 / 10.0

    val transform = ShipTransformImpl.create(Vector3d(0.0, 110.0, 0.0), Vector3d(0.0, 0.0, 0.0))

    val velocity = Vector3d(0.0, 0.0, 0.0)

    val physProp = PropData(
        Vector3i(0, 0, 0),
        Vector3d(0.0, 0.0, 1.0),
        0.0,
        representativeOmega,
        sailPositions,
        false,
        true,
        true,
        blades
    )

    val dimensionId = "minecraft:overworld"

    @Test
    fun testPropellerForces() {

        // do the math
        val modifiedSpeed: Double = (physProp.bearingSpeed * 10.0 / 3.0) * if (physProp.inverted) -1.0 else 1.0  //* 1.25, A little bit easier to generate force //TODO config?
        val bearingVector: Vector3dc = Vector3d(physProp.position).add(0.5, 0.5, 0.5)
        val axis: Vector3dc = physProp.bearingAxis!!.mul(sign(modifiedSpeed), Vector3d()).normalize()
        val rotation: Quaterniondc = Quaterniond(AxisAngle4d(Math.toRadians(physProp.bearingAngle), axis))
        val angVel: Vector3dc = axis.mul(modifiedSpeed / 60.0 * (2.0 * Math.PI), Vector3d())
        val furthestTip = Vector3d()
        val worldAxis = transform.shipToWorld.transformDirection(axis, Vector3d()).normalize(Vector3d())
        val axialVelocity = velocity.dot(worldAxis)
        val pretendPitch = Math.toRadians(4.0)
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
            val rotatedPos = rotatedDiff.add(bearingVector, Vector3d())
            val sailPosWorld: Vector3dc = transform.shipToWorld.transformPosition(rotatedPos, Vector3d())
            val sailPosRelShip: Vector3dc = rotatedPos.sub(transform.positionInShip, Vector3d())
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

            val q = 0.5 * DL().getAirDensityForY(sailPosWorld.y(), dimensionId) * ((axialVelocity).pow(2.0) + (sailVel.length()).pow(2.0))

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

        println("Net force: $netForce")
        println("Net torque: $netTorque")

    }

    @Test
    fun testOldPropellerForces() {
        val modifiedSpeed: Double = physProp.bearingSpeed * 1.5 //* 1.25, A little bit easier to generate force //TODO config?
        val bearingVector: Vector3dc = Vector3d(physProp.position).add(0.5, 0.5, 0.5)
        val axis: Vector3dc = physProp.bearingAxis!!.mul(sign(modifiedSpeed), Vector3d())
        val rotation: Quaterniondc = Quaterniond(AxisAngle4d(Math.toRadians(physProp.bearingAngle), axis))
        val angVel: Vector3dc = axis.mul(modifiedSpeed / 60.0 * (2.0 * Math.PI), Vector3d())
        val furthestTip = Vector3d()
        val netForce = Vector3d()
        val netTorque = Vector3d()

        for (pos in physProp.sailPositions!!) {
            val sailVector: Vector3dc = Vector3d(pos.x().toDouble(), pos.y().toDouble(), pos.z().toDouble())
                .add(bearingVector)
            val diff: Vector3dc = sailVector.sub(bearingVector, Vector3d())
            val rotatedDiff: Vector3dc = rotation.transform(diff, Vector3d())
            val sailVel: Vector3dc = rotatedDiff.cross(angVel, Vector3d())
            if (rotatedDiff.length() > furthestTip.length()) {
                furthestTip.set(rotatedDiff)
            }
            val force = transform.shipToWorldRotation.transform(axis.mul(sailVel.length(), Vector3d()))
                .mul(5000.0, Vector3d())
            //            Vector3d force2 = force.mul(physProp.bearingSpeed, new Vector3d());
            val sailPosWorld: Vector3dc = transform.shipToWorld.transformPosition(sailVector, Vector3d())
            val sailPosRelShip: Vector3dc = sailPosWorld.sub(transform.positionInWorld, Vector3d())
            val torque = sailPosRelShip.cross(force, Vector3d())
            val sailPosRelCenterMass: Vector3dc = transform.shipToWorld.transformPosition(sailVector, Vector3d())
                .sub(transform.positionInWorld, Vector3d())
            val worldVelAtSail: Vector3dc = Vector3d().cross(sailPosRelCenterMass, Vector3d()).add(Vector3d(), Vector3d())
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

        println("Old net force: $netForce")
        println("Old net torque: $netTorque")
    }

    @Test
    public fun testBladeForces() {
        val blades = physProp.blades
        val bladeCount = blades.size
        val angleBetweenBlades = 2 * Math.PI / bladeCount

        // Wow constructing a proguarded class is hella sus, but I think this is the best way
        val airDensityAtY = DL().getAirDensityForY(transform.positionInWorld.y(), dimensionId)
        val airTemperatureAtY = DL().getAirTemperatureForY(transform.positionInWorld.y(), dimensionId)

        val clockwiseAxis: Vector3dc = if (physProp.bearingAxis == Direction.UP.normal.toJOMLD()) {
            Direction.NORTH.normal.toJOMLD()
        } else {
            Direction.UP.normal.toJOMLD()
        }

        val velocityTowardsPropellerDir = velocity.dot(transform.shipToWorld.transformDirection(physProp.bearingAxis!!, Vector3d()).normalize())

        val netForce = Vector3d()
        val netTorque = Vector3d()

        for (i in blades.indices) {
            val blade = blades[i]
            val bladeAngle = Math.toRadians(angleBetweenBlades * i.toDouble())
            val bladePitch = Math.toRadians(blade.angle)
            val bladeWidth = if (blade.wide) 0.375 else 0.25
            val r = blade.length
            val rotatedDist = clockwiseAxis.mul(r, Vector3d()).rotateAxis(bladeAngle, physProp.bearingAxis!!.x(), physProp.bearingAxis!!.y(), physProp.bearingAxis!!.z(), Vector3d())

            val effectiveVelocity = sqrt(velocityTowardsPropellerDir.pow(2.0) + (physProp.bearingSpeed * r).pow(2.0))

            val angleOfAttack = bladePitch - atan(velocityTowardsPropellerDir / (physProp.bearingSpeed * r))

            val liftCoefficient = 2.0 * Math.PI * angleOfAttack
            val dragCoefficient = 0.01 * liftCoefficient

            val dLift = 0.5 * airDensityAtY * effectiveVelocity.pow(2.0) * bladeWidth * r * liftCoefficient
            val dDrag = 0.5 * airDensityAtY * effectiveVelocity.pow(2.0) * bladeWidth * r * dragCoefficient

            val dThrust = dLift * Math.cos(angleOfAttack) - dDrag * Math.sin(bladeAngle)

            val force = Vector3d(physProp.bearingAxis).normalize().mul(dThrust, Vector3d())
            val torque = rotatedDist.cross(force, Vector3d())

            netForce.add(force.mul(10.0))
            netTorque.add(torque)
        }

        println("Blade net force: $netForce")
        println("Blade net torque: $netTorque")
    }

    // solely for testing purposes

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
}