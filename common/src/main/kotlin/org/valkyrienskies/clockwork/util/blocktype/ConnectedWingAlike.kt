package org.valkyrienskies.clockwork.util.blocktype

import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.foundation.utility.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkShapes

abstract class ConnectedWingAlike(properties: Properties?) : Block(properties) {
    init {
        registerDefaultState(
            defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, NORTH, SOUTH, EAST, WEST, UP, DOWN)
        super.createBlockStateDefinition(builder)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val preferredFacing = getPreferredDirection(context)
        return if (preferredFacing != null && (context.player == null || !context.player!!
                .isShiftKeyDown)
        ) getNewState(
            defaultBlockState()
                .setValue(FACING, preferredFacing), context.level, context.clickedPos
        ) else getNewState(
            defaultBlockState()
                .setValue(
                    FACING, if (preferredFacing != null && context.player!!
                            .isShiftKeyDown
                    ) context.clickedFace.opposite else context.nearestLookingDirection
                ), context.level, context.clickedPos
        )
    }

    override fun rotate(state: BlockState, rot: Rotation): BlockState {
        return when (rot) {
            Rotation.COUNTERCLOCKWISE_90, Rotation.CLOCKWISE_90 -> when (state.getValue(
                FACING
            )) {
                Direction.NORTH -> state.setValue(FACING, Direction.EAST)
                Direction.EAST -> state.setValue(FACING, Direction.UP)
                Direction.UP -> state.setValue(FACING, Direction.NORTH)
                else -> state
            }

            else -> state
        }
    }

    abstract fun getNewState(state: BlockState?, level: Level?, pos: BlockPos?): BlockState?
    override fun getShape(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pContext: CollisionContext
    ): VoxelShape {
        return ClockworkShapes.WING.get(
            when (pState.getValue<Direction>(FACING)) {
                Direction.EAST, Direction.WEST -> Direction.Axis.X
                Direction.UP, Direction.DOWN -> Direction.Axis.Y
                Direction.NORTH, Direction.SOUTH -> Direction.Axis.Z
            }
        )
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving)
        level.setBlockAndUpdate(pos, getNewState(state, level, pos))
    }

    companion object {
        val FACING = BlockStateProperties.FACING
        val NORTH = BlockStateProperties.NORTH
        val SOUTH = BlockStateProperties.SOUTH
        val EAST = BlockStateProperties.EAST
        val WEST = BlockStateProperties.WEST
        val UP = BlockStateProperties.UP
        val DOWN = BlockStateProperties.DOWN
        fun getPreferredDirection(context: BlockPlaceContext): Direction? {
            var preferredAxis: Direction.Axis? = null
            for (side in Iterate.directions) {
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
                    ) if (preferredAxis != null && preferredAxis !== side.axis) {
                        preferredAxis = null
                        break
                    } else {
                        preferredAxis = side.axis
                    }
                }
            }
            return if (preferredAxis == null) null else when (preferredAxis) {
                Direction.Axis.X -> Direction.EAST
                Direction.Axis.Y -> Direction.UP
                Direction.Axis.Z -> Direction.NORTH
            }
        }
    }
}