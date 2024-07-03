package org.valkyrienskies.clockwork.kelvin.api.edges

import org.valkyrienskies.clockwork.kelvin.api.GasType

/**
 * Represents a filtered connection in the graph. Filtered connections only allow certain gases to flow through.
 */
interface FilteredEdge {
    /**
     * The current filter set for this connection. Behavior determined by the [blacklist] variable.
     */
    val filter : HashSet<GasType>

    /**
     * Determines whether this connection's filter is a Whitelist (false) or a Blacklist (true).
     *
     * A **Whitelist** means that only the specified gases are allowed to flow through the connection.
     *
     * A **Blacklist** means that all gases are allowed to flow through the connection *except* for the specified gases.
     */
    var blacklist: Boolean

    fun modFilter(newFilter: HashSet<GasType>, isBlacklist: Boolean) {
        this.filter.clear()
        this.filter.addAll(newFilter)
        this.blacklist = isBlacklist
    }
}