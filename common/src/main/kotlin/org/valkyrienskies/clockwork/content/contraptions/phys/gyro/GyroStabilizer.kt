package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl

fun gyroStabilizer(
        physShip: PhysShipImpl,
        omega: Vector3dc,
        forces: PhysShipImpl,
        strength: Double,
        targetVector: Vector3dc
) {
    val shipUp = Vector3d(0.0, 1.0, 0.0)
    val worldUp = targetVector

    physShip.poseVel.rot.transform(shipUp)
    val angleBetween = shipUp.angle(worldUp)
    val idealAngularAcceleration = Vector3d()
    if (angleBetween > .01) {
        val stabilizationRotationAxisNormalized = shipUp.cross(worldUp, Vector3d()).normalize()
        idealAngularAcceleration.add(
            stabilizationRotationAxisNormalized.mul(angleBetween, stabilizationRotationAxisNormalized)
        )
    }

    idealAngularAcceleration.sub(
        omega.x(),
        omega.y(),
        omega.z()
    )

    val stabilizationTorque = physShip.poseVel.rot.transform(
        physShip.inertia.momentOfInertiaTensor.transform(
            physShip.poseVel.rot.transformInverse(idealAngularAcceleration)
        )
    )

    stabilizationTorque.mul(strength)
    forces.applyInvariantTorque(stabilizationTorque)
}