package org.valkyrienskies.clockwork.content.physicalities.gas_thruster.data

import org.joml.Vector3d
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierCreateData
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.GasThrusterBlockEntity
import org.valkyrienskies.mod.common.util.toJOML

data class GasThrusterCreateData(override val position: Vector3ic): ForceApplierCreateData<GasThrusterData> {
    override fun fromCreateData(): GasThrusterData {
        return GasThrusterData(position, Vector3d())
    }

    companion object {
        fun fromBlockEntity(be: GasThrusterBlockEntity): GasThrusterCreateData? {
            return GasThrusterCreateData(be.blockPos.toJOML())
        }
    }
}
