package org.valkyrienskies.clockwork.kelvin.api.edges

import org.valkyrienskies.clockwork.kelvin.api.ConnectionType
import org.valkyrienskies.clockwork.kelvin.api.DuctEdge
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos

/**
 * A default edge type that has an aperture and restricts flow to one direction.
 */
class ApertureOneWayDuctEdge(
    override val type: ConnectionType,
    override val nodeA: DuctNodePos,
    override val nodeB: DuctNodePos,
    override var radius: Double = 0.125, override var length: Double = 0.5, override var currentFlowRate: Double = 0.0,
    override var aperture: Double = 0.0,
    override var reversed: Boolean = false
) : DuctEdge, ApertureEdge, OneWayEdge {
}