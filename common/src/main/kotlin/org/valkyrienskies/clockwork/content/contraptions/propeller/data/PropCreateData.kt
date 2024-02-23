package org.valkyrienskies.clockwork.content.contraptions.propeller.data

import org.joml.Vector3dc
import org.joml.Vector3ic

data class PropCreateData(
        val bearingPos: Vector3dc,
        val bearingAxis: Vector3dc,
        val bearingAngle: Double,
        val bearingSpeed: Double,
        val propellorPositions: List<Vector3ic>,
        val inverted: Boolean,
        val overStressed: Boolean
)