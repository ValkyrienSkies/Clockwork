package org.valkyrienskies.clockwork.content.contraptions.smart_propeller

import org.joml.Vector3dc
import org.joml.Vector3i

data class SmartPropData(
        val bearingPos: Vector3dc,
        val bearingAxis: Vector3dc,
        val rotationSpeed: Double,
        val rotationAngle: Double,
        val sailVectors: MutableList<Vector3i>,
        val inverted: Boolean,
        val overStressed: Boolean
)