package org.valkyrienskies.clockwork.content.logistics.gas.duct

import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createFilteredEdge
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createOneWayEdge
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createPipeEdge
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createSmartEdge
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos

enum class DuctEdgeType {
    NONE,
    PIPE,
    ONEWAY,
    FILTERED,
    SMART
    ;

    fun nextScrewdrivable(): DuctEdgeType {
        return when (this) {
            PIPE -> ONEWAY
            ONEWAY -> FILTERED
            FILTERED -> SMART
            SMART -> PIPE
            else -> PIPE
        }
    }

    companion object {
        fun createEdgeType(nodeA: DuctNodePos, nodeB: DuctNodePos, type: DuctEdgeType): DuctEdge {
            return when (type) {
                PIPE -> createPipeEdge(nodeA, nodeB)
                ONEWAY -> createOneWayEdge(nodeA, nodeB)
                FILTERED -> createFilteredEdge(nodeA, nodeB)
                SMART -> createSmartEdge(nodeA, nodeB)
                else -> throw IllegalArgumentException("Unsupported edge type: $type")
            }
        }
    }

}