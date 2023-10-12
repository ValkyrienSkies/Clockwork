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
    var idealPos: Vector3dc? = null
    var idealRot: Quaterniondc? = null

    override fun applyForces(physShip: PhysShip) {
        val idealPosCopy = idealPos
        val idealRotCopy = idealRot
        physShip as PhysShipImpl

        if (idealPosCopy != null) {
            val pConst = 160.0
            val dConst = 20.0

            val posDif = idealPosCopy.sub(physShip.transform.positionInWorld, Vector3d()).mul(pConst)
            val mass = physShip.inertia.shipMass

            // Integrate
            posDif.sub(physShip.poseVel.vel.mul(dConst, Vector3d()))

            val force = posDif.mul(mass, Vector3d())
            physShip.applyInvariantForce(force)
        }

        // Disable rotation for now
        if (false && idealRotCopy != null) {
            val pConst = 160.0
            val dConst = 20.0
            val rotDif = idealRotCopy.mul(physShip.transform.shipToWorldRotation.invert(Quaterniond()), Quaterniond()).normalize().invert()
            val rotDifVector = Vector3d(rotDif.x() * 2.0, rotDif.y() * 2.0, rotDif.z() * 2.0).mul(pConst)
            if (rotDif.w() < 0) {
                rotDifVector.mul(-1.0)
            }

            // Integrate
            rotDifVector.sub(physShip.poseVel.omega.mul(dConst, Vector3d()))

            val torque = physShip.inertia.momentOfInertiaTensor.transform(rotDifVector)
            physShip.applyInvariantTorque(torque)
        }
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): GravitronForceInducer {
            return ship.getAttachment<GravitronForceInducer>()
                ?: GravitronForceInducer().also { ship.setAttachment(GravitronForceInducer::class.java, it) }
        }
    }
}
