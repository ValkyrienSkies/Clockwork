package org.valkyrienskies.clockwork.content.physicalities.gas_thruster.data

import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierUpdateData

data class GasThrusterUpdateData(val force: Vector3dc): ForceApplierUpdateData
