package org.valkyrienskies.clockwork.kelvin.api

import java.util.EnumMap

data class GasNodeCreateData(
    val identifier: GasNodeIdentifier,
    val gasMasses: EnumMap<GasType, Double>,
    val volume: Double,
    val temperature: Double,
)
