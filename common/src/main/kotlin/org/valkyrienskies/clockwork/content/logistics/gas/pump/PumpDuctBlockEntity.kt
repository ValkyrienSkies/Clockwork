package org.valkyrienskies.clockwork.content.logistics.gas.pump

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.clockwork.util.kelvin.KNodeKineticBlockEntity
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.PumpDuctEdge
import kotlin.math.abs

class PumpDuctBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState): KNodeKineticBlockEntity(typeIn, pos, state), IConnectable {

    val pumpPressure: Double get() = (abs(getSpeed()).toDouble() / 256.0) * maxPumpPressure

    override fun lazyTick() {
        super.lazyTick()

        if (level?.isClientSide != false) return

        val dir = blockState.getValue(BlockStateProperties.FACING)
        updateConnection(level!!, blockPos, dir)
        updateConnection(level!!, blockPos, dir.opposite)
    }



    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {

        super.addBehavioursDeferred(behaviours)
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)

        val front = blockPos.relative(blockState.getValue(BlockStateProperties.FACING))
        val back = blockPos.relative(blockState.getValue(BlockStateProperties.FACING).opposite)
        if (level == null) return
        val backEdge = ClockworkMod.getKelvin(level).getEdgeBetween(getDuctNodePosition(), ClockworkUtils.getDuctNodePos(back, level))
        val frontEdge = ClockworkMod.getKelvin(level).getEdgeBetween(getDuctNodePosition(), ClockworkUtils.getDuctNodePos(front, level))

        (backEdge as? PumpDuctEdge)?.pumpPressure = pumpPressure
        (frontEdge as? PumpDuctEdge)?.pumpPressure = pumpPressure
    }

    override fun getEdge(nodeA: DuctNodePos, nodeB: DuctNodePos, level: Level, blockPos: BlockPos, direction: Direction): DuctEdge {
        val facing = blockState?.getValue(BlockStateProperties.FACING) ?: Direction.UP
        if (direction == facing) return PumpDuctEdge(nodeA, nodeB, target = nodeB, pumpPressure = pumpPressure)
        return PumpDuctEdge(nodeA, nodeB, target = nodeA, pumpPressure = pumpPressure)
    }


    companion object {
        const val maxPumpPressure: Double = 1023440.0
    }

}