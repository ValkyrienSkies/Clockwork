package org.valkyrienskies.clockwork.kelvin.api.edges

import org.valkyrienskies.clockwork.kelvin.api.*

/**
 * A default edge type that has a filter which only allows certain gas types to flow through it. Its filter can either be a Whitelist or a Blacklist.
 */
class FilteredDuctEdge(
    override val type: ConnectionType,
    override val nodeA: DuctNodePos,
    override val nodeB: DuctNodePos,
    override var radius: Double = 0.125, override var length: Double = 0.5, override var currentFlowRate: Double = 0.0,
    override val filter: HashSet<GasType> = HashSet(),
    override var blacklist: Boolean = false,
) : DuctEdge, FilteredEdge {

}