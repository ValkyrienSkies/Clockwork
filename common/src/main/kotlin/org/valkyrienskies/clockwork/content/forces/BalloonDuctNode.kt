package org.valkyrienskies.clockwork.content.forces

import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import java.util.HashSet

/**
 * Virtual Kelvin node representing the shared gas volume of a sealed blimp envelope.
 *
 * Placed at a non-integer Y coordinate (anchor.y + 0.5) so it shares the anchor's chunk
 * without colliding with any real block node. Wall heat capacity is zero because thermal
 * exchange with ambient air is handled by [BalloonThermodynamics], and pressure/temperature
 * ceilings are infinite so Kelvin will not attempt to destroy interior air blocks.
 */
class BalloonDuctNode(
    override val pos: DuctNodePos,
    override val nodeEdges: HashSet<DuctEdge> = HashSet(),
    override var volume: Double,
) : DuctNode {
    override val maxPressure: Double = Double.POSITIVE_INFINITY
    override val maxTemperature: Double = Double.POSITIVE_INFINITY
    override val heatCapacity: Double = 0.0

    override fun getEdges(): Set<DuctEdge> = nodeEdges

    override fun getEdgeTo(neighbor: DuctNodePos): DuctEdge? =
        nodeEdges.firstOrNull {
            it.nodeA == pos && it.nodeB == neighbor || it.nodeA == neighbor && it.nodeB == pos
        }
}
