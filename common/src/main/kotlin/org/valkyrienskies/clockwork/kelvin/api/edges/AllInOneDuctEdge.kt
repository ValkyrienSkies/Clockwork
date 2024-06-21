package org.valkyrienskies.clockwork.kelvin.api.edges

import org.valkyrienskies.clockwork.kelvin.api.ConnectionType
import org.valkyrienskies.clockwork.kelvin.api.DuctEdge
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.GasType

/**
 * A default edge type that has an aperture, a filter, and restricts flow to one-way.
 */
class AllInOneDuctEdge(
    override val type: ConnectionType,
    override val nodeA: DuctNodePos,
    override val nodeB: DuctNodePos,
    override var radius: Double = 0.125, override var length: Double = 0.5, override var currentFlowRate: Double = 0.0,
    override var aperture: Double = 0.0,
    override var reversed: Boolean = false, override val filter: HashSet<GasType> = HashSet(), override var blacklist: Boolean = false
) : DuctEdge, ApertureEdge, OneWayEdge, FilteredEdge {
}