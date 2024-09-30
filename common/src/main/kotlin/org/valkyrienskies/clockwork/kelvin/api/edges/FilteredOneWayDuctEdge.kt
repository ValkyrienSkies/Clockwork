package org.valkyrienskies.clockwork.kelvin.api.edges

import net.minecraft.server.level.ServerPlayer
import org.valkyrienskies.clockwork.kelvin.api.*

/**
 * A default edge type that has both a one-way connection between its nodes and a filter that only allows certain gasses to flow through it. Its directionality can be changed. Its filter can either be a Whitelist or a Blacklist.
 */
class FilteredOneWayDuctEdge(
    override val type: ConnectionType,
    override val nodeA: DuctNodePos,
    override val nodeB: DuctNodePos,
    override var radius: Double = 0.125, override var length: Double = 0.5, override var currentFlowRate: Double = 0.0,
    override val filter: HashSet<GasType> = HashSet(),
    override var blacklist: Boolean = false,
    override var reversed: Boolean = false
) : FilteredEdge, OneWayEdge {

    override fun interact(player: ServerPlayer): Boolean {
        reversed = !reversed
        return reversed
    }
}
