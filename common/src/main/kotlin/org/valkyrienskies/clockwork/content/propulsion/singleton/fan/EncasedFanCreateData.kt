package org.valkyrienskies.clockwork.content.propulsion.singleton.fan

import org.joml.Vector3dc
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierCreateData

data class EncasedFanCreateData(override val position: Vector3ic, val fanDir: Vector3dc, val fanSpeed: Double): ForceApplierCreateData<EncasedFanData>