package org.valkyrienskies.clockwork.content.physicalities.extendon

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.utility.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock

class ExtendonBlock(properties: Properties) : DirectionalBlock(properties), IBE<ExtendonBlockEntity>, INodeBlock {
    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP))

    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        (level.getBlockEntity(pos) as ExtendonBlockEntity?)?.disconnect()

        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING)
        super.createBlockStateDefinition(builder)
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

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val preferred = getPreferredFacing(context)
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

    override fun getBlockEntityClass(): Class<ExtendonBlockEntity> {
        return ExtendonBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out ExtendonBlockEntity> {
        return ClockworkBlockEntities.EXTENDON.get()
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {

        val be = level.getBlockEntity(pos) as? ExtendonBlockEntity? ?: return super.use(state, level, pos, player, hand, hit)
        if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty) be.disconnect()

        return super.use(state, level, pos, player, hand, hit)
    }
}