package org.valkyrienskies.clockwork.content.propulsion.sugar_rocket

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.joml.Vector3dc
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierData

@JsonAutoDetect( fieldVisibility = JsonAutoDetect.Visibility.ANY,)
class SugarRocketData {
    var position: Vector3ic?
    var force: Vector3dc?

    // Default constructor for Jackson, should never be invoked manually
    @Deprecated("")
    constructor() {
        position = null
        force = null
    }

    constructor(
        position: Vector3ic?,
        force: Vector3dc?
    ) {
        this.position = position
        this.force = force

    }
}