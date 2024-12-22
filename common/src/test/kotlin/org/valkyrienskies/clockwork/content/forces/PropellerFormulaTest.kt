package org.valkyrienskies.clockwork.content.forces

import net.minecraft.util.Mth
import org.joml.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import org.valkyrienskies.clockwork.util.AerodynamicUtils.getAirDensityForY
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
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
        listOf()
    )

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
            val sailPosWorld: Vector3dc = transform.shipToWorld.transformPosition(sailVector, Vector3d())
            val sailPosRelShip: Vector3dc = sailVector.sub(transform.positionInShip, Vector3d())
            if (rotatedDiff.length() > furthestTip.length()) {
                furthestTip.set(rotatedDiff)
            }
            val inflowAngle = atan(axialVelocity / sailVel.length())
            val optimalAngleOfAttack = 4.0
            val optimalPitch = inflowAngle + optimalAngleOfAttack
            physProp.currentBladePitch = if (physProp.brass) {
                Mth.lerp(0.1, physProp.currentBladePitch, optimalPitch)
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

        println("Net force: $netForce")
        println("Net torque: $netTorque")

    }

    @Test
    fun testOldPropellerForces() {
        val modifiedSpeed: Double = (physProp.bearingSpeed * 10.0 / 3.0) * 1.5 //* 1.25, A little bit easier to generate force //TODO config?
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