package org.valkyrienskies.clockwork.content.logistics.gas.pump

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.NodeBehaviorType
import org.valkyrienskies.clockwork.kelvin.api.edges.PumpDuctEdge
import org.valkyrienskies.clockwork.kelvin.api.nodes.PumpDuctNode
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.abs

class PumpDuctBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState): KineticBlockEntity(typeIn, pos, state) {



    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {

        super.addBehavioursDeferred(behaviours)
    }


    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)

        val front = blockPos.relative(blockState.getValue(BlockStateProperties.FACING)).toJOMLD()
        val back = blockPos.relative(blockState.getValue(BlockStateProperties.FACING).opposite).toJOMLD()
        val edge = ClockworkMod.getKelvin().getEdgeBetween(front, back)

        if (edge is PumpDuctEdge) {

            edge.pumpPressure  =  (abs(getSpeed()).toDouble() / 256.0) * maxPumpPressure
        }
    }

    companion object {
        const val maxPumpPressure: Double = 1023440.0
    }

}