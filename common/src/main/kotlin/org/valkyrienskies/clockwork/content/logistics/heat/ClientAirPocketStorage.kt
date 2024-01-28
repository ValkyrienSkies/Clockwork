package org.valkyrienskies.clockwork.content.logistics.heat

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.core.api.ships.datastructures.AirPocket
import org.valkyrienskies.core.api.ships.datastructures.ShipConnDataAttachment
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toMinecraft
import java.util.concurrent.ConcurrentLinkedQueue

object ClientAirPocketStorage {
    val shipToPocketsMap = HashMap<Long, HashMap<Int, AirPocket>>()

    val pocketsToUpdateQueue = ConcurrentLinkedQueue<Pair<ShipId, Int>>()
    val pocketsToDeleteQueue = ConcurrentLinkedQueue<Pair<ShipId, Int>>()

    fun serverTick(slevel: ServerLevel) {
        while (!pocketsToUpdateQueue.isEmpty()) {
            val pair = pocketsToUpdateQueue.poll()
            val shipId = pair.first
            val pocketId = pair.second

            val ship = slevel.shipObjectWorld.loadedShips.getById(shipId)
            if (ship != null) {
                val connData = ship.getAttachment(ShipConnDataAttachment::class.java)
                if (connData != null) {
                    val airPockets = connData.airPockets
                    if (airPockets != null) {
                        val pos = ship.transform.shipToWorld.transformPosition(ship.inertiaData.centerOfMassInShip, Vector3d())
                        val blockpos = BlockPos(pos.toMinecraft())
                        if (airPockets.containsKey(pocketId)) {
                            ClockworkPackets.sendToNear(slevel, blockpos, 64, AirPocketSyncPacket(shipId, airPockets[pocketId]!!))
                        }
                    }
                }
            }
        }
        while (!pocketsToDeleteQueue.isEmpty()) {
            val pair = pocketsToDeleteQueue.poll()
            val shipId = pair.first
            val pocketId = pair.second

            val ship = slevel.shipObjectWorld.loadedShips.getById(shipId)
            if (ship != null) {
                val connData = ship.getAttachment(ShipConnDataAttachment::class.java)
                if (connData != null) {
                    val airPockets = connData.airPockets
                    if (airPockets != null) {
                        val pos = ship.transform.shipToWorld.transformPosition(ship.inertiaData.centerOfMassInShip, Vector3d())
                        val blockpos = BlockPos(pos.toMinecraft())
                        if (airPockets.containsKey(pocketId)) {
                            ClockworkPackets.sendToNear(slevel, blockpos, 64, AirPocketDeletePacket(shipId, pocketId))
                        }
                    }
                }
            }
        }
    }

    fun getAirPocket(shipId: Long, pocketId: Int): AirPocket? {
        return shipToPocketsMap[shipId]?.get(pocketId)
    }

    fun getAirPockets(shipId: Long): HashMap<Int, AirPocket>? {
        return shipToPocketsMap[shipId]
    }

    fun setAirPockets(shipId: Long, pockets: HashMap<Int, AirPocket>) {
        shipToPocketsMap[shipId] = pockets
        ClockworkMod.LOGGER.info("Received air pockets for ship $shipId")
        ClockworkMod.LOGGER.info("Current air pockets for ship $shipId: ${pockets.keys.size}")
    }

    fun setAirPocket(shipId: Long, pocket: AirPocket) {
        val pockets = shipToPocketsMap[shipId]
        if (pockets != null) {
            pockets[pocket.id] = pocket
        } else {
            shipToPocketsMap[shipId] = hashMapOf(pocket.id to pocket)
        }
    }

    fun deleteAirPocket(shipId: Long, pocketId: Int) {
        val pockets = shipToPocketsMap[shipId]
        pockets?.remove(pocketId)
    }

    fun isBlockInAirPocket(shipId: Long, blockPos: Vector3ic): Boolean {
        val pockets = shipToPocketsMap[shipId]
        if (pockets != null) {
            for (pocket in pockets.values) {
                if (pocket.pocket.contains(blockPos)) {
                    return true
                }
            }
        }
        return false
    }

    fun getAirPocketIdContainingBlock(shipId: Long, blockPos: Vector3ic): Int {
        val pockets = shipToPocketsMap[shipId]
        if (pockets != null) {
            for (pocket in pockets.values) {
                if (pocket.pocket.contains(blockPos)) {
                    return pocket.id
                }
            }
        }
        return -1
    }

    fun getAirPocketContainingBlock(shipId: Long, blockPos: Vector3ic): AirPocket? {
        val pockets = shipToPocketsMap[shipId]
        if (pockets != null) {
            for (pocket in pockets.values) {
                if (pocket.pocket.contains(blockPos)) {
                    return pocket
                }
            }
        }
        return null
    }

    fun getClientPocketTemperature(shipId: Long, pocketId: Int): Double {
        val pocket = getAirPocket(shipId, pocketId)
        return pocket?.extraData?.get("kelvin/temperature_dbl_mrg_avg") as Double ?: 0.0
    }
}