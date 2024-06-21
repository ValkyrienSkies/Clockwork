package org.valkyrienskies.clockwork.kelvin.api.nodes

import org.valkyrienskies.clockwork.kelvin.api.*
import java.util.HashSet

class PumpDuctNode(
    override val pos: DuctNodePos,
    override val behavior: NodeBehaviorType,
    override val network: DuctNetwork,
    override val nodeEdges: HashSet<DuctEdge>,
    override val volume: Double,
    override val maxPressure: Double,
    override val maxTemperature: Double,
    var pumpPressure: Double,
    var pumpTarget: DuctEdge
) : DuctNode {

    fun setTargetPressure(pressure: Double) {
        pumpPressure = pressure
    }
    fun setTargetEdge(edge: DuctEdge) {
        pumpTarget = edge
    }

    override fun getEdges(): Set<DuctEdge> {
        return nodeEdges
    }

    override fun getEdgeTo(neighbor: DuctNode): DuctEdge? {
        return nodeEdges.firstOrNull { it.nodeA == this && it.nodeB == neighbor || it.nodeA == neighbor && it.nodeB == this }
    }
}