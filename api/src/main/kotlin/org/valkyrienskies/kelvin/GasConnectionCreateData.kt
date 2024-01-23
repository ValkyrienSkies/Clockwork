package org.valkyrienskies.kelvin

data class GasConnectionCreateData(
    val to: GasNodeIdentifier,
    val from: GasNodeIdentifier,
    val radius: Double = 0.125,
    var lastTickFlow: Double,
    val pumpPressureDrop: Double? = null,
)
