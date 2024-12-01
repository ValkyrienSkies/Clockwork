package org.valkyrienskies.clockwork.content.propulsion.singleton.fan

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierData

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class EncasedFanData:
    ForceApplierData<EncasedFanUpdateData> {

    override val position: Vector3ic?
    val fanDir: Vector3dc?

    var fanSpeed: Double = 0.0

    override fun updateData(data: EncasedFanUpdateData) {
        this.fanSpeed = data.fanSpeed
    }

    // Default constructor for Jackson, should never be invoked manually
    @Deprecated("")
    constructor() {
        this.position = null
        this.fanDir = null
    }

    constructor(position: Vector3ic, fanDir: Vector3dc, fanSpeed: Double) {
        this.position = position
        this.fanDir = fanDir
        this.fanSpeed = fanSpeed
    }
}