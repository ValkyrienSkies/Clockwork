package org.valkyrienskies.clockwork.kelvin.api.edges

import org.valkyrienskies.clockwork.kelvin.api.ConnectionType
import org.valkyrienskies.clockwork.kelvin.api.DuctEdge
import org.valkyrienskies.clockwork.kelvin.api.DuctNode

/**
 * A default edge type that has a one-way connection between two nodes. Its directionality can be changed.
 */
class OneWayDuctEdge(
    override val type: ConnectionType,
    override val nodeA: DuctNode,
    override val nodeB: DuctNode,
    override var radius: Double = 0.125, override var length: Double = 0.5, override var currentFlowRate: Double = 0.0,
    override var reversed: Boolean = false,
) : DuctEdge, OneWayEdge {
}