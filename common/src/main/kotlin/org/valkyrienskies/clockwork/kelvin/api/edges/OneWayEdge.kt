package org.valkyrienskies.clockwork.kelvin.api.edges

/**
 * Represents a one-way edge in the graph. One-way edges only allow gas to flow in one direction.
 */
interface OneWayEdge {

    /**
     * Controls the directionality of the edge. When false, gas can only flow from nodeA to nodeB. When true, gas can only flow from nodeB to nodeA.
     */
    var reversed: Boolean

    fun invert() {
        reversed = !reversed
    }
}