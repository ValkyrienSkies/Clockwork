package org.valkyrienskies.clockwork.content.physicalities.gas_thruster.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.joml.Vector3dc
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierData

@JsonAutoDetect( fieldVisibility = JsonAutoDetect.Visibility.ANY,)
class GasThrusterData: ForceApplierData<GasThrusterUpdateData> {
    override val position: Vector3ic?
    var force: Vector3dc?

    override fun updateData(data: GasThrusterUpdateData) {
        force = data.force
    }

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