package org.valkyrienskies.clockwork.content.logistics.gas.hoseport

import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.foundation.block.IBE
import net.createmod.catnip.data.Iterate
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.util.INodeBlock

class HosePortBlock(properties: Properties) : DirectionalBlock(properties), IBE<HosePortBlockEntity>, INodeBlock, IHaveDuctStats {
    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP))
    }

    override fun getBlockEntityClass(): Class<HosePortBlockEntity> {
        return HosePortBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out HosePortBlockEntity?>? {
        return ClockworkBlockEntities.HOSE_PORT.get()
    }

    override fun getInternalVolume(): Double {
        return 0.25
    }

    override fun getAdditionalInfoLines(): List<Component> {
        return listOf(Component.translatable("vs_clockwork.hose_port.function").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC))
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        (level.getBlockEntity(pos) as HosePortBlockEntity?)?.disconnect()

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
                FACING, if (context.player != null && context.player!!
                        .isShiftKeyDown
                ) nearestLookingDirection else nearestLookingDirection.opposite
            )
        }
        return defaultBlockState().setValue(FACING, preferred.opposite)
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {

        if (level.isClientSide) return super.use(state, level, pos, player, hand, hit)

        val be = level.getBlockEntity(pos) as? HosePortBlockEntity? ?: return super.use(state, level, pos, player, hand, hit)
        if (player.getItemInHand(InteractionHand.MAIN_HAND).item !is BlockItem && be.connectedJoint != null) {
            be.disconnect()
            if (!player.isCreative) player.addItem(ClockworkItems.EXTENDON_HOSE.asStack())
            return InteractionResult.SUCCESS
        }

        return super.use(state, level, pos, player, hand, hit)
    }
}