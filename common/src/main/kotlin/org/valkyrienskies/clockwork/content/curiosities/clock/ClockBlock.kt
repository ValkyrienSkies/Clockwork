package org.valkyrienskies.clockwork.content.curiosities.clock

import com.simibubi.create.AllShapes
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.utility.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class ClockBlock(properties: Properties) : HorizontalDirectionalBlock(properties), IBE<ClockBlockEntity> {

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(HorizontalKineticBlock.HORIZONTAL_FACING)
        super.createBlockStateDefinition(builder)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        return defaultBlockState()
            .setValue(
                BlockStateProperties.HORIZONTAL_FACING, context.horizontalDirection
                    .opposite
            )
    }

    fun getPreferredHorizontalFacing(context: BlockPlaceContext): Direction? {
        var prefferedSide: Direction? = null
        for (side in Iterate.horizontalDirections) {
            val blockState = context.level
                .getBlockState(
                    context.clickedPos
                        .relative(side)
                )
            if (blockState.block is IRotate) {
                if ((blockState.block as IRotate).hasShaftTowards(
                        context.level, context.clickedPos
                            .relative(side), blockState, side.opposite
                    )
                ) if (prefferedSide != null && prefferedSide.axis !== side.axis) {
                    prefferedSide = null
                    break
                } else {
                    prefferedSide = side
                }
            }
        }
        return prefferedSide
    }

    override fun getBlockEntityClass(): Class<ClockBlockEntity> {
        return ClockBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out ClockBlockEntity> {
        return ClockworkBlockEntities.CLOCK.get()
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return AllShapes.PLACARD.get(state.getValue(BlockStateProperties.HORIZONTAL_FACING))
    }

}