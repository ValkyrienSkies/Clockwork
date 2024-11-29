package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.duct.INodeBlock

class GasNozzleBlock(properties: Properties): HorizontalKineticBlock(properties), IBE<GasNozzleBlockEntity>, INodeBlock {
    override fun getBlockEntityClass(): Class<GasNozzleBlockEntity> {
        return GasNozzleBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GasNozzleBlockEntity> {
        return ClockworkBlockEntities.GAS_NOZZLE.get()
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(HORIZONTAL_FACING).clockWise.axis
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        if (face.axis == state.getValue(HORIZONTAL_FACING).clockWise.axis) {
            return true
        }
        return super.hasShaftTowards(world, pos, state, face)
    }
}