package org.valkyrienskies.clockwork.content.contraptions.propeller.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierData

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PropData: ForceApplierData<PropUpdateData> {
    override val position: Vector3ic?
    val bearingAxis: Vector3dc?
    val sailPositions: List<Vector3ic>?
    var bearingAngle = 0.0
    var bearingSpeed = 0.0
    var inverted = false
    var prevAngularMomentum: Vector3dc? = null
    var active: Boolean = false
    var brass: Boolean = false

    override fun updateData(data: PropUpdateData) {
        bearingAngle = data.rotationAngle
        bearingSpeed = data.rotationSpeed
        inverted = data.inverted
        active = data.active
    }

    // Default constructor for Jackson, should never be invoked manually
    @Deprecated("")
    constructor() {
        position = null
        bearingAxis = null
        sailPositions = null
    }

    constructor(
        bearingPos: Vector3ic?,
        bearingAxis: Vector3dc?,
        bearingAngle: Double,
        bearingSpeed: Double,
        sailPositions: List<Vector3ic>?,
        inverted: Boolean,
        active: Boolean,
        brass: Boolean
    ) {
        this.position = bearingPos
        this.bearingAxis = bearingAxis
        this.bearingAngle = bearingAngle
        this.bearingSpeed = bearingSpeed
        this.sailPositions = sailPositions
        this.inverted = inverted
        this.active = active
        this.brass = brass
    }
}