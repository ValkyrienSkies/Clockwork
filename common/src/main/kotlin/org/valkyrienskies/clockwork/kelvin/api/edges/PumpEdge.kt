package org.valkyrienskies.clockwork.kelvin.api.edges

import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos

interface PumpEdge {

    /**
     * Controls the directionality of the edge. When false, gas can only flow from nodeA to nodeB. When true, gas can only flow from nodeB to nodeA.
     */
    var pumpPressure: Double
    var target: DuctNodePos

}