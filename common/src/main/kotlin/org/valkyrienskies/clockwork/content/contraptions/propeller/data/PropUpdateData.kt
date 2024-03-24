package org.valkyrienskies.clockwork.content.contraptions.propeller.data

import org.joml.Quaterniondc
import org.joml.Vector2dc

data class PropUpdateData(val rotationSpeed: Double,
                          val rotationAngle: Double,
                          val inverted: Boolean,
                          val overStressed: Boolean,
                          val bearingRotation: Quaterniondc)