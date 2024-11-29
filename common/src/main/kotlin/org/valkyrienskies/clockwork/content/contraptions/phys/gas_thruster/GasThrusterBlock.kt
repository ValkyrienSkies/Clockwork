package org.valkyrienskies.clockwork.content.contraptions.phys.gas_thruster

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.utility.Iterate
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
import org.valkyrienskies.clockwork.content.logistics.gas.duct.INodeBlock


class GasThrusterBlock(properties: Properties) : DirectionalBlock(properties), INodeBlock, IBE<GasThrusterBlockEntity> {

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
        val preferred: Direction? = getPreferredFacing(context)
        if (preferred == null || (context.player != null && context.player!!
                .isShiftKeyDown)
        ) {
            val nearestLookingDirection = context.nearestLookingDirection
            return defaultBlockState().setValue(
                DirectionalKineticBlock.FACING, if (context.player != null && context.player!!
                        .isShiftKeyDown
                ) nearestLookingDirection else nearestLookingDirection.opposite
            )
        }
        return defaultBlockState().setValue(DirectionalKineticBlock.FACING, preferred.opposite)
    }

    fun getPreferredFacing(context: BlockPlaceContext): Direction? {
        var prefferedSide: Direction? = null
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

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        _onPlace(state, level, pos, oldState, isMoving)

    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        _onRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun getBlockEntityClass(): Class<GasThrusterBlockEntity> {
        return GasThrusterBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GasThrusterBlockEntity> {
        return ClockworkBlockEntities.GAS_THRUSTER.get()
    }


}