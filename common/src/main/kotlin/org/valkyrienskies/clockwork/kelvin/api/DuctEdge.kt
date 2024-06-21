package org.valkyrienskies.clockwork.kelvin.api

interface DuctEdge {

    val type: ConnectionType

    val nodeA: DuctNode
    val nodeB: DuctNode

    var radius: Double
    var length: Double
    var currentFlowRate: Double
}