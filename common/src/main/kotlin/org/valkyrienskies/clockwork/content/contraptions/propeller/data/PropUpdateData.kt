package org.valkyrienskies.clockwork.content.contraptions.propeller.data

import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeData
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierUpdateData

data class PropUpdateData(val rotationSpeed: Double, val rotationAngle: Double, val inverted: Boolean, val active: Boolean, val blades: List<BladeData>): ForceApplierUpdateData