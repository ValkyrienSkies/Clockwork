package org.valkyrienskies.clockwork.content

import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.contraptions.phys.gyro.GyroShipControl
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl

class AuricShipControl : ShipForcesInducer {

    var aurics = 0

    override fun applyForces(physShip: PhysShip) {
        physShip as PhysShipImpl

        val yForce = (aurics * 2.0) - 1.85
        val force =  Vector3d(0.0, yForce,0.0).mul(5000.0, Vector3d())

        physShip.applyInvariantForce(force)
    }

    companion object {
        fun getOrCreate(ship: ServerShip): AuricShipControl {
            return ship.getAttachment<AuricShipControl>()
                ?: AuricShipControl().also {
                    ship.saveAttachment(it)
                }
        }
    }
}