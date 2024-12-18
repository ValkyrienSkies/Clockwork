package org.valkyrienskies.clockwork.content.physicalities.gas_thruster.data

import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierCreateData

data class GasThrusterCreateData(override val position: Vector3ic): ForceApplierCreateData<GasThrusterData>
