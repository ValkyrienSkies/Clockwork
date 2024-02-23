package org.valkyrienskies.clockwork.content.contraptions.propeller.data

data class PropUpdateData(val rotationSpeed: Double,
                          val rotationAngle: Double,
                          val inverted: Boolean,
                          val overStressed: Boolean)