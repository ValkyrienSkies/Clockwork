package org.valkyrienskies.kelvin

import java.util.EnumMap

data class GasNodeResultData(
    val gasMasses: EnumMap<GasType, Double>,
    val temperature: Double,
    /**
     * The amount of gas flowing to/from each connection
     *
     * note : how am I supposed to get this data??
     */
    val gasFlows: Map<GasNodeIdentifier, Double>,
)
