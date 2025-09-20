package org.valkyrienskies.clockwork.content.logistics.gas.duct.edges

import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.SmartDuctEdge

class ClockworkSmartEdge(type: ConnectionType, nodeA: DuctNodePos, nodeB: DuctNodePos, radius: Double, length: Double,
                         currentFlowRate: Double
) : SmartDuctEdge(type, nodeA,
    nodeB,
    radius,
    length, currentFlowRate, unloaded = false
) {
}