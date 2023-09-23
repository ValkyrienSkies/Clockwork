package org.valkyrienskies.clockwork.content.contraptions.propeller.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.joml.Vector3dc
import org.joml.Vector3ic

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PropData {
    val bearingPos: Vector3dc?
    val bearingAxis: Vector3dc?
    val propellorPositions: List<Vector3ic>?
    var bearingAngle = 0.0
    var bearingSpeed = 0.0
    var inverted = false
    var prevAngularMomentum: Vector3dc? = null

    // Default constructor for Jackson, should never be invoked manually
    @Deprecated("")
    constructor() {
        bearingPos = null
        bearingAxis = null
        propellorPositions = null
    }

    constructor(
        bearingPos: Vector3dc?,
        bearingAxis: Vector3dc?,
        bearingAngle: Double,
        bearingSpeed: Double,
        propellorPositions: List<Vector3ic>?,
        inverted: Boolean
    ) {
        this.bearingPos = bearingPos
        this.bearingAxis = bearingAxis
        this.bearingAngle = bearingAngle
        this.bearingSpeed = bearingSpeed
        this.propellorPositions = propellorPositions
        this.inverted = inverted
    }
}