package org.valkyrienskies.clockwork.content.contraptions.smart_propeller.data

import org.joml.Vector3dc

data class SmartUpdateData(
    val bearingAxis: Vector3dc,
    val rotationAngle: Double,
    val rotationSpeed: Double,
    val inverted: Boolean,
    val overStressed: Boolean)