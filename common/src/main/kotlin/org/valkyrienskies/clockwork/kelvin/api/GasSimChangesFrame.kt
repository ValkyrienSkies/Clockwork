package org.valkyrienskies.clockwork.kelvin.api

data class GasSimChangesFrame(
    val newNodes: List<GasNodeCreateData>,
    val removedNodes: List<GasNodeIdentifier>,
    val nodeChanges: List<GasNodeChangesData>,
    val newConnections: List<GasConnectionCreateData>,
    val removedConnections: List<Pair<GasNodeIdentifier, GasNodeIdentifier>>,
)
