package org.valkyrienskies.clockwork.content.contraptions.phys.gas_thruster

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.joml.Vector3dc
import org.joml.Vector3ic

@JsonAutoDetect( fieldVisibility = JsonAutoDetect.Visibility.ANY,)
class GasThrusterData {
    val position: Vector3dc?
    val force: Vector3dc?

    // Default constructor for Jackson, should never be invoked manually
    @Deprecated("")
    constructor() {
        position = null
        force = null
    }

    constructor(
        position: Vector3dc?,
        force: Vector3dc?
    ) {
        this.position = position
        this.force = force

    }
}