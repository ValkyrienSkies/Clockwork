package org.valkyrienskies.clockwork.kelvin.api

import org.joml.Vector3ic

data class GasNodeIdentifier(
    val pos: Vector3ic,
    val id: GasNodeId,
)
