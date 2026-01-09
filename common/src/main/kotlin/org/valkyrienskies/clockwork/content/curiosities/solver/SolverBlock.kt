package org.valkyrienskies.clockwork.content.curiosities.solver

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.kelvin.util.INodeBlock

class SolverBlock(properties: Properties) : DirectionalBlock(properties), INodeBlock, IBE<SolverBlockEntity> {
    init {
        registerDefaultState(
            defaultBlockState()
                .setValue(FACING, Direction.UP)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
        super.createBlockStateDefinition(builder)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(FACING, context.nearestLookingDirection.opposite)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun getBlockEntityClass(): Class<SolverBlockEntity> {
        return SolverBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SolverBlockEntity> {
        return ClockworkBlockEntities.SOLVER.get()
    }
}
