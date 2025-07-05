package org.valkyrienskies.clockwork.content.logistics.gas.engine

import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerBlock.Companion.POWERED
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock

class GasEngineBlock(properties: Properties) : RotatedPillarBlock(properties), INodeBlock, IBE<GasEngineBlockEntity> {
    override fun getBlockEntityClass(): Class<GasEngineBlockEntity> {
        return GasEngineBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GasEngineBlockEntity> {
        return ClockworkBlockEntities.GAS_ENGINE.get()
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
        nodePlace(state, level, pos, oldState, movedByPiston)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        nodeRemove(state, level, pos, newState, movedByPiston)
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        if (direction.axis != level.getBlockState(self).getValue(AXIS)) return false
        return super.canConnectTo(self, other, direction, level)
    }

    companion object {
        @JvmStatic
        fun updateEngineState(level: Level, pos: BlockPos, isRemoved: Boolean) {
            val blockEntity = level.getBlockEntity(pos) as? GasEngineBlockEntity ?: return
            blockEntity.attachedEngines += if (isRemoved) -1 else 1
            if (blockEntity.attachedEngines < 0) blockEntity.attachedEngines = 0
        }
    }
}