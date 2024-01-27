package org.valkyrienskies.clockwork.kelvin.impl

import org.valkyrienskies.clockwork.kelvin.api.GasNodeIdentifier

class GasConnection(
    val from: GasNodeIdentifier,
    val to: GasNodeIdentifier,
    /**
     * Radius of the connection, in meters
     */
    var radius: Double,
    /**
     * The flow rate of gas to through this connection in kg
     */
    var lastTickFlow: Double,
    /**
     * Pressure drops, used for pumps
     */
    var pumpPressureDrop: Double?,
)




