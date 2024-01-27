package org.valkyrienskies.clockwork.kelvin.api

data class GasSimResultFrame(
    val nodesData: Map<GasNodeIdentifier, GasNodeResultData>
)
