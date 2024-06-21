package org.valkyrienskies.clockwork.kelvin.api.edges

import org.valkyrienskies.clockwork.kelvin.api.ConnectionType
import org.valkyrienskies.clockwork.kelvin.api.DuctEdge
import org.valkyrienskies.clockwork.kelvin.api.DuctNode
import org.valkyrienskies.clockwork.kelvin.api.GasType

/**
 * A default edge type that has a filter which only allows certain gas types to flow through it. Its filter can either be a Whitelist or a Blacklist.
 */
class FilteredDuctEdge(
    override val type: ConnectionType,
    override val nodeA: DuctNode,
    override val nodeB: DuctNode,
    override var radius: Double = 0.125, override var length: Double = 0.5, override var currentFlowRate: Double = 0.0,
    override val filter: HashSet<GasType> = HashSet(),
    override var blacklist: Boolean = false,
) : DuctEdge, FilteredEdge {


}