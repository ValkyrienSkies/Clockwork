package org.valkyrienskies.clockwork.content.forces

import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.api.ships.datastructures.AirPocket
import org.valkyrienskies.core.api.ships.datastructures.ShipConnDataAttachment
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import java.util.concurrent.ConcurrentLinkedQueue

class PocketForcesController: ShipForcesInducer {

    val pockets: HashMap<Int, AirPocket> = HashMap()

    val queuedChanges: ConcurrentLinkedQueue<HashMap<Int, AirPocket>> = ConcurrentLinkedQueue()

    override fun applyForces(physShip: PhysShip) {
        val physShipImpl = physShip as PhysShipImpl

        if (queuedChanges.isNotEmpty()) {
            val newPockets = queuedChanges.poll()
            if (newPockets != null) {
                pockets.clear()
                pockets.putAll(newPockets)
            }
        }


    }

    fun gameTick(level: ServerLevel, ship: ServerShip) {
        val pocketsCopy = ship.getAttachment(ShipConnDataAttachment::class.java)?.airPockets ?: return
        queuedChanges.add(pocketsCopy)
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