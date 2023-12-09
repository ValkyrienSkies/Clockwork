package org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron

import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.api.ships.getAttachment
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl

class GravitronForceInducer : ShipForcesInducer {
    var data: GravitronForceInducerData? = null

    override fun applyForces(physShip: PhysShip) {
        val dataCopy = data ?: return
        physShip as PhysShipImpl

        run {
            val pConst = 160.0
            val dConst = 20.0

            val localGrabPos: Vector3dc =
                physShip.transform.shipToWorld.transformPosition(dataCopy.grabbedPos, Vector3d())
            val idealPosDif: Vector3dc = dataCopy.idealPos.sub(localGrabPos, Vector3d())

            val posDif: Vector3d = idealPosDif.mul(pConst, Vector3d())
            val mass = physShip.inertia.shipMass

            // Integrate
            posDif.sub(physShip.poseVel.vel.mul(dConst, Vector3d()))

            val force = posDif.mul(mass, Vector3d())
            physShip.applyInvariantForce(force)
        }

        run {
            val pConst = 160.0
            val dConst = 20.0
            val rotDif =
                dataCopy.idealRot.mul(physShip.transform.shipToWorldRotation.invert(Quaterniond()), Quaterniond())
                    .normalize().invert()
            val rotDifVector = Vector3d(rotDif.x() * 2.0, rotDif.y() * 2.0, rotDif.z() * 2.0).mul(pConst)
            if (rotDif.w() < 0) {
                rotDifVector.mul(-1.0)
            }
            rotDifVector.mul(-1.0)

            // Integrate
            rotDifVector.sub(physShip.poseVel.omega.mul(dConst, Vector3d()))

            val torque = physShip.poseVel.rot.transform(
                physShip.inertia.momentOfInertiaTensor.transform(
                    physShip.poseVel.rot.transformInverse(
                        rotDifVector,
                        Vector3d()
                    )
                )
            )
            physShip.applyInvariantTorque(torque)
        }
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): GravitronForceInducer {
            return ship.getAttachment<GravitronForceInducer>()
                ?: GravitronForceInducer().also { ship.setAttachment(GravitronForceInducer::class.java, it) }
        }

        data class GravitronForceInducerData(
            val idealPos: Vector3dc,
            val idealRot: Quaterniondc,
            val grabbedPos: Vector3dc,
        )
    }
}
