package org.valkyrienskies.clockwork.kelvin.api.nodes

import org.valkyrienskies.clockwork.kelvin.api.*
import java.util.HashSet

class PipeDuctNode(
    override val pos: DuctNodePos,
    override val behavior: NodeBehaviorType,
    override val network: DuctNetwork,
    override val nodeEdges: HashSet<DuctEdge>,
    override val volume: Double,
    override val maxPressure: Double,
    override val maxTemperature: Double
) : DuctNode {

    override fun getEdges(): Set<DuctEdge> {
        return nodeEdges
    }

    override fun getEdgeTo(neighbor: DuctNodePos): DuctEdge? {
        return nodeEdges.firstOrNull { it.nodeA == this.pos && it.nodeB == neighbor || it.nodeA == neighbor && it.nodeB == this.pos }
    }
}