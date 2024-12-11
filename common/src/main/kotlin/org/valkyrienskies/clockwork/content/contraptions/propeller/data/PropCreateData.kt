package org.valkyrienskies.clockwork.content.contraptions.propeller.data

import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierCreateData
import org.valkyrienskies.clockwork.content.generic.IForceApplierBE
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD

data class PropCreateData(
    override val position: Vector3ic,
    val bearingAxis: Vector3dc,
    val bearingAngle: Double,
    val bearingSpeed: Double,
    val sailPositions: List<Vector3ic>,
    val inverted: Boolean,
    val active: Boolean,
    val brass: Boolean

) : ForceApplierCreateData<PropData> {
    override fun fromCreateData(): PropData {
        return PropData(
            position,
            bearingAxis,
            bearingAngle,
            bearingSpeed,
            sailPositions,
            inverted,
            active,
            brass
        )
    }

    companion object {
        fun fromBlockEntity(be: PropellerBearingBlockEntity): PropCreateData? {
            return PropCreateData(
                be.blockPos.toJOML(),
                be.blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD(),
                be.angle,
                be.currentOmega,
                be.sailPositions,
                be.isInverted(),
                be.active,
                be.brass
            )
        }
    }
}