package org.valkyrienskies.clockwork.kelvin.api

import java.util.*

interface DuctNode {

    val pos: DuctNodePos
    val behavior: NodeBehaviorType
    val network: DuctNetwork

    val nodeEdges: HashSet<DuctEdge>

    val volume: Double
    val maxPressure: Double
    val maxTemperature: Double

    fun getEdges(): Set<DuctEdge>

    fun getEdgeTo(neighbor: DuctNode): DuctEdge?
}