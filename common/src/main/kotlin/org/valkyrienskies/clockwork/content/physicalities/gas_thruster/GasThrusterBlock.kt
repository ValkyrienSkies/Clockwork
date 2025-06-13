package org.valkyrienskies.clockwork.content.physicalities.gas_thruster

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.utility.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.forces.GasThrusterController
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock

import org.valkyrienskies.mod.common.getShipObjectManagingPos


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


    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val preferred: Direction? = getPreferredFacing(context)
        if (preferred == null || (context.player != null && context.player!!
                .isShiftKeyDown)
        ) {
            val nearestLookingDirection = context.nearestLookingDirection
            return defaultBlockState().setValue(
                DirectionalKineticBlock.FACING, if (context.player != null && context.player!!
                        .isShiftKeyDown
                ) nearestLookingDirection.opposite else nearestLookingDirection
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
        nodePlace(state, level, pos, oldState, isMoving)

    }

    override fun getBlockEntityClass(): Class<GasThrusterBlockEntity> {
        return GasThrusterBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GasThrusterBlockEntity> {
        return ClockworkBlockEntities.GAS_THRUSTER.get()
    }
}