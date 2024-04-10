package org.valkyrienskies.clockwork.kelvin.api

import java.util.EnumMap

data class GasNodeResultData(
    val gasMasses: EnumMap<GasType, Double>,
    val temperature: Double,
)
