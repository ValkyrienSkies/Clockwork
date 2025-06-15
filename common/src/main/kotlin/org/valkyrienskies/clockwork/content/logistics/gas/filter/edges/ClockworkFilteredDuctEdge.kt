package org.valkyrienskies.clockwork.content.logistics.gas.filter.edges

import net.minecraft.server.level.ServerPlayer
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.gas.filter.FilterScreenOpenPacket
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.edges.FilteredEdge

class ClockworkFilteredDuctEdge(
    override val type: ConnectionType,
    override val nodeA: DuctNodePos,
    override val nodeB: DuctNodePos,
    override var radius: Double = 0.125, override var length: Double = 0.5, override var currentFlowRate: Double = 0.0,
    override val filter: HashSet<GasType> = HashSet(),
    override var blacklist: Boolean = false,
    override var unloaded: Boolean = false
) : FilteredEdge {

    override fun interact(player: ServerPlayer): Boolean {
        ClockworkPackets.sendTo(FilterScreenOpenPacket(nodeA, nodeA, filter, blacklist), player)
        return true
    }
}