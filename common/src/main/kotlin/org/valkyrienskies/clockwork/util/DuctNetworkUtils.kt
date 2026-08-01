package org.valkyrienskies.clockwork.util

import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.logistics.gas.smart.ClockworkSmartEdge
import org.valkyrienskies.clockwork.content.logistics.gas.filter.edges.ClockworkFilteredDuctEdge
import org.valkyrienskies.clockwork.content.logistics.gas.oneway.ClockworkOnewayDuct
import org.valkyrienskies.core.util.squared
import org.valkyrienskies.kelvin.api.*
import org.valkyrienskies.kelvin.api.edges.FilteredOneWayDuctEdge
import org.valkyrienskies.kelvin.api.edges.OneWayDuctEdge
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.kelvin.api.nodes.PumpDuctNode

object DuctNetworkUtils {
    fun createPipeNode(pos: DuctNodePos): PipeDuctNode {
        return PipeDuctNode(pos, NodeBehaviorType.PIPE, volume = 0.25, maxPressure = 16375049.0, maxTemperature = 1478.0, heatCapacity = 44.9)
    }

    fun createPumpNode(pos: DuctNodePos): PumpDuctNode {
        return PumpDuctNode(pos, NodeBehaviorType.PUMP, volume = 0.25, maxPressure = 16375049.0, maxTemperature = 1478.0, heatCapacity = 44.9)
    }

    fun createPipeEdge(nodeA: DuctNodePos, nodeB: DuctNodePos): PipeDuctEdge {
        return configureMetalDuctEdge(PipeDuctEdge(ConnectionType.PIPE, nodeA, nodeB, radius = 0.3125, length = 0.375, currentFlowRate = 0.0))
    }

    fun createOneWayEdge(nodeA: DuctNodePos, nodeB: DuctNodePos): OneWayDuctEdge {
        return configureMetalDuctEdge(ClockworkOnewayDuct(ConnectionType.ONEWAY, nodeA, nodeB))
    }

    fun createFilteredEdge(nodeA: DuctNodePos, nodeB: DuctNodePos): ClockworkFilteredDuctEdge {
        return configureMetalDuctEdge(ClockworkFilteredDuctEdge(ConnectionType.FILTERED, nodeA, nodeB, radius = 0.3125, length = 0.375, currentFlowRate = 0.0))
    }

    fun createSmartEdge(nodeA: DuctNodePos, nodeB: DuctNodePos): ClockworkSmartEdge {
        return configureMetalDuctEdge(ClockworkSmartEdge(ConnectionType.PIPE, nodeA, nodeB, radius = 0.3125, length = 0.375, currentFlowRate = 0.0))
    }

    fun <T : DuctEdge> configureMetalDuctEdge(edge: T): T {
        edge.thermalConductivityMultiplier = ClockworkConfig.KELVIN.ductThermalConductivityMultiplier
        return edge
    }

    fun DuctNodePos.magnitudeSqr(): Double {
        return this.x.squared() + this.y.squared() + this.z.squared()
    }
}
