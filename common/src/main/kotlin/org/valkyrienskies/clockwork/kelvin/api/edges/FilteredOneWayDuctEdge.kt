package org.valkyrienskies.clockwork.kelvin.api.edges

import org.valkyrienskies.clockwork.kelvin.api.ConnectionType
import org.valkyrienskies.clockwork.kelvin.api.DuctEdge
import org.valkyrienskies.clockwork.kelvin.api.DuctNode
import org.valkyrienskies.clockwork.kelvin.api.GasType

/**
 * A default edge type that has both a one-way connection between its nodes and a filter that only allows certain gasses to flow through it. Its directionality can be changed. Its filter can either be a Whitelist or a Blacklist.
 */
class FilteredOneWayDuctEdge(
    override val type: ConnectionType,
    override val nodeA: DuctNode,
    override val nodeB: DuctNode,
    override var radius: Double = 0.125, override var length: Double = 0.5, override var currentFlowRate: Double = 0.0,
) : DuctEdge {
}
