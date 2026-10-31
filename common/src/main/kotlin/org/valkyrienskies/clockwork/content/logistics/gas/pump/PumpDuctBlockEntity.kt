package org.valkyrienskies.clockwork.content.logistics.gas.pump

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.clockwork.util.kelvin.KNodeKineticBlockEntity
import org.valkyrienskies.kelvin.api.nodes.PumpDuctNode
import kotlin.math.abs

class PumpDuctBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState): KNodeKineticBlockEntity(typeIn, pos, state), IConnectable {

    val pumpPressure: Double get() = (abs(getSpeed()).toDouble() / 256.0) * maxPumpPressure

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {

        super.addBehavioursDeferred(behaviours)
    }

    override fun lazyTick() {
        super.lazyTick()
        updatePumpNode()
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)
        updatePumpNode()
    }

    fun updatePumpNode() {
        val level = level ?: return
        if (level.isClientSide) return
        val node = ClockworkMod.getKelvin(level).getNodeAt(getDuctNodePosition()) as? PumpDuctNode ?: return
        val outlet = blockPos.relative(blockState.getValue(BlockStateProperties.FACING))
        node.pumpTarget = ClockworkUtils.getDuctNodePos(outlet, level)
        node.pumpPressure = pumpPressure
    }


    companion object {
        const val maxPumpPressure: Double = 1023440.0
    }

}
