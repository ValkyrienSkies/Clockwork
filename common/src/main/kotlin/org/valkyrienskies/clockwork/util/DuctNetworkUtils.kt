package org.valkyrienskies.clockwork.util

import org.valkyrienskies.clockwork.kelvin.api.ConnectionType
import org.valkyrienskies.clockwork.kelvin.api.DuctNetwork
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.NodeBehaviorType
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
}