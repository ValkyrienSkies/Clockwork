package org.valkyrienskies.clockwork.util

import org.valkyrienskies.clockwork.kelvin.api.*
import org.valkyrienskies.clockwork.kelvin.api.edges.FilteredDuctEdge
import org.valkyrienskies.clockwork.kelvin.api.edges.FilteredOneWayDuctEdge
import org.valkyrienskies.clockwork.kelvin.api.edges.OneWayDuctEdge
import org.valkyrienskies.clockwork.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.clockwork.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.clockwork.kelvin.api.nodes.PumpDuctNode

object DuctNetworkUtils {
    fun createPipeNode(pos: DuctNodePos, network: DuctNetwork): PipeDuctNode {
        return PipeDuctNode(pos, NodeBehaviorType.PIPE, network, volume = 0.05, maxPressure = 16375049.0, maxTemperature = 1478.0)
    }

    fun createPumpNode(pos: DuctNodePos, network: DuctNetwork): PumpDuctNode {
        return PumpDuctNode(pos, NodeBehaviorType.PUMP, network, volume = 0.05, maxPressure = 16375049.0, maxTemperature = 1478.0)
    }

    fun createPipeEdge(nodeA: DuctNodePos, nodeB: DuctNodePos): PipeDuctEdge {
        return PipeDuctEdge(ConnectionType.PIPE, nodeA, nodeB, radius = 0.1875, length = 0.625, currentFlowRate = 0.0)
    }

    fun createOneWayEdge(nodeA: DuctNodePos, nodeB: DuctNodePos): OneWayDuctEdge {
        return OneWayDuctEdge(ConnectionType.ONEWAY, nodeA, nodeB, radius = 0.1875, length = 0.625, currentFlowRate = 0.0)
    }

    fun createFilteredEdge(nodeA: DuctNodePos, nodeB: DuctNodePos): FilteredDuctEdge {
        return FilteredDuctEdge(ConnectionType.FILTERED, nodeA, nodeB, radius = 0.1875, length = 0.625, currentFlowRate = 0.0)
    }

    fun createFilteredOneWayEdge(nodeA: DuctNodePos, nodeB: DuctNodePos): FilteredOneWayDuctEdge {
        return FilteredOneWayDuctEdge(ConnectionType.FILTERED_ONEWAY, nodeA, nodeB, radius = 0.1875, length = 0.625, currentFlowRate = 0.0)
    }

    fun createEdgeType(nodeA: DuctNodePos, nodeB: DuctNodePos, type: ConnectionType): DuctEdge {
        return when (type) {
            ConnectionType.PIPE -> createPipeEdge(nodeA, nodeB)
            ConnectionType.ONEWAY -> createOneWayEdge(nodeA, nodeB)
            ConnectionType.FILTERED -> createFilteredEdge(nodeA, nodeB)
            ConnectionType.FILTERED_ONEWAY -> createFilteredOneWayEdge(nodeA, nodeB)
            else -> throw IllegalArgumentException("Unsupported edge type: $type")
        }
    }
}