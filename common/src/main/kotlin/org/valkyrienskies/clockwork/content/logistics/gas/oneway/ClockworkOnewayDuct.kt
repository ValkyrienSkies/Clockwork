package org.valkyrienskies.clockwork.content.logistics.gas.oneway

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.OneWayDuctEdge

class ClockworkOnewayDuct(type: ConnectionType, nodeA: DuctNodePos, nodeB: DuctNodePos) :
    OneWayDuctEdge(type, nodeA, nodeB, radius = 0.3125, length = 0.375) {

    override fun interact(player: ServerPlayer): Boolean { return false }

}