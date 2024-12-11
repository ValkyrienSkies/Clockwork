package org.valkyrienskies.clockwork.content.propulsion.singleton.fan

import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Vector3dc
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierCreateData
import org.valkyrienskies.clockwork.content.generic.IForceApplierBE
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD

data class EncasedFanCreateData(override val position: Vector3ic, val fanDir: Vector3dc, val fanSpeed: Double): ForceApplierCreateData<EncasedFanData> {
    override fun fromCreateData(): EncasedFanData {
        return EncasedFanData(position, fanDir, fanSpeed)
    }

    companion object {
        fun fromBlockEntity(be: EncasedFanBlockEntity): EncasedFanCreateData? {
            return EncasedFanCreateData(be.blockPos.toJOML(), be.blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD(), be.getSpeed().toDouble())
        }
    }
}