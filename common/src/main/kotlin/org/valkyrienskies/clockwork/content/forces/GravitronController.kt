package org.valkyrienskies.clockwork.content.forces

import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronForceInducerData
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.world.PhysLevel

class GravitronController : ShipPhysicsListener {
    var data: GravitronForceInducerData? = null

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        val dataCopy = data ?: return

        run {
            val pConst = 160.0
            val dConst = 20.0

            val localGrabPos: Vector3dc =
                physShip.transform.shipToWorld.transformPosition(dataCopy.grabbedPos, Vector3d())
            val idealPosDif: Vector3dc = dataCopy.idealPos.sub(localGrabPos, Vector3d())

            val posDif: Vector3d = idealPosDif.mul(pConst, Vector3d())
            val mass = physShip.mass

            // Integrate
            posDif.sub(physShip.velocity.mul(dConst, Vector3d()))

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
            rotDifVector.sub(physShip.omega.mul(dConst, Vector3d()))

            val torque = physShip.transform.shipToWorldRotation.transform(
                physShip.momentOfInertia.transform(
                    physShip.transform.shipToWorldRotation.transformInverse(
                        rotDifVector,
                        Vector3d()
                    )
                )
            )
            physShip.applyInvariantTorque(torque)
        }
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): GravitronController {
            return ship.getAttachment<GravitronController>()
                ?: GravitronController().also { ship.setAttachment(it) }
        }
    }
}
