package org.valkyrienskies.clockwork.content.physicalities.spinoff_bearing

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkShapes

class SpinoffBearingBlock(properties: Properties) : DirectionalBlock(properties), IBE<SpinoffBearingBlockEntity> {

    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING)
        super.createBlockStateDefinition(builder)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val facing = if (context.isSecondaryUseActive) {
            context.nearestLookingDirection
        } else {
            context.nearestLookingDirection.opposite
        }
        return defaultBlockState().setValue(FACING, facing)
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape? {
        return ClockworkShapes.SPINOFF_BEARING.get(
            state.getValue(FACING)
        )
    }

    override fun getVisualShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return ClockworkShapes.SPINOFF_BEARING.get(
            state.getValue(FACING)
        )
    }

    override fun destroy(level: LevelAccessor, pos: BlockPos, state: BlockState) {
        super.destroy(level, pos, state)
        withBlockEntityDo(level, pos) { be ->
            be.partner?.disconnect()
            be.disconnect()
        }
    }

    override fun getBlockEntityClass(): Class<SpinoffBearingBlockEntity> {
        return SpinoffBearingBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SpinoffBearingBlockEntity> {
        return ClockworkBlockEntities.SPINOFF_BEARING.get()
    }
}
