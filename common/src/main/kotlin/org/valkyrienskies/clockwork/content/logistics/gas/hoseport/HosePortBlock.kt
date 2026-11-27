package org.valkyrienskies.clockwork.content.logistics.gas.hoseport

import com.simibubi.create.foundation.block.IBE
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
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctPlacementHelper
import org.valkyrienskies.clockwork.util.ClockworkCopyableBlock
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.util.INodeBlock

class HosePortBlock(properties: Properties) : DirectionalBlock(properties), IBE<HosePortBlockEntity>, INodeBlock, IHaveDuctStats, ClockworkCopyableBlock {
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
        return listOf(
            Component.translatable("vs_clockwork.hose_port.function1").withStyle(ChatFormatting.GRAY).withStyle(
            ChatFormatting.ITALIC),

            Component.translatable("vs_clockwork.hose_port.function2").withStyle(ChatFormatting.GRAY).withStyle(
            ChatFormatting.ITALIC)
        )
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)


        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING)
        super.createBlockStateDefinition(builder)
    }

    fun getPreferredFacing(context: BlockPlaceContext): Direction? {
        return DuctPlacementHelper.getPreferredFacing(context, requireActiveConnector = true)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val preferred = getPreferredFacing(context)
        val facing = DuctPlacementHelper.resolveDirectionalFacing(
            preferredSide = preferred,
            nearestLookingDirection = context.nearestLookingDirection,
            sneaking = context.player?.isShiftKeyDown == true,
            portOnFacingOpposite = true
        )
        return defaultBlockState().setValue(FACING, facing)
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
            if (!player.isCreative) player.addItem(be.getConnectionItem())
            return InteractionResult.SUCCESS
        }

        return super.use(state, level, pos, player, hand, hit)
    }
}
