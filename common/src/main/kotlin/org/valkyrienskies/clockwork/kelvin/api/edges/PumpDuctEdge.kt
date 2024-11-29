package org.valkyrienskies.clockwork.kelvin.api.edges

import net.minecraft.server.level.ServerPlayer
import org.valkyrienskies.clockwork.kelvin.api.ConnectionType
import org.valkyrienskies.clockwork.kelvin.api.DuctEdge
import org.valkyrienskies.clockwork.kelvin.api.DuctNode
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos

/**
 * A default edge type that has a pump connection between two nodes. It applies its pump Pressure to the flow rate calculations
 */
class PumpDuctEdge(
    override val nodeA: DuctNodePos,
    override val nodeB: DuctNodePos,
    override var target: DuctNodePos,
    override var pumpPressure: Double = 0.0,
    override val type: ConnectionType = ConnectionType.PIPE, override var radius: Double = 0.125,
    override var length: Double = 0.5, override var currentFlowRate: Double = 0.0,

) : DuctEdge, PumpEdge {

    fun invert() {
        if (target==nodeA) target = nodeB
        else target = nodeA
    }



}