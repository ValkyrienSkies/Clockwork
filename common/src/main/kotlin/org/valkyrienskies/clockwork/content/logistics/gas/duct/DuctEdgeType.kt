package org.valkyrienskies.clockwork.content.logistics.gas.duct

import net.minecraft.core.Direction
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createFilteredEdge
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createOneWayEdge
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createPipeEdge
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createSmartEdge
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos

enum class DuctEdgeType {
    NONE,
    PIPE,
    ONEWAY_FORWARD,
    ONEWAY_BACKWARD,
    FILTERED,
    SMART
    ;

    fun nextScrewdrivable(): DuctEdgeType {
        return when (this) {
            PIPE -> ONEWAY_FORWARD
            ONEWAY_FORWARD -> ONEWAY_BACKWARD
            ONEWAY_BACKWARD -> FILTERED
            FILTERED -> SMART
            SMART -> PIPE
            else -> PIPE
        }
    }

    fun isOneWay(): Boolean {
        return this == ONEWAY_BACKWARD || this == ONEWAY_FORWARD
    }

    fun flowsOutward(direction: Direction): Boolean {
        require(isOneWay()) { "Only one-way edges have a fixed flow direction" }
        // Forward flows along the negative axis; backward flows along the positive axis.
        return (this == ONEWAY_FORWARD) == (direction.axisDirection == Direction.AxisDirection.NEGATIVE)
    }

    companion object {
        fun createEdgeType(nodeA: DuctNodePos, nodeB: DuctNodePos, type: DuctEdgeType, direction: Direction): DuctEdge {
            return when (type) {
                PIPE -> createPipeEdge(nodeA, nodeB)
                ONEWAY_FORWARD, ONEWAY_BACKWARD ->
                    if (type.flowsOutward(direction)) createOneWayEdge(nodeA, nodeB)
                    else createOneWayEdge(nodeB, nodeA)
                FILTERED -> createFilteredEdge(nodeA, nodeB)
                SMART -> createSmartEdge(nodeA, nodeB)
                else -> throw IllegalArgumentException("Unsupported edge type: $type")
            }
        }
    }

}
