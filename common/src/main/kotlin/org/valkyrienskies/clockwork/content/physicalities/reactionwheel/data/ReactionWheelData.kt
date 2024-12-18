package org.valkyrienskies.clockwork.content.physicalities.reactionwheel.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierCreateData
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierData
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierUpdateData

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class ReactionWheelData: ForceApplierData<ReactionWheelUpdateData> {

    override val position: Vector3ic?
    val direction: Vector3ic?
    var currentRPM: Double = 0.0
    var previousRPM: Double = 0.0


    override fun updateData(data: ReactionWheelUpdateData) {
        this.currentRPM = data.currentSpeed
    }

    constructor(position: Vector3ic, direction: Vector3ic, currentSpeed: Double) {
        this.position = position
        this.direction = direction
        this.currentRPM = currentSpeed
    }

    // Default constructor for Jackson, should never be invoked manually
    @Deprecated("")
    constructor() {
        this.position = null
        this.direction = null
    }

    fun pushRPM(rpm: Double) {
        previousRPM = rpm
    }
}