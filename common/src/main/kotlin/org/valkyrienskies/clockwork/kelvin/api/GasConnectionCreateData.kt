package org.valkyrienskies.clockwork.kelvin.api

data class GasConnectionCreateData(
    val to: GasNodeIdentifier,
    val from: GasNodeIdentifier,
    val radius: Double = 0.125,
    var lastTickFlow: Double,
    val pumpPressureDrop: Double? = null,
)
