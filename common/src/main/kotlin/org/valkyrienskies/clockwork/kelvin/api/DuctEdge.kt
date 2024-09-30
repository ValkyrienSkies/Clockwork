package org.valkyrienskies.clockwork.kelvin.api

import net.minecraft.server.level.ServerPlayer

interface DuctEdge {

    val type: ConnectionType

    val nodeA: DuctNodePos
    val nodeB: DuctNodePos

    var radius: Double
    var length: Double
    var currentFlowRate: Double

    fun interact(player: ServerPlayer): Boolean {
        return false
    }
}