package org.valkyrienskies.clockwork.content.physicalities.reactionwheel.data

import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierCreateData
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierData
import org.valkyrienskies.clockwork.content.physicalities.reactionwheel.ReactionWheelBlockEntity
import org.valkyrienskies.mod.common.util.toJOML

data class ReactionWheelCreateData(override val position: Vector3ic, val direction: Vector3ic, val currentSpeed: Double): ForceApplierCreateData<ReactionWheelData> {
    override fun fromCreateData(): ReactionWheelData {
        return ReactionWheelData(this.position, this.direction, this.currentSpeed)
    }

    companion object {
        fun fromBlockEntity(be: ReactionWheelBlockEntity): ReactionWheelCreateData {
            val dir = when (be.blockState.getValue(BlockStateProperties.AXIS)) {
                Direction.Axis.X -> Vector3i(1,0,0)
                Direction.Axis.Y -> Vector3i(0, 1, 0)
                Direction.Axis.Z -> Vector3i(0, 0, 1)
                else -> throw IllegalArgumentException("Invalid axis")
            }
            return ReactionWheelCreateData(be.blockPos.toJOML(), dir, be.realSpeed)
        }
    }
}
