package org.valkyrienskies.clockwork.util

import org.valkyrienskies.clockwork.content.logistics.gas.filter.edges.ClockworkFilteredDuctEdge
import org.valkyrienskies.kelvin.api.*
import org.valkyrienskies.kelvin.api.edges.FilteredOneWayDuctEdge
import org.valkyrienskies.kelvin.api.edges.OneWayDuctEdge
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.kelvin.api.nodes.PumpDuctNode

object DuctNetworkUtils {
    fun createPipeNode(pos: DuctNodePos): PipeDuctNode {
        return PipeDuctNode(pos, NodeBehaviorType.PIPE, volume = 0.1, maxPressure = 16375049.0, maxTemperature = 1478.0)
    }

    fun createPumpNode(pos: DuctNodePos): PumpDuctNode {
        return PumpDuctNode(pos, NodeBehaviorType.PUMP, volume = 0.1, maxPressure = 16375049.0, maxTemperature = 1478.0)
    }

    fun createPipeEdge(nodeA: DuctNodePos, nodeB: DuctNodePos): PipeDuctEdge {
        return PipeDuctEdge(ConnectionType.PIPE, nodeA, nodeB, radius = 0.3125, length = 0.375, currentFlowRate = 0.0)
    }

    fun createOneWayEdge(nodeA: DuctNodePos, nodeB: DuctNodePos): OneWayDuctEdge {
        return OneWayDuctEdge(ConnectionType.ONEWAY, nodeA, nodeB, radius = 0.3125, length =  0.375, currentFlowRate = 0.0)
    }

    fun createFilteredEdge(nodeA: DuctNodePos, nodeB: DuctNodePos): ClockworkFilteredDuctEdge {
        return ClockworkFilteredDuctEdge(ConnectionType.FILTERED, nodeA, nodeB, radius = 0.3125, length = 0.375, currentFlowRate = 0.0)
    }

    fun createFilteredOneWayEdge(nodeA: DuctNodePos, nodeB: DuctNodePos): FilteredOneWayDuctEdge {
        return FilteredOneWayDuctEdge(ConnectionType.FILTERED_ONEWAY, nodeA, nodeB, radius = 0.3125, length = 0.375, currentFlowRate = 0.0)
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