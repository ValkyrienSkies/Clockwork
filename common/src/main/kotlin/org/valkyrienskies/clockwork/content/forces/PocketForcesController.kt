package org.valkyrienskies.clockwork.content.forces

import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl

class PocketForcesController: ShipForcesInducer {
    override fun applyForces(physShip: PhysShip) {
        val physShipImpl = physShip as PhysShipImpl
    }

    companion object {
        fun getOrCreate(ship: ServerShip): PocketForcesController? {
            if (ship.getAttachment(PocketForcesController::class.java) == null) {
                ship.saveAttachment(PocketForcesController::class.java, PocketForcesController())
            }
            return ship.getAttachment(PocketForcesController::class.java)
        }
    }
}