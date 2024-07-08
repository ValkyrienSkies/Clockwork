package org.valkyrienskies.clockwork.kelvin.api

interface DuctEdge {

    val type: ConnectionType

    val nodeA: DuctNodePos
    val nodeB: DuctNodePos

    var radius: Double
    var length: Double
    var currentFlowRate: Double

    fun interact(): Boolean {
        return false
    }
}