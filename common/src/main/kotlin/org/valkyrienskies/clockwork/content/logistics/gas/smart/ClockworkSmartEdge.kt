package org.valkyrienskies.clockwork.content.logistics.gas.smart

import net.minecraft.server.level.ServerPlayer
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.SmartDuctEdge

class ClockworkSmartEdge(type: ConnectionType, nodeA: DuctNodePos, nodeB: DuctNodePos, radius: Double, length: Double,
                         currentFlowRate: Double
) : SmartDuctEdge(type, nodeA,
    nodeB,
    radius,
    length, currentFlowRate, unloaded = false
) {

    override fun interact(player: ServerPlayer): Boolean {
        ClockworkPackets.sendTo(SmartScreenOpenPacket(nodeA, nodeB, filter, comparisonValue, moreThan), player)
        return super.interact(player)
    }
}